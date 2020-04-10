package jetbrains.buildServer.notification.slackNotifier

import com.github.salomonbrys.kotson.fromJson
import com.google.gson.Gson
import jetbrains.buildServer.controllers.BaseController
import jetbrains.buildServer.notification.slackNotifier.slack.SlackWebApiFactory
import jetbrains.buildServer.serverSide.ProjectManager
import jetbrains.buildServer.serverSide.auth.SecurityContext
import jetbrains.buildServer.serverSide.oauth.OAuthConnectionsManager
import jetbrains.buildServer.users.SUser
import jetbrains.buildServer.users.User
import jetbrains.buildServer.users.UserModel
import jetbrains.buildServer.web.openapi.WebControllerManager
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
    private val slackWebApiFactory: SlackWebApiFactory,
    private val securityContext: SecurityContext
) : BaseController() {
    companion object {
        const val PATH = "/admin/slack/oauth.html"
    }

    private val slackApi = slackWebApiFactory.createSlackWebApi()

    init {
        webControllerManager.registerController(PATH, this)
    }

    override fun doHandle(request: HttpServletRequest, response: HttpServletResponse): ModelAndView? {
        val state = Gson().fromJson<SlackOAuthState>(request.getParameter("state"))
        val userId = state.userId.toLong()
        val currentUser = getCurrentUser()
        if (currentUser?.id != userId) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "User id '${userId}' does not match current user id")
            return null
        }

        val user = userModel.findUserById(userId)
        if (user == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Can't find user with id '$userId'")
            return null
        }

        val connectionId = state.connectionId
        val connection = projectManager.projects.mapNotNull {
            oAuthConnectionsManager.findConnectionById(it, connectionId)
        }.firstOrNull()
        if (connection == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Can't find connection with id '${connectionId}")
            return null
        }

        val code = request.getParameter("code")
        val redirectUrl = request.requestURL.toString()

        val clientId = connection.parameters["clientId"]
        if (clientId == null) {
            response.sendError(
                HttpServletResponse.SC_BAD_REQUEST,
                "Can't find 'clientId' property in connection with id '${connectionId}'"
            )
            return null
        }

        val clientSecret = connection.parameters["secure:clientSecret"]
        if (clientSecret == null) {
            response.sendError(
                HttpServletResponse.SC_BAD_REQUEST,
                "Can't find 'secure:clientSecret' property in connection with id '${connectionId}'"
            )
            return null
        }

        val oauthToken = slackApi.oauthAccess(
            clientId = clientId,
            clientSecret = clientSecret,
            code = code,
            redirectUrl = redirectUrl
        )

        if (!oauthToken.ok) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Unexpected error: ${oauthToken.error}")
            return null
        }

        val userIdentity = slackApi.usersIdentity(oauthToken.accessToken)
        if (!userIdentity.ok) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Unexpected error: ${userIdentity.error}")
            return null
        }

        val slackUserId = userIdentity.user.id
        user.setUserProperty(SlackProperties.channelProperty, slackUserId)
        user.setUserProperty(SlackProperties.connectionProperty, connectionId)

        return ModelAndView(RedirectView(request.contextPath + "/profile.html?notificatorType=jbSlackNotifier&item=userNotifications"))
    }

    fun getCurrentUser(): SUser? {
        val associatedUser: User =
            securityContext.authorityHolder.associatedUser ?: return null
        return if (SUser::class.java.isAssignableFrom(associatedUser.javaClass)) {
            associatedUser as SUser
        } else {
            userModel.findUserAccount(null, associatedUser.username)
        }
    }

}

private data class SlackOAuthState(val userId: String, val connectionId: String)
