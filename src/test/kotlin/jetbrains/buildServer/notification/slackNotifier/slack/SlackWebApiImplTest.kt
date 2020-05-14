package jetbrains.buildServer.notification.slackNotifier.slack

import jetbrains.buildServer.BaseTestCase
import org.testng.annotations.Test

class SlackWebApiImplTest : BaseTestCase() {
    @Test
    fun `should post message`() {
        val slackApi = SlackWebApiImpl(
                MockRequestHandler("{" +
                        "\"ok\": true," +
                        "\"channel\": \"C1H9RESGL\"," +
                        "\"ts\": \"1503435956.000247\"" +
                        "}"
                )
        )

        val result = slackApi.postMessage(slackToken, Message("#test_channel", "Test message"))
        assertTrue(result.ok)
    }
}