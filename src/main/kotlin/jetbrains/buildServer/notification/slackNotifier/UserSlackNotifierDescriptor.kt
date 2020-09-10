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

package jetbrains.buildServer.notification.slackNotifier

import jetbrains.buildServer.notification.UserNotifierDescriptor
import jetbrains.buildServer.parameters.ParametersUtil
import jetbrains.buildServer.serverSide.ControlDescription
import jetbrains.buildServer.serverSide.InvalidProperty
import jetbrains.buildServer.serverSide.parameters.WellknownParameterArguments
import jetbrains.buildServer.serverSide.parameters.types.TextParameter
import jetbrains.buildServer.web.openapi.PluginDescriptor
import org.springframework.context.annotation.Conditional
import org.springframework.stereotype.Service

@Service
@Conditional(SlackNotifierEnabled::class)
class UserSlackNotifierDescriptor(
    private val descriptor: SlackNotifierDescriptor,
    private val pluginDescriptor: PluginDescriptor
) : UserNotifierDescriptor {
    override fun validate(properties: MutableMap<String, String>): MutableCollection<InvalidProperty> =
        descriptor.validate(properties)

    override fun getParameters(): Map<String, ControlDescription> {
        return mapOf(
            SlackProperties.connectionProperty.key to ParametersUtil.createControlDescription(
                "selection",
                mapOf(
                    WellknownParameterArguments.ARGUMENT_DESCRIPTION.name to "Connection",
                    WellknownParameterArguments.REQUIRED.name to "true"
                )
            ),
            SlackProperties.channelProperty.key to ParametersUtil.createControlDescription(
                TextParameter.KEY,
                mapOf(
                    WellknownParameterArguments.ARGUMENT_DESCRIPTION.name to "#channel or user id",
                    WellknownParameterArguments.REQUIRED.name to "true",
                    "hintUrl" to SlackNotifierChannelCompletionController.url
                )
            )
        )
    }

    override fun getType(): String = descriptor.getType()
    override fun getDisplayName(): String = descriptor.getDisplayName()
    override fun getEditParametersUrl(): String =
        pluginDescriptor.getPluginResourcesPath("editUserSlackNotifierSettings.html")
}