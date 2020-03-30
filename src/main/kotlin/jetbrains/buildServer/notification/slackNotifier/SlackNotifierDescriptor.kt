package jetbrains.buildServer.notification.slackNotifier

import jetbrains.buildServer.PluginTypes
import jetbrains.buildServer.notification.BuildTypeNotifierDescriptor
import jetbrains.buildServer.notification.UserNotifierDescriptor
import jetbrains.buildServer.parameters.ParametersUtil
import jetbrains.buildServer.serverSide.ControlDescription
import jetbrains.buildServer.serverSide.InvalidProperty
import jetbrains.buildServer.serverSide.parameters.WellknownParameterArguments
import jetbrains.buildServer.serverSide.parameters.types.SelectParameter
import jetbrains.buildServer.serverSide.parameters.types.TextParameter
import jetbrains.buildServer.users.PluginPropertyKey
import jetbrains.buildServer.web.openapi.PluginDescriptor
import org.springframework.context.annotation.Conditional
import org.springframework.stereotype.Service

@Service
@Conditional(SlackNotifierEnabled::class)
class SlackNotifierDescriptor(
        private val pluginDescriptor: PluginDescriptor,
        private val connectionOptionsSelectProvider: SlackConnectionSelectOptionsProvider
) : BuildTypeNotifierDescriptor, UserNotifierDescriptor {
    val channelPropertyName = "channel"
    val channelProperty =
            PluginPropertyKey(PluginTypes.NOTIFICATOR_PLUGIN_TYPE, type, channelPropertyName)

    val connectionPropertyName = "connection"
    val connectionProperty =
            PluginPropertyKey(PluginTypes.NOTIFICATOR_PLUGIN_TYPE, type, connectionPropertyName)

    override fun validate(properties: Map<String, String>): MutableCollection<InvalidProperty> {
        val invalidProperties = mutableListOf<InvalidProperty>()

        val channel = properties[channelProperty.key]
        if (channel.isNullOrEmpty()) {
            invalidProperties.add(InvalidProperty(channelProperty.key, "Channel or user id must not be empty"))
        }

        val connection = properties[connectionProperty.key]
        if (connection.isNullOrEmpty()) {
            invalidProperties.add(InvalidProperty(connectionProperty.key, "Connection must be selected"))
        }

        return invalidProperties
    }

    override fun describeParameters(parameters: MutableMap<String, String>): String {
        val to = parameters[channelProperty.key]
        if (to != null) {
            return "To: $to"
        }

        return ""
    }

    override fun getParameters(): MutableMap<String, ControlDescription> {
        return mutableMapOf(
                connectionProperty.key to ParametersUtil.createControlDescription(
                        "selection",
                        mapOf(
                                WellknownParameterArguments.ARGUMENT_DESCRIPTION.name to "Connection",
                                WellknownParameterArguments.REQUIRED.name to "true",
                                "selectOptionsProviderId" to connectionOptionsSelectProvider.id
                        )
                ),
                channelProperty.key to ParametersUtil.createControlDescription(
                        TextParameter.KEY,
                        mapOf(
                                WellknownParameterArguments.ARGUMENT_DESCRIPTION.name to "#channel or user id",
                                WellknownParameterArguments.REQUIRED.name to "true",
                                "hintUrl" to SlackNotifierChannelCompletionController.url
                        )
                )
        )
    }

    override fun getType(): String = Companion.type
    override fun getDisplayName(): String {
        return "Experimental Slack Notifier"
    }

    companion object {
        const val type = "jbSlackNotifier"
    }

    override fun getEditParametersUrl(): String =
            pluginDescriptor.getPluginResourcesPath("editBuildTypeSlackNotifierSettings.html")
}