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

import jetbrains.buildServer.controllers.ActionErrors
import jetbrains.buildServer.controllers.BaseFormXmlController
import jetbrains.buildServer.controllers.BasePropertiesBean
import jetbrains.buildServer.controllers.admin.projects.PluginPropertiesUtil
import jetbrains.buildServer.notification.slackNotifier.slack.AuthTestResult
import jetbrains.buildServer.notification.slackNotifier.slack.SlackWebApiFactory
import jetbrains.buildServer.serverSide.IOGuard
import jetbrains.buildServer.serverSide.InvalidProperty
import jetbrains.buildServer.serverSide.PropertiesProcessor
import jetbrains.buildServer.serverSide.SBuildServer
import jetbrains.buildServer.util.FuncThrow
import jetbrains.buildServer.web.openapi.WebControllerManager
import org.jdom.Element
import org.springframework.context.annotation.Conditional
import org.springframework.stereotype.Service
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@Service
@Conditional(SlackNotifierEnabled::class)
class SlackTestConnectionController(
        server: SBuildServer,
        webControllerManager: WebControllerManager,
        private val connection: SlackConnection,
        slackWebApiFactory: SlackWebApiFactory
) : BaseFormXmlController(server) {
    private val path = "/admin/slack/testConnection.html"
    private val slackApi = slackWebApiFactory.createSlackWebApi()

    init {
        webControllerManager.registerController(path, this)
    }

    override fun doPost(request: HttpServletRequest, response: HttpServletResponse, xmlResponse: Element) {
        val errors = ActionErrors()
        val props = getProps(request)
        errors.fillErrors(connection.propertiesProcessor, props)
        errors.fillErrors(getPropertiesProcessor(), props)
        errors.serialize(xmlResponse)
    }

    private fun getProps(request: HttpServletRequest): Map<String, String> {
        val propBean = BasePropertiesBean(emptyMap())
        PluginPropertiesUtil.bindPropertiesFromRequest(request, propBean)
        return propBean.properties
    }

    // client_id and client_secret are tested separately since there is no API for checking if they are correct
    private fun getPropertiesProcessor(): PropertiesProcessor = PropertiesProcessor {
        val errors = mutableListOf<InvalidProperty>()
        val botToken = it["secure:token"] ?: return@PropertiesProcessor errors

        val authTest = IOGuard.allowNetworkCall<AuthTestResult, Exception> { slackApi.authTest(botToken) }
        if (!authTest.ok) {
            errors.add(
                    InvalidProperty(
                            "secure:token",
                            "Bot token is invalid. Bot authentication failed with the following error: ${authTest.error}"
                    )
            )
        }
        errors
    }

    override fun doGet(request: HttpServletRequest, response: HttpServletResponse) = null
}