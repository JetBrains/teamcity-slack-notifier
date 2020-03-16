package jetbrains.buildServer.slackNotifications

import jetbrains.buildServer.controllers.BaseController
import jetbrains.buildServer.controllers.BasePropertiesBean
import jetbrains.buildServer.serverSide.BuildTypeNotFoundException
import jetbrains.buildServer.serverSide.BuildTypeSettings
import jetbrains.buildServer.serverSide.ProjectManager
import jetbrains.buildServer.serverSide.oauth.OAuthConnectionsManager
import jetbrains.buildServer.web.openapi.PluginDescriptor
import jetbrains.buildServer.web.openapi.WebControllerManager
import org.springframework.web.servlet.ModelAndView
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class BuildTypeSlackNotifierSettingsController(
    private val pluginDescriptor: PluginDescriptor,
    private val projectManager: ProjectManager,
    private val oAuthConnectionsManager: OAuthConnectionsManager,
    private val webControllerManager: WebControllerManager,
    private val slackNotifierDescriptor: SlackNotifierDescriptor
) : BaseController() {

    init {
        webControllerManager.registerController(slackNotifierDescriptor.editParametersUrl, this)
    }

    override fun doHandle(request: HttpServletRequest, response: HttpServletResponse): ModelAndView? {
        val mv = ModelAndView(pluginDescriptor.getPluginResourcesPath("editBuildTypeSlackNotifierSettings.jsp"))

        val buildTypeId = request.getParameter("buildTypeId")
        val featureId = request.getParameter("featureId")

        var buildType: BuildTypeSettings? = null
        when {
            buildTypeId.startsWith("buildType:") -> {
                buildType = projectManager.findBuildTypeByExternalId(buildTypeId.substring("buildType:".length))
            }
            buildTypeId.startsWith("template:") -> {
                buildType = projectManager.findBuildTypeTemplateByExternalId(buildTypeId.substring("template:".length))
            }
        }

        if (buildType == null) {
            throw BuildTypeNotFoundException("Can't find build type or build template with id '${buildTypeId}'")
        }

        val project = buildType.project

        val availableConnections = oAuthConnectionsManager.getAvailableConnectionsOfType(project, SlackConnection.type)
        mv.model["availableConnections"] = availableConnections

        val feature = buildType.findBuildFeatureById(featureId)

        mv.model["propertiesBean"] = BasePropertiesBean(feature?.parameters)
        mv.model["descriptor"] = slackNotifierDescriptor

        return mv
    }
}