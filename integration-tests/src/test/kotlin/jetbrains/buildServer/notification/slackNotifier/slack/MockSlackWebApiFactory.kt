package jetbrains.buildServer.notification.slackNotifier.slack

class MockSlackWebApiFactory : SlackWebApiFactory {
    private val slackWebApi = MockSlackWebApi()
    override fun createSlackWebApi(): MockSlackWebApi {
        return slackWebApi
    }
}