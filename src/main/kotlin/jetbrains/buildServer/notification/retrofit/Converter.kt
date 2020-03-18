package jetbrains.buildServer.notification.retrofit

import com.fasterxml.jackson.databind.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import retrofit2.Converter
import retrofit2.Retrofit
import java.io.IOException
import java.lang.reflect.Type

class JacksonConverterFactory(
    private val objectWriter: OutboundObjectWriter,
    private val readMapper: IncomingObjectMapper
) : Converter.Factory() {
    companion object {
        val string = String::class.java
        val byteArray = ByteArray::class.java
        val stringConverter = Converter<ResponseBody, String> { value -> value.string() }
        val byteArrayConverter = Converter<ResponseBody, ByteArray> { value -> value.bytes() }
    }

    override fun responseBodyConverter(
        type: Type,
        annotations: Array<Annotation>,
        retrofit: Retrofit
    ): Converter<ResponseBody, *> {
        return when {
            string == type -> stringConverter
            byteArray == type -> byteArrayConverter
            else -> {
                val javaType = readMapper.typeFactory.constructType(type)
                // TODO: cache readers?
                val reader = readMapper.readerFor(javaType)
                JacksonResponseBodyConverter<Any>(
                    reader
                )
            }
        }
    }

    override fun requestBodyConverter(
        type: Type,
        parameterAnnotations: Array<Annotation>,
        methodAnnotations: Array<Annotation>,
        retrofit: Retrofit
    ): Converter<*, RequestBody> {
        return JacksonRequestBodyConverter<Any>(
            objectWriter
        )
    }
}

internal class JacksonRequestBodyConverter<T>(private val adapter: ObjectWriter) : Converter<T, RequestBody> {

    @Throws(IOException::class)
    override fun convert(value: T): RequestBody {
        val content = adapter.writeValueAsBytes(value)
        return content.toRequestBody(MEDIA_TYPE, 0, content.size)
    }

    companion object {
        private val MEDIA_TYPE = "application/json; charset=UTF-8".toMediaTypeOrNull()
    }
}

internal class JacksonResponseBodyConverter<T>(private val adapter: ObjectReader) : Converter<ResponseBody, T> {

    @Throws(IOException::class)
    override fun convert(value: ResponseBody): T {
        value.use {
            return adapter.readValue(it.charStream())
        }
    }
}

class IncomingObjectMapper(module: Module) : ObjectMapper() {

    init {
        configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false).registerModule(module)
    }
}

class OutboundObjectWriter internal constructor(mapper: ObjectMapper) : ObjectWriter(mapper, mapper.serializationConfig)