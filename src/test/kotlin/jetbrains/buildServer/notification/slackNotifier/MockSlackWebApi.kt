package jetbrains.buildServer.notification.slackNotifier

import jetbrains.buildServer.notification.slackNotifier.slack.MaybeMessage
import jetbrains.buildServer.notification.slackNotifier.slack.Message
import jetbrains.buildServer.notification.slackNotifier.slack.SlackWebApi

class MockSlackWebApi : SlackWebApi {
    val messages = mutableListOf<Message>()

    override fun postMessage(token: String, payload: Message): MaybeMessage {
        messages.add(payload)
        return MaybeMessage(ok = true)

    }
}