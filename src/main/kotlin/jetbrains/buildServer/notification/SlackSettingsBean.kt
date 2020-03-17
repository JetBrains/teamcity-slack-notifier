package jetbrains.buildServer.notification

data class SlackSettingsBean(var isPaused: Boolean, var botToken: String) {
    val encryptedBotToken: String
        get() {
            return "*".repeat(botToken.length)
        }
}
