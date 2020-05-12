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
        val clientId = props["clientId"] ?: return respondError(xmlElement, "'clientId' parameter is required")
        val clientSecret = props["secure:clientSecret"] ?: return respondError(xmlElement, "'secure:clientSecret' parameter is required")
        request.session.setAttribute("slack.clientId", clientId)
        request.session.setAttribute("slack.clientSecret", clientSecret)
        val botToken = props["secure:token"] ?: return respondError(xmlElement, "'secure:token' parameter is required")
        val bot = slackApi.getBot(botToken)
        val teamId = bot.teamId ?: return respondError(xmlElement, "Invalid bot token")
        respondTeamId(xmlElement, teamId)
    }

    private fun getProps(request: HttpServletRequest): Map<String, String> {
        val propBean = BasePropertiesBean(emptyMap())
        PluginPropertiesUtil.bindPropertiesFromRequest(request, propBean)
        return propBean.properties
    }

    private fun respondError(xmlElement: Element, error: String) {
        val errorXml = Element("error")
        errorXml.addContent(error)
        xmlElement.addContent(errorXml)
    }

    private fun respondTeamId(xmlElement: Element, teamId: String) {
        val teamIdXml = Element("teamId")
        teamIdXml.addContent(teamId)
        xmlElement.addContent(teamIdXml)
    }

    override fun doGet(p0: HttpServletRequest, p1: HttpServletResponse) = null
}
