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

import jetbrains.buildServer.PluginTypes
import jetbrains.buildServer.users.PluginPropertyKey

class SlackProperties {
    companion object {
        private const val channel = "channel"
        private const val connection = "connection"
        private const val displayName = "displayName"
        private const val messageFormat = "messageFormat"
        private const val addBuildStatus = "addBuildStatus"
        private const val addBranch = "addBranch"
        private const val addChanges = "addChanges"
        private const val maximumNumberOfChanges = "maximumNumberOfChanges"

        val channelProperty = property(channel)
        val connectionProperty = property(connection)
        val displayNameProperty = property(displayName)
        val messageFormatProperty = property(messageFormat)
        val addBuildStatusProperty = property(addBuildStatus)
        val addBranchProperty = property(addBranch)
        val addChangesProperty = property(addChanges)
        val maximumNumberOfChangesProperty = property(maximumNumberOfChanges)

        private fun property(name: String): PluginPropertyKey {
            return PluginPropertyKey(PluginTypes.NOTIFICATOR_PLUGIN_TYPE, SlackNotifierDescriptor.notifierType, name)
        }
    }

    val channelKey = channelProperty.key
    val connectionKey = connectionProperty.key
    val messageFormatKey = messageFormatProperty.key
    val addBuildStatusKey = addBuildStatusProperty.key
    val addBranchKey = addBranchProperty.key
    val addChangesKey = addChangesProperty.key
    val maximumNumberOfChangesKey = maximumNumberOfChangesProperty.key
}