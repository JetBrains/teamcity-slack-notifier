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

package jetbrains.buildServer.notification.slackNotifier.healthReport

import jetbrains.buildServer.notification.slackNotifier.And
import jetbrains.buildServer.notification.slackNotifier.SlackConnection
import org.testng.annotations.Test

class SlackConnectionHealthReportTest : BaseSlackHealthReportTest<SlackConnectionHealthReport>() {
    override fun getReport(): SlackConnectionHealthReport {
        return SlackConnectionHealthReport(
            myConnectionManager,
            mySlackApiFactory
        )
    }

    @Test
    fun `should report missing token`() {
        `given project is in scope`() And
                `given connection is missing token`()
        `when health is reported`()
        `then report should contain error about missing token`()
    }

    @Test
    fun `should report invalid token`() {
        `given project is in scope`() And
                `given connection has invalid token`()
        `when health is reported`()
        `then report should contain error about invalid token`()
    }

    @Test
    fun `should report no errors for correct connection`() {
        `given project is in scope`() And
                `given connection is configured correctly`()
        `when health is reported`()
        `then report should contain no errors`()
    }

    @Test(timeOut = 10_000)
    fun `should report no errors if authTest is hanging`() {
        `given project is in scope`() And
                `given connection is configured correctly`() And
                `given authTest api method is hanging`()
        `when health is reported`()
        `then report should contain no errors`()
    }

    private fun `given connection is missing token`() {
        myConnection = myConnectionManager.addConnection(
            myProject,
            SlackConnection.type, emptyMap()
        )
    }

    private fun `given connection has invalid token`() {
        myConnection =
            myConnectionManager.addConnection(
                myProject,
                SlackConnection.type, mapOf("secure:token" to "invalid_token")
            )
    }

    private fun `given connection is configured correctly`() {
        myConnection =
            myConnectionManager.addConnection(
                myProject,
                SlackConnection.type,
                mapOf(
                    "secure:token" to "test_token",
                    "clientId" to "test_clientId",
                    "secure:clientSecret" to "test_clientSecret"
                )
            )
    }


    private fun `then report should contain error about missing token`() {
        assertResultContains {
            it.category == SlackConnectionHealthReport.invalidConnectionCategory &&
                    (it.additionalData["reason"] as String).contains("missing")
        }
    }

    private fun `then report should contain error about invalid token`() {
        assertResultContains {
            it.category == SlackConnectionHealthReport.invalidConnectionCategory &&
                    (it.additionalData["reason"] as String).contains("invalid")
        }
    }

}