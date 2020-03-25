package jetbrains.buildServer.notification.slackNotifier.slack

interface SlackWebApi {
    // https://api.slack.com/methods/chat.postMessage
    fun postMessage(token: String, payload: Message): MaybeMessage
}

