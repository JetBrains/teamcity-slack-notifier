import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.KotlinModule
import kotlinx.coroutines.runBlocking
import retrofit.IncomingObjectMapper
import retrofit.JacksonConverterFactory
import retrofit.OutboundObjectWriter
import retrofit2.Retrofit
import retrofit2.awaitResponse
import retrofit2.create
import slack.Message
import slack.SlackWebApi

fun main() {
    val mapper = ObjectMapper()
        .registerModule(KotlinModule())
        .configure(SerializationFeature.INDENT_OUTPUT, true)

    val converterFactory = JacksonConverterFactory(
        OutboundObjectWriter(mapper),
        IncomingObjectMapper(KotlinModule(reflectionCacheSize = 1024))
    )

    val retrofit = Retrofit.Builder()
        .addConverterFactory(converterFactory)
        .baseUrl("https://slack.com/api/")
        .build()

    val slack = retrofit.create<SlackWebApi>()

    runBlocking {
        val result = slack.postMessage(
            "Bearer xoxb-2280447103-946469997938-pSAdI2ByohSm7eO8wNfvgD34",
            Message(channel = "#teamcity-notifications-test", text = "Test")
        ).awaitResponse()
    }
}
