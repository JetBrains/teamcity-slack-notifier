

package jetbrains.buildServer.notification.slackNotifier

import jetbrains.buildServer.PluginTypes
import jetbrains.buildServer.notification.slackNotifier.notification.MessageTemplate
import jetbrains.buildServer.notification.slackNotifier.notification.TemplatePlaceholder
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
        private const val customTemplate = "customTemplate"
        private const val customTemplateSuccess = "customTemplateSuccess"
        private const val customTemplateFailure = "customTemplateFailure"

        const val simpleMessageFormat = "simple"
        const val verboseMessageFormat = "verbose"
        const val customMessageFormat = "custom"

        val channelProperty = property(channel)
        val connectionProperty = property(connection)
        val displayNameProperty = property(displayName)
        val messageFormatProperty = property(messageFormat)
        val addBuildStatusProperty = property(addBuildStatus)
        val addBranchProperty = property(addBranch)
        val addChangesProperty = property(addChanges)
        val maximumNumberOfChangesProperty = property(maximumNumberOfChanges)
        val customTemplateProperty = property(customTemplate)
        val customTemplateSuccessProperty = property(customTemplateSuccess)
        val customTemplateFailureProperty = property(customTemplateFailure)

        val customTemplateProperties = listOf(customTemplateProperty, customTemplateSuccessProperty, customTemplateFailureProperty)

        // Properties that are related to the notification itself
        val notificationProperties = listOf(
            messageFormatProperty, addBuildStatusProperty, addBranchProperty, addChangesProperty, maximumNumberOfChangesProperty
        ) + customTemplateProperties

        private fun property(name: String): PluginPropertyKey {
            return PluginPropertyKey(PluginTypes.NOTIFICATOR_PLUGIN_TYPE, SlackNotifierDescriptor.notifierType, name)
        }
    }

    val channelKey = channelProperty.key
    val connectionKey = connectionProperty.key
    val messageFormatKey = messageFormatProperty.key
    val addBuildStatusKey = addBuildStatusProperty.key
    val addBranchKey = addBranchProperty.key
    val addChangesKey = addChangesProperty.key
    val maximumNumberOfChangesKey = maximumNumberOfChangesProperty.key
    val customTemplateKey = customTemplateProperty.key
    val customTemplateSuccessKey = customTemplateSuccessProperty.key
    val customTemplateFailureKey = customTemplateFailureProperty.key

    // Used by the settings pages to document the template syntax
    val defaultCustomTemplate: String = MessageTemplate.defaultTemplate
    val templatePlaceholders: List<TemplatePlaceholder> = TemplatePlaceholder.values().toList()
}