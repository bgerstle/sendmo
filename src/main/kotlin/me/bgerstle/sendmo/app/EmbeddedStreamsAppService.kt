package me.bgerstle.sendmo.app

import me.bgerstle.sendmo.app.kafka.KafkaConfig
import org.apache.kafka.streams.KafkaStreams
import org.springframework.context.event.ContextClosedEvent
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service

@Service
class EmbeddedStreamsAppService(val kafkaConfig: KafkaConfig) {
    private fun ifEnabled(action: () -> Unit) {
        if (kafkaConfig.embeddedStreamsAppEnabled) {
            action()
        }
    }
    @EventListener
    fun on(event: ContextRefreshedEvent) {
        ifEnabled {
            if (SendmoStreamsApp.streams.state() == KafkaStreams.State.CREATED) {
                SendmoStreamsApp.start()
            }
        }
    }

    @EventListener
    fun on(event: ContextClosedEvent) {
        ifEnabled {
            if (SendmoStreamsApp.streams.state() != KafkaStreams.State.CREATED) {
                SendmoStreamsApp.streams.close()
            }
        }
    }
}
