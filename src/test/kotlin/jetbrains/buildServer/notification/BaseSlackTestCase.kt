package jetbrains.buildServer.notification

import jetbrains.buildServer.BaseTestCase
import jetbrains.buildServer.ExtensionHolder
import jetbrains.buildServer.serverSide.MockServerPluginDescriptior
import jetbrains.buildServer.serverSide.impl.BaseServerTestCase
import jetbrains.buildServer.serverSide.oauth.OAuthConnectionsManager
import jetbrains.buildServer.users.SUser
import org.testng.annotations.BeforeMethod

open class BaseSlackTestCase : BaseNotificationRulesTestCase() {
    protected lateinit var mySlackApi: MockSlackWebApi
    protected lateinit var myDescriptor: SlackNotifierDescriptor
    protected lateinit var myNotifier: SlackNotifier
    protected lateinit var myUser: SUser

    @BeforeMethod(alwaysRun = true)
    override fun setUp() {
        super.setUp()
        val connectionManager = OAuthConnectionsManager(myFixture.getSingletonService(ExtensionHolder::class.java))

        val slackApiFactory = MockSlackWebApiFactory()
        mySlackApi = slackApiFactory.createSlackWebApi()

        myDescriptor = SlackNotifierDescriptor(MockServerPluginDescriptior())
        myNotifier = SlackNotifier(
            myFixture.notificatorRegistry,
            slackApiFactory,
            SimpleMessageBuilder(
                SlackMessageFormatter(),
                myFixture.webLinks,
                myProjectManager
            ),
            myFixture.serverPaths,
            myProjectManager,
            connectionManager,
            myDescriptor
        )

        myFixture.addService(myDescriptor)
        myFixture.addService(myNotifier)

        connectionManager.addConnection(
            myProject,
            SlackConnection.type,
            mapOf("externalId" to "test_connection", "secure:token" to "test_token")
        )

        myUser = createUser("test_user")
        myUser.setUserProperty(myDescriptor.channelProperty, "#test_channel")
        myUser.setUserProperty(myDescriptor.connectionProperty, "test_connection")
    }

    fun assertLastMessageContains(str: String) {
        assertNotEmpty(mySlackApi.messages)
        assertContains(mySlackApi.messages.last().text, str)
    }

    private fun <T> Iterable<T>.last(): T {
        return reversed().first()
    }
}