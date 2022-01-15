package me.bgerstle.sendmo.app.kafka

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConfigurationProperties("kafka")
@ConstructorBinding
data class KafkaConfig(
    val bootstrapServers: String,
    val embeddedStreamsAppEnabled: Boolean = false
)
