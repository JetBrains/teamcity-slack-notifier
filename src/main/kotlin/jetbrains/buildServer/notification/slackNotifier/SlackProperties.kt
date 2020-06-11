package jetbrains.buildServer.notification.slackNotifier

import jetbrains.buildServer.PluginTypes
import jetbrains.buildServer.users.PluginPropertyKey

class SlackProperties {
    companion object {
        private const val channel = "channel"
        private const val connection = "connection"
        private const val displayName = "displayName"
        private const val messageFormat = "messageFormat"
        private const val addBuildStatus = "addBuildStatus"
        private const val addBranch = "addBranch"
        private const val addChanges = "addChanges"
        private const val maximumNumberOfChanges = "maximumNumberOfChanges"

        val channelProperty = property(channel)
        val connectionProperty = property(connection)
        val displayNameProperty = property(displayName)
        val messageFormatProperty = property(messageFormat)
        val addBuildStatusProperty = property(addBuildStatus)
        val addBranchProperty = property(addBranch)
        val addChangesProperty = property(addChanges)
        val maximumNumberOfChangesProperty = property(maximumNumberOfChanges)

        private fun property(name: String): PluginPropertyKey {
            return PluginPropertyKey(PluginTypes.NOTIFICATOR_PLUGIN_TYPE, SlackNotifierDescriptor.type, name)
        }
    }

    val channelKey = channelProperty.key
    val connectionKey = connectionProperty.key
    val messageFormatKey = messageFormatProperty.key
    val addBuildStatusKey = addBuildStatusProperty.key
    val addBranchKey = addBranchProperty.key
    val addChangesKey = addChangesProperty.key
    val maximumNumberOfChangesKey = maximumNumberOfChangesProperty.key
}