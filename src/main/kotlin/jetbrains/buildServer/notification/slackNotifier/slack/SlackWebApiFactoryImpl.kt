package jetbrains.buildServer.notification.slackNotifier.slack

import jetbrains.buildServer.util.HTTPRequestBuilder
import org.springframework.stereotype.Service

@Service
class SlackWebApiFactoryImpl(
    private val requestHandler: HTTPRequestBuilder.RequestHandler
) : SlackWebApiFactory {
    override fun createSlackWebApi(): SlackWebApi {
        return SlackWebApiImpl(requestHandler)
    }
}