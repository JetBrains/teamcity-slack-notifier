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

package jetbrains.buildServer.notification.slackNotifier

import jetbrains.buildServer.serverSide.InvalidProperty
import jetbrains.buildServer.serverSide.PropertiesProcessor
import jetbrains.buildServer.serverSide.oauth.OAuthConnectionDescriptor
import jetbrains.buildServer.serverSide.oauth.OAuthProvider
import jetbrains.buildServer.serverSide.oauth.github.GitHubConstants
import jetbrains.buildServer.serverSide.oauth.github.GitHubOAuthProvider
import jetbrains.buildServer.web.openapi.PluginDescriptor
import org.springframework.context.annotation.Conditional
import org.springframework.stereotype.Service

@Service
@Conditional(SlackNotifierEnabled::class)
class SlackConnection(
    private val pluginDescriptor: PluginDescriptor
) : OAuthProvider() {
    override fun getType(): String =
        Companion.type

    override fun getDisplayName(): String =
        name

    override fun getEditParametersUrl(): String =
        pluginDescriptor.getPluginResourcesPath("editConnectionParameters.jsp")

    override fun describeConnection(connection: OAuthConnectionDescriptor): String {
        val displayName = connection.connectionDisplayName
        if (displayName.isEmpty()) {
            return "Connection to a single Slack workspace"
        }

        return displayName
    }

    override fun getPropertiesProcessor(): PropertiesProcessor = PropertiesProcessor {
        val errors = mutableListOf<InvalidProperty>()

        val botToken = it["secure:token"]
        if (botToken.isNullOrEmpty()) {
            errors.add(InvalidProperty("secure:token", "Slack bot token must not be empty"))
        }

        val clientId = it["clientId"]
        if (clientId.isNullOrEmpty()) {
            errors.add(InvalidProperty("clientId", "Client ID must be specified"))
        }

        val clientSecret = it["secure:clientSecret"]
        if (clientSecret.isNullOrEmpty()) {
            errors.add(InvalidProperty("secure:clientSecret", "Client secret must be specified"))
        }

        val maxNotificationsPerBuild = it["serviceMessageMaxNotificationsPerBuild"]
        if (!maxNotificationsPerBuild.isNullOrEmpty() && maxNotificationsPerBuild.toIntOrNull() == null) {
            errors.add(InvalidProperty("serviceMessageMaxNotificationsPerBuild", "Could not parse integer value"))
        }

        errors
    }

    override fun getDefaultProperties(): MutableMap<String, String> {
        return hashMapOf("serviceMessageMaxNotificationsPerBuild" to "0")
    }

    companion object {
        const val type = "slackConnection"
        const val name = "Slack"
    }
}