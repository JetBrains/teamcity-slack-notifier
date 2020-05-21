package jetbrains.buildServer.notification.slackNotifier.auth

import com.github.salomonbrys.kotson.fromJson
import com.google.gson.Gson
import com.intellij.openapi.diagnostic.Logger
import jetbrains.buildServer.controllers.BaseController
import jetbrains.buildServer.notification.slackNotifier.SlackNotifierEnabled
import jetbrains.buildServer.notification.slackNotifier.SlackProperties
import jetbrains.buildServer.notification.slackNotifier.slack.OauthAccessToken
import jetbrains.buildServer.notification.slackNotifier.slack.SlackWebApiFactory
import jetbrains.buildServer.serverSide.ProjectManager
import jetbrains.buildServer.serverSide.oauth.OAuthConnectionsManager
import jetbrains.buildServer.users.UserModel
import jetbrains.buildServer.web.openapi.WebControllerManager
import jetbrains.buildServer.web.util.SessionUser
import org.springframework.context.annotation.Conditional
import org.springframework.stereotype.Service
import org.springframework.web.servlet.ModelAndView
import org.springframework.web.servlet.view.RedirectView
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@Service
@Conditional(SlackNotifierEnabled::class)
class SlackOauthController(
    webControllerManager: WebControllerManager,
    private val userModel: UserModel,
    private val projectManager: ProjectManager,
    private val oAuthConnectionsManager: OAuthConnectionsManager,
    private val slackWebApiFactory: SlackWebApiFactory
) : BaseController() {
    companion object {
        const val PATH = "/slack/oauth.html"
        private val LOG = Logger.getInstance(SlackOauthController::class.java.name)
    }

    private val slackApi = slackWebApiFactory.createSlackWebApi()

    init {
        webControllerManager.registerController(PATH, this)
    }

    override fun doHandle(request: HttpServletRequest, response: HttpServletResponse): ModelAndView? {
        val state = Gson().fromJson<SlackOAuthState>(request.getParameter("state"))
        val userId = state.userId.toLong()
        val currentUser = SessionUser.getUser(request.session)
        if (currentUser?.id != userId) {
            return errorMessage(request, "User id '${userId}' does not match current user id")
        }

        val user = userModel.findUserById(userId)
            ?: return errorMessage(request, "Can't find user with id '$userId'")

        val connectionId = state.connectionId
        val connection = projectManager.projects.mapNotNull {
            oAuthConnectionsManager.findConnectionById(it, connectionId)
        }.firstOrNull()
            ?: return errorMessage(request, "Can't find connection with id '${connectionId}")

        val code = request.getParameter("code")
        val redirectUrl = request.requestURL.toString()

        val clientId = connection.parameters["clientId"]
            ?: return errorMessage(request, "Can't find 'clientId' property in connection with id '${connectionId}'")

        val clientSecret = connection.parameters["secure:clientSecret"]
            ?: return errorMessage(request, "Can't find 'secure:clientSecret' property in connection with id '${connectionId}'")

        val oauthToken = slackApi.oauthAccess(
            clientId = clientId,
            clientSecret = clientSecret,
            code = code,
            redirectUrl = redirectUrl
        )

        if (!oauthToken.ok) {
            return handleOAuthTokenError(request, oauthToken)
        }

        val userIdentity = slackApi.usersIdentity(oauthToken.accessToken)
        if (!userIdentity.ok) {
            return errorMessage(request, "Unexpected error: ${userIdentity.error}")
        }

        val slackUserId = userIdentity.user.id
        user.setUserProperty(SlackProperties.channelProperty, slackUserId)
        user.setUserProperty(SlackProperties.connectionProperty, connectionId)
        user.setUserProperty(SlackProperties.displayNameProperty, userIdentity.user.displayName)

        return redirectToNotifierSettings(request)
    }

    private fun errorMessage(request: HttpServletRequest, message: String): ModelAndView {
        getOrCreateMessages(request).addMessage("settingsError", message)
        return redirectToNotifierSettings(request)
    }

    private fun redirectToNotifierSettings(request: HttpServletRequest): ModelAndView {
        return ModelAndView(
            RedirectView(request.contextPath + "/profile.html?notificatorType=jbSlackNotifier&item=userNotifications")
        )
    }

    private fun handleOAuthTokenError(request: HttpServletRequest, oauthToken: OauthAccessToken): ModelAndView {
        return when (oauthToken.error) {
            "bad_client_secret" -> errorMessage(request, "Error: invalid 'Client secret' field in provided connection")
            "invalid_client_id" -> errorMessage(request, "Error: invalid 'Client ID' field in provided connection")
            "bad_redirect_uri" -> {
                warnBadRedirectUriInfo(request)
                errorMessage(request, "Internal error: bad_redirect_uri. See notification debug logs for more detail")
            }
            else -> errorMessage(request, "Unexpected error: ${oauthToken.error}")
        }
    }

    private fun warnBadRedirectUriInfo(request: HttpServletRequest) {
        LOG.warn("Internal error: bad_redirect_uri '${request.requestURL}'")
    }
}

private data class SlackOAuthState(val userId: String, val connectionId: String)
