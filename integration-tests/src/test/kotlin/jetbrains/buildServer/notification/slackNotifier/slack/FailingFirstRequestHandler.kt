

package jetbrains.buildServer.notification.slackNotifier.slack

import jetbrains.buildServer.util.HTTPRequestBuilder
import jetbrains.buildServer.util.http.AsyncRequest
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeoutException

class FailingFirstRequestHandler(
        private val response: String
) : HTTPRequestBuilder.RequestHandler {
    @Deprecated("Deprecated in Java")
    override fun doRequest(request: HTTPRequestBuilder.Request) {
        request.onException.accept(TimeoutException("Request to Slack timeout"), request)
        request.onSuccess.consume(ResponseMock(200, response))
    }

    override fun doAsyncRequest(p0: AsyncRequest): CompletableFuture<HTTPRequestBuilder.Response> {
        TODO("Not yet implemented")
    }

    override fun doSyncRequest(p0: HTTPRequestBuilder.Request): HTTPRequestBuilder.Response {
        TODO("Not yet implemented")
    }
}