package jetbrains.buildServer.notification.slackNotifier

import jetbrains.buildServer.controllers.BaseController
import jetbrains.buildServer.controllers.BasePropertiesBean
import jetbrains.buildServer.notification.slackNotifier.slack.AggregatedSlackApi
import jetbrains.buildServer.serverSide.ProjectManager
import jetbrains.buildServer.serverSide.WebLinks
import jetbrains.buildServer.serverSide.auth.Permission
import jetbrains.buildServer.serverSide.oauth.OAuthConnectionsManager
import jetbrains.buildServer.users.UserModel
import jetbrains.buildServer.web.NotificationRulesExtension
import jetbrains.buildServer.web.openapi.PluginDescriptor
import jetbrains.buildServer.web.openapi.WebControllerManager
import org.springframework.context.annotation.Conditional
import org.springframework.stereotype.Service
import org.springframework.web.servlet.ModelAndView
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
    init {
        webControllerManager.registerController(descriptor.editParametersUrl, this)

        registerUserSettingsPageExtension()
    }

    private fun registerUserSettingsPageExtension() {
        val extensionUrl = pluginDescriptor.getPluginResourcesPath("notificationRulesMessage.html")
        NotificationRulesExtension(
            descriptor.type,
            extensionUrl,
            webControllerManager
        ).register()

        webControllerManager.registerController(extensionUrl, object : BaseController() {
            override fun doHandle(p0: HttpServletRequest, p1: HttpServletResponse): ModelAndView? {
                val mv = ModelAndView(pluginDescriptor.getPluginResourcesPath("notificationRulesMessage.jsp"))
                mv.model["editConnectionUrl"] = webLinks.getEditProjectPageUrl("_Root") + "&tab=oauthConnections"
                return mv
            }
        })
    }

    override fun doHandle(request: HttpServletRequest, response: HttpServletResponse): ModelAndView? {
        val mv = ModelAndView(pluginDescriptor.getPluginResourcesPath("editUserSlackNotifierSettings.jsp"))

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

        val slackUserId = user.getPropertyValue(SlackProperties.channelProperty)
        val selectedConnectionId = user.getPropertyValue(SlackProperties.connectionProperty)

        val availableConnections = projectManager.projects.filter {
            user.isPermissionGrantedForProject(it.projectId, Permission.VIEW_PROJECT)
        }.flatMap { project ->
            oAuthConnectionsManager.getAvailableConnectionsOfType(project, SlackConnection.type)
        }.distinctBy { it.id }

        val selectedConnection = availableConnections.find { it.id == selectedConnectionId }

        val slackUsername = selectedConnection?.let { connection ->
            connection.parameters["secure:token"]?.let { token ->
                val slackUser = aggregatedSlackApi.getUsersList(token).find {
                    it.id == slackUserId
                }

                if (slackUser == null && slackUserId != null) {
                    user.getPropertyValue(SlackProperties.displayNameProperty)
                } else {
                    slackUser?.displayName
                }
            }
        }


        mv.model["connectionsBean"] = SlackConnectionsBean(availableConnections, aggregatedSlackApi)
        mv.model["propertiesBean"] = BasePropertiesBean(user.properties.map {
            it.key.key to it.value
        }.toMap())
        mv.model["properties"] = SlackProperties()
        mv.model["user"] = user
        mv.model["slackUsername"] = slackUsername ?: ""
        mv.model["selectedConnection"] = selectedConnectionId

        return mv
    }
}