package me.bgerstle.sendmo.kafka

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import me.bgerstle.sendmo.app.kafka.JSONSerde
import me.bgerstle.sendmo.domain.*
import nl.hiddewieringa.money.asCurrency
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.TestInputTopic
import org.apache.kafka.streams.TestOutputTopic
import org.apache.kafka.streams.kstream.Consumed
import org.apache.kafka.streams.kstream.Produced
import java.util.*

class AccountCommandSerdeTests : FunSpec({
    val inputTopicName = "account-command-serde-test-in"
    val outputTopicName = "account-command-serde-test-out"
    val context = Context()

    extension(KafkaStreamsTestListener(
    builderConfig = {
        stream(
            inputTopicName,
            Consumed.with(Serdes.UUID(), JSONSerde<AccountCommand<*>>())
        )
            .mapValues<AccountReply> { value ->
                when(value) {
                    is OpenAccount -> OpenAccount.Success(AccountOpened(value.accountID, value.currency))
                }
            }
            .to(outputTopicName, Produced.with(Serdes.UUID(), JSONSerde<AccountReply>()))
    },
    driverConfig = {
        context.inputTopic =
            createInputTopic(
                inputTopicName,
                Serdes.UUID().serializer(),
                JSONSerde<AccountCommand<*>>().serializer()
            )
        context.outputTopic =
            createOutputTopic(
                outputTopicName,
                Serdes.UUID().deserializer(),
                JSONSerde<AccountReply>().deserializer()
            )
    }))

    test("successfully encodes a command and decodes its reply") {
        val accountID = UUID.randomUUID()
        val currency = "USD".asCurrency()

        context.inputTopic.pipeInput(accountID, OpenAccount(accountID, currency))
        val output = context.outputTopic.readValue()

        output shouldBe OpenAccount.Success(AccountOpened(accountID, currency))
    }
}) {
    class Context {
        lateinit var inputTopic: TestInputTopic<AccountID, AccountCommand<*>>
        lateinit var outputTopic: TestOutputTopic<AccountID, AccountReply>
    }
}
