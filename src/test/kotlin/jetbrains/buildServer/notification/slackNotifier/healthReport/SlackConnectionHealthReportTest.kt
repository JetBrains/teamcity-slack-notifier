package jetbrains.buildServer.notification.slackNotifier.healthReport

import jetbrains.buildServer.notification.slackNotifier.And
import jetbrains.buildServer.notification.slackNotifier.MockSlackWebApiFactory
import jetbrains.buildServer.notification.slackNotifier.SlackConnection
import org.testng.annotations.Test

class SlackConnectionHealthReportTest : BaseSlackHealthReportTest<SlackConnectionHealthReport>() {
    override fun getReport(): SlackConnectionHealthReport {
        return SlackConnectionHealthReport(
            myConnectionManager,
            MockSlackWebApiFactory()
        )
    }

    @Test
    fun `test should report missing token`() {
        `given project is in scope`() And
                `given connection is missing token`()
        `when health is reported`()
        `then report should contain error about missing token`()
    }

    @Test
    fun `test should report invalid token`() {
        `given project is in scope`() And
                `given connection has invalid token`()
        `when health is reported`()
        `then report should contain error about invalid token`()
    }

    @Test
    fun `test should report no errors for correct connection`() {
        `given project is in scope`() And
                `given connection is configured correctly`()
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