package jetbrains.buildServer.notification.slackNotifier

import jetbrains.buildServer.notification.slackNotifier.slack.SlackWebApiFactory
import jetbrains.buildServer.serverSide.healthStatus.*
import jetbrains.buildServer.serverSide.oauth.OAuthConnectionDescriptor
import jetbrains.buildServer.serverSide.oauth.OAuthConnectionsManager
import org.springframework.context.annotation.Conditional
import org.springframework.stereotype.Service

@Service
@Conditional(SlackNotifierEnabled::class)
class SlackConnectionHealthReport(
    private val oAuthConnectionsManager: OAuthConnectionsManager,
    private val slackApiFactory: SlackWebApiFactory
) : HealthStatusReport() {
    private val api = slackApiFactory.createSlackWebApi()

    companion object {
        const val type = "slackConnection"
        val invalidTokenCategory =
            ItemCategory("slackConnectionInvalidToken", "Slack connection token is invalid", ItemSeverity.ERROR)
        val missingTokenCategory =
            ItemCategory("slackConnectionMissingToken", "Slack connection token is missing", ItemSeverity.ERROR)
    }

    override fun report(scope: HealthStatusScope, consumer: HealthStatusItemConsumer) {
        for (project in scope.projects) {
            val connections = oAuthConnectionsManager.getAvailableConnectionsOfType(project, SlackConnection.type)
            for (connection in connections) {
                if (connection.project != project) {
                    continue
                }

                report(connection, consumer)
            }
        }
    }

    private fun report(connection: OAuthConnectionDescriptor, consumer: HealthStatusItemConsumer) {
        val token = connection.parameters["secure:token"]
        if (token == null || token == "") {
            consumer.consumeForProject(
                connection.project,
                HealthStatusItem(
                    "missingToken_" + connection.id,
                    missingTokenCategory,
                    mapOf("tokenProperty" to "secure:token", "connection" to connection)
                )
            )

            return
        }

        val permissions = api.authTest(token)
        if (!permissions.ok) {
            val error = permissions.error
            consumer.consumeForProject(
                connection.project,
                HealthStatusItem(
                    "invalidToken_" + connection.id,
                    invalidTokenCategory,
                    mapOf("error" to error, "connection" to connection)
                )
            )
            return
        }
    }

    override fun getType(): String = Companion.type
    override fun getDisplayName(): String = "Report Slack incorrectly configured connection"
    override fun getCategories(): Collection<ItemCategory> = listOf(invalidTokenCategory, missingTokenCategory)
    override fun canReportItemsFor(scope: HealthStatusScope): Boolean = true
}