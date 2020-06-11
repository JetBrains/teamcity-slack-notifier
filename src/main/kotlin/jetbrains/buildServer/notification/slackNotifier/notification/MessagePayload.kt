package jetbrains.buildServer.notification.slackNotifier.notification

import jetbrains.buildServer.notification.slackNotifier.slack.Message
import jetbrains.buildServer.notification.slackNotifier.slack.MessageAttachment

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

class MessagePayloadBuilder {
    private val text = StringBuilder()
    fun add(str: String): MessagePayloadBuilder = apply {
        text.append(str)
    }

    fun add(messagePayload: MessagePayload): MessagePayloadBuilder = apply {
        text.append(messagePayload.text)
    }

    fun newline(): MessagePayloadBuilder = apply {
        add("\n")
    }

    fun tabs(n: Int): MessagePayloadBuilder = apply {
        add("\t".repeat(n))
    }

    fun build(): MessagePayload {
        return MessagePayload(text = text.toString())
    }
}

fun messagePayload(block: MessagePayloadBuilder.() -> Unit): MessagePayload {
    val builder = MessagePayloadBuilder()
    builder.block()
    return builder.build()
}
