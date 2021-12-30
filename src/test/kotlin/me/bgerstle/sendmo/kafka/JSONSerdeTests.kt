package me.bgerstle.sendmo.kafka

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import me.bgerstle.sendmo.app.kafka.JSONSerde
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.TestInputTopic
import org.apache.kafka.streams.TestOutputTopic


class JSONSerdeTests : FunSpec({
    val inputTopicName = "jsonserde-test-in"
    val outputTopicName = "jsonserde-test-out"
    val context = Context()
    val testObjSerde = JSONSerde<TestObject>(TestObject::class.java)

    extension(KafkaStreamsTestListener(
        builderConfig = {
            stream<Int, TestObject>(inputTopicName).to(outputTopicName)
        },
        driverConfig = {
            context.inputTopic =
                createInputTopic(
                    inputTopicName,
                    Serdes.Integer().serializer(),
                    testObjSerde
                )
            context.outputTopic =
                createOutputTopic(
                    outputTopicName,
                    Serdes.Integer().deserializer(),
                    testObjSerde
                )
        }))

    test("successfully serializes and deserializes the test object") {
        val expected = TestObject("foo", "bar")
        context.inputTopic.pipeInput(expected)
        val actual = context.outputTopic.readValue()
        actual shouldBe expected
    }
}) {
    data class TestObject(val foo: String, val bar: String)

    class Context {
        lateinit var inputTopic: TestInputTopic<Int, TestObject>
        lateinit var outputTopic: TestOutputTopic<Int, TestObject>
    }
}
