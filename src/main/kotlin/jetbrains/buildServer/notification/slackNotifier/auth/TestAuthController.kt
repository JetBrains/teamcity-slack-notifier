/*
 *  Copyright 2000-2022 JetBrains s.r.o.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package jetbrains.buildServer.notification.slackNotifier.auth

import com.intellij.openapi.diagnostic.Logger
import jetbrains.buildServer.controllers.BaseController
import jetbrains.buildServer.notification.slackNotifier.SlackNotifierEnabled
import jetbrains.buildServer.notification.slackNotifier.slack.SlackWebApiFactory
import jetbrains.buildServer.web.openapi.PluginDescriptor
import jetbrains.buildServer.web.openapi.WebControllerManager
import jetbrains.buildServer.web.util.WebUtil
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
    private val log = Logger.getInstance(TestAuthController::class.java.name)

    companion object {
        const val PATH = "/admin/slack/auth/test.html"
    }

    init {
        webControllerManager.registerController(PATH, this)
    }

    override fun doHandle(request: HttpServletRequest, response: HttpServletResponse): ModelAndView {
        val code = request.getParameter("code")
        val clientId = request.session.getAttribute("slack.clientId") as? String?
                ?: return authResult(false, "Unexpected error: can't find client id in session")
        val clientSecret = request.session.getAttribute("slack.clientSecret") as? String?
                ?: return authResult(false, "Unexpected error: can't find client secret in session")

        val redirectUrl = WebUtil.getRootUrl(request) + PATH

        val oauthToken = slackApi.oauthAccess(
                clientId = clientId,
                clientSecret = clientSecret,
                code = code,
                redirectUrl = redirectUrl
        )

        if (!oauthToken.ok) {
            if (oauthToken.error == "bad_redirect_uri") {
                warnBadRedirectUriInfo(request)
            }
            return authResult(false, "Test authentication failed: ${oauthToken.error}")
        }

        val userIdentity = slackApi.usersIdentity(oauthToken.accessToken)
        if (!userIdentity.ok) {
            return authResult(false, "Unexpected error: ${userIdentity.error}")
        }

        return authResult(true, "You successfully signed in as ${userIdentity.user.displayName}")
    }

    private fun warnBadRedirectUriInfo(request: HttpServletRequest) {
        log.warn("Internal error: bad_redirect_uri '${request.requestURL}'")
    }

    private fun authResult(success: Boolean, message: String): ModelAndView {
        val mv = ModelAndView(pluginDescriptor.getPluginResourcesPath("auth/test.jsp"))
        mv.model["result"] = TestAuthResult(success, message)
        return mv
    }
}