/*
 *  Copyright 2000-2021 JetBrains s.r.o.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package jetbrains.buildServer.notification.slackNotifier.slack

import jetbrains.buildServer.BaseTestCase
import jetbrains.buildServer.util.ssl.SSLTrustStoreProvider
import org.testng.annotations.Test

class SlackWebApiImplTest : BaseTestCase() {
    private val standardResponse = "{" +
            "\"ok\": true," +
            "\"channel\": \"C1H9RESGL\"," +
            "\"ts\": \"1503435956.000247\"" +
            "}"

    private lateinit var slackApi: SlackWebApiImpl
    private lateinit var slackResponse: MaybeMessage

    @Test
    fun `should post message`() {
        `given slack is responding correctly`()
        `when message is sent`()
        `then result should be successful`()
    }

    @Test
    fun `should retry request if first fails`() {
        `given slack fails on first request`()
        `when message is sent`()
        `then result should be successful`()
    }

    @Test
    fun `should return error if request always fails`() {
        `given slack always fails`()
        `when message is sent`()
        `then result should be error`()
    }

    private fun `given slack is responding correctly`() {
        slackApi = SlackWebApiImpl(
                RequestHandlerStub(standardResponse),
                SSLTrustStoreProvider { null }
        )
    }

    private fun `given slack fails on first request`() {
        slackApi = SlackWebApiImpl(
                FailingFirstRequestHandler(standardResponse),
                SSLTrustStoreProvider { null }
        )
    }

    private fun `given slack always fails`() {
        slackApi = SlackWebApiImpl(
                AlwaysFailingRequestHandler(),
                SSLTrustStoreProvider { null }
        )
    }

    private fun `when message is sent`() {
        slackResponse = slackApi.postMessage(slackToken, Message("#test_channel", "Test message"))
    }

    private fun `then result should be successful`() {
        assertTrue(slackResponse.ok)
    }

    private fun `then result should be error`() {
        assertFalse(slackResponse.ok)
    }
}

