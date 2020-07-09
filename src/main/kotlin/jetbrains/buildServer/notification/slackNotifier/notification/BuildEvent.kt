package jetbrains.buildServer.notification.slackNotifier.notification

enum class BuildEvent(val emoji: String) {
    BUILD_SUCCESSFUL(":heavy_check_mark:"),
    BUILD_FAILED(":x:"),
    BUILD_FAILED_TO_START(":exclamation:"),
    LABELING_FAILED(":x:"),
    BUILD_FAILING(":x:"),
    BUILD_PROBABLY_HANGING(":warning:")
}