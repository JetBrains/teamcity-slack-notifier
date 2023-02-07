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
        .weigher { _: String, channels: List<Channel> -> channels.size }
        .buildAsync<String, List<Channel>>()

    private val myUsersCache = Caffeine.newBuilder()
            .expireAfterWrite(
                    TeamCityProperties.getLong(SlackNotifierProperties.cacheExpire, 300),
                    TimeUnit.SECONDS
            )
            .executor(executorServices.lowPriorityExecutorService)
            .maximumWeight(TeamCityProperties.getLong(SlackNotifierProperties.maximumUsersToCache, 50_000))
            .weigher { _: String, users: List<User> -> users.size }
            .buildAsync<String, List<User>>()

    private val myConversationMembersCache = Caffeine.newBuilder()
            .expireAfterWrite(
                    TeamCityProperties.getLong(SlackNotifierProperties.cacheExpire, 300),
                    TimeUnit.SECONDS
            )
            .executor(executorServices.lowPriorityExecutorService)
            .maximumWeight(TeamCityProperties.getLong(SlackNotifierProperties.maximumConversationMembersToCache, 50_000))
            .weigher { _: String, members: List<String> -> members.size }
            .buildAsync<String, List<String>>()

    // Main bot info (like id or team id) doesn't change over time, so it's safe to cache them indefinitely
    // If some changing info (like display name) should be cached, Guava expirable cache should be used instead
    private val myBotCache: MutableMap<String, AggregatedBot> = Collections.synchronizedMap(WeakHashMap())

    private val readTimeoutMs = 30_000L

    fun getChannelsList(token: String): List<Channel> {
        return myChannelsCache.getAsync(token, readTimeoutMs) {
            getList { cursor ->
                slackApi.conversationsList(token, cursor)
            }
        }
    }

    fun getUsersList(token: String): List<User> {
        return myUsersCache.getAsync(token, readTimeoutMs) {
            getList { cursor ->
                slackApi.usersList(token, cursor)
            }
        }
    }

    fun getConversationMembers(token: String, channelId: String): List<String> {
        return myConversationMembersCache.getAsync(token, readTimeoutMs) {
            getList { cursor ->
                slackApi.conversationsMembers(token, channelId, cursor)
            }
        }
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

    private fun <T, D> getList(dataProvider: (String?) -> D): List<T> where D : SlackList<T>, D : MaybeError {
        val result = mutableListOf<T>()
        var cursor: String? = null
        var prevCursor: String?

        do {
            val data = dataProvider(cursor)
            if (!data.ok) {
                throw SlackResponseError(data.error)
            }
            prevCursor = cursor
            cursor = data.nextCursor
            result.addAll(data.data)
        } while (cursor != "" && cursor != prevCursor)

        return result
    }
}

data class AggregatedBot(
        val id: String,
        val teamId: String?,
        val teamDomain: String?
)