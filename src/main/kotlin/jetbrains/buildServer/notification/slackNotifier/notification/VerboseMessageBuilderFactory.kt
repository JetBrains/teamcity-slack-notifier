package jetbrains.buildServer.notification.slackNotifier.notification

import jetbrains.buildServer.notification.NotificationBuildStatusProvider
import jetbrains.buildServer.notification.slackNotifier.SlackProperties
import jetbrains.buildServer.notification.slackNotifier.slack.SlackMessageFormatter
import jetbrains.buildServer.serverSide.BuildServerEx
import jetbrains.buildServer.serverSide.RelativeWebLinks
import jetbrains.buildServer.users.SUser
import org.springframework.stereotype.Service

@Service
class VerboseMessageBuilderFactory(
    private val simpleMessageBuilderFactory: SimpleMessageBuilderFactory,
    private val slackMessageFormatter: SlackMessageFormatter,
    private val webLinks: RelativeWebLinks,
    private val notificationBuildStatusProvider: NotificationBuildStatusProvider,
    private val server: BuildServerEx
) : MessageBuilderFactory {
    override fun get(user: SUser): MessageBuilder {
        val addBuildStatus = user.getBooleanProperty(SlackProperties.addBuildStatusProperty)
        val addBranch = user.getBooleanProperty(SlackProperties.addBranchProperty)
        val addChanges = user.getBooleanProperty(SlackProperties.addChangesProperty)
        val maximumNumberOfChanges = user.getPropertyValue(SlackProperties.maximumNumberOfChangesProperty)?.toIntOrNull()
                ?: 10
        val messageBuilder = simpleMessageBuilderFactory.get(user)

        return VerboseMessageBuilder(
            messageBuilder,
            VerboseMessagesOptions(
                addBuildStatus = addBuildStatus,
                addBranch = addBranch,
                addChanges = addChanges,
                maximumNumberOfChanges = maximumNumberOfChanges
            ),
            slackMessageFormatter,
            webLinks,
            notificationBuildStatusProvider,
            server
        )
    }
}