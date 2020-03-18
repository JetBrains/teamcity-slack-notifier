package jetbrains.buildServer.notification


import org.testng.annotations.Test

class SlackNotifierTest : BaseSlackTestCase() {
    @Test
    fun `test should send notification about build start`() {
        storeRules(myUser, myNotifier, newRule(NotificationRule.Event.BUILD_STARTED))

        val build = startBuild()

        assertLastMessageContains("start")
        assertLastMessageContains(build.buildNumber)
    }

    @Test
    fun `test should send notification about build success`() {
        storeRules(myUser, myNotifier, newRule(NotificationRule.Event.BUILD_FINISHED_SUCCESS))

        startBuild()
        val build = finishBuild()

        assertLastMessageContains("success")
        assertLastMessageContains(build.buildNumber)
    }
}