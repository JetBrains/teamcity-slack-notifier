/*
 *  Copyright 2000-2022 JetBrains s.r.o.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package jetbrains.buildServer.notification.slackNotifier.notification


import jetbrains.buildServer.notification.NotificationRule.Event.*
import jetbrains.buildServer.notification.slackNotifier.And
import jetbrains.buildServer.notification.slackNotifier.BaseSlackTestCase
import jetbrains.buildServer.notification.slackNotifier.SlackProperties
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
}