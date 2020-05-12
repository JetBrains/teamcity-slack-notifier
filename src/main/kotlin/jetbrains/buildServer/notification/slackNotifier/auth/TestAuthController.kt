package jetbrains.buildServer.notification.slackNotifier.auth

import jetbrains.buildServer.controllers.BaseController
import jetbrains.buildServer.controllers.BaseFormXmlController
import jetbrains.buildServer.notification.slackNotifier.SlackNotifierEnabled
import jetbrains.buildServer.notification.slackNotifier.slack.SlackWebApiFactory
import jetbrains.buildServer.web.openapi.PluginDescriptor
import jetbrains.buildServer.web.openapi.WebControllerManager
import org.springframework.context.annotation.Conditional
import org.springframework.stereotype.Service
import org.springframework.web.servlet.ModelAndView
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@Service
@Conditional(SlackNotifierEnabled::class)
class TestAuthController(
        webControllerManager: WebControllerManager,
        slackWebApiFactory: SlackWebApiFactory,
        private val pluginDescriptor: PluginDescriptor
) : BaseController() {
    private val slackApi = slackWebApiFactory.createSlackWebApi()

    init {
        webControllerManager.registerController("/admin/slack/auth/test.html", this)
    }

    override fun doHandle(request: HttpServletRequest, response: HttpServletResponse): ModelAndView? {
        val code = request.getParameter("code")
        val clientId = request.session.getAttribute("slack.clientId") as? String?
                ?: return authResult(false, "Unexpected error: can't find client id in session")
        val clientSecret = request.session.getAttribute("slack.clientSecret") as? String?
                ?: return authResult(false, "Unexpected error: can't find client secret in session")

        val redirectUrl = request.requestURL.toString()

        val oauthToken = slackApi.oauthAccess(
                clientId = clientId,
                clientSecret = clientSecret,
                code = code,
                redirectUrl = redirectUrl
        )

        if (!oauthToken.ok) {
            return authResult(false, "Test authentication failed: ${oauthToken.error}")
        }

        val userIdentity = slackApi.usersIdentity(oauthToken.accessToken)
        if (!userIdentity.ok) {
            return authResult(false, "Unexpected error: ${userIdentity.error}")
        }

        return authResult(true, "You successfully singed in as ${userIdentity.user.displayName}")
    }

    private fun authResult(success: Boolean, message: String): ModelAndView {
        val mv = ModelAndView(pluginDescriptor.getPluginResourcesPath("auth/test.jsp"))
        mv.model["result"] = TestAuthResult(success, message)
        return mv
    }
}