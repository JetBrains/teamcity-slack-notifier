

package jetbrains.buildServer.notification.slackNotifier.notification

import jetbrains.buildServer.notification.NotificationBuildStatusProvider
import jetbrains.buildServer.notification.slackNotifier.SlackProperties
import jetbrains.buildServer.notification.slackNotifier.slack.SlackMessageFormatter
import jetbrains.buildServer.serverSide.BuildServerEx
import jetbrains.buildServer.serverSide.ChangesCalculationOptionsFactory
import jetbrains.buildServer.serverSide.RelativeWebLinks
import jetbrains.buildServer.serverSide.SProject
import jetbrains.buildServer.serverSide.impl.ProjectEx
import jetbrains.buildServer.users.SUser
import org.springframework.stereotype.Service

@Service
class VerboseMessageBuilderFactory(
    private val detailsFormatter: DetailsFormatter,
    private val format: SlackMessageFormatter,
    private val links: RelativeWebLinks,
    private val notificationBuildStatusProvider: NotificationBuildStatusProvider,
    private val server: BuildServerEx,
    private val changesCalculationOptionsFactory: ChangesCalculationOptionsFactory
) : MessageBuilderFactory {
    companion object {
        const val defaultMaximumNumberOfChanges = 10
    }

    override fun get(user: SUser, project: SProject): MessageBuilder {
        val addBuildStatus = user.getBooleanProperty(SlackProperties.addBuildStatusProperty)
        val addBranch = user.getBooleanProperty(SlackProperties.addBranchProperty)
        val addChanges = user.getBooleanProperty(SlackProperties.addChangesProperty)
        val maximumNumberOfChanges = user.getPropertyValue(SlackProperties.maximumNumberOfChangesProperty)?.toIntOrNull()
                ?: defaultMaximumNumberOfChanges

        val newFormatEnabled = (project as ProjectEx).getBooleanInternalParameterOrTrue("teamcity.internal.notification.jbSlackNotifier.verboseMessages.newFormatEnabled")

        if (newFormatEnabled) {
            return NewVerboseMessageBuilder(
                EmojiMessageBuilder(
                    PlainMessageBuilder(
                        format = format,
                        links = links,
                        detailsFormatter = detailsFormatter
                    )
                ),
                VerboseMessagesOptions(
                    addBuildStatus = addBuildStatus,
                    addBranch = addBranch,
                    addChanges = addChanges,
                    maximumNumberOfChanges = maximumNumberOfChanges
                ),
                format,
                links,
                notificationBuildStatusProvider,
                server,
                changesCalculationOptionsFactory
            )
        }

        return VerboseMessageBuilder(
            PlainMessageBuilder(
                format = format,
                links = links,
                detailsFormatter = detailsFormatter
            ),
            VerboseMessagesOptions(
                addBuildStatus = addBuildStatus,
                addBranch = addBranch,
                addChanges = addChanges,
                maximumNumberOfChanges = maximumNumberOfChanges
            ),
            format,
            links,
            notificationBuildStatusProvider,
            server
        )
    }
}