package jetbrains.buildServer.notification

import jetbrains.buildServer.notification.slackNotifier.SlackConnection
import jetbrains.buildServer.serverSide.InvalidProperty
import jetbrains.buildServer.serverSide.MockServerPluginDescriptior
import jetbrains.buildServer.serverSide.PropertiesProcessor
import jetbrains.buildServer.serverSide.impl.BaseServerTestCase
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Test

class SlackConnectionTest : BaseServerTestCase() {
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
    fun `test properties processor should check for externalId`() {
        assertContains(
            processor.process(emptyMap()),
            InvalidProperty("externalId", "")
        )
        assertContains(
            processor.process(mapOf("externalId" to "")),
            InvalidProperty("externalId", "")
        )

        assertNotContains(
            processor.process(mapOf("externalId" to "jetbrains.slack.com")),
            InvalidProperty("externalId", "")
        )
    }

    @Test
    fun `test properties processor should check for token`() {
        assertContains(
            processor.process(emptyMap()),
            InvalidProperty("secure:token", "")
        )
        assertContains(
            processor.process(mapOf("secure:token" to "")),
            InvalidProperty("secure:token", "")
        )
        assertNotContains(
            processor.process(mapOf("secure:token" to "some token")),
            InvalidProperty("secure:token", "")
        )
    }
}