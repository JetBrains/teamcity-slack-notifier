package jetbrains.buildServer.notification.slackNotifier.slack

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.intellij.openapi.diagnostic.Logger
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
        var response: String? = null
        var isException = false

        val post: HTTPRequestBuilder.Request =
            HTTPRequestBuilder("$baseUrl/chat.postMessage")
                .withMethod("POST")
                .withPostStringEntity(
                    mapper.writeValueAsString(payload),
                    "application/json",
                    Charset.forName("UTF-8")
                )
                .withHeader("Authorization", "Bearer $token")
                .withHeader("Content-Type", "application/json")
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

        if (isException) {
            return MaybeMessage(ok = false, error = "Unknown error. See notification logs for more details")
        }

        if (response == null) {
            return MaybeMessage(ok = false, error = "Unknown error. See notification logs for more details")
        }

        return mapper.readValue(response!!, MaybeMessage::class.java)
    }
}