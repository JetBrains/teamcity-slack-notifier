package jetbrains.buildServer.notification.slackNotifier.auth

import jetbrains.buildServer.controllers.BaseFormXmlController
import jetbrains.buildServer.controllers.BasePropertiesBean
import jetbrains.buildServer.controllers.admin.projects.PluginPropertiesUtil
import jetbrains.buildServer.notification.slackNotifier.SlackNotifierEnabled
import jetbrains.buildServer.notification.slackNotifier.slack.AggregatedSlackApi
import jetbrains.buildServer.web.openapi.WebControllerManager
import org.jdom.Element
import org.springframework.context.annotation.Conditional
import org.springframework.stereotype.Service
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@Service
@Conditional(SlackNotifierEnabled::class)
class PrepareForTestController(
        webControllerManager: WebControllerManager,
        private val slackApi: AggregatedSlackApi
) : BaseFormXmlController() {

    init {
        webControllerManager.registerController("/admin/slack/auth/prepareForTest.html", this)
    }

    override fun doPost(request: HttpServletRequest, response: HttpServletResponse, xmlElement: Element) {
        val props = getProps(request)
        val clientId = props["clientId"]
        if (clientId == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "'clientId' parameter is required")
            return
        }

        val clientSecret = props["secure:clientSecret"]
        if (clientSecret == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "'secure:clientSecret' parameter is required")
            return
        }

        request.session.setAttribute("slack.clientId", clientId)
        request.session.setAttribute("slack.clientSecret", clientSecret)

        val botToken = props["secure:token"]
        if (botToken == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "'secure:token' parameter is required")
            return
        }

        val bot = slackApi.getBot(botToken)
        val teamId = bot.teamId
        if (teamId == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Can't find team for provided bot token")
            return
        }

        val teamIdXml = Element("teamId")
        teamIdXml.addContent(teamId)
        xmlElement.addContent(teamIdXml)
    }

    private fun getProps(request: HttpServletRequest): Map<String, String> {
        val propBean = BasePropertiesBean(emptyMap())
        PluginPropertiesUtil.bindPropertiesFromRequest(request, propBean)
        return propBean.properties
    }

    override fun doGet(p0: HttpServletRequest, p1: HttpServletResponse) = null
}
