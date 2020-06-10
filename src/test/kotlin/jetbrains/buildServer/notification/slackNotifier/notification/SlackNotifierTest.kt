package jetbrains.buildServer.notification.slackNotifier.notification


import jetbrains.buildServer.notification.NotificationRule.Event.*
import jetbrains.buildServer.notification.slackNotifier.And
import jetbrains.buildServer.notification.slackNotifier.BaseSlackTestCase
import org.testng.annotations.Test


class SlackNotifierTest : BaseSlackTestCase() {
    @Test
    fun `test should not send notification if user is subscribed to different event`() {
        `given user is subscribed to`(BUILD_FINISHED_FAILURE)
        `when build finishes`()
        `then no messages should be sent`()
    }

    @Test
    fun `test should send notification about build start`() {
        `given user is subscribed to`(BUILD_STARTED)
        val build = `when build starts`()
        `then message should contain`("start", build.buildNumber)
    }

    @Test
    fun `test should send notification about build success`() {
        `given user is subscribed to`(BUILD_FINISHED_SUCCESS)
        val build = `when build finishes`()
        `then message should contain`("success", build.buildNumber)
    }

    @Test
    fun `test should send notification about build failure`() {
        `given user is subscribed to`(BUILD_FINISHED_FAILURE)
        val build = `when build fails`()
        `then message should contain`("failed", build.buildNumber)
    }

    @Test
    fun `test should send notification about build starting to fail`() {
        `given user is subscribed to`(BUILD_FAILING)
        val build = `when build is failing`()
        `then message should contain`("failing", build.buildNumber)
    }

    @Test
    fun `test should send notification about build probably hanging`() {
        `given user is subscribed to`(BUILD_PROBABLY_HANGING)
        val build = `when build hangs`()
        `then message should contain`("hang", build.buildNumber)
    }

    @Test
    fun `test should send notification about build problem`() {
        `given user is subscribed to`(NEW_BUILD_PROBLEM_OCCURRED, BUILD_FINISHED_FAILURE)
        val build = `when build problem occurs`()
        `then message should contain`("fail", build.buildNumber)
    }

    @Test
    fun `test should send notification about responsibility change`() {
        `given user is subscribed to`(RESPONSIBILITY_CHANGES)
        `when responsibility changes`()
        `then message should contain`("investigation") And
            1.`messages should be sent`()
    }

    @Test
    fun `test build feature should send notification about build start`() {
        `given build feature is subscribed to`(BUILD_STARTED)
        val build = `when build starts`()
        `then message should contain`("start", build.buildNumber)
    }


    @Test
    fun `test build feature should send notification about build success`() {
        `given build feature is subscribed to`(BUILD_FINISHED_SUCCESS)
        val build = `when build finishes`()
        `then message should contain`("success", build.buildNumber)
    }

    @Test
    fun `test build feature should send notification about build failure`() {
        `given build feature is subscribed to`(BUILD_FINISHED_FAILURE)
        val build = `when build fails`()
        `then message should contain`("failed", build.buildNumber)
    }

    @Test
    fun `test build feature should send notification about build starting to fail`() {
        `given build feature is subscribed to`(BUILD_FAILING)
        val build = `when build is failing`()
        `then message should contain`("failing", build.buildNumber)
    }

    @Test
    fun `test build feature should send notification about build probably hanging`() {
        `given build feature is subscribed to`(BUILD_PROBABLY_HANGING)
        val build = `when build hangs`()
        `then message should contain`("hang", build.buildNumber)
    }

    @Test
    fun `test build feature should send notification about build problem`() {
        `given build feature is subscribed to`(NEW_BUILD_PROBLEM_OCCURRED, BUILD_FINISHED_FAILURE)
        val build = `when build problem occurs`()
        `then message should contain`("fail", build.buildNumber)
    }

    @Test
    fun `test notification about manual build start should contain user who triggered it`() {
        `given user is subscribed to`(BUILD_STARTED)
        val build = `when build is triggered manually`()
        `then message should contain`(build.triggeredBy.user!!.descriptiveName)
    }
}