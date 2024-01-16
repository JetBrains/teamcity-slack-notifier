

package jetbrains.buildServer.notification.slackNotifier.teamcity

import jetbrains.buildServer.serverSide.BuildTypeSettings
import jetbrains.buildServer.serverSide.ProjectManager

fun ProjectManager.findBuildTypeSettingsByExternalId(buildTypeId: String): BuildTypeSettings? {
    return when {
        buildTypeId.startsWith("buildType:") -> {
            findBuildTypeByExternalId(buildTypeId.substring("buildType:".length))
        }
        buildTypeId.startsWith("template:") -> {
            findBuildTypeTemplateByExternalId(buildTypeId.substring("template:".length))
        }

        else -> null
    }
}