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
        return message.copy(text = ":heavy_check_mark: ${message.text}")
    }

    override fun buildFailed(build: SRunningBuild): MessagePayload {
        val message = messageBuilder.buildFailed(build)
        return message.copy(text = ":x: ${message.text}")
    }

    override fun buildFailedToStart(build: SRunningBuild): MessagePayload {
        val message = messageBuilder.buildFailedToStart(build)
        return message.copy(text = ":exclamation: ${message.text}")
    }

    override fun labelingFailed(build: Build, root: VcsRoot, exception: Throwable): MessagePayload {
        val message = messageBuilder.labelingFailed(build, root, exception)
        return message.copy(text = ":x: ${message.text}")
    }

    override fun buildFailing(build: SRunningBuild): MessagePayload {
        val message = messageBuilder.buildFailing(build)
        return message.copy(text = ":x: ${message.text}")
    }

    override fun buildProbablyHanging(build: SRunningBuild): MessagePayload {
        val message = messageBuilder.buildProbablyHanging(build)
        return message.copy(text = ":warning: ${message.text}")
    }
}