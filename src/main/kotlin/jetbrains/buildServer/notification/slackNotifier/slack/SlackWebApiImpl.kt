package jetbrains.buildServer.notification.slackNotifier.slack

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.Pair
import jetbrains.buildServer.util.HTTPRequestBuilder
import java.nio.charset.Charset

class SlackWebApiImpl(
    private val requestHandler: HTTPRequestBuilder.RequestHandler
) : SlackWebApi {
    private val baseUrl = "https://slack.com/api"

    private val logger = Logger.getInstance(SlackWebApi::class.java.name)

    private val mapper = ObjectMapper()
        .registerModule(KotlinModule())
        .configure(SerializationFeature.INDENT_OUTPUT, true)
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

    override fun postMessage(token: String, payload: Message): MaybeMessage {
        val response = request(
            "chat.postMessage",
            token,
            body = mapper.writeValueAsString(payload),
            method = "POST"
        )
        if (response.isException || response.message == null) {
            return MaybeMessage(ok = false, error = unknownError)
        }
        return mapper.readValue(response.message, MaybeMessage::class.java)
    }

    override fun channelsList(token: String, cursor: String?): ChannelsList {
        val response = request(
            "channels.list",
            token,
            listOf(
                Pair("cursor", cursor),
                Pair("limit", "1000"),
                Pair("exclude_members", "true"),
                Pair("exclude_archived", "true")
            )
        )
        if (response.isException || response.message == null) {
            return ChannelsList(ok = false, error = unknownError)
        }
        return mapper.readValue(response.message, ChannelsList::class.java)
    }

    override fun usersList(token: String, cursor: String?): UsersList {
        val response = request(
            "users.list",
            token,
            listOf(
                Pair("cursor", cursor),
                Pair("limit", "1000")
            )
        )
        if (response.isException || response.message == null) {
            return UsersList(ok = false, error = unknownError)
        }

        return mapper.readValue(response.message, UsersList::class.java)
    }

    private fun request(
        path: String,
        token: String,
        parameters: List<Pair<String, String?>> = mutableListOf(),
        body: String = "",
        method: String = "GET"
    ): SlackResponse {
        var response: String? = null
        var isException = false

        val post: HTTPRequestBuilder.Request =
            HTTPRequestBuilder("$baseUrl/${path}")
                .withMethod(method)
                .withPostStringEntity(
                    body,
                    "application/json",
                    Charset.forName("UTF-8")
                )
                .withHeader("Authorization", "Bearer $token")
                .withHeader("Content-Type", "application/json")
                .addParameters(parameters)
                .onErrorResponse(HTTPRequestBuilder.ResponseConsumer {
                    response = it.bodyAsString
                })
                .onSuccess {
                    response = it.bodyAsString
                }
                .onException {
                    logger.error(
                        "Exception occurred when sending request to Slack",
                        it
                    )
                    isException = true
                }
                .build()

        requestHandler.doRequest(post)

        return SlackResponse(response, isException)
    }

    internal data class SlackResponse(val message: String?, val isException: Boolean)

    companion object {
        const val unknownError = "Unknown error. See notification logs for more details"
    }
}