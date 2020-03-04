package jetbrains.buildServer.slackNotifications

import jetbrains.buildServer.Build
import jetbrains.buildServer.responsibility.ResponsibilityEntry
import jetbrains.buildServer.responsibility.TestNameResponsibilityEntry
import jetbrains.buildServer.serverSide.*
import jetbrains.buildServer.serverSide.mute.MuteInfo
import jetbrains.buildServer.serverSide.problems.BuildProblemInfo
import jetbrains.buildServer.tests.TestName
import jetbrains.buildServer.users.SUser
import jetbrains.buildServer.vcs.VcsRoot

class SimpleMessageBuilder(
    private val format: SlackMessageFormatter,
    private val links: RelativeWebLinks
) : MessageBuilder {
    override fun buildStarted(build: SRunningBuild, users: Set<SUser?>): MessagePayload {
        return MessagePayload("Build ${format.url(links.getViewResultsUrl(build), build.buildNumber)} started")
    }

    override fun buildSuccessful(build: SRunningBuild, users: Set<SUser?>): MessagePayload {
        return MessagePayload("Build ${format.url(links.getViewResultsUrl(build), build.buildNumber)} is successful")
    }

    override fun buildFailed(build: SRunningBuild, users: Set<SUser?>): MessagePayload {
        return MessagePayload("Build ${format.url(links.getViewResultsUrl(build), build.buildNumber)} failed")
    }

    override fun buildFailedToStart(build: SRunningBuild, users: Set<SUser?>): MessagePayload {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun labelingFailed(build: Build, root: VcsRoot, exception: Throwable, users: Set<SUser?>): MessagePayload {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun buildFailing(build: SRunningBuild, users: Set<SUser?>): MessagePayload {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun buildProbablyHanging(build: SRunningBuild, users: Set<SUser?>): MessagePayload {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun responsibleChanged(buildType: SBuildType, users: Set<SUser?>): MessagePayload {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun responsibleChanged(
        oldValue: TestNameResponsibilityEntry?,
        newValue: TestNameResponsibilityEntry,
        project: SProject,
        users: Set<SUser?>
    ): MessagePayload {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun responsibleChanged(
        testNames: Collection<TestName?>,
        entry: ResponsibilityEntry,
        project: SProject,
        users: Set<SUser?>
    ): MessagePayload {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun responsibleAssigned(buildType: SBuildType, users: Set<SUser?>): MessagePayload {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun responsibleAssigned(
        oldValue: TestNameResponsibilityEntry?,
        newValue: TestNameResponsibilityEntry,
        project: SProject,
        users: Set<SUser?>
    ): MessagePayload {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun responsibleAssigned(
        testNames: Collection<TestName?>,
        entry: ResponsibilityEntry,
        project: SProject,
        users: Set<SUser?>
    ): MessagePayload {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun buildProblemResponsibleAssigned(
        buildProblems: Collection<BuildProblemInfo?>,
        entry: ResponsibilityEntry,
        project: SProject,
        users: Set<SUser?>
    ): MessagePayload {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun buildProblemResponsibleChanged(
        buildProblems: Collection<BuildProblemInfo?>,
        entry: ResponsibilityEntry,
        project: SProject,
        users: Set<SUser?>
    ): MessagePayload {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun testsMuted(tests: Collection<STest?>, muteInfo: MuteInfo, users: Set<SUser?>): MessagePayload {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun testsUnmuted(
        tests: Collection<STest?>,
        muteInfo: MuteInfo,
        user: SUser?,
        users: Set<SUser?>
    ): MessagePayload {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun buildProblemsMuted(
        buildProblems: Collection<BuildProblemInfo?>,
        muteInfo: MuteInfo,
        users: Set<SUser?>
    ): MessagePayload {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun buildProblemsUnmuted(
        buildProblems: Collection<BuildProblemInfo?>,
        muteInfo: MuteInfo,
        user: SUser?,
        users: Set<SUser?>
    ): MessagePayload {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}