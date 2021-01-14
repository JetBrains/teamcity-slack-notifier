/*
 *  Copyright 2000-2021 JetBrains s.r.o.
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