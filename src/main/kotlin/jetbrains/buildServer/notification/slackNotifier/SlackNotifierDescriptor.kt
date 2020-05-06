package jetbrains.buildServer.notification.slackNotifier

import jetbrains.buildServer.notification.NotificatorRegistry
import jetbrains.buildServer.serverSide.InvalidProperty
import org.springframework.context.annotation.Conditional
import org.springframework.stereotype.Service

@Service
@Conditional(SlackNotifierEnabled::class)
class SlackNotifierDescriptor(
        private val notificatorRegistry: NotificatorRegistry
) {
    fun validate(properties: Map<String, String>): MutableCollection<InvalidProperty> {
        val invalidProperties = mutableListOf<InvalidProperty>()

        val channel = properties[SlackProperties.channelProperty.key]
        if (channel.isNullOrEmpty()) {
            invalidProperties.add(
                InvalidProperty(
                    SlackProperties.channelProperty.key,
                    "Channel or user id must not be empty"
                )
            )
        }

        val connection = properties[SlackProperties.connectionProperty.key]
        if (connection.isNullOrEmpty()) {
            invalidProperties.add(
                InvalidProperty(
                    SlackProperties.connectionProperty.key,
                    "Connection must be selected"
                )
            )
        }

        return invalidProperties
    }


    fun getType(): String = Companion.type
    fun getDisplayName(): String {
        if (displayNameClashes()) {
            return "$defaultDisplayName (official)"
        }
        return defaultDisplayName
    }

    private fun displayNameClashes(): Boolean {
        return notificatorRegistry.notificators.find { it.displayName.trim() == defaultDisplayName } != null
    }

    companion object {
        const val type = "jbSlackNotifier"
        const val defaultDisplayName = "Slack Notifier"
    }
}