

package jetbrains.buildServer.notification.slackNotifier.notification


import jetbrains.buildServer.notification.ServiceMessageNotificationException
import jetbrains.buildServer.notification.NotificationRule.Event.*
import jetbrains.buildServer.notification.slackNotifier.And
import jetbrains.buildServer.notification.slackNotifier.BaseSlackTestCase
import org.testng.annotations.Test


class SlackNotifierTest : BaseSlackTestCase() {
    @Test
    fun `should not send notification if user is subscribed to different event`() {
        `given user is subscribed to`(BUILD_FINISHED_FAILURE)
        `when build finishes`()
        `then no messages should be sent`()
    }

    @Test
    fun `should send notification about build start`() {
        `given user is subscribed to`(BUILD_STARTED)
        val build = `when build starts`()
        `then message should contain`("start", build.buildNumber)
    }

    @Test
    fun `should send notification about build success`() {
        `given user is subscribed to`(BUILD_FINISHED_SUCCESS)
        val build = `when build finishes`()
        `then message should contain`("success", build.buildNumber)
    }

    @Test
    fun `should send notification about build failure`() {
        `given user is subscribed to`(BUILD_FINISHED_FAILURE)
        val build = `when build fails`()
        `then message should contain`("failed", build.buildNumber)
    }

    @Test
    fun `should send notification about build starting to fail`() {
        `given user is subscribed to`(BUILD_FAILING)
        val build = `when build is failing`()
        `then message should contain`("failing", build.buildNumber)
    }

    @Test
    fun `should send notification about build probably hanging`() {
        `given user is subscribed to`(BUILD_PROBABLY_HANGING)
        val build = `when build hangs`()
        `then message should contain`("hang", build.buildNumber)
    }

    @Test
    fun `should send notification about build problem`() {
        `given user is subscribed to`(NEW_BUILD_PROBLEM_OCCURRED, BUILD_FINISHED_FAILURE)
        val build = `when build problem occurs`()
        `then message should contain`("fail", build.buildNumber)
    }

    @Test
    fun `should send notification about responsibility change`() {
        `given user is subscribed to`(RESPONSIBILITY_CHANGES)
        `when responsibility changes`()
        `then message should contain`("investigation") And
                1.`messages should be sent`()
    }

    @Test
    fun `should send notification about build that requires approval`() {
        `given user is subscribed to`(QUEUED_BUILD_REQUIRES_APPROVAL)
        `given user has role for the project`(projectDevRole)
        `when approvable build is queued`()
        `then message should contain`("is waiting for approval") And
                1.`messages should be sent`()
    }

    @Test
    fun `build feature should send notification about build start`() {
        `given build feature is subscribed to`(BUILD_STARTED)
        val build = `when build starts`()
        `then message should contain`("start", build.buildNumber)
    }


    @Test
    fun `build feature should send notification about build success`() {
        `given build feature is subscribed to`(BUILD_FINISHED_SUCCESS)
        val build = `when build finishes`()
        `then message should contain`("success", build.buildNumber)
    }

    @Test
    fun `build feature should send notification about build failure`() {
        `given build feature is subscribed to`(BUILD_FINISHED_FAILURE)
        val build = `when build fails`()
        `then message should contain`("failed", build.buildNumber)
    }

    @Test
    fun `build feature should send notification about build starting to fail`() {
        `given build feature is subscribed to`(BUILD_FAILING)
        val build = `when build is failing`()
        `then message should contain`("failing", build.buildNumber)
    }

    @Test
    fun `build feature should send notification about build probably hanging`() {
        `given build feature is subscribed to`(BUILD_PROBABLY_HANGING)
        val build = `when build hangs`()
        `then message should contain`("hang", build.buildNumber)
    }

    @Test
    fun `build feature should send notification about build problem`() {
        `given build feature is subscribed to`(NEW_BUILD_PROBLEM_OCCURRED, BUILD_FINISHED_FAILURE)
        val build = `when build problem occurs`()
        `then message should contain`("fail", build.buildNumber)
    }

    @Test
    fun `build feature should send notification about build that requires approval`() {
        `given build feature is subscribed to`(QUEUED_BUILD_REQUIRES_APPROVAL)
        `when approvable build is queued`()
        `then message should contain`("is waiting for approval") And
                1.`messages should be sent`()
    }

    @Test
    fun `notification about manual build start should contain user who triggered it`() {
        `given user is subscribed to`(BUILD_STARTED)
        val build = `when build is triggered manually`()
        `then message should contain`(build.triggeredBy.user!!.descriptiveName)
    }

    @Test
    fun `build feature should send verbose notification about build success`() {
        `given build feature with verbose branch is subscribed to`(BUILD_FINISHED_SUCCESS)
        val build = `when build finishes in master`()
        `then message should contain`("success", build.buildNumber, "master")
    }

    @Test
    fun `build feature should send verbose notification about build failure`() {
        `given build feature with verbose branch is subscribed to`(BUILD_FINISHED_FAILURE)
        val build = `when build fails in master`()
        `then message should contain`("fail", build.buildNumber, "master")
    }

