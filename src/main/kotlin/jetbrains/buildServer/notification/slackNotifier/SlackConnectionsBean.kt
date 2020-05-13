package jetbrains.buildServer.notification.slackNotifier

import jetbrains.buildServer.notification.slackNotifier.slack.AggregatedBot
import jetbrains.buildServer.notification.slackNotifier.slack.AggregatedSlackApi
import jetbrains.buildServer.notification.slackNotifier.slack.SlackWebApi
import jetbrains.buildServer.serverSide.oauth.OAuthConnectionDescriptor

class SlackConnectionsBean(
        val connections: List<OAuthConnectionDescriptor>,
        private val slackApi: AggregatedSlackApi
) {
    fun getTeamForConnection(connection: OAuthConnectionDescriptor): String? {
        return getBot(connection)?.teamId
    }

    fun getTeamDomainForConnection(connection: OAuthConnectionDescriptor): String? {
        return getBot(connection)?.teamDomain
    }

    private fun getBot(connection: OAuthConnectionDescriptor): AggregatedBot? {
        val token = connection.parameters["secure:token"] ?: return null
        return slackApi.getBot(token)
    }
}
