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
        val mv = ModelAndView(pluginDescriptor.getPluginResourcesPath("auth/test.jsp"))

        val code = request.getParameter("code")
        val clientId = request.session.getAttribute("slack.clientId") as? String?
        if (clientId == null) {
            mv.model["result"] = TestAuthResult(false, "Unexpected error: can't find client id in session")
            return mv
        }
        val clientSecret = request.session.getAttribute("slack.clientSecret") as? String?
        if (clientSecret == null) {
            mv.model["result"] = TestAuthResult(false, "Unexpected error: can't find client secret in session")
            return mv
        }

        val redirectUrl = request.requestURL.toString()

        val oauthToken = slackApi.oauthAccess(
                clientId = clientId,
                clientSecret = clientSecret,
                code = code,
                redirectUrl = redirectUrl
        )

        if (!oauthToken.ok) {
            mv.model["result"] = TestAuthResult(false, "Test authentication failed: ${oauthToken.error}")
            return mv
        }

        val userIdentity = slackApi.usersIdentity(oauthToken.accessToken)
        if (!userIdentity.ok) {
            mv.model["result"] = TestAuthResult(false, "Unexpected error: ${userIdentity.error}")
            return mv
        }

        mv.model["result"] = TestAuthResult(true, "You successfully singed in as ${userIdentity.user.displayName}")
        return mv
    }
}