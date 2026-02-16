package jetbrains.buildServer.notification.slackNotifier.healthReport

import jetbrains.buildServer.notification.slackNotifier.SlackNotifierEnabled
import jetbrains.buildServer.notification.slackNotifier.SlackNotifierProperties
import jetbrains.buildServer.serverSide.SProject
import jetbrains.buildServer.serverSide.TeamCityProperties
import org.springframework.context.annotation.Conditional
import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

@Service
@Conditional(SlackNotifierEnabled::class)
class SlackFailedNotificationCollector {
    private val failures = ConcurrentHashMap<FailureKey, FailureRecord>()
    @Volatile
    private var nextCleanupTimeMs: Long = 0

    data class Failure(
        val id: String,
        val buildTypeExternalId: String?,
        val connectionId: String?,
        val receiver: String?,
        val errorCode: String,
        val reason: String,
        val isThrottlingFailure: Boolean,
        val isConfigurationFailure: Boolean,
        val occurrences: Int,
        val lastOccurredAt: Long
    )

    fun reportFailure(
        project: SProject,
        buildTypeExternalId: String?,
        connectionId: String?,
        receiver: String?,
        errorCode: String,
        reason: String
    ) {
        cleanupExpired()

        val key = FailureKey(
            projectId = project.projectId,
            buildTypeExternalId = buildTypeExternalId,
            connectionId = connectionId,
            receiver = receiver,
            errorCode = errorCode
        )

        failures.compute(key) { _, existing ->
            val now = System.currentTimeMillis()
            val prevOccurrences = existing?.occurrences ?: 0
            FailureRecord(
                reason = reason,
                occurrences = prevOccurrences + 1,
                lastOccurredAt = now
            )
        }
    }

    fun clearDeliveryErrors(project: SProject, buildTypeExternalId: String?, connectionId: String?, receiver: String) {
        cleanupExpired()

        val iterator = failures.keys.iterator()
        while (iterator.hasNext()) {
            val key = iterator.next()
            val buildTypeMatches = key.buildTypeExternalId == buildTypeExternalId ||
                    (key.buildTypeExternalId == null && buildTypeExternalId != null)
            if (key.projectId == project.projectId &&
                buildTypeMatches &&
                key.connectionId == connectionId &&
                key.receiver == receiver
            ) {
                iterator.remove()
            }
        }
    }

    fun getProjectFailures(project: SProject): List<Failure> {
        cleanupExpired()

        return failures.entries
            .asSequence()
            .filter { it.key.projectId == project.projectId }
            .map { (key, value) ->
                Failure(
                    id = buildId(key),
                    buildTypeExternalId = key.buildTypeExternalId,
                    connectionId = key.connectionId,
                    receiver = key.receiver,
                    errorCode = key.errorCode,
                    reason = value.reason,
                    isThrottlingFailure = isThrottlingRelated(key.errorCode),
                    isConfigurationFailure = isConfigurationRelated(key.errorCode),
                    occurrences = value.occurrences,
                    lastOccurredAt = value.lastOccurredAt
                )
            }
            .toList()
    }

    private fun cleanupExpired() {
        val now = System.currentTimeMillis()
        if (now < nextCleanupTimeMs) {
            return
        }

        synchronized(this) {
            val timeAfterLock = System.currentTimeMillis()
            if (timeAfterLock < nextCleanupTimeMs) {
                return
            }

            val ttlMs = TeamCityProperties.getIntervalMilliseconds(
                SlackNotifierProperties.failedNotificationHealthReportTtlHours,
                TimeUnit.HOURS.toMillis(24L)
            )
            failures.entries.removeIf { timeAfterLock - it.value.lastOccurredAt > ttlMs }
            nextCleanupTimeMs = timeAfterLock + CLEANUP_INTERVAL_MS
        }
    }

    private fun buildId(key: FailureKey): String {
        return "${key.projectId}_${key.hashCode().toUInt().toString(16)}"
    }

    private data class FailureKey(
        val projectId: String,
        val buildTypeExternalId: String?,
        val connectionId: String?,
        val receiver: String?,
        val errorCode: String
    )

    private data class FailureRecord(
        val reason: String,
        val occurrences: Int,
        val lastOccurredAt: Long
    )

    companion object {
        private const val CLEANUP_INTERVAL_MS = 60_000L
        private val configurationRelatedErrors = setOf(
            "account_inactive",
            "channel_not_found",
            "invalid_auth",
            "is_archived",
            "missing_channel_property",
            "missing_connection_property",
            "missing_scope",
            "missing_token",
            "no_permission",
            "not_authed",
            "not_in_channel",
            "token_revoked",
            "user_not_found"
        )

        fun isConfigurationRelated(errorCode: String): Boolean {
            return configurationRelatedErrors.contains(errorCode)
        }

        private val throttlingRelatedErrors = setOf(
            "ratelimited"
        )

        fun isThrottlingRelated(errorCode: String): Boolean {
            return throttlingRelatedErrors.contains(errorCode)
        }
    }
}
