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
import jetbrains.buildServer.slackNotifications.slack.SlackWebApi
import jetbrains.buildServer.tests.TestName
import jetbrains.buildServer.users.PluginPropertyKey
import jetbrains.buildServer.users.SUser
import jetbrains.buildServer.vcs.VcsRoot

class SlackNotifier(
    notifierRegistry: NotificatorRegistry,
    private val slackApi: SlackWebApi,
    private val oauthManager: OAuthConnectionsManager
) : Notificator {
    private val channelPropertyName = "channel"
    private val channelProperty = PluginPropertyKey(channelPropertyName)

    init {
        notifierRegistry.register(
            this,
            listOf(
                UserPropertyInfo(channelPropertyName, "#channel or @name")
            )
        )
    }

    override fun getDisplayName(): String = "Slack Notifier"

    override fun getNotificatorType(): String = "officialSlack"

    override fun notifyTestsMuted(tests: Collection<STest>, muteInfo: MuteInfo, users: Set<SUser>) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun notifyBuildProblemsUnmuted(
        buildProblems: Collection<BuildProblemInfo>,
        muteInfo: MuteInfo,
        user: SUser?,
        users: Set<SUser>
    ) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun notifyLabelingFailed(build: Build, root: VcsRoot, exception: Throwable, users: Set<SUser>) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun notifyResponsibleChanged(buildType: SBuildType, users: Set<SUser>) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun notifyResponsibleChanged(
        oldValue: TestNameResponsibilityEntry?,
        newValue: TestNameResponsibilityEntry,
        project: SProject,
        users: Set<SUser>
    ) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun notifyResponsibleChanged(
        testNames: Collection<TestName>,
        entry: ResponsibilityEntry,
        project: SProject,
        users: Set<SUser>
    ) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun notifyBuildSuccessful(build: SRunningBuild, users: Set<SUser>) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }


    override fun notifyBuildFailed(build: SRunningBuild, users: Set<SUser>) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun notifyBuildProblemResponsibleChanged(
        buildProblems: Collection<BuildProblemInfo>,
        entry: ResponsibilityEntry,
        project: SProject,
        users: Set<SUser>
    ) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun notifyBuildProblemsMuted(
        buildProblems: Collection<BuildProblemInfo>,
        muteInfo: MuteInfo,
        users: Set<SUser>
    ) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun notifyBuildFailedToStart(build: SRunningBuild, users: Set<SUser>) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }


    override fun notifyTestsUnmuted(
        tests: Collection<STest>,
        muteInfo: MuteInfo,
        user: SUser?,
        users: Set<SUser>
    ) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun notifyBuildProbablyHanging(build: SRunningBuild, users: Set<SUser>) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun notifyBuildProblemResponsibleAssigned(
        buildProblems: Collection<BuildProblemInfo>,
        entry: ResponsibilityEntry,
        project: SProject,
        users: Set<SUser>
    ) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun notifyBuildStarted(build: SRunningBuild, users: Set<SUser>) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun notifyResponsibleAssigned(buildType: SBuildType, users: Set<SUser>) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun notifyResponsibleAssigned(
        oldValue: TestNameResponsibilityEntry?,
        newValue: TestNameResponsibilityEntry,
        project: SProject,
        users: Set<SUser>
    ) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun notifyResponsibleAssigned(
        testNames: Collection<TestName>,
        entry: ResponsibilityEntry,
        project: SProject,
        users: Set<SUser>
    ) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun notifyBuildFailing(build: SRunningBuild, users: Set<SUser>) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    private fun sendMessage(message: MessagePayload, users: Set<SUser>, project: SProject) {
        val token = getToken(project)

        if (token == null) {
            Loggers.SERVER.warn("Won't send Slack notification because no connection of type '${SlackConnection.type}' (${SlackConnection.name}) is found in project '${project.fullName}' and its parents")
            return
        }

        for (user in users) {
            sendMessage(token, message, user)
        }
    }

    private fun sendMessage(token: String, message: MessagePayload, user: SUser) {
        val sendTo = user.getPropertyValue(channelProperty)
        if (sendTo == null) {
            Loggers.SERVER.warn("Won't send Slack notification to user with id ${user.id} as it's missing $channelProperty property")
            return
        }

        slackApi.postMessage(token, message.toSlackMessage(sendTo))
    }

    private fun getToken(project: SProject): String? {
        val connections = oauthManager.getAvailableConnectionsOfType(project, SlackConnection.type)
        for (connection in connections) {
            val token = connection.parameters["token"]
            if (token != null) {
                return token
            }
        }

        return null
    }
}