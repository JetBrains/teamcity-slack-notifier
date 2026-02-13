package jetbrains.buildServer.notification.slackNotifier.healthReport

import jetbrains.buildServer.serverSide.ProjectManager
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
class SlackFailedNotificationExtension(
    pagePlaces: PagePlaces,
    pluginDescriptor: PluginDescriptor,
    private val projectManager: ProjectManager,
    private val webLinks: WebLinks
) : HealthStatusItemPageExtension(SlackFailedNotificationHealthReport.type, pagePlaces) {
    init {
        includeUrl = pluginDescriptor.getPluginResourcesPath("/healthReport/failedNotification.jsp")
        isVisibleOutsideAdminArea = false
        register()
    }

    override fun isAvailable(request: HttpServletRequest): Boolean {
        if (!super.isAvailable(request)) {
            return false
        }

        val statusItem = getStatusItem(request)
        val data = statusItem.additionalData
        val project = data["project"] as? SProject ?: return false
        val user = SessionUser.getUser(request)
        return user.isPermissionGrantedForProject(project.projectId, Permission.EDIT_PROJECT)
    }

    override fun fillModel(model: MutableMap<String, Any>, request: HttpServletRequest) {
        super.fillModel(model, request)

        val statusItem = getStatusItem(request)
        val data = statusItem.additionalData
        model["reason"] = data["reason"] as? String ?: ""
        model["receiver"] = data["receiver"] as? String ?: ""
        model["connectionId"] = data["connectionId"] as? String ?: ""
        model["errorCode"] = data["errorCode"] as? String ?: ""
        model["isThrottlingFailure"] = data["isThrottlingFailure"] as? Boolean ?: false
        model["occurrences"] = data["occurrences"] as? Int ?: 1

        val buildTypeId = data["buildTypeId"] as? String ?: return
        val buildType = projectManager.findBuildTypeByExternalId(buildTypeId)
            ?: return
        model["editUrl"] = webLinks.getEditConfigurationPageUrl(buildTypeId)
        model["buildTypeName"] = buildType.fullName
    }
}
