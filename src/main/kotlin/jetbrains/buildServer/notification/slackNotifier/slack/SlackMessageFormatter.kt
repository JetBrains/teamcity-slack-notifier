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

package jetbrains.buildServer.notification.slackNotifier.slack

import org.springframework.stereotype.Service

@Service
class SlackMessageFormatter {
    private val escapingSymbols = mapOf(
        "&" to "&amp;",
        "<" to "&lt;",
        ">" to "&gt;"
    )

    private val escapingSymbolsStr = "[${escapingSymbols.keys.joinToString("")}]"
    private val escapingSymbolsRegex = Regex(escapingSymbolsStr)

    fun url(url: String, text: String = ""): String {
        return "<${url}|${text}>"
    }

    fun listElement(text: String): String = "- $text"

    fun bold(text: String): String = "*${text}*"

    fun italic(text: String): String = "_${text}_"

    fun escape(text: String): String {
        return text.replace(escapingSymbolsRegex) {
            escapingSymbols[it.value] ?: it.value
        }
    }
}
