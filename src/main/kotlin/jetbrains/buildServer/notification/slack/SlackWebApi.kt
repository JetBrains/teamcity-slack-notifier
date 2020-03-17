package jetbrains.buildServer.notification.slack

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST

interface SlackWebApi {
    // https://api.slack.com/methods/chat.postMessage
    @Headers("Content-type: application/json")
    @POST(value = "chat.postMessage")
    fun postMessage(@Header("Authorization") token: String, @Body payload: Message): Call<MaybeMessage>
}

