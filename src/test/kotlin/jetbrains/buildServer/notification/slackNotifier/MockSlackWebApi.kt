package jetbrains.buildServer.notification.slackNotifier

import jetbrains.buildServer.notification.slackNotifier.slack.*

class MockSlackWebApi : SlackWebApi {
    val messages = mutableListOf<Message>()

    override fun postMessage(token: String, payload: Message): MaybeMessage {
        messages.add(payload)
        return MaybeMessage(ok = true)
    }

    override fun channelsList(token: String, cursor: String?): ChannelsList {
        return ChannelsList(
            ok = true,
            channels = listOf(
                Channel("CHANNEL_ID_1", "test_channel"),
                Channel("CHANNEL_ID_2", "anotherChannel")
            )
        )
    }

    override fun usersList(token: String, cursor: String?): UsersList {
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
}