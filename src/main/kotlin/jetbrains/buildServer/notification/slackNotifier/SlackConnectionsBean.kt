package jetbrains.buildServer.notification.slackNotifier

import jetbrains.buildServer.notification.slackNotifier.slack.AggregatedSlackApi
import jetbrains.buildServer.notification.slackNotifier.slack.SlackWebApi
import jetbrains.buildServer.serverSide.oauth.OAuthConnectionDescriptor

class SlackConnectionsBean(
        val connections: List<OAuthConnectionDescriptor>,
        private val slackApi: AggregatedSlackApi
) {
    fun getTeamForConnection(connection: OAuthConnectionDescriptor): String? {
        val token = connection.parameters["secure:token"] ?: return null
        return slackApi.getBot(token).teamId
    }
}
