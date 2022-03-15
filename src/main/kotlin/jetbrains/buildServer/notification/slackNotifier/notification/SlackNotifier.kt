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

import com.intellij.openapi.diagnostic.Logger
import jetbrains.buildServer.Build
import jetbrains.buildServer.log.Loggers
import jetbrains.buildServer.notification.NotificatorAdapter
import jetbrains.buildServer.notification.NotificatorRegistry
import jetbrains.buildServer.notification.slackNotifier.SlackNotifierDescriptor
import jetbrains.buildServer.notification.slackNotifier.SlackNotifierEnabled
import jetbrains.buildServer.notification.slackNotifier.SlackNotifierProperties
import jetbrains.buildServer.notification.slackNotifier.SlackProperties
import jetbrains.buildServer.notification.slackNotifier.logging.ThrottlingLogger
import jetbrains.buildServer.notification.slackNotifier.slack.SlackWebApiFactory
import jetbrains.buildServer.responsibility.ResponsibilityEntry
import jetbrains.buildServer.responsibility.TestNameResponsibilityEntry
import jetbrains.buildServer.serverSide.*
import jetbrains.buildServer.serverSide.mute.MuteInfo
import jetbrains.buildServer.serverSide.oauth.OAuthConnectionsManager
import jetbrains.buildServer.serverSide.problems.BuildProblemInfo
import jetbrains.buildServer.tests.TestName
import jetbrains.buildServer.users.SUser
import jetbrains.buildServer.util.positioning.PositionAware
import jetbrains.buildServer.util.positioning.PositionConstraint
import jetbrains.buildServer.vcs.VcsRoot
import org.springframework.context.annotation.Conditional
import org.springframework.stereotype.Service

