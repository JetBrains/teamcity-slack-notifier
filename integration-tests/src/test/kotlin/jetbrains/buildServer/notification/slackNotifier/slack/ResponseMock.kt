

package jetbrains.buildServer.notification.slackNotifier.slack

import jetbrains.buildServer.util.HTTPRequestBuilder
import java.io.InputStream
import java.net.URI

class ResponseMock(
    private val myStatusCode: Int,
    private val myBodyString: String
): HTTPRequestBuilder.Response {
    override fun close() {
        TODO("Not yet implemented")
    }

    override fun getUri(): URI {
        TODO("Not yet implemented")
    }

    override fun getContentStream(): InputStream? {
        TODO("Not yet implemented")
    }

    override fun getBodyAsString(): String {
        return myBodyString
    }

    override fun getBodyAsString(p0: String?): String {
        return myBodyString
    }

    override fun getBodyAsStringLimit(p0: Int): String? {
        TODO("Not yet implemented")
    }

    override fun getStatusCode(): Int {
        return myStatusCode
    }

    override fun getStatusText(): String {
        TODO("Not yet implemented")
    }

    override fun getHeader(p0: String): String? {
        TODO("Not yet implemented")
    }
}