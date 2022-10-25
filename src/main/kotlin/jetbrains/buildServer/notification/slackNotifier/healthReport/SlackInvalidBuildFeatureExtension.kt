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

import jetbrains.buildServer.serverSide.BuildTypeNotFoundException
import jetbrains.buildServer.serverSide.ProjectManager
import jetbrains.buildServer.serverSide.SBuildFeatureDescriptor
import jetbrains.buildServer.serverSide.WebLinks
import jetbrains.buildServer.serverSide.auth.Permission
import jetbrains.buildServer.web.openapi.PagePlaces
import jetbrains.buildServer.web.openapi.PluginDescriptor
import jetbrains.buildServer.web.openapi.healthStatus.HealthStatusItemPageExtension
import jetbrains.buildServer.web.util.SessionUser
import org.springframework.stereotype.Service
import javax.servlet.http.HttpServletRequest


@Service
class SlackInvalidBuildFeatureExtension(
    pagePlaces: PagePlaces,
    pluginDescriptor: PluginDescriptor,
    private val webLinks: WebLinks,
    private val projectManager: ProjectManager
) : HealthStatusItemPageExtension(SlackBuildFeatureHealthReport.type, pagePlaces) {
    init {
        includeUrl = pluginDescriptor.getPluginResourcesPath("/healthReport/invalidBuildFeature.jsp")
        isVisibleOutsideAdminArea = false
        register()
    }

    override fun isAvailable(request: HttpServletRequest): Boolean {
        val user = SessionUser.getUser(request)

        val statusItem = getStatusItem(request)
        val data = statusItem.additionalData
        val buildTypeId = data["buildTypeId"] as String
        val buildType = projectManager.findBuildTypeByExternalId(buildTypeId)
        if (buildType != null) {
            if (!user.isPermissionGrantedForProject(buildType.projectId, Permission.RUN_BUILD)) {
                return false
            }
        }

        return super.isAvailable(request)
    }

    override fun fillModel(model: MutableMap<String, Any>, request: HttpServletRequest) {
        super.fillModel(model, request)
        val statusItem = getStatusItem(request)
        val data = statusItem.additionalData
        model["reason"] = data["reason"] as String
        model["feature"] = data["feature"] as SBuildFeatureDescriptor

        val type = data["type"] as String
        val id = data["buildTypeId"] as String
        if (type == "buildType") {
            model["editUrl"] = webLinks.getEditConfigurationPageUrl(id)
            val buildType = projectManager.findBuildTypeByExternalId(id)
                ?: throw BuildTypeNotFoundException("Can't find build type with external id '${id}'")
            model["buildTypeName"] = buildType.fullName
        } else {
            model["editUrl"] = webLinks.getEditTemplatePageUrl(id)
            val template = projectManager.findBuildTypeTemplateByExternalId(id)
                ?: throw BuildTypeNotFoundException("Can't find build template type with external id '${id}'")
            model["buildTypeName"] = template.fullName
        }
    }
}