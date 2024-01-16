

package jetbrains.buildServer.notification.slackNotifier.notification

data class VerboseMessagesOptions(
        val addBuildStatus: Boolean,
        val addBranch: Boolean,
        val addChanges: Boolean,
        val maximumNumberOfChanges: Int
)