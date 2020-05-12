package jetbrains.buildServer.notification.slackNotifier.slack

import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import jetbrains.buildServer.notification.slackNotifier.SlackNotifierProperties
import jetbrains.buildServer.serverSide.TeamCityProperties
import java.util.concurrent.TimeUnit

class CachingSlackWebApi(
        private val slackApi: SlackWebApi,
        private val defaultTimeoutSeconds: Long = 300
) : SlackWebApi {
    private val authTestCache = createCache<String, AuthTestResult>()
    private val botsInfoCache = createCache<String, MaybeBot>()
    private val usersInfoCache = createCache<String, MaybeUser>()
    private val conversationMembersCache = createCache<String, ConversationMembers>()
    private val userIdentityCache = createCache<String, UserIdentity>()

    /**
     * Posts new message every time, makes no sense to cache
     */
    override fun postMessage(token: String, payload: Message): MaybeMessage {
        return slackApi.postMessage(token, payload)
    }

    /**
     * All channels should be cached, not only the ones got for the specified cursor
     * See [AggregatedSlackApi]
     */
    override fun channelsList(token: String, cursor: String?): ChannelsList {
        return slackApi.channelsList(token, cursor)
    }

    /**
     * All users should be cached, not only the ones got for the specified cursor
     * See [AggregatedSlackApi]
     */
    override fun usersList(token: String, cursor: String?): UsersList {
        return slackApi.usersList(token, cursor)
    }

    override fun authTest(token: String): AuthTestResult {
        return authTestCache.get(token) {
            slackApi.authTest(token)
        }
    }

    override fun botsInfo(token: String, botId: String): MaybeBot {
        return botsInfoCache.get("$token;;$botId") {
            slackApi.botsInfo(token, botId)
        }
    }

    override fun usersInfo(token: String, userId: String): MaybeUser {
        return usersInfoCache.get("$token;;$userId") {
            slackApi.usersInfo(token, userId)
        }
    }

    override fun conversationsMembers(token: String, channelId: String): ConversationMembers {
        return conversationMembersCache.get("$token;;$channelId") {
            slackApi.conversationsMembers(token, channelId)
        }
    }

    /**
     * OAuth access happens rarely for the same code, so it doesn't make sense to cache it
     */
    override fun oauthAccess(clientId: String, clientSecret: String, code: String, redirectUrl: String): OauthAccessToken {
        return slackApi.oauthAccess(clientId, clientSecret, code, redirectUrl)
    }

    override fun usersIdentity(token: String): UserIdentity {
        return userIdentityCache.get(token) {
            slackApi.usersIdentity(token)
        }
    }

    private fun <K, V> createCache(): Cache<K, V> {
        return CacheBuilder.newBuilder()
                .expireAfterWrite(
                        TeamCityProperties.getLong(SlackNotifierProperties.cacheExpire, defaultTimeoutSeconds),
                        TimeUnit.SECONDS
                )
                .maximumSize(1000)
                .build()
    }
}