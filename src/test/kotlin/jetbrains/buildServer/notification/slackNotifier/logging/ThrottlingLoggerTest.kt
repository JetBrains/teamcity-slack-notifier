package jetbrains.buildServer.notification.slackNotifier.logging

import com.intellij.openapi.diagnostic.Logger
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import jetbrains.buildServer.BaseTestCase
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Test
import java.util.concurrent.TimeUnit

class ThrottlingLoggerTest : BaseTestCase() {
    private lateinit var logger: ThrottlingLogger
    private lateinit var mockLogger: Logger
    private val lastMessage = slot<String>()
    private var currentTime: Long = System.nanoTime()

    private val testMessage1 = "test message 1"
    private val testMessage2 = "test message 2"

    @BeforeMethod(alwaysRun = true)
    override fun setUp() {
        super.setUp()

        mockLogger = mockk()
        every { mockLogger.warn(capture(lastMessage)) } returns Unit
        logger = ThrottlingLogger(mockLogger) {
            currentTime
        }

        lastMessage.clear()
    }

    @Test
    fun `should warn`() {
        logger.warn(testMessage1)
        assertEquals(testMessage1, lastMessage.captured)
    }

    @Test
    fun `should not warn multiple times for the same message`() {
        repeat((0..10).count()) {
            logger.warn(testMessage1)
            currentTime += TimeUnit.MINUTES.toNanos(1)
        }

        io.mockk.verify(exactly = 1) { mockLogger.warn(testMessage1) }
    }

    @Test
    fun `should not warn multiple times for the same multiple messages`() {
        repeat((0..10).count()) {
            logger.warn(testMessage1)
            logger.warn(testMessage2)
            currentTime += TimeUnit.MINUTES.toNanos(1)
        }

        io.mockk.verify(exactly = 1) { mockLogger.warn(testMessage1) }
        io.mockk.verify(exactly = 1) { mockLogger.warn(testMessage2) }
    }

    @Test
    fun `should warn again after some time`() {
        logger.warn(testMessage1)
        currentTime += TimeUnit.MINUTES.toNanos(120)
        logger.warn(testMessage1)
        io.mockk.verify(exactly = 2) { mockLogger.warn(testMessage1) }
    }
}
