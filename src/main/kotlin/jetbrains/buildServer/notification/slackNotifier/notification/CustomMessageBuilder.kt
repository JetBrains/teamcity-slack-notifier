

package jetbrains.buildServer.notification.slackNotifier.notification

import com.intellij.openapi.diagnostic.Logger
import jetbrains.buildServer.BuildProblemData
import jetbrains.buildServer.BuildProblemTypes
import jetbrains.buildServer.notification.NotificationBuildStatusProvider
import jetbrains.buildServer.notification.TemplateMessageBuilder
import jetbrains.buildServer.notification.slackNotifier.SlackProperties
import jetbrains.buildServer.notification.slackNotifier.slack.SlackMessageFormatter
import jetbrains.buildServer.notification.slackNotifier.teamcity.getChanges
import jetbrains.buildServer.serverSide.*
import jetbrains.buildServer.users.SUser
import jetbrains.buildServer.vcs.SVcsModification

/**
 * Templates for the "custom" message format. [default] is used for every build event
 * unless a more specific template is provided.
 */
data class CustomMessageTemplates(
    val default: String,
    val success: String? = null,
    val failure: String? = null
) {
    val successOrDefault: String get() = success ?: default
    val failureOrDefault: String get() = failure ?: default
}

/**
 * Renders build notifications from user-provided templates, see [TemplatePlaceholder] for the
 * supported placeholders. Events that are not about a build (investigations, mutes, ...)
 * are delegated to [fallback].
 */
