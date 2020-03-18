package jetbrains.buildServer.notification

import jetbrains.buildServer.notification.slack.MaybeMessage
import jetbrains.buildServer.notification.slack.Message
import jetbrains.buildServer.notification.slack.SlackWebApi
import okhttp3.Request
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MockSlackWebApi : SlackWebApi {
    val messages = mutableListOf<Message>()

    override fun postMessage(token: String, payload: Message): Call<MaybeMessage> {
        messages.add(payload)

        return object : Call<MaybeMessage> {
            override fun enqueue(callback: Callback<MaybeMessage>) {
                callback.onResponse(this, Response.success(MaybeMessage(ok = true)))
            }

            override fun isExecuted(): Boolean {
                return true
            }

            override fun clone(): Call<MaybeMessage> {
                return this
            }

            override fun isCanceled(): Boolean {
                return false
            }

            override fun cancel() {
            }

            override fun execute(): Response<MaybeMessage> {
                return Response.success(MaybeMessage(ok = true))
            }

            override fun request(): Request {
                TODO("Not yet implemented")
            }

        }
    }
}