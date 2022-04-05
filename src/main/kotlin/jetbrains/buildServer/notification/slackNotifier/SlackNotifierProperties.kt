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

package jetbrains.buildServer.notification.slackNotifier

object SlackNotifierProperties {
    const val enable = "teamcity.internal.notification.jbSlackNotifier.enable"
    const val sendNotifications = "teamcity.internal.notification.jbSlackNotifier.notification.enable"

    const val cacheExpire = "teamcity.internal.notification.jbSlackNotifier.cache.expireSeconds"
    const val maximumChannelsToCache = "teamcity.internal.notification.jbSlackNotifier.cache.maxChannels"
    const val maximumUsersToCache = "teamcity.internal.notification.jbSlackNotifier.cache.maxUsers"
    const val maximumConversationMembersToCache =
        "teamcity.internal.notification.jbSlackNotifier.cache.maxConversationMembers"

    const val requestTimeout = "teamcity.internal.notification.jbSlackNotifier.request.timeoutMs"

    const val loggerThrottleTimeout = "teamcity.internal.notification.jbSlackNotifier.logger.throttleTimeoutMinutes"

    const val enableBotAddedHealthReport = "teamcity.internal.notification.jbSlackNotifier.healthReport.botAdded.enable"
}
