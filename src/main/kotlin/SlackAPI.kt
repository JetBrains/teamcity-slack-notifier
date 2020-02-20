import org.openapitools.client.ApiClient
import org.openapitools.client.api.ChatApi
import org.openapitools.client.auth.OAuth

fun main() {
    val oauth = OAuth()
    oauth.accessToken = ""
    val apiClient = ApiClient(oauth)
    val result = ChatApi(apiClient).chatPostMessage(
        "", null,
        null, "Test", null,
        null,
        null, null, "#teamcity-notifications-test", null,
        null, null, null, null,
        null, null
    )

    println(result)
}
