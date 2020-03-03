package jetbrains.buildServer.slackNotifications.slack

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.KotlinModule
import jetbrains.buildServer.slackNotifications.retrofit.IncomingObjectMapper
import jetbrains.buildServer.slackNotifications.retrofit.JacksonConverterFactory
import jetbrains.buildServer.slackNotifications.retrofit.OutboundObjectWriter
import retrofit2.Retrofit
import retrofit2.create

class SlackWebApiFactory {
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