

package jetbrains.buildServer.notification.slackNotifier.notification

import jetbrains.buildServer.BaseTestCase
import jetbrains.buildServer.notification.slackNotifier.slack.SlackMessageFormatter
import org.testng.annotations.Test

class SlackMessageFormatterTest : BaseTestCase() {
    private val format = SlackMessageFormatter()

    @Test
    fun `should escape prohibited symbols`() {
        assertEquals("&lt;build &amp; configuration&gt;", format.escape("<build & configuration>"))
    }
}