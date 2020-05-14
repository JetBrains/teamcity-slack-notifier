package jetbrains.buildServer.notification.slackNotifier.slack

import jetbrains.buildServer.util.HTTPRequestBuilder
import jetbrains.buildServer.util.http.HTTPResponseMock
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicInteger

class FailingFirstRequestMockHandler(
        private val response: String
) : HTTPRequestBuilder.RequestHandler {
    private var numberOfCalls = AtomicInteger()
    override fun doRequest(request: HTTPRequestBuilder.Request) {
        val callNumber = numberOfCalls.getAndIncrement()
        if (callNumber == 0) {
            request.onException.accept(TimeoutException("Request to Slack timeout"))
        }
        request.onSuccess.consume(HTTPResponseMock(200, response))
    }
}