

package jetbrains.buildServer.notification.slackNotifier.notification

import jetbrains.buildServer.serverSide.SRunningBuild

interface ServiceMessageNotificationMessageBuilder {
    /**
     * Called when a new service message notification is sent.
     * @param build started build.
     */
    fun buildRelatedNotification(build: SRunningBuild, message: String): MessagePayload
}