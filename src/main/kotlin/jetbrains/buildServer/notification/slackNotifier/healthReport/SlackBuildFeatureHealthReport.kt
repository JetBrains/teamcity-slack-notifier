package jetbrains.buildServer.notification.slackNotifier.healthReport

import jetbrains.buildServer.notification.slackNotifier.SlackNotifierDescriptor
import jetbrains.buildServer.notification.slackNotifier.SlackNotifierEnabled
import jetbrains.buildServer.notification.slackNotifier.SlackProperties
import jetbrains.buildServer.notification.slackNotifier.slack.AggregatedSlackApi
import jetbrains.buildServer.notification.slackNotifier.slack.SlackWebApiFactory
import jetbrains.buildServer.serverSide.*
import jetbrains.buildServer.serverSide.healthStatus.*
import jetbrains.buildServer.serverSide.impl.NotificationRulesBuildFeature
import jetbrains.buildServer.serverSide.oauth.OAuthConnectionsManager
import org.springframework.context.annotation.Conditional
import org.springframework.stereotype.Service

@Service
@Conditional(SlackNotifierEnabled::class)
class SlackBuildFeatureHealthReport(
    private val descriptor: SlackNotifierDescriptor,
    private val oAuthConnectionsManager: OAuthConnectionsManager,
    private val slackWebApiFactory: SlackWebApiFactory,
    private val aggregatedSlackApi: AggregatedSlackApi
) : HealthStatusReport() {
    private val slackApi = slackWebApiFactory.createSlackWebApi()

    companion object {
        const val type = "slackBuildFeatureReport"

        val invalidBuildFeatureCategory =
            ItemCategory("slackInvalidBuildFeature", "Slack notifications build feature is invalid", ItemSeverity.ERROR)
    }

    override fun report(scope: HealthStatusScope, consumer: HealthStatusItemConsumer) {
        for (buildType in scope.buildTypes) {
            report(buildType, consumer)
        }

        for (buildTemplate in scope.buildTypeTemplates) {
            report(buildTemplate, consumer)
        }
    }

    private fun report(buildType: SBuildType, consumer: HealthStatusItemConsumer) {
        val features = getFeatures(buildType)
        for (feature in features) {
            val statusItem = getHealthStatus(feature, buildType, "buildType")
            if (statusItem != null) {
                consumer.consumeForBuildType(buildType, statusItem)
            }
        }
    }

    private fun report(buildTemplate: BuildTypeTemplate, consumer: HealthStatusItemConsumer) {
        val features = getFeatures(buildTemplate)
        for (feature in features) {
            val statusItem = getHealthStatus(feature, buildTemplate, "template")
            if (statusItem != null) {
                consumer.consumeForTemplate(buildTemplate, statusItem)
            }
        }
    }

    private fun getFeatures(buildTypeSettings: BuildTypeSettings): List<SBuildFeatureDescriptor> {
        return buildTypeSettings.getBuildFeaturesOfType(NotificationRulesBuildFeature.FEATURE_TYPE)
            .filter {
                it.parameters["notifier"] == descriptor.getType()
            }
    }

    private fun <T> getHealthStatus(
        feature: SBuildFeatureDescriptor,
        buildType: T,
        type: String
    ): HealthStatusItem?
            where T : BuildTypeSettings,
                  T : BuildTypeIdentity {
        val connectionId = feature.parameters[SlackProperties.connectionProperty.key]
            ?: return generateHealthStatus(feature, type, buildType, "Slack connection is not selected")

        val buildTypeSettings: BuildTypeSettings = buildType

        val connection = oAuthConnectionsManager.findConnectionById(
            buildTypeSettings.project,
            connectionId
        )
            ?: return generateHealthStatus(
                feature,
                type,
                buildType,
                "Can't find Slack connection with id '${connectionId}' in project ${buildTypeSettings.project.fullName}"
            )

        val receiverName = feature.parameters[SlackProperties.channelProperty.key]
            ?: return generateHealthStatus(feature, type, buildType, "Channel or user id is missing")

        // Missing token is reported by [SlackConnectionHealthReport]
        val token = connection.parameters["secure:token"] ?: return null

        if (receiverName.startsWith("#")) {
            val bot = slackApi.authTest(token)

            val channel = aggregatedSlackApi.getChannelsList(token).find {
                "#${it.name}" == receiverName
            }
            if (channel == null) {
                val botName = slackApi.botsInfo(token, bot.botId).bot.name

                return generateHealthStatus(
                    feature,
                    type,
                    buildType,
                    "Can't find channel $receiverName. If it's private, you should add bot '${botName}' to this channel before it can post messages there"
                )
            }

            val members = slackApi.conversationsMembers(token, channel.id)
            if (!members.ok) {
                return generateHealthStatus(
                    feature,
                    type,
                    buildType,
                    "Can't get members of channel $receiverName." +
                            " Error: ${members.error}"
                )
            }


            if (!members.members.contains(bot.userId)) {
                return generateHealthStatus(
                    feature,
                    type,
                    buildType,
                    "Bot is not added to $receiverName channel. Bot should be added to the channel to be able to post messages"
                )
            }
        } else {
            aggregatedSlackApi.getUsersList(token).find {
                it.id == receiverName
            } ?: return generateHealthStatus(feature, type, buildType, "Can't find user with id '${receiverName}'")
        }

        return null
    }

    private fun generateHealthStatus(
        feature: SBuildFeatureDescriptor,
        type: String,
        buildTypeIdentity: BuildTypeIdentity,
        reason: String
    ): HealthStatusItem {
        return HealthStatusItem(
            feature.id + "_invalidSlackBuildFeature",
            invalidBuildFeatureCategory,
            mapOf(
                "reason" to reason,
                "feature" to feature,
                "type" to type,
                "buildTypeId" to buildTypeIdentity.externalId
            )
        )
    }

    override fun getType(): String =
        Companion.type

    override fun getDisplayName(): String = "Report Slack incorrectly configured notifications build feature"
    override fun getCategories(): Collection<ItemCategory> = listOf(invalidBuildFeatureCategory)
    override fun canReportItemsFor(scope: HealthStatusScope): Boolean = true
}