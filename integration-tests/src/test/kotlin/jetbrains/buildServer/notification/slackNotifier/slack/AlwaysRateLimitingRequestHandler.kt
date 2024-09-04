package jetbrains.buildServer.notification.slackNotifier.slack

import jetbrains.buildServer.util.HTTPRequestBuilder
import jetbrains.buildServer.util.http.AsyncRequest
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeoutException

class AlwaysRateLimitingRequestHandler : HTTPRequestBuilder.RequestHandler {
    @Deprecated("Deprecated in Java")
    override fun doRequest(request: HTTPRequestBuilder.Request) {
        for (i in 0..request.retryCount) {
            request.onError.consume(ResponseMock(429, "{\"ok\":false,\"error\":\"ratelimited\"}"))
        }
    }

    override fun doAsyncRequest(p0: AsyncRequest): CompletableFuture<HTTPRequestBuilder.Response> {
        TODO("Not yet implemented")
    }

    override fun doSyncRequest(p0: HTTPRequestBuilder.Request): HTTPRequestBuilder.Response {
        TODO("Not yet implemented")
    }
}