package jetbrains.buildServer.notification


import org.testng.annotations.Test

class SlackNotifierTest : BaseSlackTestCase() {
    @Test
    fun `test should not send notification if user is subscribed to different event`() {
        `given user is subscribed to`(NotificationRule.Event.BUILD_FINISHED_FAILURE)
        `when build finishes`()
        `then no messages should be sent`()
    }

    @Test
    fun `test should send notification about build start`() {
        `given user is subscribed to`(NotificationRule.Event.BUILD_STARTED)
        val build = `when build starts`()
        `then message should contain`("start", build.buildNumber)
    }

    @Test
    fun `test should send notification about build success`() {
        `given user is subscribed to`(NotificationRule.Event.BUILD_FINISHED_SUCCESS)
        val build = `when build finishes`()
        `then message should contain`("success", build.buildNumber)
    }

    @Test
    fun `test should send notification about build failure`() {
        `given user is subscribed to`(NotificationRule.Event.BUILD_FINISHED_FAILURE)
        val build = `when build fails`()
        `then message should contain`("failed", build.buildNumber)
    }

    @Test
    fun `test should send notification about build starting to fail`() {
        `given user is subscribed to`(NotificationRule.Event.BUILD_FAILING)
        val build = `when build is failing`()
        `then message should contain`("failing", build.buildNumber)
    }

    @Test
    fun `test should send notification about build probably hanging`() {
        `given user is subscribed to`(NotificationRule.Event.BUILD_PROBABLY_HANGING)
        val build = `when build hangs`()
        `then message should contain`("hang", build.buildNumber)
    }
}