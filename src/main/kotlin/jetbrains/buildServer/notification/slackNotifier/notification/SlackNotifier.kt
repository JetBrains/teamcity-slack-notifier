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
import jetbrains.buildServer.notification.*
import jetbrains.buildServer.notification.slackNotifier.*
import jetbrains.buildServer.notification.slackNotifier.logging.ThrottlingLogger
import jetbrains.buildServer.notification.slackNotifier.slack.SlackWebApiFactory
import jetbrains.buildServer.responsibility.ResponsibilityEntry
import jetbrains.buildServer.responsibility.TestNameResponsibilityEntry
import jetbrains.buildServer.serverSide.*
import jetbrains.buildServer.serverSide.impl.LogUtil
import jetbrains.buildServer.serverSide.mute.MuteInfo
import jetbrains.buildServer.serverSide.oauth.OAuthConnectionDescriptor
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
    private val adHocMessageBuilder: PlainAdHocMessageBuilder,
    private val projectManager: ProjectManager,
    private val oauthManager: OAuthConnectionsManager,
    private val descriptor: SlackNotifierDescriptor,
    private val notificationCountHandler: BuildPromotionNotificationCountHandler,
    private val domainNameFinder: DomainNameFinder
) : NotificatorAdapter(), ServiceMessageNotifier, PositionAware {

    private val slackApi = slackApiFactory.createSlackWebApi()

    private val logger = Logger.getInstance(SlackNotifier::class.java.name)
    private val throttlingLogger = ThrottlingLogger(logger)

    init {
        notifierRegistry.register(this)
    }

    override fun getDisplayName(): String = descriptor.getDisplayName()

    override fun getNotificatorType(): String = descriptor.getType()

    override fun getServiceMessageNotifierType(): String = descriptor.getServiceMessageNotifierType()

    override fun notifyTestsMuted(tests: Collection<STest>, muteInfo: MuteInfo, users: Set<SUser>) {
        val project = muteInfo.project ?: return
        forReceiver(users, project) { receiver, messageBuilder ->
            sendMessage(messageBuilder.testsMuted(tests, muteInfo), receiver, project)
        }
    }

    private fun forReceiver(users: Set<SUser>, project: SProject, block: (receiver: SUser, messageBuilder: MessageBuilder) -> Unit) {
        for (user in users) {
            block(user, messageBuilderFactory.get(user, project))
        }
    }

    override fun notifyBuildProblemsUnmuted(
            buildProblems: Collection<BuildProblemInfo>,
            muteInfo: MuteInfo,
            user: SUser?,
            users: Set<SUser>
    ) {
        val project = muteInfo.project ?: return
        forReceiver(users, project) { receiver, messageBuilder ->
            sendMessage(messageBuilder.buildProblemsUnmuted(buildProblems, muteInfo, receiver), receiver, project)
        }
    }

    override fun notifyQueuedBuildWaitingForApproval(queuedBuild: SQueuedBuild, users: MutableSet<SUser>) {
        forReceiver(users, queuedBuild.buildType.project) { receiver, messageBuilder ->
            sendMessage(messageBuilder.queuedBuildWaitingForApproval(queuedBuild), receiver, queuedBuild)
        }
    }

    override fun notifyLabelingFailed(build: Build, root: VcsRoot, exception: Throwable, users: Set<SUser>) {
        val project = projectManager.findProjectById(build.buildType?.projectId) ?: return
        forReceiver(users, project) { receiver, messageBuilder ->
            sendMessage(messageBuilder.labelingFailed(build, root, exception), receiver, build)
        }
    }

    override fun notifyResponsibleChanged(buildType: SBuildType, users: Set<SUser>) {
        forReceiver(users, buildType.project) { receiver, messageBuilder ->
            sendMessage(messageBuilder.responsibleChanged(buildType), receiver, buildType.project)
        }
    }

    override fun notifyResponsibleChanged(
        oldValue: TestNameResponsibilityEntry?,
        newValue: TestNameResponsibilityEntry,
        project: SProject,
        users: Set<SUser>
    ) {
        forReceiver(users, project) { receiver, messageBuilder ->
            sendMessage(messageBuilder.responsibleChanged(oldValue, newValue, project), receiver, project)
        }
    }

    override fun notifyResponsibleChanged(
        testNames: Collection<TestName>,
        entry: ResponsibilityEntry,
        project: SProject,
        users: Set<SUser>
    ) {
        forReceiver(users, project) { receiver, messageBuilder ->
            sendMessage(messageBuilder.responsibleChanged(testNames, entry, project), receiver, project)
        }
    }

    override fun notifyBuildSuccessful(build: SRunningBuild, users: Set<SUser>) {
        val project = build.buildType?.project ?: return
        forReceiver(users, project) { receiver, messageBuilder ->
            sendMessage(messageBuilder.buildSuccessful(build), receiver, build)
        }
    }


    override fun notifyBuildFailed(build: SRunningBuild, users: Set<SUser>) {
        val project = build.buildType?.project ?: return
        forReceiver(users, project) { receiver, messageBuilder ->
            sendMessage(messageBuilder.buildFailed(build), receiver, build)
        }
    }

    override fun notifyBuildProblemResponsibleChanged(
        buildProblems: Collection<BuildProblemInfo>,
        entry: ResponsibilityEntry,
        project: SProject,
        users: Set<SUser>
    ) {
        forReceiver(users, project) { receiver, messageBuilder ->
            sendMessage(messageBuilder.buildProblemResponsibleChanged(buildProblems, entry, project), receiver, project)
        }
    }

    override fun notifyBuildProblemsMuted(
        buildProblems: Collection<BuildProblemInfo>,
        muteInfo: MuteInfo,
        users: Set<SUser>
    ) {
        val project = muteInfo.project ?: return
        forReceiver(users, project) { receiver, messageBuilder ->
            sendMessage(messageBuilder.buildProblemsMuted(buildProblems, muteInfo), receiver, project)
        }
    }

    override fun notifyBuildFailedToStart(build: SRunningBuild, users: Set<SUser>) {
        val project = build.buildType?.project ?: return
        forReceiver(users, project) { receiver, messageBuilder ->
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
        forReceiver(users, project) { receiver, messageBuilder ->
            sendMessage(messageBuilder.testsUnmuted(tests, muteInfo, user), receiver, project)
        }
    }

    override fun notifyBuildProbablyHanging(build: SRunningBuild, users: Set<SUser>) {
        val project = build.buildType?.project ?: return
        forReceiver(users, project) { receiver, messageBuilder ->
            sendMessage(messageBuilder.buildProbablyHanging(build), receiver, build)
        }
    }

    override fun notifyBuildProblemResponsibleAssigned(
        buildProblems: Collection<BuildProblemInfo>,
        entry: ResponsibilityEntry,
        project: SProject,
        users: Set<SUser>
    ) {
        forReceiver(users, project) { receiver, messageBuilder ->
            sendMessage(
                    messageBuilder.buildProblemResponsibleAssigned(buildProblems, entry, project),
                    receiver,
                    project
            )
        }
    }

    override fun notifyBuildStarted(build: SRunningBuild, users: Set<SUser>) {
        val project = build.buildType?.project ?: return
        forReceiver(users, project) { receiver, messageBuilder ->
            sendMessage(messageBuilder.buildStarted(build), receiver, build)
        }
    }

    override fun notifyResponsibleAssigned(buildType: SBuildType, users: Set<SUser>) {
        forReceiver(users, buildType.project) { receiver, messageBuilder ->
            sendMessage(messageBuilder.responsibleAssigned(buildType), receiver, buildType.project)
        }
    }

    override fun notifyResponsibleAssigned(
        oldValue: TestNameResponsibilityEntry?,
        newValue: TestNameResponsibilityEntry,
        project: SProject,
        users: Set<SUser>
    ) {
        forReceiver(users, project) { receiver, messageBuilder ->
            sendMessage(messageBuilder.responsibleAssigned(oldValue, newValue, project), receiver, project)
        }
    }

    override fun notifyResponsibleAssigned(
        testNames: Collection<TestName>,
        entry: ResponsibilityEntry,
        project: SProject,
        users: Set<SUser>
    ) {
        forReceiver(users, project) { receiver, messageBuilder ->
            sendMessage(messageBuilder.responsibleAssigned(testNames, entry, project), receiver, project)
        }
    }

    override fun notifyBuildFailing(build: SRunningBuild, users: Set<SUser>) {
        val project = build.buildType?.project ?: return
        forReceiver(users, project) { receiver, messageBuilder ->
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

    private fun sendMessage(message: MessagePayload, user: SUser, queuedBuild: SQueuedBuild) {
        val buildType = queuedBuild.buildType
        val project = projectManager.findProjectByExternalId(buildType.projectExternalId)
        if (project == null) {
            logger.error(
                    "Won't send notification because can't find project for queued build" +
                            " ${buildType.buildTypeId}/${queuedBuild.buildPromotion.id}" +
                            " by external id ${buildType.projectExternalId}."
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
            logger.warn("Error sending message to $sendTo: ${result.error}")
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

    @Throws(ServiceMessageNotificationException::class)
    private fun sendMessage(message: MessagePayload, sendTo: String, token: String) {
        if (!TeamCityProperties.getBooleanOrTrue(SlackNotifierProperties.sendNotifications)) {
            throw ServiceMessageNotificationException("Slack notifications are disabled server-wide. Not sending $message to channel with id '$sendTo'.")
        }

        val result = slackApi.postMessage(token, message.toSlackMessage(sendTo))

        if (!result.ok) {
            throw ServiceMessageNotificationException("Error sending message to $sendTo: ${result.error}")
        }
    }

    @Throws(ServiceMessageNotificationException::class)
    override fun sendBuildRelatedNotification(
        message: String, runningBuild: SRunningBuild, parameters: MutableMap<String, String>
    ) {
        val buildType = runningBuild.buildType

        if (buildType == null) {
            logger.warn("Could not find a build configuration for ${LogUtil.describe(runningBuild)}")
            return
        }

        val project = buildType.project

        val connectionDescriptor = findConnectionForAdHocNotification(project, parameters)

        checkAdHocNotificationLimit(runningBuild, connectionDescriptor)

        checkExternalDomainsInMessage(message, connectionDescriptor)

        val token = getToken(project, connectionDescriptor.id)
            ?: throw ServiceMessageNotificationException(
                "No token for connection with id '${connectionDescriptor.id}'" +
                        " in project with external id '${project.externalId}' was found"
            )

        val sendTo = parameters["sendTo"]
            ?: throw ServiceMessageNotificationException("'sendTo' argument was not specified for message $message, build ID ${runningBuild.buildId}")

        val processedMessagePayload = adHocMessageBuilder.buildRelatedNotification(
            runningBuild,
            message
        )

        sendMessage(processedMessagePayload, sendTo, token)
    }

    @Throws(ServiceMessageNotificationException::class)
    private fun findConnectionForAdHocNotification(
        project: SProject,
        parameters: MutableMap<String, String>
    ): OAuthConnectionDescriptor {
        val connectionId = parameters["connectionId"]

        if (connectionId != null) {
            return oauthManager.findConnectionById(project, connectionId)
                ?: throw ServiceMessageNotificationException("Could not resolve Slack connection by ID '$connectionId'")
        }

        val descriptors = oauthManager
            .getAvailableConnectionsOfType(project, SlackConnection.type)
            .filter { desc -> getMaxAdHocNotificationsPerBuild(desc) > 0 }

        when {
            descriptors.size == 1 -> {
                return descriptors.first()
            }
            descriptors.isEmpty() -> {
                throw ServiceMessageNotificationException("Could not find any suitable Slack connection with service message notifications enabled")
            }
            else -> {
                throw ServiceMessageNotificationException("More than one suitable Slack connection was found, please specify 'connectionId' argument to explicitly select connection")
            }
        }
    }

    private fun getMaxAdHocNotificationsPerBuild(descriptor: OAuthConnectionDescriptor): Int {
        val rawLimitValue = descriptor.parameters["adHocMaxNotificationsPerBuild"] ?: return 0

        try {
            return rawLimitValue.toInt()
        } catch (e: NumberFormatException) {
            throw ServiceMessageNotificationException("Could not resolve notification limit from '$rawLimitValue' value", e)
        }
    }

    @Synchronized
    @Throws(ServiceMessageNotificationException::class)
    private fun checkAdHocNotificationLimit(
        runningBuild: SRunningBuild,
        connectionDescriptor: OAuthConnectionDescriptor
    ) {
        val buildPromotion = runningBuild.buildPromotion

        val limit = getMaxAdHocNotificationsPerBuild(connectionDescriptor)

        val notifierId = "${serviceMessageNotifierType}_${connectionDescriptor.id}"

        val currentCount = notificationCountHandler.getCounter(
            buildPromotion,
            notifierId
        )

        if (currentCount >= limit) {
            throw ServiceMessageNotificationException("Reached limit of $limit service message Slack notifications per build")
        }

        notificationCountHandler.incrementCounter(
            buildPromotion,
            notifierId
        )
    }

    @Throws(ServiceMessageNotificationException::class)
    private fun checkExternalDomainsInMessage(
        message: String,
        descriptor: OAuthConnectionDescriptor
    ) {
        val allowedDomainNamePatternsString = descriptor.parameters["adHocAllowedDomainNames"]
            ?: ""

        val allowedPatterns = domainNameFinder.getPatterns(allowedDomainNamePatternsString)

        val externalDomains = domainNameFinder.findExternalDomains(
            message,
            allowedPatterns
        )

        if (externalDomains.isNotEmpty()) {
            throw ServiceMessageNotificationException(
                "Found external domains that are not allowed by configuration: " +
                        "[${externalDomains.joinToString(" ,")}]"
            )
        }
    }

    override fun getOrderId(): String = notificatorType

    override fun getConstraint(): PositionConstraint = PositionConstraint.after("email")
}

