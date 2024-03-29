

package jetbrains.buildServer.notification.slackNotifier.notification

import jetbrains.buildServer.Build
import jetbrains.buildServer.notification.NotificationBuildStatusProvider
import jetbrains.buildServer.notification.TemplateMessageBuilder
import jetbrains.buildServer.notification.slackNotifier.slack.SlackMessageFormatter
import jetbrains.buildServer.notification.slackNotifier.teamcity.getChanges
import jetbrains.buildServer.serverSide.*
import jetbrains.buildServer.vcs.SVcsModification
import jetbrains.buildServer.vcs.VcsModification
import jetbrains.buildServer.vcs.VcsRoot

class NewVerboseMessageBuilder(
        private val messageBuilder: MessageBuilder,
        private val verboseMessagesOptions: VerboseMessagesOptions,
        private val format: SlackMessageFormatter,
        private val webLinks: RelativeWebLinks,
        private val notificationBuildStatusProvider: NotificationBuildStatusProvider,
        private val server: SBuildServer,
        private val changesCalculationOptionsFactory: ChangesCalculationOptionsFactory
) : MessageBuilder by messageBuilder {
    override fun buildStarted(build: SRunningBuild): MessagePayload = messagePayload {
        textBlock {
            add(messageBuilder.buildStarted(build))
        }

        contextBlock {
            addBranch(build)
            addChanges(build)
        }
    }

    override fun buildSuccessful(build: SRunningBuild): MessagePayload = messagePayload {
        textBlock {
            add(messageBuilder.buildSuccessful(build))
        }

        contextBlock {
            addVerboseInfo(build)
        }
    }

    private fun MessagePayloadTextBuilder<*>.addVerboseInfo(build: SBuild) {
        val finishedBuild = server.findBuildInstanceById(build.buildId) ?: build

        addBranch(finishedBuild)
        addBuildStatus(finishedBuild)
        addChanges(finishedBuild)
    }

    private fun MessagePayloadTextBuilder<*>.addVerboseInfo(queuedBuild: SQueuedBuild) {
        addBranch(queuedBuild)
        addChanges(queuedBuild)
    }

    private fun MessagePayloadTextBuilder<*>.addBuildStatus(build: Build) {
        if (verboseMessagesOptions.addBuildStatus && build is SBuild) {
            val buildStatistics: BuildStatistics = build.getBuildStatistics(
                    BuildStatisticsOptions(
                            BuildStatisticsOptions.COMPILATION_ERRORS,
                            TemplateMessageBuilder.MAX_NUM_OF_STACKTRACES
                    )
            )

            newline()
            add("Status: ${notificationBuildStatusProvider.getText(build, buildStatistics)}")
        }
    }

    private fun MessagePayloadTextBuilder<*>.addBranch(build: Build) {
        if (!verboseMessagesOptions.addBranch) {
            return
        }

        if (build is SBuild) {
            build.branch?.displayName?.let { branch ->
                add("Branch: `$branch`")
            }
        }
    }

    private fun MessagePayloadTextBuilder<*>.addBranch(queuedBuild: SQueuedBuild) {
        if (!verboseMessagesOptions.addBranch) {
            return
        }

        queuedBuild.buildPromotion.branch?.displayName.let { branch ->
            add("Branch: `$branch`")
        }
    }

    private fun MessagePayloadTextBuilder<*>.addChangesDescription(changes: List<SVcsModification>) {
        val firstChanges = changes.take(verboseMessagesOptions.maximumNumberOfChanges).toList()

        newline()
        newline()
        add(format.bold("Changes:"))
        for (change in firstChanges) {
            newline()
            newline()
            val changeDescription = shorten(change.description.trim())
            val username = change.committers.firstOrNull()?.descriptiveName ?: change.userName
            val prefix = if (username != null) {
                "$username∶ "
            } else {
                ""
            }

            // Use different colon symbol here since the date is in format hh:mm
            // and that could lead to Slack interpreting message like '...19:42: ...'
            // as :42: meaning emoji
            add(format.listElement("${prefix}$changeDescription"))
        }

        newline()
    }

    private fun getChangesLinkText(changes: List<VcsModification>): String {
        return if (changes.size == 1) {
            "View 1 change in TeamCity"
        } else {
            "View all ${changes.size} changes in TeamCity"
        }
    }

    private fun MessagePayloadTextBuilder<*>.addChanges(build: SBuild) {
        if (!verboseMessagesOptions.addChanges) {
            return
        }

        val changes = changesCalculationOptionsFactory.getChanges(build.buildPromotion)
        if (changes.isEmpty()) {
            add("No new changes")
            return
        }

        addChangesDescription(changes)

        val changesUrl = webLinks.getViewChangesUrl(build)
        add(format.url(url = changesUrl, text = getChangesLinkText(changes)))
    }

    private fun MessagePayloadTextBuilder<*>.addChanges(queuedBuild: SQueuedBuild) {
        if (!verboseMessagesOptions.addChanges) {
            return
        }

        val changes = changesCalculationOptionsFactory.getChanges(queuedBuild.buildPromotion)
        if (changes.isEmpty()) {
            add("No new changes")
            return
        }

        addChangesDescription(changes)

        val changesUrl = webLinks.getViewQueuedChangesUrl(queuedBuild)
        add(format.url(url = changesUrl, text = getChangesLinkText(changes)))
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
        textBlock  {
            add(messageBuilder.buildFailed(build))
        }

        contextBlock {
            addVerboseInfo(build)
        }
    }

    override fun buildFailedToStart(build: SRunningBuild): MessagePayload = messagePayload {
        textBlock {
            add(messageBuilder.buildFailedToStart(build))
        }

        contextBlock {
            addVerboseInfo(build)
        }
    }

    override fun labelingFailed(build: Build, root: VcsRoot, exception: Throwable): MessagePayload = messagePayload {
        textBlock {
            add(messageBuilder.labelingFailed(build, root, exception))
        }

        contextBlock {
            addVerboseInfo(build as SBuild)
        }
    }

    override fun buildFailing(build: SRunningBuild): MessagePayload = messagePayload {
        textBlock {
            add(messageBuilder.buildFailing(build))
        }
        contextBlock {
            addVerboseInfo(build)
        }
    }

    override fun buildProbablyHanging(build: SRunningBuild): MessagePayload = messagePayload {
        textBlock {
            add(messageBuilder.buildProbablyHanging(build))
        }
        contextBlock {
            addVerboseInfo(build)
        }
    }

    override fun queuedBuildWaitingForApproval(queuedBuild: SQueuedBuild): MessagePayload = messagePayload {
        textBlock {
            add(messageBuilder.queuedBuildWaitingForApproval(queuedBuild))
        }
        contextBlock {
            addVerboseInfo(queuedBuild)
        }
    }
}