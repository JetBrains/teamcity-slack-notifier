package jetbrains.buildServer.notification.slackNotifier.healthReport

import jetbrains.buildServer.notification.FeatureProviderNotificationRulesHolder
import jetbrains.buildServer.notification.slackNotifier.And
import jetbrains.buildServer.notification.slackNotifier.SlackProperties
import jetbrains.buildServer.serverSide.impl.NotificationRulesConstants
import jetbrains.buildServer.serverSide.impl.NotificationsBuildFeature
import org.testng.annotations.Test

class SlackFailedNotificationHealthReportTest : BaseSlackHealthReportTest<SlackFailedNotificationHealthReport>() {
    override fun getReport(): SlackFailedNotificationHealthReport {
        return SlackFailedNotificationHealthReport(myFailedNotificationCollector, myProjectManager)
    }

    @Test
    fun `should report configuration-related delivery failures`() {
        `given build type is in scope`() And
                `given build feature sends to receiver`("#anotherChannel")
        `when build starts`()
        `when health is reported`()
        assertResultContains {
            it.category == SlackFailedNotificationHealthReport.deliveryConfigurationCategory &&
                    (it.additionalData["reason"] as String).contains("not_in_channel")
        }
    }

    @Test
    fun `should report throttled delivery failures`() {
        `given build type is in scope`() And
                `given build feature sends to receiver`("#rate_limited")
        `when build starts`()
        `when health is reported`()
        assertResultContains {
            it.category == SlackFailedNotificationHealthReport.deliveryThrottledCategory &&
                    (it.additionalData["reason"] as String).contains("ratelimited")
        }
    }

    @Test
    fun `should report runtime delivery failures`() {
        `given build type is in scope`() And
                `given build feature sends to receiver`("#unknown_failure")
        `when build starts`()
        `when health is reported`()
        assertResultContains {
            it.category == SlackFailedNotificationHealthReport.deliveryRuntimeCategory &&
                    (it.additionalData["reason"] as String).contains("unknown_error")
        }
    }

    @Test
    fun `should clear project scoped failures on build type scoped success`() {
        `given project is in scope`()
        myFailedNotificationCollector.reportFailure(
            myProject,
            null,
            myConnection.id,
            "#test_channel",
            "ratelimited",
            "Error sending message to #test_channel: ratelimited"
        )
        myFailedNotificationCollector.clearDeliveryErrors(
            myProject,
            myBuildType.externalId,
            myConnection.id,
            "#test_channel"
        )

        `when health is reported`()
        `then report should contain no errors`()
    }

    private fun `given build feature sends to receiver`(receiver: String) {
        myBuildType.addBuildFeature(
            NotificationsBuildFeature.FEATURE_TYPE,
            mapOf(
                FeatureProviderNotificationRulesHolder.NOTIFIER to myDescriptor.getType(),
                NotificationRulesConstants.BUILD_STARTED to "true",
                SlackProperties.connectionProperty.key to myConnection.id,
                SlackProperties.channelProperty.key to receiver
            )
        )
    }
}
