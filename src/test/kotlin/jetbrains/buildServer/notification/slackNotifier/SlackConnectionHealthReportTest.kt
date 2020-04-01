package jetbrains.buildServer.notification.slackNotifier

import jetbrains.buildServer.serverSide.healthStatus.HealthStatusItem
import jetbrains.buildServer.serverSide.healthStatus.HealthStatusScope
import jetbrains.buildServer.serverSide.healthStatus.impl.ScopeBuilder
import jetbrains.buildServer.serverSide.healthStatus.reports.StubHealthStatusItemConsumer
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Test

class SlackConnectionHealthReportTest : BaseSlackTestCase() {
    private val itemsConsumer = StubHealthStatusItemConsumer()
    private lateinit var healthReport: SlackConnectionHealthReport
    private lateinit var scope: HealthStatusScope
    private lateinit var result: List<HealthStatusItem>

    @BeforeMethod(alwaysRun = true)
    override fun setUp() {
        super.setUp()

        healthReport = SlackConnectionHealthReport(myConnectionManager, MockSlackWebApiFactory())
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

    private fun `given project is in scope`() {
        scope = ScopeBuilder().run {
            addProject(myProject)
        }.build()
    }

    private fun `given connection is missing token`() {
        myConnection = myConnectionManager.addConnection(myProject, SlackConnection.type, emptyMap())
    }

    private fun `given connection has invalid token`() {
        myConnection =
            myConnectionManager.addConnection(myProject, SlackConnection.type, mapOf("secure:token" to "invalid_token"))
    }

    private fun `given connection is configured correctly`() {
        myConnection =
            myConnectionManager.addConnection(myProject, SlackConnection.type, mapOf("secure:token" to "test_token"))
    }

    private fun `when health is reported`() {
        itemsConsumer.reset()
        healthReport.report(scope, itemsConsumer)
        result = itemsConsumer.consumedItems
    }

    private fun `then report should contain error about missing token`() {
        assertNotNull(
            result.find {
                it.category == SlackConnectionHealthReport.missingTokenCategory
            }
        )
    }

    private fun `then report should contain error about invalid token`() {
        assertNotNull(
            result.find {
                it.category == SlackConnectionHealthReport.invalidTokenCategory
            }
        )
    }

    private fun `then report should contain no errors`() {
        assertEmpty(result)
    }
}