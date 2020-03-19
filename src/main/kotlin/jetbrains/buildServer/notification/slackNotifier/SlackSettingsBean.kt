package jetbrains.buildServer.notification.slackNotifier

data class SlackSettingsBean(var isPaused: Boolean, var botToken: String) {
    val encryptedBotToken: String
        get() {
            return "*".repeat(botToken.length)
        }
}
