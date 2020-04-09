package jetbrains.buildServer.notification.slackNotifier.slack

import com.google.common.cache.CacheBuilder
import jetbrains.buildServer.notification.slackNotifier.SlackNotifierProperties
import jetbrains.buildServer.serverSide.TeamCityProperties
import org.springframework.stereotype.Service
import java.util.*
import java.util.concurrent.TimeUnit

@Service
class AggregatedSlackApi(
    private val slackWebApiFactory: SlackWebApiFactory
) {
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

    // Main bot info (like id or team id) doesn't change over time, so it's safe to cache them indefinitely
    // If some changing info (like display name) should be cached, Guava expirable cache should be used instead
    private val myBotCache: MutableMap<String, AggregatedBot> = Collections.synchronizedMap(WeakHashMap())

    fun getChannelsList(token: String): List<Channel> {
        return myChannelsCache.get(token) {
            getList { cursor ->
                slackApi.channelsList(token, cursor)
            }
        }
    }

    fun getUsersList(token: String): List<User> {
        return myUsersCache.get(token) {
            getList { cursor ->
                slackApi.usersList(token, cursor)
            }
        }
    }

    fun getBot(token: String): AggregatedBot {
        return myBotCache.getOrPut(token) {
            val bot = slackApi.authTest(token)
            val botInfo = slackApi.botsInfo(token, bot.botId)
            val userInfo = slackApi.usersInfo(token, botInfo.bot.userId)
            AggregatedBot(id = bot.botId, teamId = userInfo.user?.teamId)
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

data class AggregatedBot(
        val id: String,
        val teamId: String?
)