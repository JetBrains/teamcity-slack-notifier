package jetbrains.buildServer.notification.slackNotifier.slack

import com.google.common.cache.CacheBuilder
import jetbrains.buildServer.notification.slackNotifier.SlackNotifierProperties
import jetbrains.buildServer.serverSide.TeamCityProperties
import org.springframework.stereotype.Service
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