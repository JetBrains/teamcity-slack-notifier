package jetbrains.buildServer.slackNotifications

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
     * @param users users to be notified.
     */
    fun buildStarted(build: SRunningBuild, users: Set<SUser?>): MessagePayload

    /**
     * Called when build finished successfully.
     * @param build finished build.
     * @param users users to be notified.
     */
    fun buildSuccessful(build: SRunningBuild, users: Set<SUser?>): MessagePayload

    /**
     * Called when build failed.
     * @param build finished build
     * @param users users to be notified.
     */
    fun buildFailed(build: SRunningBuild, users: Set<SUser?>): MessagePayload

    /**
     * Called when build failed with internal error, i.e. could not actually start.
     * @param build failed build
     * @param users users to be notified
     */
    fun buildFailedToStart(build: SRunningBuild, users: Set<SUser?>): MessagePayload

    /**
     * Called when labeling failed for the build.
     * @param build finished build.
     * @param root problem root.
     * @param exception cause.
     * @param users users to be notified.
     */
    fun labelingFailed(
        build: Build, root: VcsRoot,
        exception: Throwable, users: Set<SUser?>
    ): MessagePayload

    /**
     * Called when the first failed message occurred.
     * @param build running build.
     * @param users users to be notified.
     */
    fun buildFailing(build: SRunningBuild, users: Set<SUser?>): MessagePayload

    /**
     * Called when build is not sending messages to server for some time.
     * @param build running build.
     * @param users users to be notified.
     */
    fun buildProbablyHanging(build: SRunningBuild, users: Set<SUser?>): MessagePayload

    /**
     * Called when responsibility for configuration changed.
     * @param buildType configuration.
     * @param users users to be notified.
     */
    fun responsibleChanged(buildType: SBuildType, users: Set<SUser?>): MessagePayload

    /**
     * Called when responsibility for the build type is assigned on certain users.
     * @param buildType configuration.
     * @param users users to be notified.
     */
    fun responsibleAssigned(buildType: SBuildType, users: Set<SUser?>): MessagePayload

    /**
     * Called when responsibility for the test changed.
     * @param oldValue old responsibility entry (nullable).
     * @param newValue new responsibility entry.
     * @param project the project.
     * @param users users to be notified.
     */
    fun responsibleChanged(
        oldValue: TestNameResponsibilityEntry?,
        newValue: TestNameResponsibilityEntry,
        project: SProject,
        users: Set<SUser?>
    ): MessagePayload

    /**
     * Called when responsibility for the test is assigned on certain users.
     * @param oldValue old responsibility entry (nullable).
     * @param newValue new responsibility entry.
     * @param project the project.
     * @param users users to be notified.
     */
    fun responsibleAssigned(
        oldValue: TestNameResponsibilityEntry?,
        newValue: TestNameResponsibilityEntry,
        project: SProject,
        users: Set<SUser?>
    ): MessagePayload

    /**
     * Called when responsibility for several tests at once is changed.
     * @param testNames the collection of test names
     * @param entry new responsibility entry for each test
     * @param project the project
     * @param users users to be notified
     */
    fun responsibleChanged(
        testNames: Collection<TestName?>,
        entry: ResponsibilityEntry,
        project: SProject,
        users: Set<SUser?>
    ): MessagePayload

    /**
     * Called when responsibility for several tests at once is assigned.
     * @param testNames the collection of test names
     * @param entry new responsibility entry for each test
     * @param project the project
     * @param users users to be notified
     */
    fun responsibleAssigned(
        testNames: Collection<TestName?>,
        entry: ResponsibilityEntry,
        project: SProject,
        users: Set<SUser?>
    ): MessagePayload

    /**
     * Called when responsibility for several build problems is assigned.
     * @param buildProblems affected build problems
     * @param entry new responsibility entry
     * @param project corresponding project
     * @param users users to be notified
     */
    fun buildProblemResponsibleAssigned(
        buildProblems: Collection<BuildProblemInfo?>,
        entry: ResponsibilityEntry,
        project: SProject,
        users: Set<SUser?>
    ): MessagePayload

    /**
     * Called when responsibility for several build problems is changed.
     * @param buildProblems affected build problems
     * @param entry new responsibility entry
     * @param project corresponding project
     * @param users users to be notified
     */
    fun buildProblemResponsibleChanged(
        buildProblems: Collection<BuildProblemInfo?>,
        entry: ResponsibilityEntry,
        project: SProject,
        users: Set<SUser?>
    ): MessagePayload

    /**
     * Called when a set of tests are unmuted (with same mute properties). All tests are in the same project.
     * @param tests the unmuted tests
     * @param muteInfo the mute info on the moment of unmute
     * @param users users to be notified
     */
    fun testsMuted(
        tests: Collection<STest?>,
        muteInfo: MuteInfo,
        users: Set<SUser?>
    ): MessagePayload

    /**
     * Called when a set of tests are unmuted (with same mute properties). All tests are in the same project.
     *
     * @param tests the unmuted tests
     * @param user user who performed the action (if known)
     * @param users users to be notified
     */
    fun testsUnmuted(
        tests: Collection<STest?>,
        muteInfo: MuteInfo,
        user: SUser?,
        users: Set<SUser?>
    ): MessagePayload


    /**
     * Called when a set of build problems are muted (with same mute properties).
     * All problems belong the same project.
     *
     * @param buildProblems the muted problems
     * @param muteInfo mute info
     * @param users users to be notified
     */
    fun buildProblemsMuted(
        buildProblems: Collection<BuildProblemInfo?>,
        muteInfo: MuteInfo,
        users: Set<SUser?>
    ): MessagePayload

    /**
     * Called when a set of build problems are unmuted (with same mute properties).
     * All problems belong the same project.
     *
     * @param buildProblems the unmuted problems
     * @param user user who performed the action (if known)
     * @param users users to be notified
     */
    fun buildProblemsUnmuted(
        buildProblems: Collection<BuildProblemInfo?>,
        muteInfo: MuteInfo,
        user: SUser?,
        users: Set<SUser?>
    ): MessagePayload
}