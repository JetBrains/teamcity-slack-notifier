package jetbrains.buildServer.slackNotifications.slack

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface SlackWebApi {
    // https://api.slack.com/methods/chat.postMessage
    @POST(value = "chat.postMessage")
    fun postMessage(@Header("Authorization") token: String, @Body payload: Message): Call<MaybeMessage>
}

