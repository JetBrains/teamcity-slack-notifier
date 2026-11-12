

package jetbrains.buildServer.notification.slackNotifier.notification

enum class BuildEvent(val emoji: String, val text: String) {
    BUILD_STARTED(":arrow_forward:", "started"),
    BUILD_SUCCESSFUL(":white_check_mark:", "is successful"),
    BUILD_FAILED(":x:", "failed"),
    BUILD_FAILED_TO_START(":exclamation:", "failed to start"),
    LABELING_FAILED(":x:", "labeling failed"),
    BUILD_FAILING(":x:", "is failing"),
    BUILD_PROBABLY_HANGING(":warning:", "is probably hanging"),
    QUEUED_BUILD_WAITING_APPROVAL(":grey_exclamation:", "is waiting for approval")
}