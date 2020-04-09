package jetbrains.buildServer.notification.slackNotifier

import jetbrains.buildServer.serverSide.InvalidProperty
import jetbrains.buildServer.serverSide.PropertiesProcessor
import jetbrains.buildServer.serverSide.oauth.OAuthConnectionDescriptor
import jetbrains.buildServer.serverSide.oauth.OAuthProvider
import jetbrains.buildServer.web.openapi.PluginDescriptor
import org.springframework.context.annotation.Conditional
import org.springframework.stereotype.Service

@Service
@Conditional(SlackNotifierEnabled::class)
class SlackConnection(
    private val pluginDescriptor: PluginDescriptor
) : OAuthProvider() {
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

        return displayName
    }

    override fun getPropertiesProcessor(): PropertiesProcessor = PropertiesProcessor {
        val errors = mutableListOf<InvalidProperty>()

        val botToken = it["secure:token"]
        if (botToken.isNullOrEmpty()) {
            errors.add(InvalidProperty("secure:token", "Slack bot token must not be empty"))
        }

        val clientId = it["clientId"]
        if (clientId.isNullOrEmpty()) {
            errors.add(InvalidProperty("clientId", "Client ID must be specified"))
        }

        val clientSecret = it["secure:clientSecret"]
        if (clientSecret.isNullOrEmpty()) {
            errors.add(InvalidProperty("secure:clientSecret", "Client secret must be specified"))
        }

        errors
    }

    companion object {
        const val type = "slackConnection"
        const val name = "Slack"
    }
}