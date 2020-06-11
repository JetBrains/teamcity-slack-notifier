package jetbrains.buildServer.notification.slackNotifier

import jetbrains.buildServer.notification.NotificatorRegistry
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Test

class SlackNotifierDescriptorTest : BaseSlackTestCase() {
    private lateinit var descriptor: SlackNotifierDescriptor

    @BeforeMethod
    override fun setUp() {
        super.setUp()

        descriptor = SlackNotifierDescriptor(myFixture.getSingletonService(NotificatorRegistry::class.java))
    }

    @Test
    fun `should check for channel property`() {
        assertContains(
                descriptor.validate(emptyMap()),
                invalidProperty(SlackProperties.channelProperty.key)
        )
        assertContains(
                descriptor.validate(mapOf(SlackProperties.channelProperty.key to "")),
                invalidProperty(SlackProperties.channelProperty.key)
        )
        assertNotContains(
                descriptor.validate(mapOf(SlackProperties.channelProperty.key to "#some-channel")),
            invalidProperty(SlackProperties.channelProperty.key)
        )
    }

    @Test
    fun `should check for connection property`() {
        assertContains(
                descriptor.validate(emptyMap()),
                invalidProperty(SlackProperties.connectionProperty.key)
        )
        assertContains(
                descriptor.validate(mapOf(SlackProperties.connectionProperty.key to "")),
                invalidProperty(SlackProperties.channelProperty.key)
        )
        assertNotContains(
                descriptor.validate(mapOf(SlackProperties.connectionProperty.key to "test_connection")),
                invalidProperty(SlackProperties.connectionProperty.key)
        )
    }

    @Test
    fun `should check for maximum number of notifications property`() {
        assertNotContains(
                descriptor.validate(emptyMap()),
                invalidProperty(SlackProperties.maximumNumberOfChangesProperty.key)
        )
        assertContains(
                descriptor.validate(mapOf(SlackProperties.maximumNumberOfChangesProperty.key to "not a number")),
                invalidProperty(SlackProperties.maximumNumberOfChangesProperty.key)
        )
    }
}