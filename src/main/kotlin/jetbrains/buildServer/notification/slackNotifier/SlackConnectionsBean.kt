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

package jetbrains.buildServer.notification.slackNotifier

import jetbrains.buildServer.notification.slackNotifier.slack.AggregatedBot
import jetbrains.buildServer.notification.slackNotifier.slack.AggregatedSlackApi
import jetbrains.buildServer.notification.slackNotifier.slack.SlackWebApi
import jetbrains.buildServer.serverSide.oauth.OAuthConnectionDescriptor

class SlackConnectionsBean(
        val connections: List<OAuthConnectionDescriptor>,
        private val slackApi: AggregatedSlackApi
) {
    fun getTeamForConnection(connection: OAuthConnectionDescriptor): String? {
        return getBot(connection)?.teamId
    }

    fun getTeamDomainForConnection(connection: OAuthConnectionDescriptor): String? {
        return getBot(connection)?.teamDomain
    }

    private fun getBot(connection: OAuthConnectionDescriptor): AggregatedBot? {
        val token = connection.parameters["secure:token"] ?: return null
        return slackApi.getBot(token)
    }
}
