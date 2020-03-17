package jetbrains.buildServer.notification

import jetbrains.buildServer.PluginTypes
import jetbrains.buildServer.serverSide.ControlDescription
import jetbrains.buildServer.serverSide.InvalidProperty
import jetbrains.buildServer.serverSide.Parameter
import jetbrains.buildServer.users.PluginPropertyKey
import jetbrains.buildServer.web.openapi.PluginDescriptor

class SlackNotifierDescriptor(
    private val pluginDescriptor: PluginDescriptor
) : BuildTypeNotifierDescriptor {
    val channelPropertyName = "channel"
    val channelProperty =
        PluginPropertyKey(PluginTypes.NOTIFICATOR_PLUGIN_TYPE, type, channelPropertyName)

    val connectionPropertyName = "connection"
    val connectionProperty =
        PluginPropertyKey(PluginTypes.NOTIFICATOR_PLUGIN_TYPE, type, connectionPropertyName)

    override fun validate(parameteres: Map<String, Parameter>): MutableCollection<InvalidProperty> {
        return mutableListOf()
    }

    override fun getParameters(): MutableMap<String, ControlDescription> {
        return mutableMapOf()
    }

    override fun getType(): String = "jbSlackNotifier"
    override fun getDisplayName(): String = "Slack Notifier"

    override fun getEditParametersUrl(): String =
        pluginDescriptor.getPluginResourcesPath("editBuildTypeSlackNotifierSettings.html")
}