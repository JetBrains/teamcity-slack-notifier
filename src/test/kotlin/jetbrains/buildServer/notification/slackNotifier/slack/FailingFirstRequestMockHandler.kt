package jetbrains.buildServer.notification.slackNotifier.slack

import jetbrains.buildServer.util.HTTPRequestBuilder
import jetbrains.buildServer.util.http.HTTPResponseMock
import java.util.concurrent.TimeoutException

class FailingFirstRequestMockHandler(
        private val response: String
) : HTTPRequestBuilder.RequestHandler {
    override fun doRequest(request: HTTPRequestBuilder.Request) {
        request.onException.accept(TimeoutException("Request to Slack timeout"))
        request.onSuccess.consume(HTTPResponseMock(200, response))
    }
}