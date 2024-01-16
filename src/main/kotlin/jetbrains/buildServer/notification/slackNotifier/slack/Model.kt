

@file:Suppress("unused")

package jetbrains.buildServer.notification.slackNotifier.slack

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty

// TODO: add something sane like Optional<> instead
interface MaybeError {
    val ok: Boolean // error handling
    val error: String  // error handling
    val needed: String // error handling
}

data class MaybeResponse(
    override val ok: Boolean = false,
    override val error: String = "",
    override val needed: String = ""
) : MaybeError

interface TeamAware {
    val team: String
}

interface VerificationTokenAware {
    val token: String
}

interface UserInfoAware {
    val id: String
    val email: String
    val displayName: String
    val realName: String?
    val hasRealName: Boolean
    val deleted: Boolean
    val statusEmoji: String?
    val statusText: String?
}

// https://api.slack.com/types/user
data class User(
    override val id: String,
    @JsonProperty("team_id")
    val teamId: String = "",
    override val deleted: Boolean = false,
    val profile: UserProfile = UserProfile(),
    val name: String = ""
    /*val color: String,
    val tz: String,
    val tz_label: String,
    val tz_offset: String*/
) : UserInfoAware {
    override val email: String
        @JsonIgnore
        get() = profile.email
    override val displayName: String
        @JsonIgnore
        get() {
            return profile.displayName.ifEmpty { name }
        }
    override val hasRealName: Boolean
        get() = true
    override val realName: String
        @JsonIgnore
        get() = profile.realName
    override val statusEmoji: String
        @JsonIgnore
        get() = profile.statusEmoji
    override val statusText: String
        @JsonIgnore
        get() = profile.statusText
}

data class MaybeUser(
    override val ok: Boolean = false,
    override val error: String = "",
    override val needed: String = "",
    val user: User? = null
) : MaybeError

data class MaybePresence(
    override val ok: Boolean = false,
    override val error: String = "",
    override val needed: String = "",
    val presence: String = ""
) : MaybeError

data class UserProfile(
    @JsonProperty("real_name")
    val realName: String = "",
    @JsonProperty("display_name")
    val displayName: String = "",
    val email: String = "", // for some reason I can't get it even with 'users:read.email' permission granted
    @JsonProperty("status_text")
    val statusText: String = "",
    @JsonProperty("status_emoji")
    val statusEmoji: String = ""
)

data class AuthTestResult(
    override val ok: Boolean = false,
    @JsonProperty("user") val user: String = "",
    @JsonProperty("user_id") val userId: String = "",
    @JsonProperty("bot_id") val botId: String = "",
    override val error: String = "",
    override val needed: String = ""
) : MaybeError

data class MaybeUserProfile(
    override val ok: Boolean = false,
    override val error: String = "",
    override val needed: String = "",
    val profile: UserProfile? = null
) : MaybeError

// https://api.slack.com/methods/users.list
data class UsersList(
    override val ok: Boolean = false,
    override val error: String = "",
    override val needed: String = "",
    val members: List<User> = emptyList(),
    @JsonProperty("cache_ts") val timestamp: Long = Long.MIN_VALUE,
    @JsonProperty("response_metadata") val meta: CursorMetaData = noCursorMeta
) : MaybeError, SlackList<User> {
    override val nextCursor: String
        get() = meta.nextCursor

    override val data: List<User>
        get() = members
}

// https://api.slack.com/events/url_verification
data class URLVerificationResponse(val challenge: String)

// https://api.slack.com/docs/pagination
data class CursorMetaData(@JsonProperty("next_cursor") val nextCursor: String)

// https://api.slack.com/methods/apps.permissions.info
data class PermissionsList(
    override val ok: Boolean = false,
    override val error: String = "",
    override val needed: String = "",
    val info: PermissionsInfo = noPermissionsInfo
) : MaybeError

data class PermissionsScopes(
    override val ok: Boolean = false,
    override val error: String = "",
    override val needed: String = "",
    val scopes: PermissionsInfo = noPermissionsInfo
) : MaybeError

// https://api.slack.com/methods/apps.permissions.info
data class PermissionsInfo(
    @JsonProperty("team")
    val teamPermissions: PermissionDescriptor = noPermissionsDescriptor,
    @JsonProperty("channel")
    val channelPermissions: PermissionDescriptor = noPermissionsDescriptor,
    @JsonProperty("group")
    val groupPermissions: PermissionDescriptor = noPermissionsDescriptor,
    @JsonProperty("mpim")
    val mpimPermissions: PermissionDescriptor = noPermissionsDescriptor,
    @JsonProperty("im")
    val imPermissions: PermissionDescriptor = noPermissionsDescriptor,
    @JsonProperty("app_home")
    val appHomePermissions: PermissionDescriptor = noPermissionsDescriptor
)

