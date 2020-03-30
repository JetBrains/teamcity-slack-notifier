package jetbrains.buildServer.notification.slackNotifier.slack

interface SlackWebApi {
    // https://api.slack.com/methods/chat.postMessage
    fun postMessage(token: String, payload: Message): MaybeMessage

    // https://api.slack.com/methods/channels.list
    fun channelsList(token: String, cursor: String? = null): ChannelsList

    // https://api.slack.com/methods/users.list
    fun usersList(token: String, cursor: String? = null): UsersList
}

