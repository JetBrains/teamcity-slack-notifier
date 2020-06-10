package jetbrains.buildServer.notification.slackNotifier.notification

import jetbrains.buildServer.notification.slackNotifier.SlackProperties
import jetbrains.buildServer.notification.slackNotifier.slack.SlackMessageFormatter
import jetbrains.buildServer.serverSide.RelativeWebLinks
import jetbrains.buildServer.users.SUser
import org.springframework.stereotype.Service

@Service
class VerboseMessageBuilderFactory(
        private val format: SlackMessageFormatter,
        private val links: RelativeWebLinks,
        private val detailsFormatter: DetailsFormatter
) : MessageBuilderFactory {
    override fun get(user: SUser): MessageBuilder {
        val addBuildStatus = user.getPropertyValue(SlackProperties.addBuildStatusProperty)?.toBoolean() ?: false

        return VerboseMessageBuilder(
                VerboseMessagesOptions(
                        addBuildStatus = addBuildStatus
                ),
                format = format,
                links = links,
                detailsFormatter = detailsFormatter
        )
    }
}