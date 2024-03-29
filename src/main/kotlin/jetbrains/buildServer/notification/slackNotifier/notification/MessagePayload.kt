

@file:Suppress("unused")

package jetbrains.buildServer.notification.slackNotifier.notification

import jetbrains.buildServer.log.Loggers
import jetbrains.buildServer.notification.slackNotifier.slack.*

data class MessagePayload(
    val text: String,
    val userName: String? = null,
    val parse: String? = null,
    val attachments: List<MessageAttachment> = emptyList(),
    val blocks: List<MessageBlock> = emptyList(),
    val markdown: Boolean = false
) {
    fun toSlackMessage(channel: String): Message = Message(
        channel = channel,
        text = text,
        userName = userName,
        parse = parse,
        attachments = attachments,
        markdown = markdown,
        blocks = blocks
    )
}

class MessagePayloadBuilder {
    private val messageText = StringBuilder()
    private val blocks = mutableListOf<MessageBlock>()

    fun text(block: MessagePayloadBlockBuilder.() -> Unit) {
        val builder = MessagePayloadBlockBuilder()
        builder.block()
        messageText.append(builder.build().text.text)
    }

    fun textBlock(block: MessagePayloadBlockBuilder.() -> Unit) {
        val builder = MessagePayloadBlockBuilder()
        builder.block()
        blocks.add(builder.build())
    }

    fun actionsBlock(block: MessagePayloadActionsBuilder.() -> Unit) {
        val builder = MessagePayloadActionsBuilder()
        builder.block()
        blocks.add(builder.build())
    }

    fun contextBlock(block: MessagePayloadContextBuilder.() -> Unit) {
        val builder = MessagePayloadContextBuilder()
        builder.block()
        blocks.addAll(builder.build())
    }

    fun build(): MessagePayload {
        val text = messageText.toString()
        if (text.isNotEmpty() && blocks.isNotEmpty()) {
            throw IllegalStateException("Can't send message with both text and blocks")
        }
        if (text.isNotEmpty()) {
            return MessagePayload(text = text)
        }

        return MessagePayload(text = getTextFromBlocks(), blocks = blocks)
    }

    private fun getTextFromBlocks(): String {
        return blocks.joinToString("\n") {
            if (it is TextBlock) {
                it.text.text
            } else {
                ""
            }
        }.trim()
    }
}

fun messagePayload(block: MessagePayloadBuilder.() -> Unit): MessagePayload {
    val builder = MessagePayloadBuilder()
    builder.block()
    return builder.build()
}

interface MessagePayloadTextBuilder<T: MessagePayloadTextBuilder<T>> {
    fun add(text: String): T
    fun newline(): T
}

class MessagePayloadBlockBuilder: MessagePayloadTextBuilder<MessagePayloadBlockBuilder> {
    private val textBuilder = StringBuilder()

    override fun add(text: String): MessagePayloadBlockBuilder = apply {
        textBuilder.append(text)
    }

    fun add(messagePayload: MessagePayload): MessagePayloadBlockBuilder = apply {
        textBuilder.append(messagePayload.text)
    }

    override fun newline(): MessagePayloadBlockBuilder = apply {
        add("\n")
    }

    fun build(): TextBlock {
        return TextBlock(text = MessageBlockText(type = "mrkdwn", text = textBuilder.toString()))
    }
}

class MessagePayloadActionsBuilder {
    private val actions = mutableListOf<MessageAction>()
    fun button(text: String, url: String) {
        actions.add(MessageAction(type = "button", url = url, text = MessageActionText(type = "plain_text", text = text)))
    }

    fun build(): MessageActions {
        return MessageActions(elements = actions)
    }
}

class MessagePayloadContextBuilder: MessagePayloadTextBuilder<MessagePayloadContextBuilder> {
    private val blocks = mutableListOf<ContextBlock>()

    override fun add(text: String): MessagePayloadContextBuilder {
        blocks.add(ContextBlock(elements = listOf(ContextBlockElement(text = text))))
        return this
    }

    override fun newline(): MessagePayloadContextBuilder {
        return this
    }

    fun build(): List<ContextBlock> {
        return blocks
    }
}