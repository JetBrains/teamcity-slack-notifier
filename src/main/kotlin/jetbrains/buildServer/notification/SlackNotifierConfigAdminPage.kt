package jetbrains.buildServer.notification

import jetbrains.buildServer.controllers.admin.AdminPage
import jetbrains.buildServer.web.openapi.Groupable
import jetbrains.buildServer.web.openapi.PagePlaces
import jetbrains.buildServer.web.openapi.PluginDescriptor
import jetbrains.buildServer.web.openapi.PositionConstraint
import javax.servlet.http.HttpServletRequest

class SlackNotifierConfigAdminPage(
    pagePlaces: PagePlaces,
    private val pluginDescriptor: PluginDescriptor,

    slackNotifier: SlackNotifier,
    slackNotifierDescriptor: SlackNotifierDescriptor
) : AdminPage(pagePlaces) {
    private val config = slackNotifier.getConfig()

    init {
        includeUrl = getEditUrl()
        pluginName = slackNotifierDescriptor.type
        tabTitle = "Slack Notifier"

        setPosition(PositionConstraint.after("email"))
        setPosition(PositionConstraint.before("jabber"))
        register()
    }

    private fun getEditUrl(): String =
        pluginDescriptor.getPluginResourcesPath("editSlackNotifierSettings.jsp")

    override fun fillModel(model: MutableMap<String, Any>, request: HttpServletRequest) {
        super.fillModel(model, request)
        model["slackSettings"] = SlackSettingsBean(config.isPaused, config.botToken)
    }

    override fun getGroup(): String =
        Groupable.SERVER_RELATED_GROUP
}