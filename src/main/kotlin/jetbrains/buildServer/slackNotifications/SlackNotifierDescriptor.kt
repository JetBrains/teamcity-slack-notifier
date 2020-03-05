package jetbrains.buildServer.slackNotifications

import jetbrains.buildServer.PluginTypes
import jetbrains.buildServer.notification.BuildTypeNotifierDescriptor
import jetbrains.buildServer.parameters.ParametersUtil
import jetbrains.buildServer.serverSide.ControlDescription
import jetbrains.buildServer.serverSide.InvalidProperty
import jetbrains.buildServer.serverSide.Parameter
import jetbrains.buildServer.serverSide.parameters.WellknownParameterArguments
import jetbrains.buildServer.serverSide.parameters.types.TextParameter
import jetbrains.buildServer.users.PluginPropertyKey

class SlackNotifierDescriptor : BuildTypeNotifierDescriptor {
    override fun validate(parameteres: Map<String, Parameter>): MutableCollection<InvalidProperty> {
        return mutableListOf()
    }

    override fun getParameters(): MutableMap<String, ControlDescription> {
        return mutableMapOf(
            channelProperty.key to ParametersUtil.createControlDescription(
                TextParameter.KEY,
                mapOf(
                    WellknownParameterArguments.ARGUMENT_DESCRIPTION.name to "#channel or @name"
                )
            )
        )
    }

    override fun getType(): String = "jbSlackNotifier"
    override fun getDisplayName(): String = "Slack Notifier"

    override fun getEditParametersUrl(): String? {
        return null
    }

    companion object {
        const val channelPropertyName = "channel"
        val channelProperty =
            PluginPropertyKey(PluginTypes.NOTIFICATOR_PLUGIN_TYPE, "jbSlackNotifier", channelPropertyName)
    }
}