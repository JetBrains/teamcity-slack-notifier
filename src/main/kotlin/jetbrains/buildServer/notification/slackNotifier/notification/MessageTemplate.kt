

package jetbrains.buildServer.notification.slackNotifier.notification

/**
 * Placeholders that can be used in custom notification templates, e.g. `{build.link}`.
 * TeamCity parameter references (`%build.number%`, `%env.TARGET%`, ...) are resolved
 * by the build's own value resolver before placeholders are substituted.
 */
enum class TemplatePlaceholder(val key: String, val description: String) {
    BUILD_EMOJI("build.emoji", "emoji for the build event, e.g. :white_check_mark: or :x:"),
    BUILD_EVENT("build.event", "what happened: started, is successful, failed, failed to start, is failing, is probably hanging"),
    BUILD_LINK("build.link", "project name and a link to the build, e.g. My Project / Build #42"),
    BUILD_URL("build.url", "URL of the build results page"),
    BUILD_NUMBER("build.number", "build number"),
    BUILD_NAME("build.name", "build configuration name"),
    BUILD_BRANCH("build.branch", "branch name, empty if the build has no branch"),
    BUILD_STATUS("build.status", "build status text, e.g. Tests failed: 2 (1 new), passed: 50"),
    BUILD_TRIGGERED_BY("build.triggeredBy", "who or what triggered the build"),
    PROJECT_NAME("project.name", "full project name"),
    CHANGES("changes", "list of changes in the build, one per line, with committer and commit message"),
    CHANGES_COUNT("changes.count", "number of changes in the build"),
    CHANGES_LINK("changes.link", "link to the changes tab of the build, empty when there are no changes"),
    COMMITTERS("committers", "comma-separated names of the users who committed changes to the build"),
    COMMITTERS_MENTIONS("committers.mentions", "Slack mentions (@user) of the committers who signed in to Slack in TeamCity, names for the others"),
    TESTS_FAILED("tests.failed", "list of failed tests, one per line"),
    TESTS_FAILED_COUNT("tests.failed.count", "number of failed tests"),
    PROBLEMS("problems", "list of build problems (failure reasons other than failed tests), one per line"),
    PROBLEMS_COUNT("problems.count", "number of build problems");

    val token: String get() = "{$key}"

    companion object {
        private val byKey = values().associateBy { it.key }
        fun byKey(key: String): TemplatePlaceholder? = byKey[key]
    }
}

/**
 * Minimal, safe placeholder substitution: `{name}` tokens are replaced by values provided
 * by [resolve]; there is no expression language and no access to arbitrary objects.
 */
object MessageTemplate {
    private val placeholderRegex = Regex("\\{([a-zA-Z][a-zA-Z0-9_.]*)}")

    const val defaultTemplate = "{build.emoji} {build.link} *{build.event}*\n" +
            "Status: {build.status}\n" +
            "Changes:\n{changes}"

    /**
     * Replaces every known placeholder with the value returned by [resolve].
     * Unknown placeholders are left untouched so that the message stays readable.
     */
    fun render(template: String, resolve: (TemplatePlaceholder) -> String): String {
        return placeholderRegex.replace(template) { match ->
            val placeholder = TemplatePlaceholder.byKey(match.groupValues[1])
            if (placeholder == null) match.value else resolve(placeholder)
        }.trim()
    }

    /** Returns the placeholder tokens used in the template that are not known, e.g. `{build.foo}`. */
    fun unknownPlaceholders(template: String): List<String> {
        return placeholderRegex.findAll(template)
            .filter { TemplatePlaceholder.byKey(it.groupValues[1]) == null }
            .map { it.value }
            .distinct()
            .toList()
    }

    fun usedPlaceholders(template: String): Set<TemplatePlaceholder> {
        return placeholderRegex.findAll(template)
            .mapNotNull { TemplatePlaceholder.byKey(it.groupValues[1]) }
            .toSet()
    }
}
