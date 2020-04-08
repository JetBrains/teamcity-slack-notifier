package jetbrains.buildServer.notification.slackNotifier

import jetbrains.buildServer.PluginTypes
import jetbrains.buildServer.users.PluginPropertyKey

class SlackProperties {
    companion object {
        val channelPropertyName = "channel"
        val channelProperty =
            PluginPropertyKey(PluginTypes.NOTIFICATOR_PLUGIN_TYPE, SlackNotifierDescriptor.type, channelPropertyName)

        val connectionPropertyName = "connection"
        val connectionProperty =
            PluginPropertyKey(PluginTypes.NOTIFICATOR_PLUGIN_TYPE, SlackNotifierDescriptor.type, connectionPropertyName)
    }

    val channelKey = channelProperty.key
    val connectionKey = connectionProperty.key
}