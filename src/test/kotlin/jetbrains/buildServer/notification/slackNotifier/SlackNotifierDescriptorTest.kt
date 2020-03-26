package jetbrains.buildServer.notification.slackNotifier

import jetbrains.buildServer.ExtensionHolder
import jetbrains.buildServer.serverSide.MockServerPluginDescriptior
import jetbrains.buildServer.serverSide.oauth.OAuthConnectionsManager
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Test

class SlackNotifierDescriptorTest : BaseSlackTestCase() {
    private lateinit var descriptor: SlackNotifierDescriptor

    @BeforeMethod
    override fun setUp() {
        super.setUp()

        val connectionManager = OAuthConnectionsManager(myFixture.getSingletonService(ExtensionHolder::class.java))

        descriptor = SlackNotifierDescriptor(
            MockServerPluginDescriptior(),
            SlackConnectionSelectOptionsProvider(myFixture.projectManager, connectionManager)
        )
    }

    @Test
    fun `test should check for channel property`() {
        assertContains(
                descriptor.validate(emptyMap()),
                invalidProperty(descriptor.channelProperty.key)
        )
        assertContains(
                descriptor.validate(mapOf(descriptor.channelProperty.key to "")),
                invalidProperty(descriptor.channelProperty.key)
        )
        assertNotContains(
                descriptor.validate(mapOf(descriptor.channelProperty.key to "#some-channel")),
                invalidProperty(descriptor.channelProperty.key)
        )
    }

    @Test
    fun `test should check for connection property`() {
        assertContains(
                descriptor.validate(emptyMap()),
                invalidProperty(descriptor.connectionProperty.key)
        )
        assertContains(
                descriptor.validate(mapOf(descriptor.connectionProperty.key to "")),
                invalidProperty(descriptor.channelProperty.key)
        )
        assertNotContains(
                descriptor.validate(mapOf(descriptor.connectionProperty.key to "test_connection")),
                invalidProperty(descriptor.connectionProperty.key)
        )
    }
}