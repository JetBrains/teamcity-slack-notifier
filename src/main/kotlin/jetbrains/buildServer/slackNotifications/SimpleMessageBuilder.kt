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
    private val links: RelativeWebLinks,

    private val projectManager: ProjectManager
) : MessageBuilder {
    private val maxTestsNumberToShow = 10

    override fun buildStarted(build: SRunningBuild, users: Set<SUser?>): MessagePayload {
        return MessagePayload("Build ${buildUrl(build)} started")
    }

    override fun buildSuccessful(build: SRunningBuild, users: Set<SUser?>): MessagePayload {
        return MessagePayload("Build ${buildUrl(build)} is successful")
    }

    override fun buildFailed(build: SRunningBuild, users: Set<SUser?>): MessagePayload {
        return MessagePayload("Build ${buildUrl(build)} failed")
    }

    override fun buildFailedToStart(build: SRunningBuild, users: Set<SUser?>): MessagePayload {
        return MessagePayload("Build ${buildUrl(build)} failed to start")
    }

    override fun labelingFailed(build: Build, root: VcsRoot, exception: Throwable, users: Set<SUser?>): MessagePayload {
        return MessagePayload(
            "Labeling failed for root ${root.name}. More details on ${format.url(
                links.getViewResultsUrl(
                    build
                ), "build result page"
            )}"
        )
    }

    override fun buildFailing(build: SRunningBuild, users: Set<SUser?>): MessagePayload {
        return MessagePayload("Build ${buildUrl(build)} is failing")
    }

    override fun buildProbablyHanging(build: SRunningBuild, users: Set<SUser?>): MessagePayload {
        return MessagePayload("Build ${buildUrl(build)} is probably hanging")
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
        return MessagePayload(
            "You are assigned for investigation of ${format.url(
                links.getConfigurationHomePageUrl(
                    buildType
                ), buildType.fullName
            )} failure"
        )
    }

    override fun responsibleAssigned(
        oldValue: TestNameResponsibilityEntry?,
        newValue: TestNameResponsibilityEntry,
        project: SProject,
        users: Set<SUser?>
    ): MessagePayload {
        return MessagePayload(
            "You are assigned for investigation of" +
                    " ${format.url(
                        links.getTestDetailsUrl(project.externalId, newValue.testNameId),
                        newValue.testName.asString
                    )} test failure in" +
                    " ${format.url(links.getProjectPageUrl(project.externalId), project.fullName)}"
        )
    }

    override fun responsibleAssigned(
        testNames: Collection<TestName?>,
        entry: ResponsibilityEntry,
        project: SProject,
        users: Set<SUser?>
    ): MessagePayload {
        val testNamesNotNull = testNames.asSequence().filterNotNull().toList()
        val firstTestNames = testNamesNotNull.asSequence().take(maxTestsNumberToShow).toList()
        val postfix = if (testNamesNotNull.size > firstTestNames.size) {
            "\n" + format.listElement("...")
        } else {
            ""
        }

        val testNamesFormatted = firstTestNames.joinToString("\n") {
            format.listElement(it.asString)
        } + postfix

        return MessagePayload(
            "You are assigned for investigation of ${testNamesNotNull.size} tests " +
                    "in ${format.url(links.getProjectPageUrl(project.externalId), project.fullName)}:\n" +
                    testNamesFormatted
        )
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
        val user = muteInfo.mutingUser?.username ?: "Anonymous"
        val project = muteInfo.project ?: "already deleted project"
        return MessagePayload("$user muted ${tests.size} tests in $project.")
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

    private fun buildUrl(build: Build): String {
        val projectName =
            projectManager.findProjectByExternalId(build.projectExternalId)?.fullName ?: "<deleted project>"
        val buildName = "Build ${number(build)}"
        return "$projectName / ${format.url(links.getViewResultsUrl(build), buildName)}"
    }

    private fun number(build: Build) = "#${build.buildNumber}"
}