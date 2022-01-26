/*
 *  Copyright 2000-2022 JetBrains s.r.o.
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

package jetbrains.buildServer.notification.slackNotifier.healthReport

import jetbrains.buildServer.notification.slackNotifier.BaseSlackTestCase
import jetbrains.buildServer.serverSide.healthStatus.HealthStatusItem
import jetbrains.buildServer.serverSide.healthStatus.HealthStatusReport
import jetbrains.buildServer.serverSide.healthStatus.HealthStatusScope
import jetbrains.buildServer.serverSide.healthStatus.impl.ScopeBuilder
import jetbrains.buildServer.serverSide.healthStatus.reports.StubHealthStatusItemConsumer
import org.testng.annotations.BeforeMethod

abstract class BaseSlackHealthReportTest<T : HealthStatusReport> : BaseSlackTestCase() {
    private val itemsConsumer = StubHealthStatusItemConsumer()
    private lateinit var scope: HealthStatusScope
    private lateinit var result: List<HealthStatusItem>

    @BeforeMethod(alwaysRun = true)
    override fun setUp() {
        super.setUp()
    }

    abstract fun getReport(): T

    protected fun `given project is in scope`() {
        scope = ScopeBuilder().run {
            addProject(myProject)
        }.build()
    }

    protected fun `given build type is in scope`() {
        scope = ScopeBuilder().run {
            addBuildType(myBuildType)
        }.build()
    }

    protected fun `when health is reported`() {
        itemsConsumer.reset()
        getReport().report(scope, itemsConsumer)
        result = itemsConsumer.consumedItems
    }

    protected fun `then report should contain no errors`() {
        assertEmpty(result)
    }

    protected fun assertResultContains(filter: (HealthStatusItem) -> Boolean) {
        assertNotNull(result.toString(), result.find(filter))
    }
}