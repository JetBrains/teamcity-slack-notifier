package jetbrains.buildServer.notification.slackNotifier.notification

import jetbrains.buildServer.Build
import jetbrains.buildServer.notification.NotificationBuildStatusProvider
import jetbrains.buildServer.notification.TemplateMessageBuilder
import jetbrains.buildServer.notification.slackNotifier.slack.SlackMessageFormatter
import jetbrains.buildServer.responsibility.ResponsibilityEntry
import jetbrains.buildServer.responsibility.TestNameResponsibilityEntry
import jetbrains.buildServer.serverSide.*
import jetbrains.buildServer.serverSide.mute.MuteInfo
import jetbrains.buildServer.serverSide.problems.BuildProblemInfo
import jetbrains.buildServer.tests.TestName
import jetbrains.buildServer.users.SUser
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
) : MessageBuilder {
    private val changeDateFormat = SimpleDateFormat("d MMM HH:mm")

    override fun buildStarted(build: SRunningBuild): MessagePayload = messagePayload {
        textBlock {
            add(messageBuilder.buildStarted(build))
            addBranch(build)
            addChanges(build)
        }

        addActionsBlock(build)
    }

    override fun buildSuccessful(build: SRunningBuild): MessagePayload = messagePayload {
        textBlock {
            add(messageBuilder.buildSuccessful(build))
            addVerboseInfo(build, BuildEvent.BUILD_SUCCESSFUL)
        }

        addActionsBlock(build)
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
        add("Changes:")
        for (change in firstChanges) {
            newline()
            val changeDescription = shorten(change.description.trim())
            val username = change.userName?.let {
                " by ${it.trim()} "
            } ?: ""
            val changeAdditionalInfo = "${username}at ${changeDateFormat.format(change.vcsDate)}"

            add("$changeDescription ${format.italic(changeAdditionalInfo)}")
        }
    }

    private fun MessagePayloadBuilder.addActionsBlock(build: Build) {
        if (!verboseMessagesOptions.addChanges) {
            return
        }

        val changes = build.getChanges(SelectPrevBuildPolicy.SINCE_LAST_BUILD, true)
        if (changes.isEmpty()) {
            return
        }

        actionsBlock {
            val changesUrl = webLinks.getViewChangesUrl(build)

            val text = if (changes.size == 1) {
                "View 1 change in TeamCity"
            } else {
                "View ${changes.size} changes in TeamCity"
            }

            button(text = text, url = changesUrl)
        }
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
        textBlock {
            add(messageBuilder.buildFailed(build))
            addVerboseInfo(build, BuildEvent.BUILD_FAILED)
        }

        addActionsBlock(build)
    }

    override fun buildFailedToStart(build: SRunningBuild): MessagePayload = messagePayload {
        textBlock {
            add(messageBuilder.buildFailedToStart(build))
            addVerboseInfo(build, BuildEvent.BUILD_FAILED_TO_START)
        }

        addActionsBlock(build)
    }

    override fun labelingFailed(build: Build, root: VcsRoot, exception: Throwable): MessagePayload = messagePayload {
        textBlock {
            add(messageBuilder.labelingFailed(build, root, exception))
            addVerboseInfo(build, BuildEvent.LABELING_FAILED)
        }

        addActionsBlock(build)
    }

    override fun buildFailing(build: SRunningBuild): MessagePayload = messagePayload {
        textBlock {
            add(messageBuilder.buildFailing(build))
            addVerboseInfo(build, BuildEvent.BUILD_FAILING)
        }

        addActionsBlock(build)
    }

    override fun buildProbablyHanging(build: SRunningBuild): MessagePayload = messagePayload {
        textBlock {
            add(messageBuilder.buildProbablyHanging(build))
            addVerboseInfo(build, BuildEvent.BUILD_PROBABLY_HANGING)
        }

        addActionsBlock(build)
    }

    override fun responsibleChanged(buildType: SBuildType): MessagePayload {
        return messageBuilder.responsibleChanged(buildType)
    }

    override fun responsibleChanged(oldValue: TestNameResponsibilityEntry?, newValue: TestNameResponsibilityEntry, project: SProject): MessagePayload {
        return messageBuilder.responsibleChanged(oldValue, newValue, project)
    }

    override fun responsibleChanged(testNames: Collection<TestName?>, entry: ResponsibilityEntry, project: SProject): MessagePayload {
        return messageBuilder.responsibleChanged(testNames, entry, project)
    }

    override fun responsibleAssigned(buildType: SBuildType): MessagePayload {
        return messageBuilder.responsibleAssigned(buildType)
    }

    override fun responsibleAssigned(oldValue: TestNameResponsibilityEntry?, newValue: TestNameResponsibilityEntry, project: SProject): MessagePayload {
        return messageBuilder.responsibleAssigned(oldValue, newValue, project)
    }

    override fun responsibleAssigned(testNames: Collection<TestName?>, entry: ResponsibilityEntry, project: SProject): MessagePayload {
        return messageBuilder.responsibleAssigned(testNames, entry, project)
    }

    override fun buildProblemResponsibleAssigned(buildProblems: Collection<BuildProblemInfo?>, entry: ResponsibilityEntry, project: SProject): MessagePayload {
        return messageBuilder.buildProblemResponsibleAssigned(buildProblems, entry, project)
    }

    override fun buildProblemResponsibleChanged(buildProblems: Collection<BuildProblemInfo?>, entry: ResponsibilityEntry, project: SProject): MessagePayload {
        return messageBuilder.buildProblemResponsibleChanged(buildProblems, entry, project)
    }

    override fun testsMuted(tests: Collection<STest?>, muteInfo: MuteInfo): MessagePayload {
        return messageBuilder.testsMuted(tests, muteInfo)
    }

    override fun testsUnmuted(tests: Collection<STest?>, muteInfo: MuteInfo, user: SUser?): MessagePayload {
        return messageBuilder.testsUnmuted(tests, muteInfo, user)
    }

    override fun buildProblemsMuted(buildProblems: Collection<BuildProblemInfo?>, muteInfo: MuteInfo): MessagePayload {
        return messageBuilder.buildProblemsMuted(buildProblems, muteInfo)
    }

    override fun buildProblemsUnmuted(buildProblems: Collection<BuildProblemInfo?>, muteInfo: MuteInfo, user: SUser?): MessagePayload {
        return messageBuilder.buildProblemsUnmuted(buildProblems, muteInfo, user)
    }
}