/*
 *  Copyright 2000-2020 JetBrains s.r.o.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

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
    private var currentTime: Long = TimeUnit.DAYS.toNanos(1)

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
