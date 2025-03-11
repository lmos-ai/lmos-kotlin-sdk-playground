/*
 * SPDX-FileCopyrightText: Robert Winkler
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package ai.ancf.lmos.wot.integration


import io.opentelemetry.instrumentation.annotations.WithSpan
import org.eclipse.lmos.arc.agents.AgentProvider
import org.eclipse.lmos.arc.agents.getAgentByName
import org.eclipse.lmos.sdk.agents.ConversationalAgent
import org.eclipse.lmos.sdk.model.AgentRequest
import org.eclipse.lmos.sdk.model.AgentResult
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class ArcConversationalAgent(agentProvider: AgentProvider) : ConversationalAgent {

    private val agent = agentProvider.getAgentByName("ChatAgent") as org.eclipse.lmos.arc.agents.ChatAgent

    private val log : Logger = LoggerFactory.getLogger(ArcConversationalAgent::class.java)

    @WithSpan
    override suspend fun chat(message: AgentRequest): AgentResult {
        return executeAgent(message, agent::execute)
    }
}