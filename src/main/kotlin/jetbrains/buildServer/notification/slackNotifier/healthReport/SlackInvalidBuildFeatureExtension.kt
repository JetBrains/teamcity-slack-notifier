package jetbrains.buildServer.notification.slackNotifier.healthReport

import jetbrains.buildServer.serverSide.BuildTypeNotFoundException
import jetbrains.buildServer.serverSide.ProjectManager
import jetbrains.buildServer.serverSide.SBuildFeatureDescriptor
import jetbrains.buildServer.serverSide.WebLinks
import jetbrains.buildServer.web.openapi.PagePlaces
import jetbrains.buildServer.web.openapi.PluginDescriptor
import jetbrains.buildServer.web.openapi.healthStatus.HealthStatusItemPageExtension
import org.springframework.stereotype.Service
import javax.servlet.http.HttpServletRequest


@Service
class SlackInvalidBuildFeatureExtension(
    pagePlaces: PagePlaces,
    pluginDescriptor: PluginDescriptor,
    private val webLinks: WebLinks,
    private val projectManager: ProjectManager
) : HealthStatusItemPageExtension(SlackBuildFeatureHealthReport.type, pagePlaces) {
    init {
        includeUrl = pluginDescriptor.getPluginResourcesPath("/healthReport/invalidBuildFeature.jsp")
        isVisibleOutsideAdminArea = false
        register()
    }

    override fun fillModel(model: MutableMap<String, Any>, request: HttpServletRequest) {
        super.fillModel(model, request)
        val statusItem = getStatusItem(request)
        val data = statusItem.additionalData
        model["reason"] = data["reason"] as String
        model["feature"] = data["feature"] as SBuildFeatureDescriptor

        val type = data["type"] as String
        val id = data["buildTypeId"] as String
        if (type == "buildType") {
            model["editUrl"] = webLinks.getEditConfigurationPageUrl(id)
            val buildType = projectManager.findBuildTypeByExternalId(id)
                ?: throw BuildTypeNotFoundException("Can't find build type with external id '${id}'")
            model["buildTypeName"] = buildType.fullName
        } else {
            model["editUrl"] = webLinks.getEditTemplatePageUrl(id)
            val template = projectManager.findBuildTypeTemplateByExternalId(id)
                ?: throw BuildTypeNotFoundException("Can't find build template type with external id '${id}'")
            model["buildTypeName"] = template.fullName
        }
    }
}