package jetbrains.buildServer.notification.slackNotifier

import jetbrains.buildServer.controllers.BaseController
import jetbrains.buildServer.controllers.FormUtil
import jetbrains.buildServer.notification.slackNotifier.SlackNotifier
import jetbrains.buildServer.notification.slackNotifier.SlackSettingsBean
import jetbrains.buildServer.web.openapi.WebControllerManager
import org.springframework.web.servlet.ModelAndView
import org.springframework.web.servlet.view.RedirectView
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class SlackNotifierConfigController(
    private val webControllerManager: WebControllerManager,
    private val slackNotifier: SlackNotifier
) : BaseController() {

    private val slackNotifierConfig = slackNotifier.getConfig()

    init {
        webControllerManager.registerController("/admin/jbSlackNotifier/notifierSettings.html", this)
    }

    override fun doHandle(request: HttpServletRequest, response: HttpServletResponse): ModelAndView? {
        val bean = SlackSettingsBean(
            slackNotifierConfig.isPaused,
            slackNotifierConfig.botToken
        )
        FormUtil.bindFromRequest(request, bean)
        slackNotifierConfig.isPaused = bean.isPaused
        slackNotifierConfig.botToken = bean.botToken
        slackNotifierConfig.save()

        return ModelAndView(RedirectView(request.contextPath + "/admin/admin.html?item=jbSlackNotifier"))
    }

}