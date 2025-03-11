/*
 * SPDX-FileCopyrightText: Robert Winkler
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package ai.ancf.lmos.wot.integration


import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.runBlocking
import org.eclipse.lmos.sdk.agents.WotConversationalAgent
import org.eclipse.lmos.sdk.agents.lastMessage
import org.eclipse.lmos.sdk.agents.toAgentRequest
import org.eclipse.lmos.sdk.model.AgentEvent
import java.util.concurrent.CountDownLatch
import kotlin.test.Test

class QuickTest {

    @Test
    fun `should control my lamp`() = runBlocking {
        //val latch = CountDownLatch(3)

        val agent = WotConversationalAgent.create("http://localhost:9080/chatagent")
        /*
        agent.consumeEvent("agentEvent") {
            println("Event: $it")
            latch.countDown()
        }
        */
        //val command = "What is the state of my lamp?"
        val command = "Turn all lamps on"
        println("User: $command")
        val answer = agent.chat(command.toAgentRequest())
        println("Agent: $answer.lastMessage()")
        //latch.await()
    }

    @Test
    fun `scrape a URL`() = runBlocking {
        val latch = CountDownLatch(3)

        val agent = WotConversationalAgent.create("http://localhost:8181/scraper")

        /*
        agent.consumeEvent("agentEvent", AgentEvent::class) {
            println("Event: $it")
            latch.countDown()
        }
        */

        agent.consumeEvent("agentEvent", AgentEvent::class).onEach {
            println("Event: $it")
            latch.countDown()
        }.launchIn(CoroutineScope(Dispatchers.IO))


        //val command = "What is the state of my lamp?"
        val command = "Scrape the page https://eclipse.dev/lmos/\""
        println("User: $command")
        val answer = agent.chat(command.toAgentRequest())
        println("Agent: ${answer.lastMessage()}")
        latch.await()
    }
}