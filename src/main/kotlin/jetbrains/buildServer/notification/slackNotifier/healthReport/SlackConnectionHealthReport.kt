

package jetbrains.buildServer.notification.slackNotifier.healthReport

import jetbrains.buildServer.notification.slackNotifier.SlackConnection
import jetbrains.buildServer.notification.slackNotifier.SlackNotifierEnabled
import jetbrains.buildServer.notification.slackNotifier.slack.SlackWebApiFactory
import jetbrains.buildServer.serverSide.SProject
import jetbrains.buildServer.serverSide.healthStatus.*
import jetbrains.buildServer.serverSide.oauth.OAuthConnectionDescriptor
import jetbrains.buildServer.serverSide.oauth.OAuthConnectionsManager
import org.springframework.context.annotation.Conditional
import org.springframework.stereotype.Service
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeoutException

@Service
@Conditional(SlackNotifierEnabled::class)
class SlackConnectionHealthReport(
    private val oAuthConnectionsManager: OAuthConnectionsManager,
    slackApiFactory: SlackWebApiFactory
) : HealthStatusReport() {
    private val api = slackApiFactory.createSlackWebApi()

    companion object {
        const val type = "slackConnection"
        val invalidConnectionCategory =
            ItemCategory("slackConnectionIsInvalid", "Slack connection is invalid", ItemSeverity.ERROR)
    }

    override fun report(scope: HealthStatusScope, consumer: HealthStatusItemConsumer) {
        for (project in scope.projects) {
            val connections = oAuthConnectionsManager.getAvailableConnectionsOfType(
                project,
                SlackConnection.type
            )
            for (connection in connections) {
                if (connection.project != project) {
                    continue
                }

                report(connection, consumer, project)
            }
        }
    }

    private fun report(connection: OAuthConnectionDescriptor, consumer: HealthStatusItemConsumer, project: SProject) {
        val token = connection.parameters["secure:token"]
        if (token == null || token == "") {
            consumer.consumeForProject(
                connection.project,
                HealthStatusItem(
                    "missingToken_" + connection.id,
                    invalidConnectionCategory,
                    mapOf(
                        "reason" to "Connection is missing Slack bot token ('secure:token') property.",
                        "connection" to connection,
                        "project" to project
                    )
                )
            )
            return
        }

        val permissions = try {
            api.authTest(token)
        } catch (e: TimeoutException) {
            return
        } catch (e: ExecutionException) {
            return
        }
        if (!permissions.ok) {
            val error = permissions.error
            val reason = if (error == "not_authed" || error == "invalid_auth") {
                "Provided token is invalid or expired."
            } else {
                "Unknown error: $error."
            }

            consumer.consumeForProject(
                connection.project,
                HealthStatusItem(
                    "invalidToken_" + connection.id,
                    invalidConnectionCategory,
                    mapOf("reason" to reason, "connection" to connection, "project" to project)
                )
            )
        }

        val clientId = connection.parameters["clientId"]
        if (clientId.isNullOrEmpty()) {
            consumer.consumeForProject(
                connection.project,
                HealthStatusItem(
                    "missingClientId_" + connection.id,
                    invalidConnectionCategory,
                    mapOf(
                        "reason" to "Connection is missing Slack client id ('clientId') property.",
                        "connection" to connection,
                        "project" to project
                    )
                )
            )
            return
        }

        val clientSecret = connection.parameters["secure:clientSecret"]
        if (clientSecret.isNullOrEmpty()) {
            consumer.consumeForProject(
                connection.project,
                HealthStatusItem(
                    "missingClientSecret_" + connection.id,
                    invalidConnectionCategory,
                    mapOf(
                        "reason" to "Connection is missing Slack client secret ('secure:clientSecret') property.",
                        "connection" to connection,
                        "project" to project
                    )
                )
            )
            return
        }
    }

    override fun getType(): String =
        Companion.type

    override fun getDisplayName(): String = "Report Slack incorrectly configured connection"
    override fun getCategories(): Collection<ItemCategory> = listOf(invalidConnectionCategory)
    override fun canReportItemsFor(scope: HealthStatusScope): Boolean = true
}