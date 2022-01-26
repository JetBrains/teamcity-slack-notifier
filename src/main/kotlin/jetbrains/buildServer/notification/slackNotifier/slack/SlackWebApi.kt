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

interface SlackWebApi {
    // https://api.slack.com/methods/chat.postMessage
    fun postMessage(token: String, payload: Message): MaybeMessage

    // https://api.slack.com/methods/conversations.list
    fun conversationsList(
            token: String,
            cursor: String? = null,
            types: String = "public_channel,private_channel"
    ): ChannelsList

    // https://api.slack.com/methods/users.list
    fun usersList(token: String, cursor: String? = null): UsersList

    // https://api.slack.com/methods/auth.test
    fun authTest(token: String): AuthTestResult

    // https://api.slack.com/methods/bots.info
    fun botsInfo(token: String, botId: String): MaybeBot

    // https://api.slack.com/methods/users.info
    fun usersInfo(token: String, userId: String): MaybeUser

    // https://api.slack.com/methods/conversations.members
    fun conversationsMembers(token: String, channelId: String, cursor: String? = null): ConversationMembers

    // https://api.slack.com/methods/oauth.access
    fun oauthAccess(clientId: String, clientSecret: String, code: String, redirectUrl: String): OauthAccessToken

    fun usersIdentity(token: String): UserIdentity

    // https://api.slack.com/methods/team.info
    fun teamInfo(token: String, team: String): MaybeTeam
}

