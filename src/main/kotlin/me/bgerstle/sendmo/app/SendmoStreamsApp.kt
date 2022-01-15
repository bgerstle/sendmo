package me.bgerstle.sendmo.app

import me.bgerstle.sendmo.app.account.*
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.StreamsConfig
import org.apache.kafka.streams.kstream.KStream
import org.springframework.kafka.config.KafkaStreamsConfiguration
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

    private val props =
        Properties().apply {
            ClassLoader
                .getSystemResourceAsStream("kafka.properties")
                .use(::load)
        }

    lateinit var streams: KafkaStreams

    val didStart: Boolean
        get() = ::streams.isInitialized

    fun start(overrideBootstrapServers: String? = null) {
        overrideBootstrapServers?.let {
            props.set(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, it)
        }
        streams = KafkaStreams(topology, props)

        // FIXME: not in prod!
        streams.cleanUp()

        streams.start()
        streams.stopOnShutdown()
    }
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

fun main(args: Array<String>) {
    SendmoStreamsApp.start()
}
