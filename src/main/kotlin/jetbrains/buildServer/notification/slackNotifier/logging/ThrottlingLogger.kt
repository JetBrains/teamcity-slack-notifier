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
import com.intellij.util.containers.ConcurrentWeakHashMap
import jetbrains.buildServer.notification.slackNotifier.SlackNotifierProperties
import jetbrains.buildServer.serverSide.TeamCityProperties
import java.util.concurrent.TimeUnit

/**
 * Logger that will repeat the same message not more than once per hour (or once per teamcity.internal.notification.jbSlackNotifier.logger.throttleTimeoutMinutes)
 * Messages are stored in WeakHashMap as keys. This means that throttling might not actually work if full GC is run.
 * This is a trade-off from storing hard references to messages and always remembering them for the full hour
 * so that memory can be freed if java needs it.
 *
 * TODO: before moving this to core: implement all other logger methods.
 * Open question: should the same message be throttled it's first logger as warning and then as info?
 */
class ThrottlingLogger(
        private val logger: Logger,
        private val currentTimeInNanos: () -> Long = { System.nanoTime() }
) {
    private val lastTimeMessageWasSent = ConcurrentWeakHashMap<String, Long>()
    private val defaultLastTime = TimeUnit.DAYS.toNanos(-1)

    fun warn(message: String) {
        lastTimeMessageWasSent.compute(message) { _, lastTime ->
            val now = currentTimeInNanos()
            val timeout = TeamCityProperties.getLong(SlackNotifierProperties.loggerThrottleTimeout, 60)
            if (now - (lastTime ?: defaultLastTime) > TimeUnit.MINUTES.toNanos(timeout)) {
                logger.warn(message)
                now
            } else {
                lastTime
            }
        }
    }
}