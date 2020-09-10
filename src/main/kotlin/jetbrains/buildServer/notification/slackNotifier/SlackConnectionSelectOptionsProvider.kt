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

import jetbrains.buildServer.PluginTypes
import jetbrains.buildServer.serverSide.ProjectManager
import jetbrains.buildServer.serverSide.auth.Permission
import jetbrains.buildServer.serverSide.oauth.OAuthConnectionsManager
import jetbrains.buildServer.serverSide.parameters.SelectOption
import jetbrains.buildServer.serverSide.parameters.UserSelectOptionsProvider
import jetbrains.buildServer.users.PluginPropertyKey
import jetbrains.buildServer.users.SUser
import org.springframework.context.annotation.Conditional
import org.springframework.stereotype.Service

@Service
@Conditional(SlackNotifierEnabled::class)
class SlackConnectionSelectOptionsProvider(
        private val projectManager: ProjectManager,
        private val oAuthConnectionsManager: OAuthConnectionsManager
) : UserSelectOptionsProvider {
    override fun getSelectOptions(user: SUser): MutableList<SelectOption> {
        val projects = projectManager.projects.filter {
            user.isPermissionGrantedForProject(it.projectId, Permission.VIEW_PROJECT)
        }

        return (listOf((SelectOption("", "-- Choose connection --"))) + projects.flatMap { project ->
            oAuthConnectionsManager.getAvailableConnectionsOfType(project, SlackConnection.type)
        }.distinctBy {
            it.id
        }.map {
            SelectOption(it.id, it.connectionDisplayName)
        }).toMutableList()
    }

    override fun getId(): String {
        return PluginPropertyKey(PluginTypes.NOTIFICATOR_PLUGIN_TYPE, SlackNotifierDescriptor.type, "connectionSelectOptionsProvider").key
    }
}