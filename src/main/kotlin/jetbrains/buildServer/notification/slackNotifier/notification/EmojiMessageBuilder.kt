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
        return addEmoji(message, BuildEvent.BUILD_SUCCESSFUL)
    }

    private fun addEmoji(message: MessagePayload, buildEvent: BuildEvent): MessagePayload {
        return message.copy(text = "${buildEvent.emoji} ${message.text}")
    }

    override fun buildFailed(build: SRunningBuild): MessagePayload {
        val message = messageBuilder.buildFailed(build)
        return addEmoji(message, BuildEvent.BUILD_FAILED)
    }

    override fun buildFailedToStart(build: SRunningBuild): MessagePayload {
        val message = messageBuilder.buildFailedToStart(build)
        return addEmoji(message, BuildEvent.BUILD_FAILED_TO_START)
    }

    override fun labelingFailed(build: Build, root: VcsRoot, exception: Throwable): MessagePayload {
        val message = messageBuilder.labelingFailed(build, root, exception)
        return addEmoji(message, BuildEvent.LABELING_FAILED)
    }

    override fun buildFailing(build: SRunningBuild): MessagePayload {
        val message = messageBuilder.buildFailing(build)
        return addEmoji(message, BuildEvent.BUILD_FAILING)
    }

    override fun buildProbablyHanging(build: SRunningBuild): MessagePayload {
        val message = messageBuilder.buildProbablyHanging(build)
        return addEmoji(message, BuildEvent.BUILD_PROBABLY_HANGING)
    }
}