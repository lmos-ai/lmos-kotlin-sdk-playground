/*
 * SPDX-FileCopyrightText: Robert Winkler
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package ai.ancf.lmos.wot.integration


import com.fasterxml.jackson.databind.node.IntNode
import com.fasterxml.jackson.databind.node.NullNode
import kotlinx.coroutines.test.runTest
import org.eclipse.thingweb.Servient
import org.eclipse.thingweb.Wot
import org.eclipse.thingweb.binding.http.HttpProtocolClientFactory
import org.eclipse.thingweb.binding.http.HttpProtocolServer
import org.eclipse.thingweb.binding.mqtt.MqttClientConfig
import org.eclipse.thingweb.binding.mqtt.MqttProtocolClientFactory
import org.eclipse.thingweb.binding.mqtt.MqttProtocolServer
import org.eclipse.thingweb.thing.schema.*
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.testcontainers.containers.GenericContainer
import org.testcontainers.utility.DockerImageName
import kotlin.test.Test
import kotlin.test.assertEquals

private const val PROPERTY_NAME = "property1"
private const val PROPERTY_NAME_2 = "property2"

private const val ACTION_NAME = "action1"

private const val ACTION_NAME_2 = "action2"

private const val ACTION_NAME_3 = "action3"

private const val ACTION_NAME_4 = "action4"

private const val EVENT_NAME = "event1"

class WoTIntegrationTest {

    companion object {
        private lateinit var hiveMqContainer: GenericContainer<*>

        @BeforeAll
        @JvmStatic
        fun setUp() = runTest {
            hiveMqContainer = GenericContainer(DockerImageName.parse("hivemq/hivemq-ce:latest"))
                .withExposedPorts(1883)
            hiveMqContainer.start()
        }

        @AfterAll
        @JvmStatic
        fun tearDown() {
            hiveMqContainer.stop()
        }
    }

    private fun createServient(protocol: String): Pair<Servient, String> {
        return when (protocol) {
            "HTTP" -> {
                val server = HttpProtocolServer()
                val clientFactory = HttpProtocolClientFactory()
                val servient = Servient(servers = listOf(server), clientFactories = listOf(clientFactory))
                servient to "http://localhost:8080"
            }
            "MQTT" -> {
                val clientConfig = MqttClientConfig(hiveMqContainer.host, hiveMqContainer.getMappedPort(1883), "client")
                val serverConfig = MqttClientConfig(hiveMqContainer.host, hiveMqContainer.getMappedPort(1883), "server")
                val server = MqttProtocolServer(serverConfig)
                val clientFactory = MqttProtocolClientFactory(clientConfig)
                val servient = Servient(servers = listOf(server), clientFactories = listOf(clientFactory))
                servient to "mqtt://${hiveMqContainer.host}:${hiveMqContainer.getMappedPort(1883)}"
            }
            else -> throw IllegalArgumentException("Unsupported protocol: $protocol")
        }
    }

    private fun createExposedThing(wot: Wot): WoTExposedThing {
        return wot.produce {
            id = "myid"
            title = "MyThing"
            intProperty(PROPERTY_NAME) { observable = true }
            intProperty(PROPERTY_NAME_2) { observable = true }
            action<String, String>(ACTION_NAME) {
                title = ACTION_NAME
                input = stringSchema {
                    title = "Action Input"
                    minLength = 10
                    default = "test"
                }
                output = StringSchema()
            }
            action<String, String>(ACTION_NAME_2) { title = ACTION_NAME_2; output = StringSchema() }
            action<String, String>(ACTION_NAME_3) { title = ACTION_NAME_3; input = StringSchema() }
            action<String, String>(ACTION_NAME_4) { title = ACTION_NAME_4 }
            event<String, Nothing, Nothing>(EVENT_NAME) { data = StringSchema() }
        }.apply {
            setPropertyReadHandler(PROPERTY_NAME) { 10.toInteractionInputValue() }
            setPropertyReadHandler(PROPERTY_NAME_2) { 5.toInteractionInputValue() }
            setPropertyWriteHandler(PROPERTY_NAME) { input, _ ->
                val inputInt = input.value()
                inputInt.asInt().toInteractionInputValue()
            }
            setActionHandler(ACTION_NAME) { input, _ ->
                val inputString = input.value()
                "${inputString.asText()} 10".toInteractionInputValue()
            }
            setActionHandler(ACTION_NAME_2) { _, _ -> "10".toInteractionInputValue() }
            setActionHandler(ACTION_NAME_3) { _, _ -> InteractionInput.Value(NullNode.instance) }
            setActionHandler(ACTION_NAME_4) { _, _ -> InteractionInput.Value(NullNode.instance) }
            setPropertyObserveHandler(PROPERTY_NAME) { 10.toInteractionInputValue() }
        }
    }

    private suspend fun validateExposedThing(
        wot: Wot,
        exposedThing: WoTExposedThing,
        baseUrl: String
    )  {
        val thingDescription = wot.requestThingDescription("$baseUrl/myid")
        val consumedThing = wot.consume(thingDescription)

        val readProperty = consumedThing.readProperty(PROPERTY_NAME)
        val propertyResponse = readProperty.value()
        assertEquals(10, propertyResponse.asInt())

        consumedThing.writeProperty(PROPERTY_NAME, 20.toInteractionInputValue())

        val output = consumedThing.invokeAction(ACTION_NAME, "actionInput".toInteractionInputValue(), null)
        val actionResponse = output.value()
        assertEquals("actionInput 10", actionResponse.asText())

        val responseMap = consumedThing.readAllProperties()
        assertEquals(2, responseMap.size)
        assertEquals(IntNode(10), responseMap[PROPERTY_NAME]?.value())
        assertEquals(IntNode(5), responseMap[PROPERTY_NAME_2]?.value())

        consumedThing.observeProperty(PROPERTY_NAME, listener = { println("Property observed: $it") })
        exposedThing.emitPropertyChange(PROPERTY_NAME, 30.toInteractionInputValue())
    }

    @Test
    fun `Should fetch thing with HTTP and MQTT`() = runTest {
        listOf("HTTP", "MQTT").forEach { protocol ->
            val (servient, baseUrl) = createServient(protocol)
            val wot = Wot.create(servient)
            val exposedThing = createExposedThing(wot)

            servient.start()
            servient.addThing(exposedThing)
            servient.expose("myid")

            validateExposedThing(wot, exposedThing, baseUrl)
        }
    }
}