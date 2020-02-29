package jetbrains.buildServer.slackNotifications

import jetbrains.buildServer.slackNotifications.slack.Message
import jetbrains.buildServer.slackNotifications.slack.MessageAttachment

data class MessagePayload(
    val text: String,
    val userName: String? = null,
    val parse: String? = null,
    val attachments: List<MessageAttachment> = listOf(),
    val markdown: Boolean = false
) {
    fun toSlackMessage(channel: String): Message = Message(
        channel = channel,
        text = text,
        userName = userName,
        parse = parse,
        attachments = attachments,
        markdown = markdown
    )
}