    @Test
    fun `build feature should send build status in verbose notification about build success`() {
        `given build feature with verbose build status is subscribed to`(BUILD_FINISHED_SUCCESS)
        val build = `when build finishes with custom status`()
        `then message should contain`("success", build.buildNumber, "Custom build status")
    }

    @Test
    fun `build feature should send build status in verbose notification about build failure`() {
        `given build feature with verbose build status is subscribed to`(BUILD_FINISHED_FAILURE)
        val build = `when build fails with custom status`()
        `then message should contain`("fail", build.buildNumber, "Custom build status")
    }

    @Test
    fun `build feature should send build status in verbose notification`() {
        `given build feature with verbose build status is subscribed to`(BUILD_FINISHED_SUCCESS)
        val build = `when build finishes`()
        `then message should contain`(build.buildNumber, "Success") And
                `then message should not contain`("Running")
    }

    @Test
    fun `build feature should send changes in verbose notification about build success`() {
        `given build feature with verbose changes is subscribed to`(BUILD_FINISHED_SUCCESS)
        val build = `when build finishes with changes`()
        `then message should contain`("success", build.buildNumber, "committer1", "Commit message")
    }

    @Test
    fun `build feature should send username in verbose notification changes about build success`() {
        `given build feature with verbose changes is subscribed to`(BUILD_FINISHED_SUCCESS)
        `when build finishes with user changes`()
        `then message should contain user descriptive name`()
    }

    @Test
    fun `build feature should send changes in verbose notification about build failure`() {
        `given build feature with verbose changes is subscribed to`(BUILD_FINISHED_FAILURE)
        val build = `when build fails with changes`()
        `then message should contain`("fail", build.buildNumber, "committer1", "Commit message")
    }

    @Test
    fun `build feature should not send changes in verbose notification if no options are provided`() {
        `given build feature with verbose without settings is subscribed to`(BUILD_FINISHED_SUCCESS)
        `when build finishes with changes`()
        `then message should not contain`("committer1", "Commit message")
    }
    @Test
    fun `build feature should send changes in verbose notification for a composite build`() {
        `given build feature in composite build with verbose changes is subscribed to`(BUILD_FINISHED_SUCCESS)
        `when composite build finishes with changes from dependent build`()
        `then message should contain`("committer1", "Commit message")
    }

    @Test
    fun `build feature should not send changes in verbose notification for a composite build without show changes from dependencies option`() {
        `given build feature in composite build with verbose changes that doesnt show changes from dependecies is subscribed to`(BUILD_FINISHED_SUCCESS)
        `when composite build finishes with changes from dependent build`()
        `then message should not contain`("committer1", "Commit message")
    }


    @Test
    fun `notification message should limit number of changes in a message`() {
        `given build feature with 2 max changes is subscribed to`(BUILD_FINISHED_SUCCESS)
        `when build finishes with multiple changes`(4)
        `then message should contain`("View all 4 changes in TeamCity")
    }

    @Test
    fun `change word should be in singular when only one commit is present`() {
        `given build feature with 2 max changes is subscribed to`(BUILD_FINISHED_SUCCESS)
        `when build finishes with multiple changes`(1)
        `then message should contain`("View 1 change in TeamCity")
    }

    @Test
    fun `long change description should be shortened`() {
        `given build feature with 2 max changes is subscribed to`(BUILD_FINISHED_SUCCESS)
        `when build finishes with a long change description`()
        `then message should be short`()
    }

    @Test
    fun `build feature should send notification to the parameterized receiver`() {
        `given build feature has parameterized receiver`()
        `when build finishes`()
        `then message should contain`("success")
    }

    @Test
    fun `service message notification should fail if no connections allow it`() {
        `when service message notification is sent`()
        0.`messages should be sent`()
    }

    @Test
    fun `service message notification should be sent if exactly one connection allows it`() {
        `given there is connection allowing service message notifications`(1)
        `when service message notification is sent`()
        `then message should contain`("service message")
    }

    @Test
    fun `service message notification should fail if more than one connection allow it`() {
        `given there are more multiple connections allowing service message notifications`()
        `when service message notification is sent`()
        0.`messages should be sent`()
    }

    @Test
    fun `service message notification should fail if notification limit is reached`() {
        `given there is connection allowing service message notifications`(1)
        `when multiple service message notifications are sent`(count = 2)
        1.`messages should be sent`()
    }

    @Test
    fun `service message notification should honor -1 notification limit`() {
        `given there is connection allowing service message notifications`(-1)
        `when service message notification is sent`()
        `then message should contain`("service message")
    }

    @Test
    fun `service message notification should fail if there are external domains not in whitelist`() {
        `given there is connection allowing service message notifications`(1)
        `when service message notification is sent`(message = "link to externaldomain.com")
        0.`messages should be sent`()
    }

    @Test
    fun `service message notification should be sent if there are whitelisted external domains`() {
        `given there is an allowed external domain`(1, setOf("externaldomain.com"))
        `when service message notification is sent`(message = "link to externaldomain.com")
        `then message should contain`("externaldomain.com")
    }
}