package jetbrains.buildServer.notification.slackNotifier.slack

import jetbrains.buildServer.notification.slackNotifier.slack.MockSlackWebApi
import jetbrains.buildServer.notification.slackNotifier.slack.SlackWebApiFactory

class MockSlackWebApiFactory : SlackWebApiFactory {
    private val slackWebApi = MockSlackWebApi()
    override fun createSlackWebApi(): MockSlackWebApi {
        return slackWebApi
    }
}