package jetbrains.buildServer.notification.slackNotifier.healthReport

import jetbrains.buildServer.notification.slackNotifier.SlackNotifierEnabled
import jetbrains.buildServer.serverSide.ProjectManager
import jetbrains.buildServer.serverSide.SProject
import jetbrains.buildServer.serverSide.healthStatus.HealthStatusItem
import jetbrains.buildServer.serverSide.healthStatus.HealthStatusItemConsumer
import jetbrains.buildServer.serverSide.healthStatus.HealthStatusReport
import jetbrains.buildServer.serverSide.healthStatus.HealthStatusScope
import jetbrains.buildServer.serverSide.healthStatus.ItemCategory
import jetbrains.buildServer.serverSide.healthStatus.ItemSeverity
import org.springframework.context.annotation.Conditional
import org.springframework.stereotype.Service

@Service
@Conditional(SlackNotifierEnabled::class)
class SlackFailedNotificationHealthReport(
    private val failuresCollector: SlackFailedNotificationCollector,
    private val projectManager: ProjectManager
) : HealthStatusReport() {
    override fun report(scope: HealthStatusScope, consumer: HealthStatusItemConsumer) {
        for (project in collectScopeProjects(scope)) {
            val failures = failuresCollector.getProjectFailures(project)

            for (failure in failures) {
                val category = when {
                    failure.isThrottlingFailure -> deliveryThrottledCategory
                    failure.isConfigurationFailure -> deliveryConfigurationCategory
                    else -> deliveryRuntimeCategory
                }

                val data = mutableMapOf<String, Any>(
                    "project" to project,
                    "reason" to failure.reason,
                    "receiver" to (failure.receiver ?: ""),
                    "connectionId" to (failure.connectionId ?: ""),
                    "errorCode" to failure.errorCode,
                    "isThrottlingFailure" to failure.isThrottlingFailure,
                    "occurrences" to failure.occurrences
                )
                failure.buildTypeExternalId?.let { data["buildTypeId"] = it }

                val item = HealthStatusItem("${type}_${failure.id}", category, data)
                val buildType = failure.buildTypeExternalId?.let { projectManager.findBuildTypeByExternalId(it) }
                if (buildType != null && buildType.project.projectId == project.projectId) {
                    consumer.consumeForBuildType(buildType, item)
                } else {
                    consumer.consumeForProject(project, item)
                }
            }
        }
    }

    private fun collectScopeProjects(scope: HealthStatusScope): Set<SProject> {
        val projects = linkedSetOf<SProject>()
        projects.addAll(scope.projects)
        projects.addAll(scope.buildTypes.map { it.project })
        projects.addAll(scope.buildTypeTemplates.map { it.project })
        return projects
    }

    override fun getType(): String = type

    override fun getDisplayName(): String = "Report failed Slack notifications"

    override fun getCategories(): Collection<ItemCategory> =
        listOf(deliveryConfigurationCategory, deliveryThrottledCategory, deliveryRuntimeCategory)

    override fun canReportItemsFor(scope: HealthStatusScope): Boolean {
        return scope.isItemWithSeverityAccepted(ItemSeverity.ERROR) || scope.isItemWithSeverityAccepted(ItemSeverity.WARN)
    }

    companion object {
        const val type = "slackFailedNotificationReport"

        val deliveryConfigurationCategory = ItemCategory(
            "slackNotificationDeliveryConfigurationFailure",
            "Slack notification delivery failed due to configuration",
            ItemSeverity.ERROR
        )

        val deliveryThrottledCategory = ItemCategory(
            "slackNotificationDeliveryThrottled",
            "Slack notification delivery is throttled",
            ItemSeverity.WARN
        )

        val deliveryRuntimeCategory = ItemCategory(
            "slackNotificationDeliveryRuntimeFailure",
            "Slack notification delivery failed due to runtime errors",
            ItemSeverity.WARN
        )
    }
}
