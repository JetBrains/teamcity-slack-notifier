package jetbrains.buildServer.notification.slackNotifier

object SlackNotifierProperties {
    const val enable = "teamcity.internal.notification.jbSlackNotifier.enable"

    const val cacheExpire = "teamcity.internal.notification.jbSlackNotifier.cache.expireSeconds"
    const val maximumChannelsToCache = "teamcity.internal.notification.jbSlackNotifier.cache.maxChannels"
    const val maximumUsersToCache = "teamcity.internal.notification.jbSlackNotifier.cache.maxUsers"
    const val maximumConversationMembersToCache = "teamcity.internal.notification.jbSlackNotifier.cache.maxConversationMembers"

    const val requestTimeout = "teamcity.internal.notification.jbSlackNotifier.request.timeoutMs"

    const val loggerThrottleTimeout = "teamcity.internal.notification.jbSlackNotifier.logger.throttleTimeoutMinutes"
}
