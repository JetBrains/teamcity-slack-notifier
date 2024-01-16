

package jetbrains.buildServer.notification.slackNotifier.notification

import jetbrains.buildServer.serverSide.SProject
import jetbrains.buildServer.users.SUser

interface MessageBuilderFactory {
    fun get(user: SUser, project: SProject): MessageBuilder
}