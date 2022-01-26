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

import jetbrains.buildServer.serverSide.oauth.OAuthConnectionDescriptor
import jetbrains.buildServer.web.openapi.PagePlaces
import jetbrains.buildServer.web.openapi.PluginDescriptor
import jetbrains.buildServer.web.openapi.healthStatus.HealthStatusItemPageExtension
import org.springframework.stereotype.Service
import javax.servlet.http.HttpServletRequest

@Service
class SlackInvalidConnectionExtension(
    pagePlaces: PagePlaces,
    pluginDescriptor: PluginDescriptor
) : HealthStatusItemPageExtension(SlackConnectionHealthReport.type, pagePlaces) {
    init {
        includeUrl = pluginDescriptor.getPluginResourcesPath("/healthReport/invalidConnection.jsp")
        isVisibleOutsideAdminArea = false
        register()
    }

    override fun fillModel(model: MutableMap<String, Any>, request: HttpServletRequest) {
        super.fillModel(model, request)

        val statusItem = getStatusItem(request)

        val data = statusItem.additionalData
        model["connection"] = data["connection"] as OAuthConnectionDescriptor
        model["reason"] = data["reason"] as String
    }
}