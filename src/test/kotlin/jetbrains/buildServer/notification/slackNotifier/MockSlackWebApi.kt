package jetbrains.buildServer.notification.slackNotifier

import jetbrains.buildServer.notification.slackNotifier.slack.*

class MockSlackWebApi : SlackWebApi {
    val messages = mutableListOf<Message>()

    override fun postMessage(token: String, payload: Message): MaybeMessage {
        if (incorrectToken(token)) {
            return MaybeMessage(ok = false)
        }

        messages.add(payload)
        return MaybeMessage(ok = true)
    }

    override fun channelsList(token: String, cursor: String?): ChannelsList {
        if (incorrectToken(token)) {
            return ChannelsList(ok = false)
        }

        return ChannelsList(
            ok = true,
            channels = listOf(
                Channel("CHANNEL_ID_1", "test_channel"),
                Channel("CHANNEL_ID_2", "anotherChannel")
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

    override fun authTest(token: String): MaybeResponse {
        if (incorrectToken(token)) {
            return MaybeResponse(ok = false, error = "not_authed")
        }

        return MaybeResponse(ok = true)
    }

    private fun incorrectToken(token: String) = token != "test_token"
}