package jetbrains.buildServer.notification.slackNotifier.notification

import jetbrains.buildServer.notification.slackNotifier.slack.SlackMessageFormatter
import jetbrains.buildServer.serverSide.RelativeWebLinks
import jetbrains.buildServer.users.SUser
import org.springframework.stereotype.Service

@Service
class SimpleMessageBuilderFactory(
        private val format: SlackMessageFormatter,
        private val links: RelativeWebLinks,
        private val detailsFormatter: DetailsFormatter
) : MessageBuilderFactory {
    override fun get(user: SUser): MessageBuilder {
        return SimpleMessageBuilder(format, links, detailsFormatter)
    }
}