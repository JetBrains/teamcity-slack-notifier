

package jetbrains.buildServer.notification.slackNotifier.slack

class StoringMessagesSlackWebApiImpl(
    private val api: SlackWebApi
) : SlackWebApi by api, StoringMessagesSlackWebApi {
    override val messages = mutableListOf<Message>()

    override fun postMessage(token: String, payload: Message): MaybeMessage {
        val result = api.postMessage(token, payload)
        if (result.ok) {
            messages.add(payload)
        }
        return result
    }
}