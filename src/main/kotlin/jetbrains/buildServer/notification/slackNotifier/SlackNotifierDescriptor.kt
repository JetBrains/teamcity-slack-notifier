package jetbrains.buildServer.notification.slackNotifier

import jetbrains.buildServer.serverSide.InvalidProperty
import jetbrains.buildServer.web.openapi.PluginDescriptor
import org.springframework.context.annotation.Conditional
import org.springframework.stereotype.Service

@Service
@Conditional(SlackNotifierEnabled::class)
class SlackNotifierDescriptor(
    private val pluginDescriptor: PluginDescriptor,
    private val connectionOptionsSelectProvider: SlackConnectionSelectOptionsProvider
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
        return "Experimental Slack Notifier"
    }

    companion object {
        const val type = "jbSlackNotifier"
    }
}