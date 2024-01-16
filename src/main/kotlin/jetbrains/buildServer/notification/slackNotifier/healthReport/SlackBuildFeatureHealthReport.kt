

package jetbrains.buildServer.notification.slackNotifier.healthReport

import com.intellij.openapi.diagnostic.Logger
import jetbrains.buildServer.notification.slackNotifier.SlackNotifierDescriptor
import jetbrains.buildServer.notification.slackNotifier.SlackNotifierEnabled
import jetbrains.buildServer.notification.slackNotifier.SlackNotifierProperties
import jetbrains.buildServer.notification.slackNotifier.SlackProperties
import jetbrains.buildServer.notification.slackNotifier.slack.AggregatedSlackApi
import jetbrains.buildServer.notification.slackNotifier.slack.CachingSlackWebApi
import jetbrains.buildServer.notification.slackNotifier.slack.SlackResponseError
import jetbrains.buildServer.notification.slackNotifier.slack.SlackWebApiFactory
import jetbrains.buildServer.serverSide.*
import jetbrains.buildServer.serverSide.executors.ExecutorServices
import jetbrains.buildServer.serverSide.healthStatus.*
import jetbrains.buildServer.serverSide.impl.LogUtil
import jetbrains.buildServer.serverSide.impl.NotificationsBuildFeature
import jetbrains.buildServer.serverSide.oauth.OAuthConnectionsManager
import org.springframework.context.annotation.Conditional
import org.springframework.stereotype.Service
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeoutException

@Service
@Conditional(SlackNotifierEnabled::class)
class SlackBuildFeatureHealthReport(
    private val descriptor: SlackNotifierDescriptor,
    private val oAuthConnectionsManager: OAuthConnectionsManager,
    slackWebApiFactory: SlackWebApiFactory,
    private val aggregatedSlackApi: AggregatedSlackApi,
    executorServices: ExecutorServices
) : HealthStatusReport() {
    private val slackApi = CachingSlackWebApi(slackWebApiFactory.createSlackWebApi(),
        defaultTimeoutSeconds = 60, executorServices = executorServices)

    companion object {
        const val type = "slackBuildFeatureReport"

        val logger = Logger.getInstance(SlackBuildFeatureHealthReport::class.java.name)

        val invalidBuildFeatureCategory =
            ItemCategory(
                "slackInvalidBuildFeature",
                "Slack notifications build feature is invalid",
                ItemSeverity.ERROR
            )

        val botIsNotConfiguredCategory =
            ItemCategory(
                "slackBotIsNotConfigured",
                "Slack app bot is not configured correctly",
                ItemSeverity.WARN
            )
    }

    override fun report(scope: HealthStatusScope, consumer: HealthStatusItemConsumer) {
        var errors = 0

        for (buildType in scope.buildTypes) {
            errors += report(buildType, consumer)
        }

        for (buildTemplate in scope.buildTypeTemplates) {
            errors += report(buildTemplate, consumer)
        }
        if (errors != 0) {
            logger.warn("Could not generate health report for $errors build features" +
                    " due to error responses from Slack." +
                    " See notifications log for details. To see all failed features, enable the debug preset.")
        }
    }

    private fun report(buildType: SBuildType, consumer: HealthStatusItemConsumer): Int {
        val features = getFeatures(buildType)
        var errorRequests = 0
        for (feature in features) {
            val statusItem = try {
                getHealthStatus(feature, buildType, "buildType")
            } catch (e: TimeoutException) {
                null
            } catch (e: ExecutionException) {
                null
            } catch (e: SlackResponseError) {
                logger.debug("Error while generating health report for feature with id=${feature.id} " +
                        "in build type with id=${buildType.buildTypeId}: ${e.message}")
                errorRequests++
                null
            }
            if (statusItem != null) {
                consumer.consumeForBuildType(buildType, statusItem)
            }
        }
        return errorRequests
    }

    private fun report(buildTemplate: BuildTypeTemplate, consumer: HealthStatusItemConsumer): Int {
        val features = getFeatures(buildTemplate)
        var errorRequests = 0
        for (feature in features) {
            val statusItem = try {
                getHealthStatus(feature, buildTemplate, "template")
            } catch (e: TimeoutException) {
                null
            } catch (e: ExecutionException) {
                null
            } catch (e: SlackResponseError) {
                logger.debug("Error while generating health report for feature" +
                        " with id=${feature.id} in template ${LogUtil.describe(buildTemplate)}: ${e.message}")
                errorRequests++
                null
            }
            if (statusItem != null) {
                consumer.consumeForTemplate(buildTemplate, statusItem)
            }
        }
        return errorRequests
    }

    private fun getFeatures(buildTypeSettings: BuildTypeSettings): List<SBuildFeatureDescriptor> {
        return buildTypeSettings.getBuildFeaturesOfType(NotificationsBuildFeature.FEATURE_TYPE)
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
                  T : BuildTypeIdentity,
                  T : ParametersSupport {
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

        val receiverName =
            feature.parameters[SlackProperties.channelProperty.key]?.let { buildType.valueResolver.resolve(it).result }
                ?: return generateHealthStatus(feature, type, buildType, "Channel or user id is missing")

        // Missing token is reported by [SlackConnectionHealthReport]
        val token = connection.parameters["secure:token"] ?: return null

        if (receiverName.startsWith("#")) {
            val bot = slackApi.authTest(token)
            val channels = aggregatedSlackApi.getChannelsList(token)
            val channel = channels.find {
                "#${it.name}" == receiverName
            }
            if (channel == null) {
                val botName = try {
                    slackApi.botsInfo(token, bot.botId).bot.name
                } catch (e: TimeoutException) {
                    return null
                }

                return generateHealthStatus(
                    feature,
                    type,
                    buildType,
                    "Can't find channel $receiverName. If it's private, you should add bot '${botName}' to this channel before it can post messages there"
                )
            }

            if (TeamCityProperties.getBooleanOrTrue(SlackNotifierProperties.enableBotAddedHealthReport)) {
                val members = aggregatedSlackApi.getConversationMembers(token, channel.id)

                if (!members.contains(bot.userId) && !members.contains(bot.botId)) {
                    val botName = slackApi.botsInfo(token, bot.botId).bot.name

                    return generateHealthStatus(
                        feature,
                        type,
                        buildType,
                        "Bot '$botName' is not added to $receiverName channel. Bot should be added to the channel to be able to post messages",
                        category = botIsNotConfiguredCategory
                    )
                }
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
        reason: String,
        category: ItemCategory = invalidBuildFeatureCategory
    ): HealthStatusItem {
        return HealthStatusItem(
            feature.id + "_invalidSlackBuildFeature",
            category,
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
    override fun getCategories(): Collection<ItemCategory> = listOf(invalidBuildFeatureCategory, botIsNotConfiguredCategory)
    override fun canReportItemsFor(scope: HealthStatusScope): Boolean = true
}