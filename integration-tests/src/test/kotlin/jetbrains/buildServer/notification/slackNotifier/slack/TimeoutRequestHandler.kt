

package jetbrains.buildServer.notification.slackNotifier.slack

import jetbrains.buildServer.util.HTTPRequestBuilder
import jetbrains.buildServer.util.http.AsyncRequest
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeoutException

class TimeoutRequestHandler(
    private val response: String
) : HTTPRequestBuilder.RequestHandler {
    @Deprecated("Deprecated in Java")
    override fun doRequest(request: HTTPRequestBuilder.Request) {
        Thread.sleep(3_000)
        request.onException.accept(TimeoutException("Request to Slack timeout"), request)
    }

    override fun doAsyncRequest(p0: AsyncRequest): CompletableFuture<HTTPRequestBuilder.Response> {
        TODO("Not yet implemented")
    }

    override fun doSyncRequest(p0: HTTPRequestBuilder.Request): HTTPRequestBuilder.Response {
        TODO("Not yet implemented")
    }
}