// https://api.slack.com/methods/apps.permissions.info
data class PermissionDescriptor(
    val resources: ResourcesList,
    val scopes: List<String> = emptyList()
)

// https://api.slack.com/methods/apps.permissions.info
data class ResourcesList(val ids: List<String> = emptyList())

// https://api.slack.com/methods/team.info
data class TeamInfo(
    override val ok: Boolean = false,
    override val error: String = "",
    override val needed: String = "",
    val team: Team = noTeam
) : MaybeError

// https://api.slack.com/methods/oauth.token
data class OauthToken(
    override val ok: Boolean = false,
    override val error: String = "",
    override val needed: String = "",
    @JsonProperty("access_token")
    val accessToken: String,
    @JsonProperty("token_type")
    val tokenType: String,
    @JsonProperty("app_id")
    val appId: String,
    @JsonProperty("team_name")
    val teamName: String,
    @JsonProperty("team_id")
    val teamId: String,
    @JsonProperty("installer_user_id")
    val installerUser: String,
    @JsonProperty("authorizing_user_id")
    val authorizingUser: String,
    val permissions: List<ResourcePermission> = listOf()
) : MaybeError

// https://api.slack.com/methods/oauth.access
data class OauthAccessToken(
    override val ok: Boolean = false,
    override val error: String = "",
    override val needed: String = "",
    @JsonProperty("access_token")
    val accessToken: String = "",
    @JsonProperty("team_name")
    val teamName: String = "",
    @JsonProperty("team_id")
    val teamId: String = "",
    @JsonProperty("bot")
    val botToken: BotToken? = null
) : MaybeError

// https://api.slack.com/docs/oauth#bots
data class BotToken(
    @JsonProperty("bot_user_id") val botUserId: String,
    @JsonProperty("bot_access_token") val botToken: String
)

data class MaybeBot(
    override val ok: Boolean = false,
    override val error: String = "",
    override val needed: String = "",
    val bot: Bot = Bot()
) : MaybeError

data class Bot(
        val id: String = "",
        val name: String = "",
        @JsonProperty("user_id")
        val userId: String = ""
)

data class ResourcePermission(
    val scopes: List<String> = emptyList(),
    @JsonProperty("resource_type")
    val type: String,
    @JsonProperty("resource_id")
    val id: String
)

data class Team(
    val id: String,
    val name: String = "",
    val domain: String = "",
    @JsonProperty("email_domain")
    val emailDomain: String = "",
    val icon: Icon = noIcon
)

data class MaybeTeam(
        override val ok: Boolean = false,
        override val error: String = "",
        override val needed: String = "",
        val team: Team = Team("")
): MaybeError

data class Icon(
    @JsonProperty("image_default")
    val default: Boolean = true,
    @JsonProperty("image_132")
    val image132: String = ""
)

interface MessageAttachment

data class TextMessageAttachment(val text: String)

// https://api.slack.com/docs/message-formatting
data class RichMessageAttachment(
    @JsonProperty("mrkdwn_in")
    val markdownIn: List<String> = listOf("text"),
    /*@JsonProperty("mrkdwn")
    val markdown: Boolean = false,*/
    val title: String? = null,
    val pretext: String? = null,
    val fallback: String = "...",
    val text: String = "",
    val color: String? = null,
    @JsonProperty("callback_id")
    val callbackId: String? = null,
    val actions: List<Action> = listOf(),
    val fields: List<Field> = listOf(),
    val footer: String? = null,
    @JsonProperty("footer_icon")
    val footerIcon: String? = null,
    @JsonProperty("thumb_url")
    val thumbURL: String? = null
) : MessageAttachment

// https://api.slack.com/methods/chat.postMessage
data class Message(
    val channel: String,
    val text: String? = null,
    @JsonProperty("username")
    val userName: String? = null,
    val parse: String? = null,
    val attachments: List<MessageAttachment> = emptyList(),
    @JsonProperty("mrkdwn")
    val markdown: Boolean = false,
    val blocks: List<MessageBlock> = emptyList()
)

data class Action(
    val name: String? = null,
    val text: String = "",
    val type: String,
    val value: String? = null,
    val url: String? = null
)

