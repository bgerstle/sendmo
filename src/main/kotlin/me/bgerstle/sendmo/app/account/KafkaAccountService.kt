package me.bgerstle.sendmo.app.account

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.reactor.asFlux
import me.bgerstle.sendmo.app.account.KafkaAccountService.Topics.ACCOUNTS
import me.bgerstle.sendmo.app.account.KafkaAccountService.Topics.ACCOUNT_COMMANDS
import me.bgerstle.sendmo.app.account.KafkaAccountService.Topics.ACCOUNT_COMMAND_REPLIES
import me.bgerstle.sendmo.app.kafka.JSONSerde
import me.bgerstle.sendmo.app.kafka.KafkaConfig
import me.bgerstle.sendmo.domain.*
import org.apache.kafka.clients.admin.AdminClient
import org.apache.kafka.clients.admin.AdminClientConfig
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.serialization.Serdes
import org.springframework.stereotype.Service
import reactor.core.Disposable
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.publisher.SignalType
import reactor.kafka.receiver.KafkaReceiver
import reactor.kafka.receiver.ReceiverOptions
import reactor.kafka.sender.KafkaSender
import reactor.kafka.sender.SenderOptions
import reactor.kafka.sender.SenderRecord
import reactor.kotlin.core.publisher.toFlux
import java.util.*

typealias AccountsMap = Map<AccountID, Account>

@Service
class KafkaAccountService(
    private val config: KafkaConfig
) : ReactiveAccountService {
    private val commandOutput: KafkaSender<UUID, OpenAccount> = KafkaSender.create(
        SenderOptions
            .create<UUID, OpenAccount>()
            .producerProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, config.bootstrapServers)
            .producerProperty(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true)
            .withKeySerializer(Serdes.UUID().serializer())
            .withValueSerializer(JSONSerde<OpenAccount>().serializer())
    )

    private val accountsFlow = MutableStateFlow<AccountsMap>(emptyMap())

    private val accountsFlowDisposable: Disposable

    init {
        accountsFlowDisposable = createAccountsReceiver()
            .receive()
            .subscribe { record ->
                record.value()?.let { account ->
                    accountsFlow.update { accountsMap ->
                        accountsMap + (account.accountID to account as Account)
                    }
                }
            }
    }

    // FIXME: re-emits intermediate results each time
    // FIXME: will break if repartitioned
    override fun accounts(): Flux<Collection<Account>> = accountsFlow.map { it.values }.asFlux()


    override fun <R : AccountReply> enqueue(command: AccountCommand<R>): Mono<R> {
        when (command) {
            is OpenAccount -> {
                val record = ProducerRecord<UUID, OpenAccount>(
                    ACCOUNT_COMMANDS,
                    command.accountID,
                    command
                )
                val requestID = UUID.randomUUID()
                record.headers().add("RequestID", requestID.toString().encodeToByteArray())
                return commandOutput
                    .send(Mono.just(SenderRecord.create(record, requestID)))
                    .next()
                    .flatMap { result ->
                        result.exception()?.let { exception ->
                            return@flatMap Mono.error(exception)
                        }
                        result.correlationMetadata()?.let { requestID ->
                            return@flatMap consumeReply(requestID, TopicPartition(
                                ACCOUNT_COMMAND_REPLIES, result.recordMetadata().partition())
                            )
                        }
                    }
            }
        }
    }

    fun <R: Any> consumeReply(requestID: UUID, partition: TopicPartition): Mono<R> {
        return replyReceiverForPartition(partition)
            .receive()
            .filter { record ->
                val requestIDHeader = record.headers().lastHeader("RequestID")
                val recordRequestID = requestIDHeader?.value()?.let { String(it) }.let(UUID::fromString)
                recordRequestID == requestID
            }
            .next()
            .map { record ->
                // is it possible to "ack" this message so no other consumers see it?
                // maybe write a tombstone message to the topic and make it compacted?
                record.value() as R
            }
    }

    fun replyReceiverForPartition(partition: TopicPartition): KafkaReceiver<UUID, AccountReply> {
        return ReceiverOptions.create<UUID, AccountReply>()
            .withKeyDeserializer(Serdes.UUID().deserializer())
            .withValueDeserializer(JSONSerde<AccountReply>().deserializer())
            .consumerProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, config.bootstrapServers)
            .consumerProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
            .consumerProperty(ConsumerConfig.ISOLATION_LEVEL_CONFIG, "read_committed")
            .assignment(listOf(partition))
            .let { KafkaReceiver.create(it) }
    }

    fun createAccountsReceiver(): KafkaReceiver<UUID, AccountAggregate> =
        ReceiverOptions.create<UUID, AccountAggregate>()
            .withKeyDeserializer(Serdes.UUID().deserializer())
            .withValueDeserializer(JSONSerde<AccountAggregate>().deserializer())
            .consumerProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, config.bootstrapServers)
            .consumerProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
            .consumerProperty(ConsumerConfig.ISOLATION_LEVEL_CONFIG, "read_committed")
            .consumerProperty(ConsumerConfig.GROUP_ID_CONFIG, "in-memory-accounts-${UUID.randomUUID()}")
            .subscription(listOf(ACCOUNTS))
            .let { KafkaReceiver.create(it) }

    object Topics {
        val ACCOUNT_COMMANDS = "account-commands"
        val ACCOUNT_EVENTS = "account-events"
        val ACCOUNTS = "accounts"
        val ACCOUNT_COMMAND_REPLIES = "account-commands-replies"
    }
}

