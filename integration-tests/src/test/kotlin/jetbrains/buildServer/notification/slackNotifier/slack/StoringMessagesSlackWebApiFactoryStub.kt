

package jetbrains.buildServer.notification.slackNotifier.slack

interface StoringMessagesSlackWebApiFactory : SlackWebApiFactory {
    override fun createSlackWebApi(): StoringMessagesSlackWebApi
}

class StoringMessagesSlackWebApiFactoryStub : StoringMessagesSlackWebApiFactory {
    private val slackWebApi = StoringMessagesSlackWebApiImpl(SlackWebApiStub())
    override fun createSlackWebApi(): StoringMessagesSlackWebApi {
        return slackWebApi
    }
}