@Service
@Conditional(SlackNotifierEnabled::class)
class SlackNotifier(
        notifierRegistry: NotificatorRegistry,
        slackApiFactory: SlackWebApiFactory,
        private val messageBuilderFactory: ChoosingMessageBuilderFactory,
        private val projectManager: ProjectManager,
        private val oauthManager: OAuthConnectionsManager,

        private val descriptor: SlackNotifierDescriptor
) : NotificatorAdapter(), PositionAware {

    private val slackApi = slackApiFactory.createSlackWebApi()

    private val logger = Logger.getInstance(SlackNotifier::class.java.name)
    private val throttlingLogger = ThrottlingLogger(logger)

    init {
        notifierRegistry.register(
            this
        )
    }

    override fun getDisplayName(): String = descriptor.getDisplayName()
    override fun getNotificatorType(): String = descriptor.getType()

    override fun notifyTestsMuted(tests: Collection<STest>, muteInfo: MuteInfo, users: Set<SUser>) {
        val project = muteInfo.project ?: return
        forReceiver(users) { receiver, messageBuilder ->
            sendMessage(messageBuilder.testsMuted(tests, muteInfo), receiver, project)
        }
    }

    private fun forReceiver(users: Set<SUser>, block: (receiver: SUser, messageBuilder: MessageBuilder) -> Unit) {
        for (user in users) {
            block(user, messageBuilderFactory.get(user))
        }
    }

    override fun notifyBuildProblemsUnmuted(
            buildProblems: Collection<BuildProblemInfo>,
            muteInfo: MuteInfo,
            user: SUser?,
            users: Set<SUser>
    ) {
        val project = muteInfo.project ?: return
        forReceiver(users) { receiver, messageBuilder ->
            sendMessage(messageBuilder.buildProblemsUnmuted(buildProblems, muteInfo, receiver), receiver, project)
        }
    }

    override fun notifyLabelingFailed(build: Build, root: VcsRoot, exception: Throwable, users: Set<SUser>) {
        forReceiver(users) { receiver, messageBuilder ->
            sendMessage(messageBuilder.labelingFailed(build, root, exception), receiver, build)
        }
    }

    override fun notifyResponsibleChanged(buildType: SBuildType, users: Set<SUser>) {
        forReceiver(users) { receiver, messageBuilder ->
            sendMessage(messageBuilder.responsibleChanged(buildType), receiver, buildType.project)
        }
    }

    override fun notifyResponsibleChanged(
        oldValue: TestNameResponsibilityEntry?,
        newValue: TestNameResponsibilityEntry,
        project: SProject,
        users: Set<SUser>
    ) {
        forReceiver(users) { receiver, messageBuilder ->
            sendMessage(messageBuilder.responsibleChanged(oldValue, newValue, project), receiver, project)
        }
    }

    override fun notifyResponsibleChanged(
        testNames: Collection<TestName>,
        entry: ResponsibilityEntry,
        project: SProject,
        users: Set<SUser>
    ) {
        forReceiver(users) { receiver, messageBuilder ->
            sendMessage(messageBuilder.responsibleChanged(testNames, entry, project), receiver, project)
        }
    }

    override fun notifyBuildSuccessful(build: SRunningBuild, users: Set<SUser>) {
        forReceiver(users) { receiver, messageBuilder ->
            sendMessage(messageBuilder.buildSuccessful(build), receiver, build)
        }
    }


    override fun notifyBuildFailed(build: SRunningBuild, users: Set<SUser>) {
        forReceiver(users) { receiver, messageBuilder ->
            sendMessage(messageBuilder.buildFailed(build), receiver, build)
        }
    }

    override fun notifyBuildProblemResponsibleChanged(
        buildProblems: Collection<BuildProblemInfo>,
        entry: ResponsibilityEntry,
        project: SProject,
        users: Set<SUser>
    ) {
        forReceiver(users) { receiver, messageBuilder ->
            sendMessage(messageBuilder.buildProblemResponsibleChanged(buildProblems, entry, project), receiver, project)
        }
    }

    override fun notifyBuildProblemsMuted(
        buildProblems: Collection<BuildProblemInfo>,
        muteInfo: MuteInfo,
        users: Set<SUser>
    ) {
        val project = muteInfo.project ?: return
        forReceiver(users) { receiver, messageBuilder ->
            sendMessage(messageBuilder.buildProblemsMuted(buildProblems, muteInfo), receiver, project)
        }
    }

    override fun notifyBuildFailedToStart(build: SRunningBuild, users: Set<SUser>) {
        forReceiver(users) { receiver, messageBuilder ->
            sendMessage(messageBuilder.buildFailedToStart(build), receiver, build)
        }
    }

    override fun notifyTestsUnmuted(
        tests: Collection<STest>,
        muteInfo: MuteInfo,
        user: SUser?,
        users: Set<SUser>
    ) {
        val project = muteInfo.project ?: return
        forReceiver(users) { receiver, messageBuilder ->
            sendMessage(messageBuilder.testsUnmuted(tests, muteInfo, user), receiver, project)
        }
    }

    override fun notifyBuildProbablyHanging(build: SRunningBuild, users: Set<SUser>) {
        forReceiver(users) { receiver, messageBuilder ->
            sendMessage(messageBuilder.buildProbablyHanging(build), receiver, build)
        }
    }

    override fun notifyBuildProblemResponsibleAssigned(
        buildProblems: Collection<BuildProblemInfo>,
        entry: ResponsibilityEntry,
        project: SProject,
        users: Set<SUser>
    ) {
        forReceiver(users) { receiver, messageBuilder ->
            sendMessage(
                    messageBuilder.buildProblemResponsibleAssigned(buildProblems, entry, project),
                    receiver,
                    project
            )
        }
    }

    override fun notifyBuildStarted(build: SRunningBuild, users: Set<SUser>) {
        forReceiver(users) { receiver, messageBuilder ->
            sendMessage(messageBuilder.buildStarted(build), receiver, build)
        }
    }

    override fun notifyResponsibleAssigned(buildType: SBuildType, users: Set<SUser>) {
        forReceiver(users) { receiver, messageBuilder ->
            sendMessage(messageBuilder.responsibleAssigned(buildType), receiver, buildType.project)
        }
    }

    override fun notifyResponsibleAssigned(
        oldValue: TestNameResponsibilityEntry?,
        newValue: TestNameResponsibilityEntry,
        project: SProject,
        users: Set<SUser>
    ) {
        forReceiver(users) { receiver, messageBuilder ->
            sendMessage(messageBuilder.responsibleAssigned(oldValue, newValue, project), receiver, project)
        }
    }

    override fun notifyResponsibleAssigned(
        testNames: Collection<TestName>,
        entry: ResponsibilityEntry,
        project: SProject,
        users: Set<SUser>
    ) {
        forReceiver(users) { receiver, messageBuilder ->
            sendMessage(messageBuilder.responsibleAssigned(testNames, entry, project), receiver, project)
        }
    }

    override fun notifyBuildFailing(build: SRunningBuild, users: Set<SUser>) {
        forReceiver(users) { receiver, messageBuilder ->
            sendMessage(messageBuilder.buildFailing(build), receiver, build)
        }
    }

    private fun sendMessage(message: MessagePayload, user: SUser, build: Build) {
        val project = projectManager.findProjectByExternalId(build.buildType?.projectExternalId)
        if (project == null) {
            logger.error(
                    "Won't send notification because can't find project for build" +
                            " ${build.buildType?.buildTypeId ?: ""}/${build.buildId}" +
                            " by external id ${build.buildType?.projectExternalId}."
            )
            return
        }
        sendMessage(message, user, project)
    }

    private fun sendMessage(message: MessagePayload, user: SUser, project: SProject) {
        if (!TeamCityProperties.getBooleanOrTrue(SlackNotifierProperties.sendNotifications)) {
            if (Loggers.SERVER.isDebugEnabled) {
                Loggers.SERVER.debug("Slack notifications are disabled. Not sending $message to user with id '${user.id}'.")
            }
            return
        }

        val sendTo = user.getPropertyValue(SlackProperties.channelProperty)
        if (sendTo == null) {
            throttlingLogger.warn("Won't send Slack notification to user with id ${user.id} as it's missing ${SlackProperties.channelProperty} property")
            return
        }

        val connectionId = user.getPropertyValue(SlackProperties.connectionProperty)
        if (connectionId == null) {
            throttlingLogger.warn("Won't send Slack notification to user with id ${user.id} as it's missing ${SlackProperties.connectionProperty} property")
            return
        }

        val token = getToken(project, connectionId)
        if (token == null) {
            throttlingLogger.warn(
                "Won't send Slack notification to user with id ${user.id}" +
                        " as no token for connection with id '${connectionId}'" +
                        " in project with external id '${project.externalId}' was found"
            )
            return
        }

        val result = slackApi.postMessage(token, message.toSlackMessage(sendTo))

        if (!result.ok) {
            logger.error("Error sending message to $sendTo: ${result.error}")
        }
    }

    private fun getToken(project: SProject, connectionId: String): String? {
        val connection = oauthManager.findConnectionById(project, connectionId) ?: return null

        val token = connection.parameters["secure:token"]
        if (token != null) {
            return token
        }

        return null
    }

    override fun getOrderId(): String = notificatorType

    override fun getConstraint(): PositionConstraint = PositionConstraint.after("email")
}

