/*
 *  Copyright 2000-2020 JetBrains s.r.o.
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

import jetbrains.buildServer.notification.NotificationBuildStatusProvider
import jetbrains.buildServer.notification.slackNotifier.SlackProperties
import jetbrains.buildServer.notification.slackNotifier.slack.SlackMessageFormatter
import jetbrains.buildServer.serverSide.BuildServerEx
import jetbrains.buildServer.serverSide.RelativeWebLinks
import jetbrains.buildServer.users.SUser
import org.springframework.stereotype.Service

@Service
class VerboseMessageBuilderFactory(
    private val detailsFormatter: DetailsFormatter,
    private val format: SlackMessageFormatter,
    private val links: RelativeWebLinks,
    private val notificationBuildStatusProvider: NotificationBuildStatusProvider,
    private val server: BuildServerEx
) : MessageBuilderFactory {
    companion object {
        const val defaultMaximumNumberOfChanges = 10
    }

    override fun get(user: SUser): MessageBuilder {
        val addBuildStatus = user.getBooleanProperty(SlackProperties.addBuildStatusProperty)
        val addBranch = user.getBooleanProperty(SlackProperties.addBranchProperty)
        val addChanges = user.getBooleanProperty(SlackProperties.addChangesProperty)
        val maximumNumberOfChanges = user.getPropertyValue(SlackProperties.maximumNumberOfChangesProperty)?.toIntOrNull()
                ?: defaultMaximumNumberOfChanges

        return VerboseMessageBuilder(
            PlainMessageBuilder(
                format = format,
                links = links,
                detailsFormatter = detailsFormatter
            ),
            VerboseMessagesOptions(
                addBuildStatus = addBuildStatus,
                addBranch = addBranch,
                addChanges = addChanges,
                maximumNumberOfChanges = maximumNumberOfChanges
            ),
            format,
            links,
            notificationBuildStatusProvider,
            server
        )
    }
}