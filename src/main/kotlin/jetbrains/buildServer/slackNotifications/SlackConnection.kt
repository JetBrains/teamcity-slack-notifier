package jetbrains.buildServer.slackNotifications

import jetbrains.buildServer.log.Loggers
import jetbrains.buildServer.serverSide.oauth.OAuthConnectionDescriptor
import jetbrains.buildServer.serverSide.oauth.OAuthProvider
import jetbrains.buildServer.web.openapi.PluginDescriptor

class SlackConnection(
    private val pluginDescriptor: PluginDescriptor
) : OAuthProvider() {
    init {
        Loggers.SERVER.info("Loaded slack connection")
    }

    override fun getType(): String =
        Companion.type

    override fun getDisplayName(): String =
        name

    override fun getEditParametersUrl(): String =
        pluginDescriptor.getPluginResourcesPath("editConnectionParameters.jsp")

    override fun describeConnection(connection: OAuthConnectionDescriptor): String {
        return "Connection to a single Slack workspace"
    }

    companion object {
        const val type = "slackConnection"
        const val name = "Slack Connection"
    }
}