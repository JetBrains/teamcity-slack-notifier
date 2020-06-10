package jetbrains.buildServer.notification.slackNotifier.notification

import jetbrains.buildServer.users.SUser

interface MessageBuilderFactory {
    fun get(user: SUser): MessageBuilder
}