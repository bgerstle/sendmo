package me.bgerstle.sendmo.kafka

import io.kotest.core.extensions.Extension
import io.kotest.core.listeners.AfterContainerListener
import io.kotest.core.listeners.AfterTestListener
import io.kotest.core.listeners.BeforeContainerListener
import io.kotest.core.listeners.BeforeTestListener
import io.kotest.core.test.TestCase
import io.kotest.core.test.TestResult
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.StreamsConfig.*
import org.apache.kafka.streams.TopologyTestDriver
import java.util.*

class KafkaStreamsTestListener(
    val builderConfig: StreamsBuilder.() -> Unit,
    val driverConfig: TopologyTestDriver.() -> Unit,
    val props: Properties = defaultProperties()
) : BeforeTestListener, AfterTestListener, Extension {
    lateinit private var testDriver: TopologyTestDriver

    override suspend fun beforeTest(testCase: TestCase) {
        super.beforeTest(testCase)

        testDriver = TopologyTestDriver(
            StreamsBuilder().apply(builderConfig).build(),
            props
        ).apply(driverConfig)
    }

    override suspend fun afterTest(testCase: TestCase, result: TestResult) {
        super.afterTest(testCase, result)

        testDriver.close()
    }

    companion object {
        fun defaultProperties(): Properties =
            Properties().apply {
                // Give the Streams application a unique name.  The name must be unique in the Kafka cluster
                // against which the application is run.
                put(APPLICATION_ID_CONFIG, "sendmo-test")
                put(CLIENT_ID_CONFIG, "sendmo-test-client")
                // Where to find Kafka broker(s).
                // Where to find Kafka broker(s).
                put(BOOTSTRAP_SERVERS_CONFIG, "localhost:9092")
                // Specify default (de)serializers for record keys and for record values.
                // Specify default (de)serializers for record keys and for record values.
                put(
                    DEFAULT_KEY_SERDE_CLASS_CONFIG,
                    Serdes.String().javaClass.name
                )
                put(
                    DEFAULT_VALUE_SERDE_CLASS_CONFIG,
                    Serdes.String().javaClass.name
                )
                // Records should be flushed every 10 seconds. This is less than the default
                // in order to keep this example interactive.
                // Records should be flushed every 10 seconds. This is less than the default
                // in order to keep this example interactive.
                put(COMMIT_INTERVAL_MS_CONFIG, 10 * 1000)
                // For illustrative purposes we disable record caches.
                // For illustrative purposes we disable record caches.
                put(CACHE_MAX_BYTES_BUFFERING_CONFIG, 0)
                // Use a temporary directory for storing state, which will be automatically removed after the test.
                // Use a temporary directory for storing state, which will be automatically removed after the test.
                put(STATE_DIR_CONFIG, "/tmp/sendmo-kafka")
            }
    }
}
