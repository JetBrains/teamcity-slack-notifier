package jetbrains.buildServer.slackNotifications

class SlackMessageFormatter {
    fun url(url: String, text: String = ""): String {
        return "<${url}|${text}>"
    }

    fun listElement(text: String): String = "- $text"
}