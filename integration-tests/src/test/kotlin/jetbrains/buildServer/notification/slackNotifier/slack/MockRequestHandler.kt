package jetbrains.buildServer.notification.slackNotifier.slack

import jetbrains.buildServer.util.HTTPRequestBuilder
import jetbrains.buildServer.util.http.HTTPResponseMock

class MockRequestHandler(
        private val response: String
) : HTTPRequestBuilder.RequestHandler {
    override fun doRequest(request: HTTPRequestBuilder.Request) {
        request.onSuccess.consume(HTTPResponseMock(200, response))
    }
}