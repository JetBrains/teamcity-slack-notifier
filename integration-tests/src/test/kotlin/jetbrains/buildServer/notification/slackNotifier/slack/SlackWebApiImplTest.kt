

package jetbrains.buildServer.notification.slackNotifier.slack

import jetbrains.buildServer.serverSide.impl.BaseServerTestCase
import jetbrains.buildServer.util.TestFor
import jetbrains.buildServer.util.ssl.SSLTrustStoreProvider
import org.testng.annotations.Test
import java.util.concurrent.TimeoutException

class SlackWebApiImplTest : BaseServerTestCase() {
    private val standardResponse = "{" +
            "\"ok\": true," +
            "\"channel\": \"C1H9RESGL\"," +
            "\"ts\": \"1503435956.000247\"" +
            "}"

    private lateinit var slackApi: SlackWebApi
    private lateinit var slackResponse: MaybeError
    private lateinit var aggregatedSlackApi: AggregatedSlackApi

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

    @Test
    fun `should return error if request always fails`() {
        `given slack always fails`()
        `when message is sent`()
        `then result should be error`()
    }

    @Test(timeOut = 10_000)
    @TestFor(issues = ["TW-84904"])
    fun `should not repeat timeout calls`() {
        `given slack api is caching and times out`()
        `when userinfo is called multiple times`()
        `then result should be error`()
    }

    @Test(timeOut = 10_000)
    @TestFor(issues = ["TW-84904"])
    fun `should not repeat timeout aggregating calls`() {
        `given slack api is aggregating and times out`()
        `then multiple same aggregating requests are not hanging`()
    }


    private fun `given slack is responding correctly`() {
        slackApi = SlackWebApiImpl(
            RequestHandlerStub(standardResponse),
            SSLTrustStoreProvider { null }
        )
    }

    private fun `given slack fails on first request`() {
        slackApi = SlackWebApiImpl(
            FailingFirstRequestHandler(standardResponse),
            SSLTrustStoreProvider { null }
        )
    }

    private fun `given slack api is caching and times out`() {
        slackApi = CachingSlackWebApi(
            SlackWebApiImpl(
                TimeoutRequestHandler(standardResponse),
                SSLTrustStoreProvider { null }
            ),
            myFixture.executorServices
        )
    }

    private fun `given slack api is aggregating and times out`() {
        aggregatedSlackApi = AggregatedSlackApi(
            object : SlackWebApiFactory {
                override fun createSlackWebApi(): SlackWebApi {
                    return SlackWebApiImpl(
                        TimeoutRequestHandler(standardResponse),
                        SSLTrustStoreProvider { null }
                    )
                }
            },
            myFixture.executorServices
        )
    }

    private fun `given slack always fails`() {
        slackApi = SlackWebApiImpl(
            AlwaysFailingRequestHandler(),
            SSLTrustStoreProvider { null }
        )
    }

    private fun `when message is sent`() {
        slackResponse = slackApi.postMessage(slackToken, Message("#test_channel", "Test message"))
    }

    private fun `when userinfo is called multiple times`() {
        repeat(10) { slackResponse = slackApi.usersInfo(slackToken, "TestUserId") }
    }

    private fun `then multiple same aggregating requests are not hanging`() {
        repeat(10) {
            try {
                aggregatedSlackApi.getChannelsList(slackToken)
            } catch (e: SlackResponseError) {
                // timeout expected
            }
        }
    }

    private fun `then result should be successful`() {
        assertTrue(slackResponse.ok)
    }

    private fun `then result should be error`() {
        assertFalse(slackResponse.ok)
    }
}