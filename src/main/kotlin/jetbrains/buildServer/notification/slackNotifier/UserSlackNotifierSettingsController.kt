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

import jetbrains.buildServer.controllers.BaseController
import jetbrains.buildServer.controllers.BasePropertiesBean
import jetbrains.buildServer.log.Loggers
import jetbrains.buildServer.notification.slackNotifier.notification.VerboseMessageBuilderFactory
import jetbrains.buildServer.notification.slackNotifier.slack.AggregatedSlackApi
import jetbrains.buildServer.notification.slackNotifier.slack.SlackResponseError
import jetbrains.buildServer.notification.slackNotifier.slack.User
import jetbrains.buildServer.serverSide.ProjectManager
import jetbrains.buildServer.serverSide.WebLinks
import jetbrains.buildServer.serverSide.auth.Permission
import jetbrains.buildServer.serverSide.oauth.OAuthConnectionsManager
import jetbrains.buildServer.users.SUser
import jetbrains.buildServer.users.UserModel
import jetbrains.buildServer.web.NotificationRulesExtension
import jetbrains.buildServer.web.openapi.PluginDescriptor
import jetbrains.buildServer.web.openapi.WebControllerManager
import jetbrains.buildServer.web.util.SessionUser
import jetbrains.buildServer.web.util.WebUtil
import org.springframework.context.annotation.Conditional
import org.springframework.stereotype.Service
import org.springframework.web.servlet.ModelAndView
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeoutException
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@Service
@Conditional(SlackNotifierEnabled::class)
class UserSlackNotifierSettingsController(
    private val pluginDescriptor: PluginDescriptor,
    private val projectManager: ProjectManager,
    private val oAuthConnectionsManager: OAuthConnectionsManager,
    private val webControllerManager: WebControllerManager,
    private val descriptor: UserSlackNotifierDescriptor,
    private val userModel: UserModel,
    private val aggregatedSlackApi: AggregatedSlackApi,
    private val webLinks: WebLinks
) : BaseController() {
    private val userNotificationSettingsURL = pluginDescriptor.getPluginResourcesPath("userNotificationSettingsURL.html")

    init {
        webControllerManager.registerController(descriptor.editParametersUrl, this)

        registerUserSettingsPageExtension()
        registerUserNotificationSettingsController()
    }

    private fun registerUserSettingsPageExtension() {
        val extensionUrl = pluginDescriptor.getPluginResourcesPath("notificationRulesMessage.html")
        NotificationRulesExtension(
            descriptor.type,
            extensionUrl,
            webControllerManager
        ).register()

        webControllerManager.registerController(extensionUrl, object : BaseController() {
            override fun doHandle(request: HttpServletRequest, response: HttpServletResponse): ModelAndView? {
                val user = SessionUser.getUser(request) ?: return null

                val mv = ModelAndView(pluginDescriptor.getPluginResourcesPath("notificationRulesMessage.jsp"))

                mv.model["editConnectionUrl"] = webLinks.getEditProjectPageUrl("_Root") + "&tab=oauthConnections"
                mv.model["user"] = user
                mv.model["rootProject"] = projectManager.rootProject
                return mv
            }
        })
    }

    private fun registerUserNotificationSettingsController() {
        webControllerManager.registerController(userNotificationSettingsURL, object : BaseController() {
            override fun doHandle(request: HttpServletRequest, response: HttpServletResponse): ModelAndView? {
                val user = getUser(request, response) ?: return null
                for (property in SlackProperties.notificationProperties) {
                    val value = request.getParameter(property.key)
                    user.setUserProperty(property, value)
                }
                return null
            }
        })
    }

    override fun doHandle(request: HttpServletRequest, response: HttpServletResponse): ModelAndView? {
        val mv = ModelAndView(pluginDescriptor.getPluginResourcesPath("editUserSlackNotifierSettings.jsp"))
        val user = getUser(request, response) ?: return null

        val currentUser = SessionUser.getUser(request)
        if (currentUser == null) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Unauthenticated")
            return null
        }

        val slackUserId = user.getPropertyValue(SlackProperties.channelProperty)
        val selectedConnectionId = user.getPropertyValue(SlackProperties.connectionProperty) ?: ""

        val availableConnections = projectManager.projects.filter {
            user.isPermissionGrantedForProject(it.projectId, Permission.VIEW_PROJECT)
        }.flatMap { project ->
            oAuthConnectionsManager.getAvailableConnectionsOfType(project, SlackConnection.type)
        }.distinctBy { it.id }

        val selectedConnection = availableConnections.find { it.id == selectedConnectionId }

        val slackUsername = selectedConnection?.let { connection ->
            connection.parameters["secure:token"]?.let { token ->
                val users = try {
                    aggregatedSlackApi.getUsersList(token)
                } catch (e: TimeoutException) {
                    emptyList<User>()
                } catch (e: ExecutionException) {
                    emptyList<User>()
                } catch (e: SlackResponseError) {
                    emptyList<User>()
                }
                val slackUser = users.find {
                    it.id == slackUserId
                }

                if (slackUser == null && !slackUserId.isNullOrEmpty()) {
                    user.getPropertyValue(SlackProperties.displayNameProperty)
                } else {
                    slackUser?.displayName
                }
            }
        }

        val defaultProperties = mapOf(
            SlackProperties.maximumNumberOfChangesProperty.key to VerboseMessageBuilderFactory.defaultMaximumNumberOfChanges.toString()
        )

        mv.model["connectionsBean"] = SlackConnectionsBean(availableConnections, aggregatedSlackApi)
        mv.model["propertiesBean"] = BasePropertiesBean(user.properties.filter {
            it.key != SlackProperties.channelProperty
        }.map {
            it.key.key to it.value
        }.toMap(), defaultProperties)
        mv.model["properties"] = SlackProperties()
        mv.model["user"] = user
        mv.model["slackUsername"] = slackUsername ?: ""
        mv.model["selectedConnection"] = selectedConnectionId
        mv.model["displaySettings"] = currentUser.id == user.id
        mv.model["rootUrl"] = WebUtil.getRootUrl(request)
        mv.model["editConnectionUrl"] = webLinks.getEditProjectPageUrl("_Root") + "&tab=oauthConnections"
        mv.model["editNotificationSettingsUrl"] = userNotificationSettingsURL
        mv.model["rootProject"] = projectManager.rootProject

        return mv
    }

    private fun getUser(request: HttpServletRequest, response: HttpServletResponse): SUser? {
        val userId = request.getParameter("holderId")
        if (userId == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "'holderId' parameter is required")
            return null
        }

        val user = userModel.findUserById(userId.toLong())
        if (user == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "User with id '$userId' not found")
            return null
        }

        return user
    }
}