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

import com.intellij.openapi.diagnostic.Logger
import jetbrains.buildServer.controllers.BaseAutocompletionController
import jetbrains.buildServer.controllers.Completion
import jetbrains.buildServer.notification.slackNotifier.slack.AggregatedSlackApi
import jetbrains.buildServer.serverSide.ProjectManager
import jetbrains.buildServer.serverSide.auth.SecurityContext
import jetbrains.buildServer.serverSide.oauth.OAuthConnectionsManager
import jetbrains.buildServer.web.openapi.WebControllerManager
import org.springframework.context.annotation.Conditional
import org.springframework.stereotype.Service
import javax.servlet.http.HttpServletRequest

@Service
@Conditional(SlackNotifierEnabled::class)
class SlackNotifierChannelCompletionController(
    securityContext: SecurityContext,
    webControllerManager: WebControllerManager,
    private val projectManager: ProjectManager,
    private val oAuthConnectionsManager: OAuthConnectionsManager,
    private val descriptor: SlackNotifierDescriptor,
    private val aggregatedSlackApi: AggregatedSlackApi
) : BaseAutocompletionController(securityContext) {

    // Don't move this magic constant to properties as it's optimized for good looking in autocomplete UI
    // Making it too big might mess with UI
    private val maxLabelSize = 50

    private val log = Logger.getInstance(SlackNotifierChannelCompletionController::class.java.name)

    companion object {
        const val url = "/admin/notifications/jbSlackNotifier/autocompleteUserId.html"
    }

    init {
        webControllerManager.registerController(url, this)
    }

    override fun getCompletionData(request: HttpServletRequest): List<Completion> {
        val term = getParameter(request, "term") ?: return mutableListOf()
        val connectionId = getParameter(request, SlackProperties.connectionProperty.key) ?: return mutableListOf()

        val connection = projectManager.projects.asSequence().mapNotNull { project ->
            oAuthConnectionsManager.findConnectionById(project, connectionId)
        }.firstOrNull()

        if (connection == null) {
            log.warn(
                    "Can't compute autocompletion because no connection with id '${connectionId}' found"
            )
            return mutableListOf()
        }

        val token = connection.parameters["secure:token"]
        if (token == null) {
            log.warn("Can't compute autocompletion because no 'secure:token' property is found in connection with id '${connectionId}'")
            return mutableListOf()
        }

        return getCompletion(term, token)
    }

    private fun getParameter(request: HttpServletRequest, name: String): String? {
        val value = request.getParameter(name)
        if (value == null) {
            log.warn("Can't compute autocompletion for request with no '${name}' parameter")
        }
        return value
    }

    private fun getCompletion(term: String, token: String): List<Completion> {
        return getChannelsCompletionWithHeader(term, token) + getUsersCompletionWithHeader(term, token)
    }

    private fun getChannelsCompletionWithHeader(term: String, token: String): List<Completion> {
        val channels = getChannelsCompletion(term, token)
        if (channels.isEmpty()) {
            return channels
        }

        return listOf(Completion("", "Channels", "", false)) + channels
    }

    private fun getChannelsCompletion(term: String, token: String): List<Completion> {
        val channelsList = aggregatedSlackApi.getChannelsList(token)
        val lowercaseTerm = term.toLowerCase()

        return channelsList.filter {
            "#${it.name.toLowerCase()}".contains(lowercaseTerm)
        }.map {
            var purpose = it.purpose?.value ?: ""
            if (purpose.length > maxLabelSize) {
                purpose = purpose.substring(0, maxLabelSize - 3) + "..."
            }

            val channel = "#${it.name}"

            Completion(
                channel,
                channel,
                purpose
            )
        }.sortedBy { it.label }
    }

    private fun getUsersCompletionWithHeader(term: String, token: String): List<Completion> {
        val users = getUsersCompletion(term, token)
        if (users.isEmpty()) {
            return users
        }

        return listOf(Completion("", "Users", "", false)) + users
    }

    private fun getUsersCompletion(term: String, token: String): List<Completion> {
        val usersList = aggregatedSlackApi.getUsersList(token)
        val lowercaseTerm = term.toLowerCase()

        return usersList.filter {
            !it.deleted && it.hasRealName
        }.filter {
            it.realName.toLowerCase().contains(lowercaseTerm) ||
                    "@${it.name}".contains(lowercaseTerm)
        }.map {
            Completion(it.id, it.realName, "@${it.name}")
        }.sortedBy { it.label }
    }
}