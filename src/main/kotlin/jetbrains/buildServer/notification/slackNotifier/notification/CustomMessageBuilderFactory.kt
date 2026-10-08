

package jetbrains.buildServer.notification.slackNotifier.notification

import jetbrains.buildServer.notification.NotificationBuildStatusProvider
import jetbrains.buildServer.notification.slackNotifier.SlackProperties
import jetbrains.buildServer.notification.slackNotifier.slack.SlackMessageFormatter
import jetbrains.buildServer.serverSide.BuildServerEx
import jetbrains.buildServer.serverSide.ChangesCalculationOptionsFactory
import jetbrains.buildServer.serverSide.RelativeWebLinks
import jetbrains.buildServer.serverSide.SProject
import jetbrains.buildServer.users.SUser
import org.springframework.stereotype.Service

@Service
class CustomMessageBuilderFactory(
    private val simpleMessageBuilderFactory: SimpleMessageBuilderFactory,
    private val detailsFormatter: DetailsFormatter,
    private val format: SlackMessageFormatter,
    private val links: RelativeWebLinks,
    private val notificationBuildStatusProvider: NotificationBuildStatusProvider,
    private val server: BuildServerEx,
    private val changesCalculationOptionsFactory: ChangesCalculationOptionsFactory
) : MessageBuilderFactory {
    override fun get(user: SUser, project: SProject): MessageBuilder {
        val templates = CustomMessageTemplates(
            default = template(user, SlackProperties.customTemplateProperty) ?: MessageTemplate.defaultTemplate,
            success = template(user, SlackProperties.customTemplateSuccessProperty),
            failure = template(user, SlackProperties.customTemplateFailureProperty)
        )
        val maximumNumberOfChanges = user.getPropertyValue(SlackProperties.maximumNumberOfChangesProperty)?.toIntOrNull()
            ?: VerboseMessageBuilderFactory.defaultMaximumNumberOfChanges

        return CustomMessageBuilder(
            fallback = simpleMessageBuilderFactory.get(user, project),
            templates = templates,
            maximumNumberOfChanges = maximumNumberOfChanges,
            format = format,
            links = links,
            detailsFormatter = detailsFormatter,
            notificationBuildStatusProvider = notificationBuildStatusProvider,
            server = server,
            changesCalculationOptionsFactory = changesCalculationOptionsFactory
        )
    }

    private fun template(user: SUser, property: jetbrains.buildServer.users.PluginPropertyKey): String? =
        user.getPropertyValue(property)?.takeIf { it.isNotBlank() }
}
