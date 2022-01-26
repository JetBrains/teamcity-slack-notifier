/*
 *  Copyright 2000-2022 JetBrains s.r.o.
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
import jetbrains.buildServer.responsibility.ResponsibilityEntry
import jetbrains.buildServer.responsibility.TestNameResponsibilityEntry
import jetbrains.buildServer.serverSide.SBuildType
import jetbrains.buildServer.serverSide.SProject
import jetbrains.buildServer.serverSide.SRunningBuild
import jetbrains.buildServer.serverSide.STest
import jetbrains.buildServer.serverSide.mute.MuteInfo
import jetbrains.buildServer.serverSide.problems.BuildProblemInfo
import jetbrains.buildServer.tests.TestName
import jetbrains.buildServer.users.SUser
import jetbrains.buildServer.vcs.VcsRoot

interface MessageBuilder {
    /**
     * Called when new build started.
     * @param build started build.
     */
    fun buildStarted(build: SRunningBuild): MessagePayload

    /**
     * Called when build finished successfully.
     * @param build finished build.
     */
    fun buildSuccessful(build: SRunningBuild): MessagePayload

    /**
     * Called when build failed.
     * @param build finished build
     */
    fun buildFailed(build: SRunningBuild): MessagePayload

    /**
     * Called when build failed with internal error, i.e. could not actually start.
     * @param build failed build
     */
    fun buildFailedToStart(build: SRunningBuild): MessagePayload

    /**
     * Called when labeling failed for the build.
     * @param build finished build.
     * @param root problem root.
     * @param exception cause.
     */
    fun labelingFailed(
            build: Build, root: VcsRoot,
            exception: Throwable
    ): MessagePayload

    /**
     * Called when the first failed message occurred.
     * @param build running build.
     */
    fun buildFailing(build: SRunningBuild): MessagePayload

    /**
     * Called when build is not sending messages to server for some time.
     * @param build running build.
     */
    fun buildProbablyHanging(build: SRunningBuild): MessagePayload

    /**
     * Called when responsibility for configuration changed.
     * @param buildType configuration.
     */
    fun responsibleChanged(buildType: SBuildType): MessagePayload

    /**
     * Called when responsibility for the build type is assigned on certain users.
     * @param buildType configuration.
     */
    fun responsibleAssigned(buildType: SBuildType): MessagePayload

    /**
     * Called when responsibility for the test changed.
     * @param oldValue old responsibility entry (nullable).
     * @param newValue new responsibility entry.
     * @param project the project.
     */
    fun responsibleChanged(
            oldValue: TestNameResponsibilityEntry?,
            newValue: TestNameResponsibilityEntry,
            project: SProject
    ): MessagePayload

    /**
     * Called when responsibility for the test is assigned on certain users.
     * @param oldValue old responsibility entry (nullable).
     * @param newValue new responsibility entry.
     * @param project the project.
     */
    fun responsibleAssigned(
            oldValue: TestNameResponsibilityEntry?,
            newValue: TestNameResponsibilityEntry,
            project: SProject
    ): MessagePayload

    /**
     * Called when responsibility for several tests at once is changed.
     * @param testNames the collection of test names
     * @param entry new responsibility entry for each test
     * @param project the project
     */
    fun responsibleChanged(
            testNames: Collection<TestName?>,
            entry: ResponsibilityEntry,
            project: SProject
    ): MessagePayload

    /**
     * Called when responsibility for several tests at once is assigned.
     * @param testNames the collection of test names
     * @param entry new responsibility entry for each test
     * @param project the project
     */
    fun responsibleAssigned(
            testNames: Collection<TestName?>,
            entry: ResponsibilityEntry,
            project: SProject
    ): MessagePayload

    /**
     * Called when responsibility for several build problems is assigned.
     * @param buildProblems affected build problems
     * @param entry new responsibility entry
     * @param project corresponding project
     */
    fun buildProblemResponsibleAssigned(
            buildProblems: Collection<BuildProblemInfo?>,
            entry: ResponsibilityEntry,
            project: SProject
    ): MessagePayload

    /**
     * Called when responsibility for several build problems is changed.
     * @param buildProblems affected build problems
     * @param entry new responsibility entry
     * @param project corresponding project
     */
    fun buildProblemResponsibleChanged(
            buildProblems: Collection<BuildProblemInfo?>,
            entry: ResponsibilityEntry,
            project: SProject
    ): MessagePayload

    /**
     * Called when a set of tests are unmuted (with same mute properties). All tests are in the same project.
     * @param tests the unmuted tests
     * @param muteInfo the mute info on the moment of unmute
     */
    fun testsMuted(
            tests: Collection<STest?>,
            muteInfo: MuteInfo
    ): MessagePayload

    /**
     * Called when a set of tests are unmuted (with same mute properties). All tests are in the same project.
     *
     * @param tests the unmuted tests
     * @param user user who performed the action (if known)
     */
    fun testsUnmuted(
            tests: Collection<STest?>,
            muteInfo: MuteInfo,
            user: SUser?
    ): MessagePayload


    /**
     * Called when a set of build problems are muted (with same mute properties).
     * All problems belong the same project.
     *
     * @param buildProblems the muted problems
     * @param muteInfo mute info
     */
    fun buildProblemsMuted(
            buildProblems: Collection<BuildProblemInfo?>,
            muteInfo: MuteInfo
    ): MessagePayload

    /**
     * Called when a set of build problems are unmuted (with same mute properties).
     * All problems belong the same project.
     *
     * @param buildProblems the unmuted problems
     * @param user user who performed the action (if known)
     */
    fun buildProblemsUnmuted(
            buildProblems: Collection<BuildProblemInfo?>,
            muteInfo: MuteInfo,
            user: SUser?
    ): MessagePayload
}