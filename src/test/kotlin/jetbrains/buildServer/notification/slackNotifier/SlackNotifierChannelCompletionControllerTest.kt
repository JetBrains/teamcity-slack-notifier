package jetbrains.buildServer.notification.slackNotifier

import com.github.salomonbrys.kotson.fromJson
import com.google.gson.Gson
import jetbrains.buildServer.ExtensionHolder
import jetbrains.buildServer.controllers.BaseControllerTestCase
import jetbrains.buildServer.controllers.Completion
import jetbrains.buildServer.notification.NotificatorRegistry
import jetbrains.buildServer.notification.slackNotifier.slack.AggregatedSlackApi
import jetbrains.buildServer.notification.slackNotifier.slack.MockSlackWebApiFactory
import jetbrains.buildServer.serverSide.oauth.OAuthConnectionDescriptor
import jetbrains.buildServer.serverSide.oauth.OAuthConnectionsManager
import jetbrains.buildServer.web.openapi.WebControllerManager
import org.testng.annotations.Test

class SlackNotifierChannelCompletionControllerTest :
    BaseControllerTestCase<SlackNotifierChannelCompletionController>() {
    lateinit var myConnection: OAuthConnectionDescriptor
    lateinit var myDescriptor: SlackNotifierDescriptor

    lateinit var myCompletions: Array<Completion>

    override fun createController(): SlackNotifierChannelCompletionController {

        val oauthManager = OAuthConnectionsManager(
            myFixture.getSingletonService(ExtensionHolder::class.java),
            myFixture.webLinks
        )
        myConnection = oauthManager.addConnection(myProject, "test_type", mapOf("secure:token" to "test_token"))
        myDescriptor = SlackNotifierDescriptor(myFixture.getSingletonService(NotificatorRegistry::class.java))

        return SlackNotifierChannelCompletionController(
            myFixture.securityContext,
            myFixture.getSingletonService(WebControllerManager::class.java),
            myFixture.projectManager,
            oauthManager,
            myDescriptor,
            AggregatedSlackApi(MockSlackWebApiFactory())
        )
    }

    @Test
    fun `test response should be empty to empty request`() {
        `given search is empty`()
        `when completions are requested`()
        `result size should be`(0)
    }

    @Test
    fun `test first item in channels search should be not selectable label`() {
        `given search is`("test_cha")
        `when completions are requested`()
        `result size should be`(2) And
                0.completion.`should not be selectable`() And
                0.completion.`should have label`("Channels")
    }

    @Test
    fun `test first item in users search should be not selectable label`() {
        `given search is`("test_us")
        `when completions are requested`()
        `result size should be`(2) And
                0.completion.`should not be selectable`() And
                0.completion.`should have label`("Users")
    }

    @Test
    fun `test should suggest channel`() {
        `given search is`("test_cha")
        `when completions are requested`()
        `result size should be`(2) And
                1.completion.`should be selectable`() And
                1.completion.`should have value`("#test_channel") And
                1.completion.`should have label`("#test_channel")
    }

    @Test
    fun `test should suggest channel case insensitive`() {
        `given search is`("channel")
        `when completions are requested`()
        `result size should be`(3) And
                1.completion.`should be selectable`() And
                1.completion.`should have value`("#anotherChannel") And
                2.completion.`should be selectable`() And
                2.completion.`should have value`("#test_channel")
    }

    @Test
    fun `test should suggest user by name`() {
        `given search is`("test_us")
        `when completions are requested`()
        `result size should be`(2) And
                1.completion.`should be selectable`() And
                1.completion.`should have value`("USER_ID_1") And
                1.completion.`should have label`("Test User")
    }

    @Test
    fun `test should suggest user by real name`() {
        `given search is`("Test Us")
        `when completions are requested`()
        `result size should be`(2) And
                1.completion.`should be selectable`() And
                1.completion.`should have value`("USER_ID_1") And
                1.completion.`should have label`("Test User")
    }

    private fun `given search is`(term: String) {
        doGet(
            "term", term,
            SlackProperties.connectionProperty.key, myConnection.id
        )
    }

    private fun `given search is empty`() {
        doGet()
    }

    private fun `when completions are requested`() {
        myCompletions = Gson().fromJson(myResponse.returnedContent)
    }

    private fun `result size should be`(size: Int) {
        assertEquals(size, myCompletions.size)
    }

    private val Int.completion: Completion
        get() = myCompletions[this]

    private fun Completion.`should be selectable`() {
        assertTrue(this.isSelectable)
    }

    private fun Completion.`should not be selectable`() {
        assertFalse(this.isSelectable)
    }

    private fun Completion.`should have value`(value: String) {
        assertEquals(value, this.value)
    }

    private fun Completion.`should have label`(label: String) {
        assertEquals(label, this.label)
    }
}