package jetbrains.buildServer.notification.slackNotifier.healthReport

import jetbrains.buildServer.serverSide.oauth.OAuthConnectionDescriptor
import jetbrains.buildServer.web.openapi.PagePlaces
import jetbrains.buildServer.web.openapi.PluginDescriptor
import jetbrains.buildServer.web.openapi.healthStatus.HealthStatusItemPageExtension
import org.springframework.stereotype.Service
import javax.servlet.http.HttpServletRequest

@Service
class SlackInvalidConnectionExtension(
    pagePlaces: PagePlaces,
    pluginDescriptor: PluginDescriptor
) : HealthStatusItemPageExtension(SlackConnectionHealthReport.type, pagePlaces) {
    init {
        includeUrl = pluginDescriptor.getPluginResourcesPath("/healthReport/invalidConnection.jsp")
        isVisibleOutsideAdminArea = false
        register()
    }

    override fun fillModel(model: MutableMap<String, Any>, request: HttpServletRequest) {
        super.fillModel(model, request)

        val statusItem = getStatusItem(request)

        val data = statusItem.additionalData
        model["connection"] = data["connection"] as OAuthConnectionDescriptor
        model["reason"] = data["reason"] as String
    }
}