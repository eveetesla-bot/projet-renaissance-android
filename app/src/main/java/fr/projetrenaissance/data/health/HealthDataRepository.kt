package fr.projetrenaissance.data.health

import androidx.room.withTransaction
import fr.projetrenaissance.data.AppDatabase
import fr.projetrenaissance.data.HealthRecordEntity
import fr.projetrenaissance.data.HealthSyncStateEntity
import fr.projetrenaissance.data.UserPreferencesRepository
import fr.projetrenaissance.domain.HealthDeduplication
import fr.projetrenaissance.domain.HealthDeviceKind
import fr.projetrenaissance.domain.HealthRecordType
import fr.projetrenaissance.domain.HealthRecordingMethod
import fr.projetrenaissance.domain.HealthSample
import fr.projetrenaissance.domain.HealthSourceCategory
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

data class HealthSyncReport(
    val imported: Int,
    val deleted: Int,
    val synchronizedTypes: Int,
    val errors: List<String>,
    val completedAt: Long = System.currentTimeMillis(),
)

class HealthDataRepository(
    private val database: AppDatabase,
    private val gateway: HealthConnectGateway,
    private val preferences: UserPreferencesRepository,
) {
    private val dao = database.dao()

    fun records(profileId: String): Flow<List<HealthRecordEntity>> = dao.observeHealthRecords(profileId)
    fun syncStates(profileId: String): Flow<List<HealthSyncStateEntity>> = dao.observeHealthSyncStates(profileId)
    fun availability(): HealthConnectAvailability = gateway.availability()
    fun permissions(): Set<String> = gateway.readPermissions()
    suspend fun grantedPermissions(): Set<String> = gateway.grantedPermissions()
    suspend fun revokeAllPermissions() = gateway.revokeAllPermissions()

    suspend fun sync(profileId: String): HealthSyncReport {
        if (gateway.availability() != HealthConnectAvailability.AVAILABLE) {
            return HealthSyncReport(0, 0, 0, listOf("Health Connect indisponible"))
        }
        val granted = gateway.grantedPermissions()
        var imported = 0
        var deleted = 0
        var synchronized = 0
        val errors = mutableListOf<String>()
        HealthRecordType.entries.forEach { type ->
            val permission = gateway.readPermissions(setOf(type)).single()
            if (permission !in granted) {
                dao.upsertHealthSyncState(HealthSyncStateEntity(profileId, type.name, null, System.currentTimeMillis(), null, null, "NOT_GRANTED"))
                return@forEach
            }
            try {
                val state = dao.healthSyncState(profileId, type.name)
                val result = if (state?.changesToken == null) initialSync(profileId, type) else incrementalSync(profileId, type, state.changesToken)
                imported += result.first
                deleted += result.second
                synchronized++
            } catch (error: Exception) {
                val message = "${type.name}: ${error.message ?: error::class.simpleName}"
                errors += message
                dao.upsertHealthSyncState(HealthSyncStateEntity(profileId, type.name, null, System.currentTimeMillis(), null, message, "ERROR"))
            }
        }
        recomputePreferred(profileId)
        return HealthSyncReport(imported, deleted, synchronized, errors)
    }

    suspend fun deleteImported(profileId: String) = database.withTransaction {
        dao.deleteImportedHealthRecords(profileId)
        dao.deleteHealthSyncStates(profileId)
    }

    suspend fun setPrimarySource(profileId: String, type: HealthRecordType, source: HealthSourceCategory): Boolean {
        val hasCompatibleData = dao.healthRecords(profileId).any {
            it.recordType == type.name && it.sourceCategory == source.name
        }
        if (!hasCompatibleData) return false
        val defaults = defaultPriorities().getValue(type)
        val ordered = listOf(source) + defaults.filterNot { it == source }
        preferences.setSourcePriority(profileId, type.name, ordered.map { it.name })
        recomputePreferred(profileId)
        return true
    }

    private suspend fun initialSync(profileId: String, type: HealthRecordType): Pair<Int, Int> {
        val records = gateway.readInitial(profileId, type, Instant.now().minus(30, ChronoUnit.DAYS))
        val token = gateway.changesToken(type)
        database.withTransaction {
            if (records.isNotEmpty()) dao.upsertHealthRecords(records)
            dao.upsertHealthSyncState(HealthSyncStateEntity(profileId, type.name, token, System.currentTimeMillis(), System.currentTimeMillis(), null, "GRANTED"))
        }
        return records.size to 0
    }

    private suspend fun incrementalSync(profileId: String, type: HealthRecordType, initialToken: String): Pair<Int, Int> {
        var token = initialToken
        var imported = 0
        var deleted = 0
        do {
            val page = gateway.changes(profileId, token)
            if (page.tokenExpired) return initialSync(profileId, type)
            database.withTransaction {
                if (page.upserts.isNotEmpty()) dao.upsertHealthRecords(page.upserts)
                page.deletedIds.forEach { dao.deleteHealthRecord(profileId, it) }
            }
            imported += page.upserts.size
            deleted += page.deletedIds.size
            token = page.nextToken
        } while (page.hasMore)
        dao.upsertHealthSyncState(HealthSyncStateEntity(profileId, type.name, token, System.currentTimeMillis(), System.currentTimeMillis(), null, "GRANTED"))
        return imported to deleted
    }

    private suspend fun recomputePreferred(profileId: String) {
        val entities = dao.healthRecords(profileId)
        val samples = entities.mapNotNull(::toSample)
        val priorities = configuredPriorities(profileId)
        val preferredIds = HealthDeduplication.selectPreferred(samples, priorities).mapTo(hashSetOf()) { it.healthConnectId }
        if (entities.isNotEmpty()) dao.upsertHealthRecords(
            entities.map {
                it.copy(
                    sourceLabel = canonicalLabel(it.sourceCategory, it.sourceLabel),
                    isPreferred = it.healthConnectId in preferredIds,
                )
            },
        )
    }

    private suspend fun configuredPriorities(profileId: String): Map<HealthRecordType, List<HealthSourceCategory>> =
        defaultPriorities().mapValues { (type, defaults) ->
            val saved = preferences.sourcePriority(profileId, type.name).first()
                .mapNotNull { runCatching { HealthSourceCategory.valueOf(it) }.getOrNull() }
            if (saved.isEmpty()) defaults else saved + defaults.filterNot { it in saved }
        }

    private fun toSample(entity: HealthRecordEntity): HealthSample? = runCatching {
        HealthSample(
            healthConnectId = entity.healthConnectId,
            type = HealthRecordType.valueOf(entity.recordType),
            startTimeMillis = entity.startTime,
            endTimeMillis = entity.endTime,
            value = entity.value,
            sourcePackage = entity.sourcePackage,
            sourceLabel = entity.sourceLabel,
            sourceCategory = HealthSourceCategory.valueOf(entity.sourceCategory),
            recordingMethod = when (entity.recordingMethod) {
                1 -> HealthRecordingMethod.MEASURED
                2 -> HealthRecordingMethod.AUTOMATIC
                3 -> HealthRecordingMethod.MANUAL
                else -> HealthRecordingMethod.UNKNOWN
            },
            deviceKind = when (entity.deviceType) {
                1, 5 -> HealthDeviceKind.WATCH
                // 3 = balance (Withings Body…), 4 = bague, 6 = bracelet,
                // 7 = ceinture cardio : tous de vrais capteurs de mesure.
                3, 4, 6, 7 -> HealthDeviceKind.SENSOR
                2 -> HealthDeviceKind.PHONE
                null -> HealthDeviceKind.UNKNOWN
                else -> HealthDeviceKind.OTHER
            },
            lastModifiedMillis = entity.lastModifiedAt,
        )
    }.getOrNull()

    private fun defaultPriorities(): Map<HealthRecordType, List<HealthSourceCategory>> = HealthRecordType.entries.associateWith { type ->
        when (type) {
            HealthRecordType.EXERCISE_SESSION -> listOf(HealthSourceCategory.RENAISSANCE, HealthSourceCategory.BASIC_FIT, HealthSourceCategory.WITHINGS, HealthSourceCategory.GOOGLE_FIT)
            HealthRecordType.HYDRATION -> listOf(HealthSourceCategory.RENAISSANCE, HealthSourceCategory.WITHINGS, HealthSourceCategory.GOOGLE_FIT)
            else -> listOf(HealthSourceCategory.WITHINGS, HealthSourceCategory.DEVICE, HealthSourceCategory.GOOGLE_FIT, HealthSourceCategory.RENAISSANCE, HealthSourceCategory.OTHER)
        }
    }

    private fun canonicalLabel(category: String, current: String): String = when (category) {
        "WITHINGS" -> "Withings"
        "GOOGLE_FIT" -> "Google Fit"
        "BASIC_FIT" -> "Basic-Fit"
        "RENAISSANCE" -> "Projet Renaissance"
        "DEVICE" -> "Votre téléphone"
        else -> current
    }
}
