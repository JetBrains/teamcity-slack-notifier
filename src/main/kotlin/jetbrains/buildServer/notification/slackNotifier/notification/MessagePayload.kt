/*
 *  Copyright 2000-2021 JetBrains s.r.o.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package jetbrains.buildServer.notification.slackNotifier.notification

import jetbrains.buildServer.notification.slackNotifier.slack.*
import java.lang.IllegalStateException

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

class MessagePayloadBlockBuilder {
    private val text = StringBuilder()

    fun add(str: String): MessagePayloadBlockBuilder = apply {
        text.append(str)
    }

    fun add(messagePayload: MessagePayload): MessagePayloadBlockBuilder = apply {
        text.append(messagePayload.text)
    }

    fun newline(): MessagePayloadBlockBuilder = apply {
        add("\n")
    }

    fun build(): TextBlock {
        return TextBlock(text = MessageBlockText(type = "mrkdwn", text = text.toString()))
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
