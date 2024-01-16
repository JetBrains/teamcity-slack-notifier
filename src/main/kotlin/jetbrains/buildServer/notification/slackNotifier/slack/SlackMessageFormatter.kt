

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

    fun escape(text: String): String {
        return text.replace(escapingSymbolsRegex) {
            escapingSymbols[it.value] ?: it.value
        }
    }
}