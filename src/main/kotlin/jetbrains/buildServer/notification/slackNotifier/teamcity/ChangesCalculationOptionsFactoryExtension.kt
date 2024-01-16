

package jetbrains.buildServer.notification.slackNotifier.teamcity

import jetbrains.buildServer.serverSide.BuildPromotion
import jetbrains.buildServer.serverSide.ChangesCalculationOptions
import jetbrains.buildServer.serverSide.ChangesCalculationOptionsFactory
import jetbrains.buildServer.vcs.SVcsModification
import jetbrains.buildServer.vcs.SelectPrevBuildPolicy

fun ChangesCalculationOptionsFactory.getChanges(buildPromotion: BuildPromotion): List<SVcsModification> {
    val options: ChangesCalculationOptions = create()
    options.isReturnChangesIfNoPreviousBuildFound = true
    options.includeDependencyChanges = null
    options.prevBuildPolicy = SelectPrevBuildPolicy.SINCE_LAST_BUILD
    return buildPromotion.getChanges(options).vcsChanges.toList()
}