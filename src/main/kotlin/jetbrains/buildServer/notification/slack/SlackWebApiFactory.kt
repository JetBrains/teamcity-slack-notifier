package jetbrains.buildServer.notification.slack

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.KotlinModule
import jetbrains.buildServer.notification.retrofit.IncomingObjectMapper
import jetbrains.buildServer.notification.retrofit.JacksonConverterFactory
import jetbrains.buildServer.notification.retrofit.OutboundObjectWriter
import retrofit2.Retrofit
import retrofit2.create

class SlackWebApiFactory {
    // TODO: Set up proper proxy, see HTTPRequestBuilder:577
    fun createSlackWebApi(): SlackWebApi {
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

        return retrofit.create()
    }
}