class CustomMessageBuilder(
    private val fallback: MessageBuilder,
    private val templates: CustomMessageTemplates,
    private val maximumNumberOfChanges: Int,
    private val format: SlackMessageFormatter,
    private val links: RelativeWebLinks,
    private val detailsFormatter: DetailsFormatter,
    private val notificationBuildStatusProvider: NotificationBuildStatusProvider,
    private val server: SBuildServer,
    private val changesCalculationOptionsFactory: ChangesCalculationOptionsFactory
) : MessageBuilder by fallback {
    private val logger = Logger.getInstance(CustomMessageBuilder::class.java.name)

    override fun buildStarted(build: SRunningBuild): MessagePayload =
        render(build, BuildEvent.BUILD_STARTED, templates.default)

    override fun buildSuccessful(build: SRunningBuild): MessagePayload =
        render(build, BuildEvent.BUILD_SUCCESSFUL, templates.successOrDefault)

    override fun buildFailed(build: SRunningBuild): MessagePayload =
        render(build, BuildEvent.BUILD_FAILED, templates.failureOrDefault)

    override fun buildFailedToStart(build: SRunningBuild): MessagePayload =
        render(build, BuildEvent.BUILD_FAILED_TO_START, templates.failureOrDefault)

    override fun buildFailing(build: SRunningBuild): MessagePayload =
        render(build, BuildEvent.BUILD_FAILING, templates.failureOrDefault)

    override fun buildProbablyHanging(build: SRunningBuild): MessagePayload =
        render(build, BuildEvent.BUILD_PROBABLY_HANGING, templates.default)

    private fun render(build: SBuild, event: BuildEvent, template: String): MessagePayload {
        // The running build instance may not have the final statistics yet, same trick as in the verbose format
        val actualBuild = server.findBuildInstanceById(build.buildId) ?: build
        val context = BuildTemplateContext(actualBuild, event)
        val text = MessageTemplate.render(resolveParameterReferences(actualBuild, template)) { context.value(it) }
        return MessagePayload(text = text)
    }

    /** Resolves TeamCity parameter references like `%build.number%` or `%env.TARGET%`. */
    private fun resolveParameterReferences(build: SBuild, template: String): String {
        if (!template.contains('%')) {
            return template
        }

        return try {
            build.valueResolver.resolve(template).result
        } catch (e: Exception) {
            logger.warn("Failed to resolve parameter references in Slack message template for build ${build.buildId}: ${e.message}")
            template
        }
    }

    private data class Committer(val name: String, val slackUserId: String?)

    private inner class BuildTemplateContext(private val build: SBuild, private val event: BuildEvent) {
        private val changes: List<SVcsModification> by lazy {
            changesCalculationOptionsFactory.getChanges(build.buildPromotion)
        }

        private val committers: List<Committer> by lazy {
            changes.flatMap { change -> committersOf(change) }.distinct()
        }

        private val failedTests: List<STestRun> by lazy {
            build.shortStatistics.failedTests
        }

        private val problems: List<BuildProblemData> by lazy {
            build.failureReasons.filter { it.type != BuildProblemTypes.TC_FAILED_TESTS_TYPE }
        }

        fun value(placeholder: TemplatePlaceholder): String = when (placeholder) {
            TemplatePlaceholder.BUILD_EMOJI -> event.emoji
            TemplatePlaceholder.BUILD_EVENT -> event.text
            TemplatePlaceholder.BUILD_LINK -> detailsFormatter.buildUrl(build)
            TemplatePlaceholder.BUILD_URL -> links.getViewResultsUrl(build)
            TemplatePlaceholder.BUILD_NUMBER -> build.buildNumber
            TemplatePlaceholder.BUILD_NAME -> format.escape(build.buildType?.name ?: "")
            TemplatePlaceholder.BUILD_BRANCH -> build.branch?.displayName?.let { format.escape(it) } ?: ""
            TemplatePlaceholder.BUILD_STATUS -> statusText()
            TemplatePlaceholder.BUILD_TRIGGERED_BY -> triggeredBy()
            TemplatePlaceholder.PROJECT_NAME -> format.escape(build.buildType?.project?.fullName ?: "")
            TemplatePlaceholder.CHANGES -> changesList()
            TemplatePlaceholder.CHANGES_COUNT -> changes.size.toString()
            TemplatePlaceholder.CHANGES_LINK -> changesLink()
            TemplatePlaceholder.COMMITTERS -> committers.joinToString(", ") { format.escape(it.name) }
            TemplatePlaceholder.COMMITTERS_MENTIONS -> committers.joinToString(", ") { mention(it) }
            TemplatePlaceholder.TESTS_FAILED -> list(failedTests.map { format.escape(it.test.name.asString) })
            TemplatePlaceholder.TESTS_FAILED_COUNT -> build.shortStatistics.failedTestCount.toString()
            TemplatePlaceholder.PROBLEMS -> list(problems.map { format.escape(it.description) })
            TemplatePlaceholder.PROBLEMS_COUNT -> problems.size.toString()
        }

        private fun statusText(): String {
            val buildStatistics = build.getBuildStatistics(
                BuildStatisticsOptions(
                    BuildStatisticsOptions.COMPILATION_ERRORS,
                    TemplateMessageBuilder.MAX_NUM_OF_STACKTRACES
                )
            )
            return format.escape(notificationBuildStatusProvider.getText(build, buildStatistics))
        }

        private fun triggeredBy(): String {
            val triggeredBy = build.triggeredBy
            val text = if (triggeredBy.isTriggeredByUser) {
                triggeredBy.user?.descriptiveName ?: triggeredBy.asString
            } else {
                triggeredBy.asString
            }
            return format.escape(text)
        }

        private fun committersOf(change: SVcsModification): List<Committer> {
            val users: Collection<SUser> = change.committers
            if (users.isEmpty()) {
                val userName = change.userName ?: return emptyList()
                return listOf(Committer(userName, null))
            }

            return users.map { user ->
                Committer(user.descriptiveName, user.getPropertyValue(SlackProperties.channelProperty)?.takeIf { isSlackUserId(it) })
            }
        }

        private fun mention(committer: Committer): String {
            val slackUserId = committer.slackUserId ?: return format.escape(committer.name)
            return "<@$slackUserId>"
        }

        private fun changesList(): String {
            if (changes.isEmpty()) {
                return "No new changes"
            }

            val lines = changes.take(maximumNumberOfChanges).map { change ->
                val author = committersOf(change).firstOrNull()?.name
                val description = singleLine(change.description, maxChangeDescriptionLength)
                val prefix = if (author != null) "${format.escape(author)}∶ " else ""
                format.listElement("$prefix${format.escape(description)}")
            }

            return withRemainder(lines, changes.size)
        }

        private fun changesLink(): String {
            if (changes.isEmpty()) {
                return ""
            }

            val text = if (changes.size == 1) "View 1 change in TeamCity" else "View all ${changes.size} changes in TeamCity"
            return format.url(url = links.getViewChangesUrl(build), text = text)
        }

        private fun list(items: List<String>): String {
            val lines = items.take(maximumNumberOfListItems).map { format.listElement(it) }
            return withRemainder(lines, items.size)
        }

        private fun withRemainder(lines: List<String>, total: Int): String {
            val remainder = total - lines.size
            if (remainder <= 0) {
                return lines.joinToString("\n")
            }
            return (lines + format.listElement("... and $remainder more")).joinToString("\n")
        }
    }

    companion object {
        const val maxChangeDescriptionLength = 200
        const val maximumNumberOfListItems = 10

        private val slackUserIdRegex = Regex("[UW][A-Z0-9]{2,}")
        private val lineBreaksRegex = Regex("\\s*\\R+\\s*")

        fun isSlackUserId(value: String): Boolean = slackUserIdRegex.matches(value)

        /** Collapses a multi-line text (e.g. a commit message with a bullet list body) into one line. */
        fun singleLine(text: String, maximumLength: Int): String {
            val collapsed = text.trim().replace(lineBreaksRegex, " ")
            val postfix = "..."
            return if (collapsed.length > maximumLength) {
                collapsed.substring(0, maximumLength - postfix.length).trimEnd() + postfix
            } else {
                collapsed
            }
        }
    }
}
