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
import jetbrains.buildServer.notification.NotificationBuildStatusProvider
import jetbrains.buildServer.notification.TemplateMessageBuilder
import jetbrains.buildServer.notification.slackNotifier.slack.SlackMessageFormatter
import jetbrains.buildServer.serverSide.*
import jetbrains.buildServer.vcs.SelectPrevBuildPolicy
import jetbrains.buildServer.vcs.VcsRoot
import java.text.SimpleDateFormat

class VerboseMessageBuilder(
        private val messageBuilder: MessageBuilder,
        private val verboseMessagesOptions: VerboseMessagesOptions,
        private val format: SlackMessageFormatter,
        private val webLinks: RelativeWebLinks,
        private val notificationBuildStatusProvider: NotificationBuildStatusProvider,
        private val server: SBuildServer
) : MessageBuilder by messageBuilder {
    private val changeDateFormat = SimpleDateFormat("d MMM HH∶mm")

    override fun buildStarted(build: SRunningBuild): MessagePayload = messagePayload {
        text {
            add(messageBuilder.buildStarted(build))
            addBranch(build)
            addChanges(build)
        }
    }

    override fun buildSuccessful(build: SRunningBuild): MessagePayload = messagePayload {
        text {
            add(messageBuilder.buildSuccessful(build))
            addVerboseInfo(build, BuildEvent.BUILD_SUCCESSFUL)
        }
    }

    private fun MessagePayloadBlockBuilder.addVerboseInfo(build: Build, buildEvent: BuildEvent) {
        val finishedBuild = server.findBuildInstanceById(build.buildId) ?: build

        addBranch(finishedBuild)
        addBuildStatus(finishedBuild, buildEvent)
        addChanges(finishedBuild)
    }

    private fun MessagePayloadBlockBuilder.addBuildStatus(build: Build, buildEvent: BuildEvent) {
        if (verboseMessagesOptions.addBuildStatus && build is SBuild) {
            val buildStatistics: BuildStatistics = build.getBuildStatistics(
                    BuildStatisticsOptions(
                            BuildStatisticsOptions.COMPILATION_ERRORS,
                            TemplateMessageBuilder.MAX_NUM_OF_STACKTRACES
                    )
            )

            newline()
            add("${buildEvent.emoji} ${notificationBuildStatusProvider.getText(build, buildStatistics)}")
        }
    }

    private fun MessagePayloadBlockBuilder.addBranch(build: Build) {
        if (!verboseMessagesOptions.addBranch) {
            return
        }

        if (build is SBuild) {
            build.branch?.displayName?.let { branch ->
                add(" in branch \"$branch\"")
            }
        }
    }

    private fun MessagePayloadBlockBuilder.addChanges(build: Build) {
        if (!verboseMessagesOptions.addChanges) {
            return
        }

        val changes = build.getChanges(SelectPrevBuildPolicy.SINCE_LAST_BUILD, true)
        if (changes.isEmpty()) {
            return
        }

        val firstChanges = changes.take(verboseMessagesOptions.maximumNumberOfChanges).toList()

        newline()
        newline()
        add(format.bold("Changes"))
        for (change in firstChanges) {
            newline()
            newline()
            val changeDescription = shorten(change.description.trim())
            val date = changeDateFormat.format(change.vcsDate)
            val username = change.userName
            val prefix = if (username != null) {
                "${format.bold(username)} at $date"
            } else {
                "At $date"
            }

            // Use different colon symbol here since the date is in format hh:mm
            // and that could lead to Slack interpreting message like '...19:42: ...'
            // as :42: meaning emoji
            add("${prefix}∶ $changeDescription")
        }

        newline()

        val text = if (changes.size == 1) {
            "View 1 change in TeamCity"
        } else {
            "View all ${changes.size} changes in TeamCity"
        }

        val changesUrl = webLinks.getViewChangesUrl(build)
        add(format.url(url = changesUrl, text = text))
    }

    private fun shorten(text: String, maximumLength: Int = 80): String {
        val firstLine = text.splitToSequence("\n").firstOrNull() ?: return ""
        val postfix = "..."
        val maximumLengthWithPostfix = maximumLength - postfix.length
        return if (firstLine.length > maximumLengthWithPostfix) {
            firstLine.substring(0, maximumLengthWithPostfix) + postfix
        } else {
            firstLine
        }
    }

    override fun buildFailed(build: SRunningBuild): MessagePayload = messagePayload {
        text  {
            add(messageBuilder.buildFailed(build))
            addVerboseInfo(build, BuildEvent.BUILD_FAILED)
        }
    }

    override fun buildFailedToStart(build: SRunningBuild): MessagePayload = messagePayload {
        text {
            add(messageBuilder.buildFailedToStart(build))
            addVerboseInfo(build, BuildEvent.BUILD_FAILED_TO_START)
        }
    }

    override fun labelingFailed(build: Build, root: VcsRoot, exception: Throwable): MessagePayload = messagePayload {
        text {
            add(messageBuilder.labelingFailed(build, root, exception))
            addVerboseInfo(build, BuildEvent.LABELING_FAILED)
        }
    }

    override fun buildFailing(build: SRunningBuild): MessagePayload = messagePayload {
        text {
            add(messageBuilder.buildFailing(build))
            addVerboseInfo(build, BuildEvent.BUILD_FAILING)
        }
    }

    override fun buildProbablyHanging(build: SRunningBuild): MessagePayload = messagePayload {
        text {
            add(messageBuilder.buildProbablyHanging(build))
            addVerboseInfo(build, BuildEvent.BUILD_PROBABLY_HANGING)
        }
    }
}