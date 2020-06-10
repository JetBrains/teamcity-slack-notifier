package jetbrains.buildServer.notification.slackNotifier.notification

import jetbrains.buildServer.Build
import jetbrains.buildServer.notification.slackNotifier.slack.SlackMessageFormatter
import jetbrains.buildServer.serverSide.ProjectManager
import jetbrains.buildServer.serverSide.RelativeWebLinks
import jetbrains.buildServer.serverSide.mute.MuteInfo

class DetailsFormatter(
        private val format: SlackMessageFormatter,
        private val links: RelativeWebLinks,
        private val projectManager: ProjectManager
) {
    fun userName(muteInfo: MuteInfo) = muteInfo.mutingUser?.username
    fun projectName(muteInfo: MuteInfo): String {
        val project = muteInfo.project ?: return "<deleted project>"
        val url = links.getProjectPageUrl(project.externalId)
        return format.url(url, project.fullName)
    }

    fun buildUrl(build: Build): String {
        val projectName =
                projectManager.findProjectByExternalId(build.projectExternalId)?.fullName ?: "<deleted project>"

        val buildType = build.buildType
        val buildTypeName = buildType?.name ?: ""

        val buildName = "$buildTypeName ${number(build)}"
        return "$projectName / ${format.url(links.getViewResultsUrl(build), buildName)}"
    }

    private fun number(build: Build) = "#${build.buildNumber}"
}