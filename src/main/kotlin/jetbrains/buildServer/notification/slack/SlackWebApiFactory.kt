package jetbrains.buildServer.notification.slack

interface SlackWebApiFactory {
    fun createSlackWebApi(): SlackWebApi
}