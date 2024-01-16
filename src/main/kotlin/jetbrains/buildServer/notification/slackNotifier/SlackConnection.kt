

package jetbrains.buildServer.notification.slackNotifier

import com.intellij.openapi.util.text.StringUtil
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
        val builder = StringBuilder()

        val displayName = connection.connectionDisplayName
        if (displayName.isEmpty()) {
            builder.append("Connection to a single Slack workspace")
        } else {
            builder.append("Connection name: $displayName")
        }

        val rawLimitValue = connection.parameters["serviceMessageMaxNotificationsPerBuild"]
        if (rawLimitValue != null) {
            val limit: Long? = rawLimitValue.toLongOrNull()
            if (limit != null && limit != 0L) {
                builder
                    .append(System.lineSeparator())
                    .append("Service message notifications are enabled")
                    .append(System.lineSeparator())

                when (limit) {
                    -1L -> {
                        builder.append("Each build may produce unlimited number of notifications")
                    }
                    else -> {
                        builder.append("Each build may produce $limit ${StringUtil.pluralize("notification", limit.toInt())}")
                    }
                }

                val allowedDomains = connection.parameters["serviceMessageAllowedDomainNames"] ?: ""
                if (allowedDomains.isNotEmpty()) {
                    builder
                        .append(System.lineSeparator())
                        .append("Allowed domain name patterns: $allowedDomains")
                }
            }
        }

        return builder.toString()
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

        val maxNotificationsPerBuild = it["serviceMessageMaxNotificationsPerBuild"]
        if (!maxNotificationsPerBuild.isNullOrEmpty() && maxNotificationsPerBuild.toIntOrNull() == null) {
            errors.add(InvalidProperty("serviceMessageMaxNotificationsPerBuild", "Could not parse integer value"))
        }

        errors
    }

    override fun getDefaultProperties(): MutableMap<String, String> {
        return hashMapOf("serviceMessageMaxNotificationsPerBuild" to "0")
    }

    companion object {
        const val type = "slackConnection"
        const val name = "Slack"
    }
}