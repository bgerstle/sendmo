package me.bgerstle.sendmo.app.kafka

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.JavaType
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.apache.kafka.common.header.Headers
import org.apache.kafka.common.serialization.Deserializer
import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.common.serialization.Serializer
import org.zalando.jackson.datatype.money.MoneyModule

class JSONSerde<T>(
    val clazz: Class<T>,
    val objectMapper: ObjectMapper
) : Serde<T> {
    override fun serializer(): Serializer<T> = JSONSerializer<T>(objectMapper)

    override fun deserializer(): Deserializer<T> = JSONDeserializer<T>(clazz, objectMapper)

    class JSONSerializer<T>(val objectMapper: ObjectMapper) : Serializer<T> {
        override fun serialize(topic: String?, data: T): ByteArray = serialize(topic, null, data)

        override fun serialize(topic: String?, headers: Headers?, data: T): ByteArray =
            objectMapper.writeValueAsBytes(data)

        override fun close() {}
        override fun configure(configs: MutableMap<String, *>?, isKey: Boolean) {}
    }

    class JSONDeserializer<T>(val clazz: Class<T>,
                              val objectMapper: ObjectMapper) : Deserializer<T> {
        override fun deserialize(topic: String?, data: ByteArray): T = deserialize(topic, null, data)

        override fun deserialize(topic: String?, headers: Headers?, data: ByteArray): T =
            objectMapper.readValue(data, objectMapper.constructType(clazz))

        override fun close() {}
        override fun configure(configs: MutableMap<String, *>?, isKey: Boolean) {}
    }

    override fun close() {}
    override fun configure(configs: MutableMap<String, *>?, isKey: Boolean) {}

    companion object {
        val defaultObjectMapper = ObjectMapper().apply {
            registerModule(MoneyModule())
            registerKotlinModule()
            registerModule(Jdk8Module())
        }
    }
}

inline fun <reified T> JSONSerde(objectMapper: ObjectMapper = JSONSerde.defaultObjectMapper) = JSONSerde<T>(T::class.java, objectMapper)

