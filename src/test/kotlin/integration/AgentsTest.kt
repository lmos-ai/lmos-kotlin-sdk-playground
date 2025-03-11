/*
 * SPDX-FileCopyrightText: Robert Winkler
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package ai.ancf.lmos.wot.integration


import kotlinx.coroutines.test.runTest
import org.eclipse.thingweb.JsonMapper
import org.eclipse.thingweb.Servient
import org.eclipse.thingweb.Wot
import org.eclipse.thingweb.binding.http.HttpProtocolClientFactory
import org.eclipse.thingweb.binding.mqtt.MqttClientConfig
import org.eclipse.thingweb.binding.mqtt.MqttProtocolClientFactory
import org.eclipse.thingweb.thing.schema.toInteractionInputValue
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.testcontainers.containers.GenericContainer
import org.testcontainers.utility.DockerImageName
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

class AgentsTest {

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

    private lateinit var servient: Servient

    @BeforeTest
    fun setup() = runTest {
        val mqttConfig = MqttClientConfig("localhost", 54884, "wotClient")
        servient = Servient(
            clientFactories = listOf(
                HttpProtocolClientFactory(),
                MqttProtocolClientFactory(mqttConfig)
            )
        )
        servient.start() // Start the Servient before each test
    }

    @AfterTest
    fun teardown() = runTest {
        servient.shutdown() // Ensure the Servient is stopped after each test
    }

    @Test
    fun `Should talk to mqtt and http agent`() = runTest {
        val wot = Wot.create(servient)

        // Test HTTP agent
        val httpAgentTD = wot.requestThingDescription("http://localhost:8080/agent")
        val httpAgent = wot.consume(httpAgentTD)
        var output = httpAgent.invokeAction("ask", "What is Paris?".toInteractionInputValue())
        println(output.value())

        // Test MQTT agent
        val mqttAgentTD = wot.requestThingDescription("mqtt://localhost:54884/agent")
        println(JsonMapper.instance.writeValueAsString(mqttAgentTD))

        val mqttAgent = wot.consume(mqttAgentTD)
        output = mqttAgent.invokeAction("ask", "What is London?".toInteractionInputValue())
        println(output.value())
    }
}
