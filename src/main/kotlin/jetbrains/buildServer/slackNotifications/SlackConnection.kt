package jetbrains.buildServer.slackNotifications

import jetbrains.buildServer.log.Loggers
import jetbrains.buildServer.serverSide.InvalidProperty
import jetbrains.buildServer.serverSide.PropertiesProcessor
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
        val displayName = connection.connectionDisplayName
        if (displayName.isEmpty()) {
            return "Connection to a single Slack workspace"
        }

        val connectionId = connection.parameters["externalId"]

        val externalId = "External id: $connectionId"

        return "${displayName}\n${externalId}"
    }

    override fun getPropertiesProcessor(): PropertiesProcessor = PropertiesProcessor {
        val errors = mutableListOf<InvalidProperty>()

        // TODO: Check that this id is unique
        val externalId = it["externalId"]
        if (externalId.isNullOrEmpty()) {
            errors.add(InvalidProperty("externalId", "External id should not be empty"))
        }

        // TODO: Add check that token is valid
        val botToken = it["secure:token"]
        if (botToken.isNullOrEmpty()) {
            errors.add(InvalidProperty("secure:token", "Bot token should not be empty"))
        }

        errors
    }

    companion object {
        const val type = "slackConnection"
        const val name = "Slack Connection"
    }
}