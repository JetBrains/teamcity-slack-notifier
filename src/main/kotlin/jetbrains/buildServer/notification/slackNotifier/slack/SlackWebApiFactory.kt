

package jetbrains.buildServer.notification.slackNotifier.slack

interface SlackWebApiFactory {
    fun createSlackWebApi(): SlackWebApi
}