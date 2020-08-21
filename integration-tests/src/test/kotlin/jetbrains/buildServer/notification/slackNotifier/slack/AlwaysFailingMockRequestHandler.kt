package jetbrains.buildServer.notification.slackNotifier.slack

import jetbrains.buildServer.util.HTTPRequestBuilder
import java.util.concurrent.TimeoutException

class AlwaysFailingMockRequestHandler : HTTPRequestBuilder.RequestHandler {
    override fun doRequest(request: HTTPRequestBuilder.Request) {
        for (i in 0..request.retryCount) {
            request.onException.accept(TimeoutException("Slack request timeout"))
        }
    }
}