package me.bgerstle.sendmo.app

import me.bgerstle.sendmo.app.account.*
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.kstream.KStream
import java.util.*

fun <K, V: Any> KStream<K, V?>.filterNotNull(): KStream<K, V> =
    filter { _, v -> v != null }.mapValues { v -> v!! }

object SendmoStreamsApp {
    private val topology =
        StreamsBuilder().apply {
            addStateStore(AccountCommandHandler.accountAggregateStoreBuilder)

            val results = accountCommands.transform({ AccountCommandHandler() }, AccountCommandHandler.ACCOUNT_AGGREGATE_STORE_NAME)

            results
                .mapValues { result -> result.first }
                .toAccountReplyStream()

            results
                .mapValues { result -> result.second }
                .filterNotNull()
                .toAccountStream()

            results
                .mapValues { result -> result.third }
                .filterNotNull()
                .toAccountEventStream()
        }.build()

    val props =
        Properties().apply {
            ClassLoader
                .getSystemResourceAsStream("kafka.properties")
                .use(::load)
        }

    fun run() {
        val streams = KafkaStreams(topology, props)

        // FIXME: not in prod!
        streams.cleanUp()

        streams.start()
        streams.stopOnShutdown()
    }
}

fun main(args: Array<String>) {
    SendmoStreamsApp.run()
}

fun KafkaStreams.stopOnShutdown() {
    Thread.currentThread().setUncaughtExceptionHandler { _, _ -> close() }
    Runtime.getRuntime().addShutdownHook(Thread() {
        try {
            close()
        } catch (ignored: Exception) {
            println("Failed to stop streams: $ignored")
        }
    })
}
