

package jetbrains.buildServer.notification.slackNotifier.slack

interface StoringMessagesSlackWebApi : SlackWebApi {
    val messages: List<Message>
}