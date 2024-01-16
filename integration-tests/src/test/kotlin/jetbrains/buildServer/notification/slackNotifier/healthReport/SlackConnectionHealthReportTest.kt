

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
    fun `should report missing client secret`() {
        `given project is in scope`() And
                `given connection is missing client secret`()
        `when health is reported`()
        `then report should contain error about missing client secret`()
    }

    @Test
    fun `should report missing clientId`() {
        `given project is in scope`() And
                `given connection is missing client id`()
        `when health is reported`()
        `then report should contain error about missing client id`()
    }

    @Test
    fun `should report no errors for correct connection`() {
        `given project is in scope`() And
                `given connection is configured correctly`()
        `when health is reported`()
        `then report should contain no errors`()
    }

    @Test(timeOut = 30_000)
    fun `should report no errors if authTest is hanging`() {
        `given project is in scope`() And
                `given connection is configured correctly`() And
                `given authTest api method is hanging`()
        `when health is reported`()
        `then report should contain no errors`()
    }

    private fun `given connection is missing client secret`() {
        myConnection = myConnectionManager.addConnection(
            myProject,
            SlackConnection.type,
            mapOf("secure:token" to "test_token", "clientId" to "test_clientId")
        )
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

    private fun `given connection is missing client id`() {
        myConnection =
            myConnectionManager.addConnection(
                myProject,
                SlackConnection.type,
                mapOf(
                    "secure:token" to "test_token",
                    "secure:clientSecret" to "test_clientSecret"
                )
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
            val reason = it.additionalData["reason"] as String
            it.category == SlackConnectionHealthReport.invalidConnectionCategory &&
                    reason.contains("missing") &&
                    reason.contains("bot token")
        }
    }

    private fun `then report should contain error about invalid token`() {
        assertResultContains {
            val reason = it.additionalData["reason"] as String
            it.category == SlackConnectionHealthReport.invalidConnectionCategory &&
                    reason.contains("invalid") &&
                    reason.contains("token")
        }
    }

    private fun `then report should contain error about missing client secret`() {
        assertResultContains {
            val reason = it.additionalData["reason"] as String
            it.category == SlackConnectionHealthReport.invalidConnectionCategory &&
                    reason.contains("missing")  &&
                    reason.contains("client secret")
        }
    }

    private fun `then report should contain error about missing client id`() {
        assertResultContains {
            val reason = it.additionalData["reason"] as String
            it.category == SlackConnectionHealthReport.invalidConnectionCategory &&
                    reason.contains("missing")  &&
                    reason.contains("client id")
        }
    }

}