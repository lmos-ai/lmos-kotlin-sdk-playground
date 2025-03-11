/*
 * SPDX-FileCopyrightText: Robert Winkler
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package ai.ancf.lmos.wot.integration


import kotlinx.coroutines.test.runTest
import org.eclipse.thingweb.Servient
import org.eclipse.thingweb.Wot
import org.eclipse.thingweb.binding.http.HttpProtocolClientFactory
import org.eclipse.thingweb.binding.http.HttpsProtocolClientFactory
import org.eclipse.thingweb.credentials.BearerCredentials
import org.eclipse.thingweb.security.BearerSecurityScheme
import org.eclipse.thingweb.thing.schema.genericReadProperty
import org.eclipse.thingweb.thing.schema.genericWriteProperty
import kotlin.test.BeforeTest
import kotlin.test.Test

class TestWebThings {

    private lateinit var wot: Wot

    @BeforeTest
    fun setup() = runTest {
        val http = HttpProtocolClientFactory()
        val https = HttpsProtocolClientFactory()
        val servient = Servient(
            clientFactories = listOf(http, https),
            credentialStore = mapOf("https://plugfest.webthings.io" to
                    BearerCredentials("dummy")
            )
        )
        wot =  Wot.create(servient)
    }

    @Test
    fun `Should control devices`() = runTest {
        val thingDescription = wot.requestThingDescription("https://plugfest.webthings.io/things/virtual-things-2",
            BearerSecurityScheme()
        )

        val testThing = wot.consume(thingDescription)
        val status = testThing.genericReadProperty<Boolean>("on")

        println(status)

        testThing.genericWriteProperty("level", 50)
        testThing.genericWriteProperty("on", true)
    }
}