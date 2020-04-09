package jetbrains.buildServer.notification.slackNotifier.slack

interface SlackWebApi {
    // https://api.slack.com/methods/chat.postMessage
    fun postMessage(token: String, payload: Message): MaybeMessage

    // https://api.slack.com/methods/channels.list
    fun channelsList(token: String, cursor: String? = null): ChannelsList

    // https://api.slack.com/methods/users.list
    fun usersList(token: String, cursor: String? = null): UsersList

    // https://api.slack.com/methods/auth.test
    fun authTest(token: String): AuthTestResult

    // https://api.slack.com/methods/bots.info
    fun botsInfo(token: String, botId: String): MaybeBot

    // https://api.slack.com/methods/conversations.members
    fun conversationsMembers(token: String, channelId: String): ConversationMembers

    // https://api.slack.com/methods/oauth.access
    fun oauthAccess(clientId: String, clientSecret: String, code: String, redirectUrl: String): OauthAccessToken

    fun usersIdentity(token: String): UserIdentity
}

