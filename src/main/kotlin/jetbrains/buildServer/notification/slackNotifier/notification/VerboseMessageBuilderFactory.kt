package jetbrains.buildServer.notification.slackNotifier.notification

import jetbrains.buildServer.notification.slackNotifier.SlackProperties
import jetbrains.buildServer.notification.slackNotifier.slack.SlackMessageFormatter
import jetbrains.buildServer.users.SUser
import org.springframework.stereotype.Service

@Service
class VerboseMessageBuilderFactory(
        private val simpleMessageBuilderFactory: SimpleMessageBuilderFactory,
        private val slackMessageFormatter: SlackMessageFormatter
) : MessageBuilderFactory {
    override fun get(user: SUser): MessageBuilder {
        val addBuildStatus = user.getPropertyValue(SlackProperties.addBuildStatusProperty)?.toBoolean() ?: false
        val addBranch = user.getPropertyValue(SlackProperties.addBranchProperty)?.toBoolean() ?: false
        val messageBuilder = simpleMessageBuilderFactory.get(user)

        return VerboseMessageBuilder(
                messageBuilder,
                VerboseMessagesOptions(
                        addBuildStatus = addBuildStatus,
                        addBranch = addBranch
                ),
                slackMessageFormatter
        )
    }
}