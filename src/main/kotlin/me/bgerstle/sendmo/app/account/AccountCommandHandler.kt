package me.bgerstle.sendmo.app.account

import me.bgerstle.sendmo.app.kafka.JSONSerde
import me.bgerstle.sendmo.domain.*
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.KeyValue
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.kstream.Consumed
import org.apache.kafka.streams.kstream.KStream
import org.apache.kafka.streams.kstream.Produced
import org.apache.kafka.streams.kstream.Transformer
import org.apache.kafka.streams.processor.ProcessorContext
import org.apache.kafka.streams.state.KeyValueStore
import org.apache.kafka.streams.state.StoreBuilder
import org.apache.kafka.streams.state.Stores

typealias AccountAggregateKeyValueStore = KeyValueStore<AccountID, AccountAggregate>

typealias TransformResult = Triple<AccountReply, AccountAggregate?, AccountEvent?>
typealias KeyAndResult = KeyValue<AccountID, TransformResult>

data class AccountAlreadyExists(val accountID: AccountID) : Throwable("Account $accountID already exists.") {}

class AccountCommandHandler : Transformer<AccountID, AccountCommand<*>, KeyAndResult> {
    lateinit var accountAggregates: AccountAggregateKeyValueStore

    override fun init(context: ProcessorContext) {
        accountAggregates = context.getStateStore<AccountAggregateKeyValueStore>(ACCOUNT_AGGREGATE_STORE_NAME)
    }

    override fun transform(key: AccountID, value: AccountCommand<*>): KeyAndResult {
        val result: TransformResult = when (value) {
            is OpenAccount -> handle(key, value)
        }
        return KeyValue(key, result)
    }

    fun handle(key: AccountID, openAccount: OpenAccount): Triple<OpenAccount.Reply, AccountAggregate?, AccountOpened?> {
        val account = accountAggregates[key]
        if (account == null) {
            val event = AccountOpened(accountID = openAccount.accountID, currency = openAccount.currency)
            val account = AccountAggregate(event)

            accountAggregates.put(key, account)

            return Triple(OpenAccount.Success(event), account, event)
        } else {
            return Triple(OpenAccount.Failure(AccountAlreadyExists(openAccount.accountID)), null, null)
        }
    }

    companion object {
        val ACCOUNT_AGGREGATE_STORE_NAME = "Accounts"

        val accountAggregateStoreBuilder: StoreBuilder<AccountAggregateKeyValueStore>
            get() =
                Stores.keyValueStoreBuilder(
                    Stores.persistentKeyValueStore(ACCOUNT_AGGREGATE_STORE_NAME),
                    Serdes.UUID(),
                    JSONSerde<AccountAggregate>()
                )
    }

    override fun close() { }
}

val StreamsBuilder.accountCommands: KStream<AccountID, AccountCommand<*>>
    get() =
        stream(
            KafkaAccountService.Topics.ACCOUNT_COMMANDS,
            Consumed.with(
                Serdes.UUID(),
                JSONSerde<AccountCommand<*>>()
            )
        )

fun KStream<AccountID, AccountReply>.toAccountReplyStream() =
    to(
        KafkaAccountService.Topics.ACCOUNT_COMMAND_REPLIES,
        Produced.with(
            Serdes.UUID(),
            JSONSerde<AccountReply>()
        )
    )

fun KStream<AccountID, AccountAggregate>.toAccountStream() =
    to(
        KafkaAccountService.Topics.ACCOUNTS,
        Produced.with(
            Serdes.UUID(),
            JSONSerde<AccountAggregate>()
        )
    )

fun KStream<AccountID, AccountEvent>.toAccountEventStream() =
    to(
        KafkaAccountService.Topics.ACCOUNT_EVENTS,
        Produced.with(
            Serdes.UUID(),
            JSONSerde<AccountEvent>()
        )
    )

