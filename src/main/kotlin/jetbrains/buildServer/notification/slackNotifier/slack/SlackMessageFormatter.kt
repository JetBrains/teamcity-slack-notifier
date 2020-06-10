package jetbrains.buildServer.notification.slackNotifier.slack

import org.springframework.stereotype.Service

@Service
class SlackMessageFormatter {
    fun url(url: String, text: String = ""): String {
        return "<${url}|${text}>"
    }

    fun listElement(text: String): String = "- $text"
}