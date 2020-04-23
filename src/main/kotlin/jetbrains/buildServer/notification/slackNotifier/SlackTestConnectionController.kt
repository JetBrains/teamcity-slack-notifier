package jetbrains.buildServer.notification.slackNotifier

import jetbrains.buildServer.controllers.ActionErrors
import jetbrains.buildServer.controllers.BaseFormXmlController
import jetbrains.buildServer.controllers.BasePropertiesBean
import jetbrains.buildServer.controllers.FormUtil
import jetbrains.buildServer.controllers.admin.projects.PluginPropertiesUtil
import jetbrains.buildServer.serverSide.SBuildServer
import jetbrains.buildServer.web.openapi.WebControllerManager
import org.jdom.Element
import org.springframework.context.annotation.Conditional
import org.springframework.stereotype.Service
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@Service
@Conditional(SlackNotifierEnabled::class)
class SlackTestConnectionController(
        server: SBuildServer,
        webControllerManager: WebControllerManager,
        private val connection: SlackConnection
) : BaseFormXmlController(server) {
    private val path = "/admin/slack/testConnection.html"

    init {
        webControllerManager.registerController(path, this)
    }

    override fun doPost(request: HttpServletRequest, response: HttpServletResponse, xmlResponse: Element) {
        val errors = ActionErrors()
        val props = getProps(request)
        errors.fillErrors(connection.propertiesProcessor, props)
        errors.serialize(xmlResponse)
    }

    private fun getProps(request: HttpServletRequest): Map<String, String> {
        val propBean = BasePropertiesBean(emptyMap())
        PluginPropertiesUtil.bindPropertiesFromRequest(request, propBean)
        return propBean.properties
    }

    override fun doGet(request: HttpServletRequest, response: HttpServletResponse) = null
}