package jetbrains.buildServer.notification.slackNotifier.notification

import jetbrains.buildServer.Build
import jetbrains.buildServer.serverSide.*
import jetbrains.buildServer.vcs.VcsRoot

/**
 * Adds emojis to the start of notification messages
 */
class EmojiMessageBuilder(
        private val messageBuilder: MessageBuilder
) : MessageBuilder by messageBuilder {
    override fun buildSuccessful(build: SRunningBuild): MessagePayload {
        val message = messageBuilder.buildSuccessful(build)
        return addEmoji(message, BuildEventEmojis.buildSuccessful)
    }

    private fun addEmoji(message: MessagePayload, emoji: String): MessagePayload {
        return message.copy(text = "$emoji ${message.text}")
    }

    override fun buildFailed(build: SRunningBuild): MessagePayload {
        val message = messageBuilder.buildFailed(build)
        return addEmoji(message, BuildEventEmojis.buildFailed)
    }

    override fun buildFailedToStart(build: SRunningBuild): MessagePayload {
        val message = messageBuilder.buildFailedToStart(build)
        return addEmoji(message, BuildEventEmojis.buildFailedToStart)
    }

    override fun labelingFailed(build: Build, root: VcsRoot, exception: Throwable): MessagePayload {
        val message = messageBuilder.labelingFailed(build, root, exception)
        return addEmoji(message, BuildEventEmojis.labelingFailed)
    }

    override fun buildFailing(build: SRunningBuild): MessagePayload {
        val message = messageBuilder.buildFailing(build)
        return addEmoji(message, BuildEventEmojis.buildFailing)
    }

    override fun buildProbablyHanging(build: SRunningBuild): MessagePayload {
        val message = messageBuilder.buildProbablyHanging(build)
        return addEmoji(message, BuildEventEmojis.buildProbablyHanging)
    }
}