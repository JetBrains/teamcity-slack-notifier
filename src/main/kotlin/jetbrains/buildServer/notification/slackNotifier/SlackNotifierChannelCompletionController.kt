package jetbrains.buildServer.notification.slackNotifier

import com.google.common.cache.CacheBuilder
import com.intellij.openapi.diagnostic.Logger
import jetbrains.buildServer.controllers.BaseAutocompletionController
import jetbrains.buildServer.controllers.Completion
import jetbrains.buildServer.notification.slackNotifier.slack.Channel
import jetbrains.buildServer.notification.slackNotifier.slack.SlackList
import jetbrains.buildServer.notification.slackNotifier.slack.SlackWebApiFactory
import jetbrains.buildServer.notification.slackNotifier.slack.User
import jetbrains.buildServer.notification.slackNotifier.teamcity.findBuildTypeSettingsByExternalId
import jetbrains.buildServer.serverSide.BuildTypeNotFoundException
import jetbrains.buildServer.serverSide.ProjectManager
import jetbrains.buildServer.serverSide.TeamCityProperties
import jetbrains.buildServer.serverSide.auth.SecurityContext
import jetbrains.buildServer.serverSide.oauth.OAuthConnectionsManager
import jetbrains.buildServer.web.openapi.WebControllerManager
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit
import javax.servlet.http.HttpServletRequest

@Service
class SlackNotifierChannelCompletionController(
        securityContext: SecurityContext,
        webControllerManager: WebControllerManager,
        private val projectManager: ProjectManager,
        private val oAuthConnectionsManager: OAuthConnectionsManager,
        slackWebApiFactory: SlackWebApiFactory,
        private val descriptor: SlackNotifierDescriptor
) : BaseAutocompletionController(securityContext) {
    private val slackApi = slackWebApiFactory.createSlackWebApi()

    // Minor copy-paste here, but de-duplicating it takes twice as much lines
    private val myChannelsCache = CacheBuilder.newBuilder()
        .expireAfterWrite(
            TeamCityProperties.getLong(SlackNotifierProperties.cacheExpire, 300),
            TimeUnit.SECONDS
        )
        .maximumWeight(TeamCityProperties.getLong(SlackNotifierProperties.maximumChannelsToCache, 50_000))
        .weigher { _: String, channels: List<Channel> -> channels.size }
        .build<String, List<Channel>>()

    private val myUsersCache = CacheBuilder.newBuilder()
        .expireAfterWrite(
            TeamCityProperties.getLong(SlackNotifierProperties.cacheExpire, 300),
            TimeUnit.SECONDS
        )
            .maximumWeight(TeamCityProperties.getLong(SlackNotifierProperties.maximumUsersToCache, 50_000))
            .weigher { _: String, users: List<User> -> users.size }
            .build<String, List<User>>()

    // Don't move this magic constant to properties as it's optimized for good looking in autocomplete UI
    // Making it too big might mess with UI
    private val maxLabelSize = 50

    private val log = Logger.getInstance(SlackNotifierChannelCompletionController::class.java.name)

    companion object {
        const val url = "/admin/notifications/jbSlackNotifier/autocompleteUserId.html"
    }

    init {
        webControllerManager.registerController(url, this)
    }

    override fun getCompletionData(request: HttpServletRequest): List<Completion> {
        val term = getParameter(request, "term") ?: return mutableListOf()
        val connectionId = getParameter(request, descriptor.connectionProperty.key) ?: return mutableListOf()

        val connection = projectManager.projects.asSequence().mapNotNull { project ->
            oAuthConnectionsManager.findConnectionById(project, connectionId)
        }.firstOrNull()

        if (connection == null) {
            log.warn(
                    "Can't compute autocompletion because no connection with id '${connectionId}' found"
            )
            return mutableListOf()
        }

        val token = connection.parameters["secure:token"]
        if (token == null) {
            log.warn("Can't compute autocompletion because no 'secure:token' property is found in connection with id '${connectionId}'")
            return mutableListOf()
        }

        return getCompletion(term, token)
    }

    private fun getParameter(request: HttpServletRequest, name: String): String? {
        val value = request.getParameter(name)
        if (value == null) {
            log.warn("Can't compute autocompletion for request with no '${name}' parameter")
        }
        return value
    }

    private fun getCompletion(term: String, token: String): List<Completion> {
        return getChannelsCompletionWithHeader(term, token) + getUsersCompletionWithHeader(term, token)
    }

    private fun getChannelsCompletionWithHeader(term: String, token: String): List<Completion> {
        val channels = getChannelsCompletion(term, token)
        if (channels.isEmpty()) {
            return channels
        }

        return listOf(Completion("", "Channels", "", false)) + channels
    }

    private fun getChannelsCompletion(term: String, token: String): List<Completion> {
        val channelsList = getChannelsList(token)
        val lowercaseTerm = term.toLowerCase()

        return channelsList.filter {
            "#${it.name.toLowerCase()}".contains(lowercaseTerm)
        }.map {
            var purpose = it.purpose?.value ?: ""
            if (purpose.length > maxLabelSize) {
                purpose = purpose.substring(0, maxLabelSize - 3) + "..."
            }

            val channel = "#${it.name}"

            Completion(
                channel,
                channel,
                purpose
            )
        }.sortedBy { it.label }
    }

    private fun getUsersCompletionWithHeader(term: String, token: String): List<Completion> {
        val users = getUsersCompletion(term, token)
        if (users.isEmpty()) {
            return users
        }

        return listOf(Completion("", "Users", "", false)) + users
    }

    private fun getUsersCompletion(term: String, token: String): List<Completion> {
        val usersList = getUsersList(token)
        val lowercaseTerm = term.toLowerCase()

        return usersList.filter {
            !it.deleted && it.hasRealName
        }.filter {
            it.realName.toLowerCase().contains(lowercaseTerm) ||
                    "@${it.name}".contains(lowercaseTerm)
        }.map {
            Completion(it.id, it.realName, "@${it.name}")
        }.sortedBy { it.label }
    }

    private fun getChannelsList(token: String): List<Channel> {
        return myChannelsCache.get(token) {
            getList { cursor ->
                slackApi.channelsList(token, cursor)
            }
        }
    }

    private fun getUsersList(token: String): List<User> {
        return myUsersCache.get(token) {
            getList { cursor ->
                slackApi.usersList(token, cursor)
            }
        }
    }

    private fun <T> getList(dataProvider: (String?) -> SlackList<T>): List<T> {
        val result = mutableListOf<T>()
        var cursor: String? = null
        var prevCursor: String?

        do {
            val data = dataProvider(cursor)
            prevCursor = cursor
            cursor = data.nextCursor
            result.addAll(data.data)
        } while (cursor != "" && cursor != null && cursor != prevCursor)

        return result
    }
}