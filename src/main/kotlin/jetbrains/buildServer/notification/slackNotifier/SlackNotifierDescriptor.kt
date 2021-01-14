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

import jetbrains.buildServer.notification.NotificatorRegistry
import jetbrains.buildServer.serverSide.InvalidProperty
import org.springframework.context.annotation.Conditional
import org.springframework.stereotype.Service

@Service
@Conditional(SlackNotifierEnabled::class)
class SlackNotifierDescriptor(
        private val notificatorRegistry: NotificatorRegistry
) {
    fun validate(properties: Map<String, String>): MutableCollection<InvalidProperty> {
        val invalidProperties = mutableListOf<InvalidProperty>()

        val channel = properties[SlackProperties.channelProperty.key]
        if (channel.isNullOrEmpty()) {
            invalidProperties.add(
                InvalidProperty(
                    SlackProperties.channelProperty.key,
                    "Channel or user id must not be empty"
                )
            )
        }

        val connection = properties[SlackProperties.connectionProperty.key]
        if (connection.isNullOrEmpty()) {
            invalidProperties.add(
                    InvalidProperty(
                            SlackProperties.connectionProperty.key,
                            "Connection must be selected"
                    )
            )
        }

        properties[SlackProperties.maximumNumberOfChangesProperty.key]?.let { maximumNumberOfChanges ->
            if (maximumNumberOfChanges.isEmpty()) {
                return@let
            }

            val asInt = maximumNumberOfChanges.toIntOrNull()

            if (asInt == null) {
                invalidProperties.add(
                        InvalidProperty(
                                SlackProperties.maximumNumberOfChangesProperty.key,
                                "Maximum number of changes must be integer"
                        )
                )
                return@let
            }

            if (asInt < 0) {
                invalidProperties.add(
                        InvalidProperty(
                                SlackProperties.maximumNumberOfChangesProperty.key,
                                "Maximum number of changes must not be less than 0"
                        )
                )
            }
        }

        return invalidProperties
    }


    fun getType(): String = notifierType
    fun getDisplayName(): String {
        if (displayNameClashes()) {
            return "$defaultDisplayName (official)"
        }
        return defaultDisplayName
    }

    private fun displayNameClashes(): Boolean {
        return notificatorRegistry.notificators.filter {
            it.notificatorType != notifierType
        }.find { it.displayName.trim() == defaultDisplayName } != null
    }

    companion object {
        const val notifierType = "jbSlackNotifier"
        const val defaultDisplayName = "Slack Notifier"
    }
}