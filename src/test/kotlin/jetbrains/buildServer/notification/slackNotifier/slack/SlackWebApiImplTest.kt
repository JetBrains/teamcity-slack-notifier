package jetbrains.buildServer.notification.slackNotifier.slack

import jetbrains.buildServer.BaseTestCase
import org.testng.annotations.Test

class SlackWebApiImplTest : BaseTestCase() {
    private val standardResponse = "{" +
            "\"ok\": true," +
            "\"channel\": \"C1H9RESGL\"," +
            "\"ts\": \"1503435956.000247\"" +
            "}"

    private lateinit var slackApi: SlackWebApiImpl
    private lateinit var slackResponse: MaybeMessage

    @Test
    fun `should post message`() {
        `given slack is responding correctly`()
        `when message is sent`()
        `then result should be successful`()
    }

    @Test
    fun `should retry request if first fails`() {
        `given slack fails on first request`()
        `when message is sent`()
        `then result should be successful`()
    }

    private fun `given slack is responding correctly`() {
        slackApi = SlackWebApiImpl(
                MockRequestHandler(standardResponse)
        )
    }

    private fun `given slack fails on first request`() {
        slackApi = SlackWebApiImpl(
                FailingFirstRequestMockHandler(standardResponse)
        )
    }

    private fun `when message is sent`() {
        slackResponse = slackApi.postMessage(slackToken, Message("#test_channel", "Test message"))
    }

    private fun `then result should be successful`() {
        assertTrue(slackResponse.ok)
    }
}