data class Field(
    val title: String,
    val value: String = "",
    val short: Boolean = true
)

data class CommandResponse(
    val text: String,
    val attachments: List<MessageAttachment> = listOf(),
    @JsonProperty("response_type")
    val responseType: String = "in_channel",
    @JsonProperty("replace_original")
    val replaceOriginal: Boolean = false
)

interface MessageBlock

data class TextBlock(
    val type: String = "section",
    val text: MessageBlockText
): MessageBlock

data class MessageBlockText(
    val type: String,
    val text: String
)

data class MessageActions(
    val type: String = "actions",
    val elements: List<MessageAction>
): MessageBlock

data class ContextBlock(
    val type: String = "context",
    val elements: List<ContextBlockElement>
): MessageBlock

data class ContextBlockElement(
    val type: String = "mrkdwn",
    val text: String
)

data class MessageAction(
    val type: String,
    val text: MessageActionText,
    val url: String
)

data class MessageActionText(
    val type: String,
    val text: String
)

// https://api.slack.com/methods/chat.postMessage
data class MaybeMessage(
    override val ok: Boolean = false,
    override val error: String = "",
    override val needed: String = "",
    val message: PostedMessage = noMessage
) : MaybeError

data class MaybeChannel(
    override val ok: Boolean = false,
    override val error: String = "",
    override val needed: String = "",
    val channel: Channel = noChannel
) : MaybeError

interface SlackList<T> {
    val nextCursor: String
    val data: List<T>
}

// completed list, gained with cursor pagination
data class AggregatedSlackList<T>(
    override val ok: Boolean = false,
    override val error: String = "",
    override val needed: String = "",
    val data: List<T> = emptyList()
) : MaybeError

// https://api.slack.com/methods/channels.list
data class ChannelsList(
    override val ok: Boolean = false,
    override val error: String = "",
    override val needed: String = "",
    val channels: List<Channel> = emptyList(),
    @JsonProperty("response_metadata") val meta: CursorMetaData = noCursorMeta
) : MaybeError, SlackList<Channel> {
    override val nextCursor: String
        get() = meta.nextCursor

    override val data: List<Channel>
        get() = channels
}

// https://api.slack.com/methods/chat.postMessage
data class PostedMessage(
    val type: String = "",
    val user: String = ""
)

// https://api.slack.com/methods/users.identity
data class UserIdentity(
    override val ok: Boolean = false,
    override val error: String = "",
    override val needed: String = "",
    val user: IdentityUser = noIdentityUser,
    val team: Team = noTeam
) : MaybeError

data class IdentityUser(
    val name: String = "",
    override val id: String = "",
    override val email: String = ""
) : UserInfoAware {
    override val displayName: String
        @JsonIgnore
        get() = name
    override val hasRealName: Boolean
        get() = false
    override val realName: String
        @JsonIgnore
        get() = ""
    override val deleted: Boolean
        @JsonIgnore
        get() = false
    override val statusEmoji: String?
        @JsonIgnore
        get() = null
    override val statusText: String?
        @JsonIgnore
        get() = null
}

data class ChatUnfurl(
    val channel: String,
    @JsonProperty("ts")
    val timeStamp: String,
    val unfurls: Map<String, MessageAttachment> = emptyMap(),
    @JsonProperty("user_auth_url")
    val authURL: String? = null,
    @JsonProperty("user_auth_message")
    val authMessage: String? = null,
    @JsonProperty("user_auth_required")
    val authRequired: Boolean = authURL != null
)

data class IMTarget(
    @JsonProperty("user")
    val userId: String
)

data class Channel(
    val id: String = "",
    val name: String = "",
    val purpose: ChannelPurpose? = noPurpose,
    val members: List<String> = emptyList()
)

data class ChannelPurpose(
    val value: String = ""
)

data class EphemeralMessage(
    val channel: String,
    val text: String,
    val user: String,
    val attachments: List<MessageAttachment>
)

// https://api.slack.com/methods/emoji.list
data class EmojiList(
    override val ok: Boolean = false,
    override val error: String = "",
    override val needed: String = "",
    val emoji: Map<String, String>
) : MaybeError

data class ConversationMembers(
        override val ok: Boolean = false,
        override val error: String = "",
        override val needed: String = "",
        val members: List<String> = emptyList(),
        @JsonProperty("response_metadata") val meta: CursorMetaData = noCursorMeta
) : MaybeError, SlackList<String> {
    override val data = members
    override val nextCursor = meta.nextCursor
}