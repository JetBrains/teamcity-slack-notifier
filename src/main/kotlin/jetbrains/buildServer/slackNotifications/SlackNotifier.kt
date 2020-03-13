package jetbrains.buildServer.slackNotifications

import jetbrains.buildServer.Build
import jetbrains.buildServer.log.Loggers
import jetbrains.buildServer.notification.Notificator
import jetbrains.buildServer.notification.NotificatorRegistry
import jetbrains.buildServer.responsibility.ResponsibilityEntry
import jetbrains.buildServer.responsibility.TestNameResponsibilityEntry
import jetbrains.buildServer.serverSide.*
import jetbrains.buildServer.serverSide.mute.MuteInfo
import jetbrains.buildServer.serverSide.oauth.OAuthConnectionsManager
import jetbrains.buildServer.serverSide.problems.BuildProblemInfo
import jetbrains.buildServer.slackNotifications.slack.SlackWebApiFactory
import jetbrains.buildServer.tests.TestName
import jetbrains.buildServer.users.SUser
import jetbrains.buildServer.vcs.VcsRoot
import kotlinx.coroutines.runBlocking
import retrofit2.await

class SlackNotifier(
    notifierRegistry: NotificatorRegistry,
    private val slackApiFactory: SlackWebApiFactory,
    private val messageBuilder: MessageBuilder,
    private val serverPaths: ServerPaths,
    private val projectManager: ProjectManager,

    private val oauthManager: OAuthConnectionsManager,
    private val descriptor: SlackNotifierDescriptor
) : Notificator {

    private val slackApi = slackApiFactory.createSlackWebApi()
    private val config = SlackNotifierConfig(serverPaths, descriptor, this)

    init {
        notifierRegistry.register(
            this,
            listOf(
                UserPropertyInfo(descriptor.channelPropertyName, "#channel or user id")
            )
        )
    }

    override fun getDisplayName(): String = descriptor.displayName
    override fun getNotificatorType(): String = descriptor.type

    override fun notifyTestsMuted(tests: Collection<STest>, muteInfo: MuteInfo, users: Set<SUser>) {
        val project = muteInfo.project ?: return
        sendMessage(messageBuilder.testsMuted(tests, muteInfo, users), users, project)
    }

    override fun notifyBuildProblemsUnmuted(
        buildProblems: Collection<BuildProblemInfo>,
        muteInfo: MuteInfo,
        user: SUser?,
        users: Set<SUser>
    ) {
        val project = muteInfo.project ?: return
        sendMessage(messageBuilder.buildProblemsUnmuted(buildProblems, muteInfo, user, users), users, project)
    }

    override fun notifyLabelingFailed(build: Build, root: VcsRoot, exception: Throwable, users: Set<SUser>) {
        sendMessage(messageBuilder.labelingFailed(build, root, exception, users), users, build)
    }

    override fun notifyResponsibleChanged(buildType: SBuildType, users: Set<SUser>) {
        sendMessage(messageBuilder.responsibleChanged(buildType, users), users, buildType.project)
    }

    override fun notifyResponsibleChanged(
        oldValue: TestNameResponsibilityEntry?,
        newValue: TestNameResponsibilityEntry,
        project: SProject,
        users: Set<SUser>
    ) {
        sendMessage(messageBuilder.responsibleChanged(oldValue, newValue, project, users), users, project)
    }

    override fun notifyResponsibleChanged(
        testNames: Collection<TestName>,
        entry: ResponsibilityEntry,
        project: SProject,
        users: Set<SUser>
    ) {
        sendMessage(messageBuilder.responsibleChanged(testNames, entry, project, users), users, project)
    }

    override fun notifyBuildSuccessful(build: SRunningBuild, users: Set<SUser>) {
        sendMessage(messageBuilder.buildSuccessful(build, users), users, build)
    }


    override fun notifyBuildFailed(build: SRunningBuild, users: Set<SUser>) {
        sendMessage(messageBuilder.buildFailed(build, users), users, build)
    }

    override fun notifyBuildProblemResponsibleChanged(
        buildProblems: Collection<BuildProblemInfo>,
        entry: ResponsibilityEntry,
        project: SProject,
        users: Set<SUser>
    ) {
        sendMessage(messageBuilder.buildProblemResponsibleChanged(buildProblems, entry, project, users), users, project)
    }

    override fun notifyBuildProblemsMuted(
        buildProblems: Collection<BuildProblemInfo>,
        muteInfo: MuteInfo,
        users: Set<SUser>
    ) {
        val project = muteInfo.project ?: return
        sendMessage(messageBuilder.buildProblemsMuted(buildProblems, muteInfo, users), users, project)
    }

    override fun notifyBuildFailedToStart(build: SRunningBuild, users: Set<SUser>) {
        sendMessage(messageBuilder.buildFailedToStart(build, users), users, build)
    }

    override fun notifyTestsUnmuted(
        tests: Collection<STest>,
        muteInfo: MuteInfo,
        user: SUser?,
        users: Set<SUser>
    ) {
        val project = muteInfo.project ?: return
        sendMessage(messageBuilder.testsUnmuted(tests, muteInfo, user, users), users, project)
    }

    override fun notifyBuildProbablyHanging(build: SRunningBuild, users: Set<SUser>) {
        sendMessage(messageBuilder.buildProbablyHanging(build, users), users, build)
    }

    override fun notifyBuildProblemResponsibleAssigned(
        buildProblems: Collection<BuildProblemInfo>,
        entry: ResponsibilityEntry,
        project: SProject,
        users: Set<SUser>
    ) {
        sendMessage(
            messageBuilder.buildProblemResponsibleAssigned(buildProblems, entry, project, users),
            users,
            project
        )
    }

    override fun notifyBuildStarted(build: SRunningBuild, users: Set<SUser>) {
        sendMessage(messageBuilder.buildStarted(build, users), users, build)
    }

    override fun notifyResponsibleAssigned(buildType: SBuildType, users: Set<SUser>) {
        sendMessage(messageBuilder.responsibleAssigned(buildType, users), users, buildType.project)
    }

    override fun notifyResponsibleAssigned(
        oldValue: TestNameResponsibilityEntry?,
        newValue: TestNameResponsibilityEntry,
        project: SProject,
        users: Set<SUser>
    ) {
        sendMessage(messageBuilder.responsibleAssigned(oldValue, newValue, project, users), users, project)
    }

    override fun notifyResponsibleAssigned(
        testNames: Collection<TestName>,
        entry: ResponsibilityEntry,
        project: SProject,
        users: Set<SUser>
    ) {
        sendMessage(messageBuilder.responsibleAssigned(testNames, entry, project, users), users, project)
    }

    override fun notifyBuildFailing(build: SRunningBuild, users: Set<SUser>) {
        sendMessage(messageBuilder.buildFailing(build, users), users, build)
    }

    private fun sendMessage(message: MessagePayload, users: Set<SUser>, build: Build) {
        val project = projectManager.findProjectByExternalId(build.buildType?.projectExternalId)
        if (project == null) {
            Loggers.SERVER.warn(
                "Won't send notification because can't find project for build" +
                        " ${build.buildType?.buildTypeId ?: ""}/${build.buildId}" +
                        " by external id ${build.buildType?.projectExternalId}."
            )
            return
        }
        sendMessage(message, users, project)
    }

    private fun sendMessage(message: MessagePayload, users: Set<SUser>, project: SProject) {
        for (user in users) {
            sendMessage(message, user, project)
        }
    }

    private fun sendMessage(message: MessagePayload, user: SUser, project: SProject) {
        val sendTo = user.getPropertyValue(descriptor.channelProperty)
        if (sendTo == null) {
            Loggers.SERVER.warn("Won't send Slack notification to user with id ${user.id} as it's missing ${descriptor.channelProperty} property")
            return
        }

        val connectionId = user.getPropertyValue(descriptor.connectionProperty)
        if (connectionId == null) {
            Loggers.SERVER.warn("Won't send Slack notification to user with id ${user.id} as it's missing ${descriptor.connectionProperty} property")
            return
        }

        val token = getToken(project, connectionId)
        if (token == null) {
            Loggers.SERVER.warn(
                "Won't send Slack notification to user with id ${user.id}" +
                        " as no token for connection with id '${connectionId}'" +
                        " in project with external id '${project.externalId}' was found"
            )
            return
        }

        val result = runBlocking { slackApi.postMessage("Bearer $token", message.toSlackMessage(sendTo)).await() }

        if (!result.ok) {
            Loggers.SERVER.warn("Error sending message to $sendTo: ${result.error}")
        }
    }

    private fun getToken(project: SProject, connectionId: String): String? {
        val connection = oauthManager.getAvailableConnectionsOfType(project, SlackConnection.type)
            .find { it.parameters["externalId"] == connectionId } ?: return null

        val token = connection.parameters["secure:token"]
        if (token != null) {
            return token
        }

        return null
    }

    fun getConfig(): SlackNotifierConfig {
        return config
    }

    fun clearAllErrors() {

    }
}

