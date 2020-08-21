package jetbrains.buildServer.notification.slackNotifier

import jetbrains.buildServer.serverSide.MockServerPluginDescriptior
import jetbrains.buildServer.serverSide.PropertiesProcessor
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Test

class SlackConnectionTest : BaseSlackTestCase() {
    private lateinit var connection: SlackConnection
    private lateinit var processor: PropertiesProcessor

    @BeforeMethod(alwaysRun = true)
    override fun setUp() {
        super.setUp()

        connection = SlackConnection(
                MockServerPluginDescriptior()
        )
        processor = connection.propertiesProcessor
    }

    @Test
    fun `test properties processor should check for token`() {
        assertContains(
            processor.process(emptyMap()),
                invalidProperty("secure:token")
        )
        assertContains(
            processor.process(mapOf("secure:token" to "")),
                invalidProperty("secure:token")
        )
        assertNotContains(
            processor.process(mapOf("secure:token" to "some token")),
                invalidProperty("secure:token")
        )
    }
}