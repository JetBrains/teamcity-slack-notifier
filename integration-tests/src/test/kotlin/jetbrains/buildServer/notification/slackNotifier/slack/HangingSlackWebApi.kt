/*
 *  Copyright 2000-2021 JetBrains s.r.o.
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

import java.lang.IllegalStateException

class HangingSlackWebApi(
    private val slackWebApi: SlackWebApi,
    private val methodsThatHang: Set<String>
) : SlackWebApi {
    override fun postMessage(token: String, payload: Message): MaybeMessage = hang { slackWebApi.postMessage(token, payload) }
    override fun conversationsList(token: String, cursor: String?, types: String): ChannelsList = hang { slackWebApi.conversationsList(token, cursor, types) }
    override fun usersList(token: String, cursor: String?): UsersList = hang { slackWebApi.usersList(token, cursor) }
    override fun authTest(token: String): AuthTestResult = hang { slackWebApi.authTest(token) }
    override fun botsInfo(token: String, botId: String): MaybeBot = hang { slackWebApi.botsInfo(token, botId) }
    override fun usersInfo(token: String, userId: String): MaybeUser = hang { slackWebApi.usersInfo(token, userId) }
    override fun conversationsMembers(token: String, channelId: String, cursor: String?): ConversationMembers = hang { slackWebApi.conversationsMembers(token, channelId, cursor) }
    override fun oauthAccess(
        clientId: String,
        clientSecret: String,
        code: String,
        redirectUrl: String
    ): OauthAccessToken = hang { slackWebApi.oauthAccess(clientId, clientSecret, code, redirectUrl) }

    override fun usersIdentity(token: String): UserIdentity = hang { slackWebApi.usersIdentity(token) }
    override fun teamInfo(token: String, team: String): MaybeTeam = hang { slackWebApi.teamInfo(token, team) }

    private inline fun <reified T : Any> hang(block: () -> T): T {
        val stacktrace = Thread.currentThread().stackTrace
        val caller = stacktrace[1].methodName
        if (caller in methodsThatHang) {
            Thread.sleep(1_000_000)
            throw IllegalStateException("Slack API hanging for too long. Use @Test with timeout to not wait for the whole sleep duration")
        }

        return block()
    }
}
