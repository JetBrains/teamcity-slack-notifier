/*
 *  Copyright 2000-2020 JetBrains s.r.o.
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
import jetbrains.buildServer.notification.slackNotifier.slack.SlackMessageFormatter
import jetbrains.buildServer.responsibility.ResponsibilityEntry
import jetbrains.buildServer.responsibility.TestNameResponsibilityEntry
import jetbrains.buildServer.serverSide.*
import jetbrains.buildServer.serverSide.mute.MuteInfo
import jetbrains.buildServer.serverSide.problems.BuildProblemInfo
import jetbrains.buildServer.tests.TestName
import jetbrains.buildServer.users.SUser
import jetbrains.buildServer.vcs.VcsRoot

/**
 * Builds plain text notification without any details or emojis
 */
class PlainMessageBuilder(
        private val format: SlackMessageFormatter,
        private val links: RelativeWebLinks,
        private val detailsFormatter: DetailsFormatter
) : MessageBuilder {
    private val maxBuildProblemsToShow = 10

    override fun buildStarted(build: SRunningBuild): MessagePayload {
        val triggeredBy = build.triggeredBy
        val prefix = if (triggeredBy.isTriggeredByUser) {
            " by ${triggeredBy.user!!.descriptiveName}"
        } else {
            ""
        }
        return MessagePayload("${detailsFormatter.buildUrl(build)} ${format.bold("started")}${prefix}")
    }

    override fun buildSuccessful(build: SRunningBuild): MessagePayload {
        return MessagePayload("${detailsFormatter.buildUrl(build)} ${format.bold("is successful")}")
    }

    override fun buildFailed(build: SRunningBuild): MessagePayload {
        return MessagePayload("${detailsFormatter.buildUrl(build)} ${format.bold("failed")}")
    }

    override fun buildFailedToStart(build: SRunningBuild): MessagePayload {
        return MessagePayload("${detailsFormatter.buildUrl(build)} ${format.bold("failed to start")}")
    }

    override fun labelingFailed(build: Build, root: VcsRoot, exception: Throwable): MessagePayload {
        return MessagePayload(
                "Labeling failed for root ${root.name}. More details on ${format.url(
                        links.getViewResultsUrl(
                                build
                        ), "build result page"
                )}"
        )
    }

    override fun buildFailing(build: SRunningBuild): MessagePayload {
        return MessagePayload("${detailsFormatter.buildUrl(build)} ${format.bold("is failing")}")
    }

    override fun buildProbablyHanging(build: SRunningBuild): MessagePayload {
        return MessagePayload("${detailsFormatter.buildUrl(build)} ${format.bold("is probably hanging")}")
    }

    override fun responsibleChanged(buildType: SBuildType): MessagePayload {
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
            project: SProject
    ): MessagePayload {
        return MessagePayload(
                "Investigation of" +
                        " ${format.url(
                                links.getTestDetailsUrl(project.externalId, newValue.testNameId),
                                newValue.testName.asString
                        )} test failure changed in" +
                        " ${format.url(links.getProjectPageUrl(project.externalId), project.fullName)}"
        )
    }

    override fun responsibleChanged(
            testNames: Collection<TestName?>,
            entry: ResponsibilityEntry,
            project: SProject
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

    override fun responsibleAssigned(buildType: SBuildType): MessagePayload {
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
            project: SProject
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
            project: SProject
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
            project: SProject
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
            project: SProject
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

    override fun testsMuted(tests: Collection<STest?>, muteInfo: MuteInfo): MessagePayload {
        val user = detailsFormatter.userName(muteInfo)
        val project = detailsFormatter.projectName(muteInfo)
        return if (user == null) {
            MessagePayload("${tests.size} tests were muted in $project")
        } else {
            MessagePayload("$user muted ${tests.size} tests in $project.")
        }
    }

    override fun testsUnmuted(
            tests: Collection<STest?>,
            muteInfo: MuteInfo,
            user: SUser?
    ): MessagePayload {
        val username = detailsFormatter.userName(muteInfo)
        val project = detailsFormatter.projectName(muteInfo)
        return if (username == null) {
            MessagePayload("${tests.size} tests were unmuted in $project.")
        } else {
            MessagePayload("$username unmuted ${tests.size} tests in $project.")
        }
    }

    override fun buildProblemsMuted(
            buildProblems: Collection<BuildProblemInfo?>,
            muteInfo: MuteInfo
    ): MessagePayload {
        val user = detailsFormatter.userName(muteInfo)
        val project = detailsFormatter.projectName(muteInfo)

        return if (user == null) {
            MessagePayload("${buildProblems.size} problems were muted in $project")
        } else {
            MessagePayload("$user muted ${buildProblems.size} problems in $project")
        }
    }

    override fun buildProblemsUnmuted(
            buildProblems: Collection<BuildProblemInfo?>,
            muteInfo: MuteInfo,
            user: SUser?
    ): MessagePayload {
        val username = detailsFormatter.userName(muteInfo)
        val project = detailsFormatter.projectName(muteInfo)
        return if (username == null) {
            MessagePayload("${buildProblems.size} problems were unmuted in $project.")
        } else {
            MessagePayload("$username unmuted ${buildProblems.size} problems in $project.")
        }
    }
}