package jetbrains.buildServer.notification

import jetbrains.buildServer.notification.slack.SlackWebApi
import jetbrains.buildServer.notification.slack.SlackWebApiFactory

class MockSlackWebApiFactory : SlackWebApiFactory {
    private val slackWebApi = MockSlackWebApi()
    override fun createSlackWebApi(): MockSlackWebApi {
        return slackWebApi
    }
}