package jetbrains.buildServer.notification.slackNotifier

import jetbrains.buildServer.serverSide.oauth.OAuthConnectionDescriptor
import jetbrains.buildServer.web.openapi.PagePlaces
import jetbrains.buildServer.web.openapi.PluginDescriptor
import jetbrains.buildServer.web.openapi.healthStatus.HealthStatusItemPageExtension
import org.springframework.stereotype.Service
import javax.servlet.http.HttpServletRequest

@Service
class SlackConnectionInvalidTokenExtension(
    private val pagePlaces: PagePlaces,
    private val pluginDescriptor: PluginDescriptor
) : HealthStatusItemPageExtension(SlackConnectionHealthReport.type, pagePlaces) {
    init {
        includeUrl = pluginDescriptor.getPluginResourcesPath("/healthReport/invalidToken.jsp")
        isVisibleOutsideAdminArea = false
        register()
    }

    override fun fillModel(model: MutableMap<String, Any>, request: HttpServletRequest) {
        super.fillModel(model, request)

        val statusItem = getStatusItem(request)
        model["category"] = statusItem.category.id

        val data = statusItem.additionalData
        model["connection"] = data["connection"] as OAuthConnectionDescriptor

        if (statusItem.category == SlackConnectionHealthReport.invalidTokenCategory) {
            model["error"] = data["error"] as String
        } else if (statusItem.category == SlackConnectionHealthReport.missingTokenCategory) {
            model["tokenProperty"] = data["tokenProperty"] as String
        }
    }
}