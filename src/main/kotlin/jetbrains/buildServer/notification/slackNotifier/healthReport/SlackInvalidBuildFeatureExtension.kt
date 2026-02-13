

package jetbrains.buildServer.notification.slackNotifier.healthReport

import jetbrains.buildServer.serverSide.BuildTypeNotFoundException
import jetbrains.buildServer.serverSide.ProjectManager
import jetbrains.buildServer.serverSide.SBuildFeatureDescriptor
import jetbrains.buildServer.serverSide.SProject
import jetbrains.buildServer.serverSide.WebLinks
import jetbrains.buildServer.serverSide.auth.Permission
import jetbrains.buildServer.web.openapi.PagePlaces
import jetbrains.buildServer.web.openapi.PluginDescriptor
import jetbrains.buildServer.web.openapi.healthStatus.HealthStatusItemPageExtension
import jetbrains.buildServer.web.util.SessionUser
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

    override fun isAvailable(request: HttpServletRequest): Boolean {
        if (!super.isAvailable(request)) {
            return false
        }

        val user = SessionUser.getUser(request)

        val statusItem = getStatusItem(request)
        val data = statusItem.additionalData
        val buildTypeId = data["buildTypeId"] as? String
        if (buildTypeId != null) {
            val buildType = projectManager.findBuildTypeByExternalId(buildTypeId)
            if (buildType != null) {
                return user.isPermissionGrantedForProject(buildType.projectId, Permission.EDIT_PROJECT)
            }

            val template = projectManager.findBuildTypeTemplateByExternalId(buildTypeId) ?: return false
            return user.isPermissionGrantedForProject(template.projectId, Permission.EDIT_PROJECT)
        }

        val project = data["project"] as? SProject ?: return false
        return user.isPermissionGrantedForProject(project.projectId, Permission.EDIT_PROJECT)
    }

    override fun fillModel(model: MutableMap<String, Any>, request: HttpServletRequest) {
        super.fillModel(model, request)
        val statusItem = getStatusItem(request)
        val data = statusItem.additionalData
        model["reason"] = data["reason"] as String

        val feature = data["feature"] as? SBuildFeatureDescriptor
        if (feature == null) {
            val failedFeaturesCount = data["failedFeaturesCount"] as? Int ?: 0
            model["failedFeaturesCount"] = failedFeaturesCount
            return
        }
        model["feature"] = feature

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
