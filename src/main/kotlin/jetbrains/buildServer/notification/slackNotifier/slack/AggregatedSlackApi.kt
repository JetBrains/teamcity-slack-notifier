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

package jetbrains.buildServer.notification.slackNotifier.slack

import com.github.benmanes.caffeine.cache.Caffeine
import jetbrains.buildServer.notification.slackNotifier.SlackNotifierProperties
import jetbrains.buildServer.notification.slackNotifier.concurrency.getAsync
import jetbrains.buildServer.serverSide.TeamCityProperties
import jetbrains.buildServer.serverSide.executors.ExecutorServices
import org.springframework.stereotype.Service
import java.util.*
import java.util.concurrent.TimeUnit

@Service
class AggregatedSlackApi(
    slackWebApiFactory: SlackWebApiFactory,
    executorServices: ExecutorServices
) {
    private val slackApi = slackWebApiFactory.createSlackWebApi()

    // Minor copy-paste here, but de-duplicating it takes twice as many lines
    private val myChannelsCache = Caffeine.newBuilder()
        .expireAfterWrite(
            TeamCityProperties.getLong(SlackNotifierProperties.cacheExpire, 300),
            TimeUnit.SECONDS
        )
        .executor(executorServices.lowPriorityExecutorService)
        .maximumWeight(TeamCityProperties.getLong(SlackNotifierProperties.maximumChannelsToCache, 50_000))
        .weigher { _: String, channels: AggregatedSlackList<Channel> -> channels.data.size }
        .buildAsync<String, AggregatedSlackList<Channel>>()

    private val myUsersCache = Caffeine.newBuilder()
            .expireAfterWrite(
                    TeamCityProperties.getLong(SlackNotifierProperties.cacheExpire, 300),
                    TimeUnit.SECONDS
            )
            .executor(executorServices.lowPriorityExecutorService)
            .maximumWeight(TeamCityProperties.getLong(SlackNotifierProperties.maximumUsersToCache, 50_000))
            .weigher { _: String, users: AggregatedSlackList<User> -> users.data.size }
            .buildAsync<String, AggregatedSlackList<User>>()

    private val myConversationMembersCache = Caffeine.newBuilder()
            .expireAfterWrite(
                    TeamCityProperties.getLong(SlackNotifierProperties.cacheExpire, 300),
                    TimeUnit.SECONDS
            )
            .executor(executorServices.lowPriorityExecutorService)
            .maximumWeight(TeamCityProperties.getLong(SlackNotifierProperties.maximumConversationMembersToCache, 50_000))
            .weigher { _: String, members: AggregatedSlackList<String> -> members.data.size }
            .buildAsync<String, AggregatedSlackList<String>>()

    // Main bot info (like id or team id) doesn't change over time, so it's safe to cache them indefinitely
    // If some changing info (like display name) should be cached, Guava expirable cache should be used instead
    private val myBotCache: MutableMap<String, AggregatedBot> = Collections.synchronizedMap(WeakHashMap())

    private val readTimeoutMs = 5_000L

    // TW-84904
    // Error responses should also be cached. Without it, consecutive failures may drain the rate limits.
    // In worst case, it can result into consecutive timeouts,
    // so the Slack health reports may take extremely long time to calculate.
    fun getChannelsList(token: String): List<Channel> {
        val res = myChannelsCache.getAsync(token, readTimeoutMs) {
            getList { cursor ->
                slackApi.conversationsList(token, cursor)
            }
        }
        if (!res.ok) throw SlackResponseError(res.error)
        return res.data
    }

    fun getUsersList(token: String): List<User> {
        val res = myUsersCache.getAsync(token, readTimeoutMs) {
            getList { cursor ->
                slackApi.usersList(token, cursor)
            }
        }
        if (!res.ok) throw SlackResponseError(res.error)
        return res.data
    }

    fun getConversationMembers(token: String, channelId: String): List<String> {
        val res = myConversationMembersCache.getAsync(token, readTimeoutMs) {
            getList { cursor ->
                slackApi.conversationsMembers(token, channelId, cursor)
            }
        }
        if (!res.ok) throw SlackResponseError(res.error)
        return res.data
    }

    fun getBot(token: String): AggregatedBot {
        return myBotCache.getOrPut(token) {
            val bot = slackApi.authTest(token)
            val botInfo = slackApi.botsInfo(token, bot.botId)
            val userInfo = slackApi.usersInfo(token, botInfo.bot.userId)
            val teamId = userInfo.user?.teamId
            val team = teamId?.let {
                slackApi.teamInfo(token, it)
            }
            AggregatedBot(id = bot.botId, teamId = teamId, teamDomain = team?.team?.domain)
        }
    }

    private fun <T, D> getList(dataProvider: (String?) -> D): AggregatedSlackList<T> where D : SlackList<T>, D : MaybeError {
        val result = mutableListOf<T>()
        var cursor: String? = null
        var prevCursor: String?

        do {
            val data = dataProvider(cursor)
            if (!data.ok) {
                return AggregatedSlackList(ok = false, error = data.error, needed = data.needed)
            }
            prevCursor = cursor
            cursor = data.nextCursor
            result.addAll(data.data)
        } while (cursor != "" && cursor != prevCursor)

        return AggregatedSlackList(ok = true, data = result)
    }
}

data class AggregatedBot(
        val id: String,
        val teamId: String?,
        val teamDomain: String?
)