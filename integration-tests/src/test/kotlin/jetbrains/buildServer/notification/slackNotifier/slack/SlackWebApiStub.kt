

package jetbrains.buildServer.notification.slackNotifier.slack


const val slackToken = "test_token"

class SlackWebApiStub : SlackWebApi {

    override fun postMessage(token: String, payload: Message): MaybeMessage {
        if (incorrectToken(token)) {
            return MaybeMessage(ok = false)
        }

        if (payload.channel == "#anotherChannel") {
            return MaybeMessage(ok = false, error = "not_in_channel")
        }

        if (payload.channel == "#rate_limited") {
            return MaybeMessage(ok = false, error = "ratelimited")
        }

        if (payload.channel == "#unknown_failure") {
            return MaybeMessage(ok = false)
        }

        return MaybeMessage(ok = true)
    }

    override fun conversationsList(token: String, cursor: String?, types: String): ChannelsList {
        if (incorrectToken(token)) {
            return ChannelsList(ok = false)
        }

        return ChannelsList(
                ok = true,
                channels = listOf(
                    Channel("CHANNEL_ID_1", "test_channel"),
                    Channel("CHANNEL_ID_2", "anotherChannel"),
                    Channel("CHANNEL_ID_3", "big_conversation")
                )
        )
    }

    override fun usersList(token: String, cursor: String?): UsersList {
        if (incorrectToken(token)) {
            return UsersList(ok = false)
        }

        return UsersList(
            ok = true,
            members = listOf(
                User(
                    "USER_ID_1",
                    name = "test_user",
                    profile = UserProfile(displayName = "test_user", realName = "Test User")
                ),
                User(
                    "USER_ID_2",
                    name = "anotherUser",
                    profile = UserProfile(displayName = "anotherUser", realName = "Another User")
                )
            )
        )
    }

    override fun authTest(token: String): AuthTestResult {
        if (incorrectToken(token)) {
            return AuthTestResult(ok = false, error = "not_authed")
        }

        return AuthTestResult(ok = true, botId = "botId", userId = "botId")
    }

    override fun botsInfo(token: String, botId: String): MaybeBot {
        if (incorrectToken(token)) {
            return MaybeBot(ok = false, error = "not_authed")
        }

        if (botId == "botId") {
            return MaybeBot(ok = true, bot = Bot(id = "botId", name = "Test Bot"))
        }

        return MaybeBot(ok = false, error = "no_bot_found")
    }

    override fun conversationsMembers(token: String, channelId: String, cursor: String?): ConversationMembers {
        if (incorrectToken(token)) {
            return ConversationMembers(ok = false)
        }

        if (channelId == "CHANNEL_ID_1") {
            return ConversationMembers(ok = true, members = listOf("botId", "user_1"))
        }

        if (channelId == "CHANNEL_ID_2") {
            return ConversationMembers(ok = true, members = listOf("user_1"))
        }

        if (channelId == "CHANNEL_ID_3") {
            when (cursor) {
                null -> {
                    return ConversationMembers(
                        ok = true,
                        members = listOf("user_1", "user_2"),
                        meta = CursorMetaData(nextCursor = "123")
                    )
                }
                "123" -> {
                    return ConversationMembers(
                        ok = true,
                        members = listOf("botId", "user_3")
                    )
                }
                else -> {
                    return ConversationMembers(
                        ok = false,
                        error = "invalid_cursor"
                    )
                }
            }
        }

        return ConversationMembers(ok = false, error = "no_channel_found")
    }

    override fun oauthAccess(
        clientId: String,
        clientSecret: String,
        code: String,
        redirectUrl: String
    ): OauthAccessToken {
        return OauthAccessToken(ok = true)
    }

    override fun usersIdentity(token: String): UserIdentity {
        return UserIdentity(ok = true)
    }

    override fun usersInfo(token: String, userId: String): MaybeUser {
        return MaybeUser(ok = true)
    }

    override fun teamInfo(token: String, team: String): MaybeTeam {
        return MaybeTeam(ok = true)
    }

    private fun incorrectToken(token: String) = token != slackToken
}
