

package jetbrains.buildServer.notification.slackNotifier.notification

import jetbrains.buildServer.Build
import jetbrains.buildServer.notification.slackNotifier.slack.SlackMessageFormatter
import jetbrains.buildServer.serverSide.BuildPromotionEx
import jetbrains.buildServer.serverSide.ProjectManager
import jetbrains.buildServer.serverSide.RelativeWebLinks
import jetbrains.buildServer.serverSide.SBuild
import jetbrains.buildServer.serverSide.SQueuedBuild
import jetbrains.buildServer.serverSide.mute.MuteInfo
import org.springframework.stereotype.Service

@Service
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
                format.escape(projectManager.findProjectByExternalId(build.projectExternalId)?.fullName ?: "<deleted project>")

        val buildType = build.buildType
        val buildTypeName = format.escape(buildType?.name ?: "")

        val buildName = "$buildTypeName ${number(build)}"
        return "$projectName / ${format.url(links.getViewResultsUrl(build), buildName)}"
    }

    fun serviceMessageBuildUrl(build: SBuild): String {
        val anchorBuild = anchorBuild(build) ?: return buildUrl(build)
        val buildType = anchorBuild.buildType ?: return buildUrl(build)

        val projectName = format.escape(buildType.project.fullName)
        val buildTypeName = format.escape(buildType.name)
        val buildName = "Build ${number(anchorBuild)}"
        return "$projectName / $buildTypeName / ${format.url(links.getViewResultsUrl(anchorBuild), buildName)}"
    }

    fun buildUrl(queuedBuild: SQueuedBuild): String {
        val buildType = queuedBuild.buildType
        val projectName =
                format.escape(projectManager.findProjectByExternalId(buildType.projectExternalId)?.fullName ?: "<deleted project>")

        val buildTypeName = format.escape(buildType.name)
        return "$projectName / ${format.url(links.getQueuedBuildUrl(queuedBuild), buildTypeName)}"
    }

    private fun number(build: Build) = "#${build.buildNumber}"

    private fun anchorBuild(build: SBuild): SBuild? =
        (build.buildPromotion as? BuildPromotionEx)?.anchorBuildPromotion?.associatedBuild
}
