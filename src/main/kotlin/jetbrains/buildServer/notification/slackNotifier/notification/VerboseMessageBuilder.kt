package jetbrains.buildServer.notification.slackNotifier.notification

import jetbrains.buildServer.Build
import jetbrains.buildServer.notification.slackNotifier.slack.SlackMessageFormatter
import jetbrains.buildServer.responsibility.ResponsibilityEntry
import jetbrains.buildServer.responsibility.TestNameResponsibilityEntry
import jetbrains.buildServer.serverSide.*
import jetbrains.buildServer.serverSide.mute.MuteInfo
import jetbrains.buildServer.serverSide.problems.BuildProblemInfo
import jetbrains.buildServer.tests.TestName
import jetbrains.buildServer.users.SUser
import jetbrains.buildServer.vcs.VcsRoot

class VerboseMessageBuilder(
        private val verboseMessagesOptions: VerboseMessagesOptions,
        private val format: SlackMessageFormatter,
        private val links: RelativeWebLinks,
        private val detailsFormatter: DetailsFormatter
) : MessageBuilder {
    override fun buildStarted(build: SRunningBuild): MessagePayload {
        val triggeredBy = build.triggeredBy
        val prefix = if (triggeredBy.isTriggeredByUser) {
            " by ${triggeredBy.user!!.descriptiveName}"
        } else {
            ""
        }
        return MessagePayload("${detailsFormatter.buildUrl(build)} started${prefix}")
    }

    override fun buildSuccessful(build: SRunningBuild): MessagePayload {
        val buildStatusPrefix = if (verboseMessagesOptions.addBuildStatus) {
            " Status: ${build.buildStatus.text}."
        } else {
            ""
        }
        return MessagePayload(":heavy_check_mark: ${detailsFormatter.buildUrl(build)} is successful.${buildStatusPrefix}")
    }

    override fun buildFailed(build: SRunningBuild): MessagePayload {
        TODO("Not yet implemented")
    }

    override fun buildFailedToStart(build: SRunningBuild): MessagePayload {
        TODO("Not yet implemented")
    }

    override fun labelingFailed(build: Build, root: VcsRoot, exception: Throwable): MessagePayload {
        TODO("Not yet implemented")
    }

    override fun buildFailing(build: SRunningBuild): MessagePayload {
        TODO("Not yet implemented")
    }

    override fun buildProbablyHanging(build: SRunningBuild): MessagePayload {
        TODO("Not yet implemented")
    }

    override fun responsibleChanged(buildType: SBuildType): MessagePayload {
        TODO("Not yet implemented")
    }

    override fun responsibleChanged(oldValue: TestNameResponsibilityEntry?, newValue: TestNameResponsibilityEntry, project: SProject): MessagePayload {
        TODO("Not yet implemented")
    }

    override fun responsibleChanged(testNames: Collection<TestName?>, entry: ResponsibilityEntry, project: SProject): MessagePayload {
        TODO("Not yet implemented")
    }

    override fun responsibleAssigned(buildType: SBuildType): MessagePayload {
        TODO("Not yet implemented")
    }

    override fun responsibleAssigned(oldValue: TestNameResponsibilityEntry?, newValue: TestNameResponsibilityEntry, project: SProject): MessagePayload {
        TODO("Not yet implemented")
    }

    override fun responsibleAssigned(testNames: Collection<TestName?>, entry: ResponsibilityEntry, project: SProject): MessagePayload {
        TODO("Not yet implemented")
    }

    override fun buildProblemResponsibleAssigned(buildProblems: Collection<BuildProblemInfo?>, entry: ResponsibilityEntry, project: SProject): MessagePayload {
        TODO("Not yet implemented")
    }

    override fun buildProblemResponsibleChanged(buildProblems: Collection<BuildProblemInfo?>, entry: ResponsibilityEntry, project: SProject): MessagePayload {
        TODO("Not yet implemented")
    }

    override fun testsMuted(tests: Collection<STest?>, muteInfo: MuteInfo): MessagePayload {
        TODO("Not yet implemented")
    }

    override fun testsUnmuted(tests: Collection<STest?>, muteInfo: MuteInfo, user: SUser?): MessagePayload {
        TODO("Not yet implemented")
    }

    override fun buildProblemsMuted(buildProblems: Collection<BuildProblemInfo?>, muteInfo: MuteInfo): MessagePayload {
        TODO("Not yet implemented")
    }

    override fun buildProblemsUnmuted(buildProblems: Collection<BuildProblemInfo?>, muteInfo: MuteInfo, user: SUser?): MessagePayload {
        TODO("Not yet implemented")
    }
}