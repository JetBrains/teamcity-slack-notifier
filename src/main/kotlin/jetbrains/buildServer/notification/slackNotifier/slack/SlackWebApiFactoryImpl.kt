

package jetbrains.buildServer.notification.slackNotifier.slack

import jetbrains.buildServer.util.HTTPRequestBuilder
import jetbrains.buildServer.util.ssl.SSLTrustStoreProvider
import org.springframework.stereotype.Service

@Service
class SlackWebApiFactoryImpl(
    private val requestHandler: HTTPRequestBuilder.RequestHandler,
    private val sslTrustStoreProvider: SSLTrustStoreProvider
) : SlackWebApiFactory {
    override fun createSlackWebApi(): SlackWebApi {
        return SlackWebApiImpl(requestHandler, sslTrustStoreProvider)
    }
}