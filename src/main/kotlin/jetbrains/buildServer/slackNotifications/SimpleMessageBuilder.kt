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
    private val maxBuildProblemsToShow = 10

    override fun buildStarted(build: SRunningBuild, users: Set<SUser?>): MessagePayload {
        return MessagePayload("${buildUrl(build)} started")
    }

    override fun buildSuccessful(build: SRunningBuild, users: Set<SUser?>): MessagePayload {
        return MessagePayload("${buildUrl(build)} is successful")
    }

    override fun buildFailed(build: SRunningBuild, users: Set<SUser?>): MessagePayload {
        return MessagePayload("${buildUrl(build)} failed")
    }

    override fun buildFailedToStart(build: SRunningBuild, users: Set<SUser?>): MessagePayload {
        return MessagePayload("${buildUrl(build)} failed to start")
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
        return MessagePayload("${buildUrl(build)} is failing")
    }

    override fun buildProbablyHanging(build: SRunningBuild, users: Set<SUser?>): MessagePayload {
        return MessagePayload("${buildUrl(build)} is probably hanging")
    }

    override fun responsibleChanged(buildType: SBuildType, users: Set<SUser?>): MessagePayload {
        return MessagePayload(
            "Investigation of ${format.url(
                links.getConfigurationHomePageUrl(
                    buildType
                ), buildType.fullName
            )} failure changed"
        )
    }

    override fun responsibleChanged(
        oldValue: TestNameResponsibilityEntry?,
        newValue: TestNameResponsibilityEntry,
        project: SProject,
        users: Set<SUser?>
    ): MessagePayload {
        return MessagePayload(
            "Investigation of" +
                    " ${format.url(
                        links.getTestDetailsUrl(project.externalId, newValue.testNameId),
                        newValue.testName.asString
                    )} test failure chagned in" +
                    " ${format.url(links.getProjectPageUrl(project.externalId), project.fullName)}"
        )
    }

    override fun responsibleChanged(
        testNames: Collection<TestName?>,
        entry: ResponsibilityEntry,
        project: SProject,
        users: Set<SUser?>
    ): MessagePayload {
        val testNamesNotNull = testNames.asSequence().filterNotNull().toList()
        val testNamesFormatted = formatProblems(testNamesNotNull) {
            format.listElement(it.asString)
        }

        return MessagePayload(
            "Investigation of ${testNamesNotNull.size} tests changed " +
                    "in ${format.url(links.getProjectPageUrl(project.externalId), project.fullName)}:\n" +
                    testNamesFormatted
        )
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
        val testNamesFormatted = formatProblems(testNamesNotNull) {
            format.listElement(it.asString)
        }

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
        val buildProblemsNotNull = buildProblems.asSequence().filterNotNull().toList()
        val buildProblemsFormatted = formatProblems(buildProblemsNotNull) {
            format.listElement(it.buildProblemDescription ?: it.toString())
        }

        return MessagePayload(
            "You are assigned for investigation of ${buildProblemsNotNull.size} build problems " +
                    "in ${format.url(links.getProjectPageUrl(project.externalId), project.fullName)}:\n" +
                    buildProblemsFormatted
        )
    }

    private fun <T> formatProblems(problems: Collection<T>, formatter: (T) -> String): String {
        val firstBuildProblems = problems.asSequence().take(maxBuildProblemsToShow).toList()
        val postfix = if (problems.size > firstBuildProblems.size) {
            "\n" + format.listElement("...")
        } else {
            ""
        }

        return firstBuildProblems.joinToString("\n") { formatter(it) } + postfix
    }

    override fun buildProblemResponsibleChanged(
        buildProblems: Collection<BuildProblemInfo?>,
        entry: ResponsibilityEntry,
        project: SProject,
        users: Set<SUser?>
    ): MessagePayload {
        val buildProblemsNotNull = buildProblems.asSequence().filterNotNull().toList()
        val buildProblemsFormatted = formatProblems(buildProblemsNotNull) {
            format.listElement(it.buildProblemDescription ?: it.toString())
        }

        return MessagePayload(
            "Investigation for ${buildProblemsNotNull.size} build problems changed " +
                    "in ${format.url(links.getProjectPageUrl(project.externalId), project.fullName)}:\n" +
                    buildProblemsFormatted
        )
    }

    override fun testsMuted(tests: Collection<STest?>, muteInfo: MuteInfo, users: Set<SUser?>): MessagePayload {
        val user = userName(muteInfo)
        val project = projectName(muteInfo)
        return if (user == null) {
            MessagePayload("${tests.size} tests were muted in $project")
        } else {
            MessagePayload("$user muted ${tests.size} tests in $project.")
        }
    }

    override fun testsUnmuted(
        tests: Collection<STest?>,
        muteInfo: MuteInfo,
        user: SUser?,
        users: Set<SUser?>
    ): MessagePayload {
        val username = userName(muteInfo)
        val project = projectName(muteInfo)
        return if (username == null) {
            MessagePayload("${tests.size} tests were unmuted in $project.")
        } else {
            MessagePayload("$username unmuted ${tests.size} tests in $project.")
        }
    }

    override fun buildProblemsMuted(
        buildProblems: Collection<BuildProblemInfo?>,
        muteInfo: MuteInfo,
        users: Set<SUser?>
    ): MessagePayload {
        val user = userName(muteInfo)
        val project = projectName(muteInfo)

        return if (user == null) {
            MessagePayload("${buildProblems.size} problems were muted in $project")
        } else {
            MessagePayload("$user muted ${buildProblems.size} problems in $project")
        }
    }

    override fun buildProblemsUnmuted(
        buildProblems: Collection<BuildProblemInfo?>,
        muteInfo: MuteInfo,
        user: SUser?,
        users: Set<SUser?>
    ): MessagePayload {
        val username = userName(muteInfo)
        val project = projectName(muteInfo)
        return if (username == null) {
            MessagePayload("${buildProblems.size} problems were unmuted in $project.")
        } else {
            MessagePayload("$username unmuted ${buildProblems.size} problems in $project.")
        }
    }

    private fun userName(muteInfo: MuteInfo) = muteInfo.mutingUser?.username
    private fun projectName(muteInfo: MuteInfo): String {
        val project = muteInfo.project ?: return "<deleted project>"
        val url = links.getProjectPageUrl(project.externalId)
        return format.url(url, project.fullName)
    }

    private fun buildUrl(build: Build): String {
        val projectName =
            projectManager.findProjectByExternalId(build.projectExternalId)?.fullName ?: "<deleted project>"

        val buildType = build.buildType
        val buildTypeName = buildType?.name ?: ""

        val buildName = "$buildTypeName ${number(build)}"
        return "$projectName / ${format.url(links.getViewResultsUrl(build), buildName)}"
    }

    private fun number(build: Build) = "#${build.buildNumber}"
}