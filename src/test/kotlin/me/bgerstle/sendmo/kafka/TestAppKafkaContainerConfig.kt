package me.bgerstle.sendmo.kafka

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.test.context.support.TestPropertySourceUtils
import org.testcontainers.containers.KafkaContainer
import org.testcontainers.utility.DockerImageName

@TestConfiguration
class TestAppKafkaContainerConfig internal constructor() :
    ApplicationContextInitializer<ConfigurableApplicationContext> {
    private val kafkaContainer: KafkaContainer

    init {
        kafkaContainer = KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.0.0"))
        kafkaContainer.start()
    }

    @Bean
    fun kafka(): KafkaContainer {
        return kafkaContainer
    }

    override fun initialize(configurableApplicationContext: ConfigurableApplicationContext) {
        TestPropertySourceUtils.addInlinedPropertiesToEnvironment(
            configurableApplicationContext,
            "kafka.bootstrap-servers=" + kafkaContainer.getBootstrapServers()
        )
    }
}
