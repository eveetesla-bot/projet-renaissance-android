package fr.projetrenaissance.data.health

import android.content.Context
import android.content.pm.PackageManager
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.changes.DeletionChange
import androidx.health.connect.client.changes.UpsertionChange
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.BodyFatRecord
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.FloorsClimbedRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.HeartRateVariabilityRmssdRecord
import androidx.health.connect.client.records.HydrationRecord
import androidx.health.connect.client.records.LeanBodyMassRecord
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.RestingHeartRateRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.SpeedRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.records.Vo2MaxRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.records.metadata.Device
import androidx.health.connect.client.request.ChangesTokenRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import fr.projetrenaissance.data.HealthRecordEntity
import fr.projetrenaissance.domain.HealthRecordType
import fr.projetrenaissance.domain.HealthSourceClassifier
import java.time.Instant
import kotlin.reflect.KClass
import org.json.JSONObject

enum class HealthConnectAvailability { AVAILABLE, UPDATE_REQUIRED, UNAVAILABLE }

data class HealthChangesPage(
    val upserts: List<HealthRecordEntity>,
    val deletedIds: List<String>,
    val nextToken: String,
    val hasMore: Boolean,
    val tokenExpired: Boolean,
)

class HealthConnectGateway(private val context: Context) {
    fun availability(): HealthConnectAvailability = when (HealthConnectClient.getSdkStatus(context)) {
        HealthConnectClient.SDK_AVAILABLE -> HealthConnectAvailability.AVAILABLE
        HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> HealthConnectAvailability.UPDATE_REQUIRED
        else -> HealthConnectAvailability.UNAVAILABLE
    }

    fun permissionContract() = PermissionController.createRequestPermissionResultContract()

    fun readPermissions(types: Set<HealthRecordType> = HealthRecordType.entries.toSet()): Set<String> =
        types.mapTo(linkedSetOf()) { HealthPermission.getReadPermission(recordClass(it)) }

    suspend fun grantedPermissions(): Set<String> = client().permissionController.getGrantedPermissions()

    suspend fun revokeAllPermissions() {
        client().permissionController.revokeAllPermissions()
    }

    suspend fun readInitial(profileId: String, type: HealthRecordType, since: Instant): List<HealthRecordEntity> = when (type) {
        HealthRecordType.STEPS -> read(profileId, type, StepsRecord::class, since)
        HealthRecordType.DISTANCE -> read(profileId, type, DistanceRecord::class, since)
        HealthRecordType.ACTIVE_CALORIES -> read(profileId, type, ActiveCaloriesBurnedRecord::class, since)
        HealthRecordType.TOTAL_CALORIES -> read(profileId, type, TotalCaloriesBurnedRecord::class, since)
        HealthRecordType.HEART_RATE -> read(profileId, type, HeartRateRecord::class, since)
        HealthRecordType.RESTING_HEART_RATE -> read(profileId, type, RestingHeartRateRecord::class, since)
        HealthRecordType.HEART_RATE_VARIABILITY_RMSSD -> read(profileId, type, HeartRateVariabilityRmssdRecord::class, since)
        HealthRecordType.WEIGHT -> read(profileId, type, WeightRecord::class, since)
        HealthRecordType.SLEEP_SESSION -> read(profileId, type, SleepSessionRecord::class, since)
        HealthRecordType.EXERCISE_SESSION -> read(profileId, type, ExerciseSessionRecord::class, since)
        HealthRecordType.SPEED -> read(profileId, type, SpeedRecord::class, since)
        HealthRecordType.FLOORS_CLIMBED -> read(profileId, type, FloorsClimbedRecord::class, since)
        HealthRecordType.VO2_MAX -> read(profileId, type, Vo2MaxRecord::class, since)
        HealthRecordType.HYDRATION -> read(profileId, type, HydrationRecord::class, since)
        HealthRecordType.BODY_FAT -> read(profileId, type, BodyFatRecord::class, since)
        HealthRecordType.LEAN_BODY_MASS -> read(profileId, type, LeanBodyMassRecord::class, since)
    }

    suspend fun changesToken(type: HealthRecordType): String =
        client().getChangesToken(ChangesTokenRequest(setOf(recordClass(type))))

    suspend fun changes(profileId: String, token: String): HealthChangesPage {
        val response = client().getChanges(token)
        val upserts = response.changes.filterIsInstance<UpsertionChange>().map { toEntity(profileId, it.record) }
        val deletions = response.changes.filterIsInstance<DeletionChange>().map { it.recordId }
        return HealthChangesPage(upserts, deletions, response.nextChangesToken, response.hasMore, response.changesTokenExpired)
    }

    private suspend fun <T : Record> read(
        profileId: String,
        type: HealthRecordType,
        recordClass: KClass<T>,
        since: Instant,
    ): List<HealthRecordEntity> {
        val result = mutableListOf<HealthRecordEntity>()
        var pageToken: String? = null
        do {
            val response = client().readRecords(
                ReadRecordsRequest(
                    recordType = recordClass,
                    timeRangeFilter = TimeRangeFilter.between(since, Instant.now()),
                    pageSize = 500,
                    pageToken = pageToken,
                ),
            )
            result += response.records.map { toEntity(profileId, it, type) }
            pageToken = response.pageToken
        } while (pageToken != null)
        return result
    }

    private fun toEntity(profileId: String, record: Record, forcedType: HealthRecordType? = null): HealthRecordEntity {
        val type = forcedType ?: typeOf(record)
        val metadata = record.metadata
        val packageName = metadata.dataOrigin.packageName
        val resolvedLabel = appLabel(packageName)
        val category = HealthSourceClassifier.classify(packageName, resolvedLabel, context.packageName)
        val label = canonicalLabel(category.name, resolvedLabel)
        val (start, end) = times(record)
        val (value, secondary, payload) = values(record)
        return HealthRecordEntity(
            profileId = profileId,
            healthConnectId = metadata.id,
            recordType = type.name,
            startTime = start.toEpochMilli(),
            endTime = end.toEpochMilli(),
            value = value,
            secondaryValue = secondary,
            payloadJson = payload.toString(),
            sourcePackage = packageName,
            sourceLabel = label,
            sourceCategory = category.name,
            deviceManufacturer = metadata.device?.manufacturer,
            deviceModel = metadata.device?.model,
            deviceType = metadata.device?.type,
            recordingMethod = metadata.recordingMethod,
            lastModifiedAt = metadata.lastModifiedTime.toEpochMilli(),
            importedAt = System.currentTimeMillis(),
            dedupeKey = "${type.name}:${start.epochSecond / 60}:${end.epochSecond / 60}",
            isPreferred = true,
        )
    }

    private fun times(record: Record): Pair<Instant, Instant> = when (record) {
        is StepsRecord -> record.startTime to record.endTime
        is DistanceRecord -> record.startTime to record.endTime
        is ActiveCaloriesBurnedRecord -> record.startTime to record.endTime
        is TotalCaloriesBurnedRecord -> record.startTime to record.endTime
        is HeartRateRecord -> record.startTime to record.endTime
        is SleepSessionRecord -> record.startTime to record.endTime
        is ExerciseSessionRecord -> record.startTime to record.endTime
        is SpeedRecord -> record.startTime to record.endTime
        is FloorsClimbedRecord -> record.startTime to record.endTime
        is HydrationRecord -> record.startTime to record.endTime
        is RestingHeartRateRecord -> record.time to record.time
        is HeartRateVariabilityRmssdRecord -> record.time to record.time
        is WeightRecord -> record.time to record.time
        is Vo2MaxRecord -> record.time to record.time
        is BodyFatRecord -> record.time to record.time
        is LeanBodyMassRecord -> record.time to record.time
        else -> metadataTime(record)
    }

    private fun values(record: Record): Triple<Double?, Double?, JSONObject> = when (record) {
        is StepsRecord -> Triple(record.count.toDouble(), null, JSONObject())
        is DistanceRecord -> Triple(record.distance.inMeters, null, JSONObject())
        is ActiveCaloriesBurnedRecord -> Triple(record.energy.inKilocalories, null, JSONObject())
        is TotalCaloriesBurnedRecord -> Triple(record.energy.inKilocalories, null, JSONObject())
        is HeartRateRecord -> {
            val samples = record.samples.map { it.beatsPerMinute.toDouble() }
            Triple(samples.average().takeUnless(Double::isNaN), samples.size.toDouble(), JSONObject().put("samples", samples.size))
        }
        is RestingHeartRateRecord -> Triple(record.beatsPerMinute.toDouble(), null, JSONObject())
        is HeartRateVariabilityRmssdRecord -> Triple(record.heartRateVariabilityMillis, null, JSONObject())
        is WeightRecord -> Triple(record.weight.inKilograms, null, JSONObject())
        is SleepSessionRecord -> Triple((record.endTime.epochSecond - record.startTime.epochSecond) / 3600.0, record.stages.size.toDouble(), JSONObject().put("title", record.title))
        is ExerciseSessionRecord -> Triple((record.endTime.epochSecond - record.startTime.epochSecond) / 60.0, record.exerciseType.toDouble(), JSONObject().put("title", record.title))
        is SpeedRecord -> {
            val values = record.samples.map { it.speed.inMetersPerSecond }
            Triple(values.average().takeUnless(Double::isNaN), values.size.toDouble(), JSONObject().put("samples", values.size))
        }
        is FloorsClimbedRecord -> Triple(record.floors, null, JSONObject())
        is Vo2MaxRecord -> Triple(record.vo2MillilitersPerMinuteKilogram, null, JSONObject())
        is HydrationRecord -> Triple(record.volume.inLiters, null, JSONObject())
        is BodyFatRecord -> Triple(record.percentage.value, null, JSONObject())
        is LeanBodyMassRecord -> Triple(record.mass.inKilograms, null, JSONObject())
        else -> Triple(null, null, JSONObject())
    }

    private fun typeOf(record: Record): HealthRecordType = when (record) {
        is StepsRecord -> HealthRecordType.STEPS
        is DistanceRecord -> HealthRecordType.DISTANCE
        is ActiveCaloriesBurnedRecord -> HealthRecordType.ACTIVE_CALORIES
        is TotalCaloriesBurnedRecord -> HealthRecordType.TOTAL_CALORIES
        is HeartRateRecord -> HealthRecordType.HEART_RATE
        is RestingHeartRateRecord -> HealthRecordType.RESTING_HEART_RATE
        is HeartRateVariabilityRmssdRecord -> HealthRecordType.HEART_RATE_VARIABILITY_RMSSD
        is WeightRecord -> HealthRecordType.WEIGHT
        is SleepSessionRecord -> HealthRecordType.SLEEP_SESSION
        is ExerciseSessionRecord -> HealthRecordType.EXERCISE_SESSION
        is SpeedRecord -> HealthRecordType.SPEED
        is FloorsClimbedRecord -> HealthRecordType.FLOORS_CLIMBED
        is Vo2MaxRecord -> HealthRecordType.VO2_MAX
        is HydrationRecord -> HealthRecordType.HYDRATION
        is BodyFatRecord -> HealthRecordType.BODY_FAT
        is LeanBodyMassRecord -> HealthRecordType.LEAN_BODY_MASS
        else -> error("Type Health Connect non pris en charge : ${record::class.simpleName}")
    }

    private fun recordClass(type: HealthRecordType): KClass<out Record> = when (type) {
        HealthRecordType.STEPS -> StepsRecord::class
        HealthRecordType.DISTANCE -> DistanceRecord::class
        HealthRecordType.ACTIVE_CALORIES -> ActiveCaloriesBurnedRecord::class
        HealthRecordType.TOTAL_CALORIES -> TotalCaloriesBurnedRecord::class
        HealthRecordType.HEART_RATE -> HeartRateRecord::class
        HealthRecordType.RESTING_HEART_RATE -> RestingHeartRateRecord::class
        HealthRecordType.HEART_RATE_VARIABILITY_RMSSD -> HeartRateVariabilityRmssdRecord::class
        HealthRecordType.WEIGHT -> WeightRecord::class
        HealthRecordType.SLEEP_SESSION -> SleepSessionRecord::class
        HealthRecordType.EXERCISE_SESSION -> ExerciseSessionRecord::class
        HealthRecordType.SPEED -> SpeedRecord::class
        HealthRecordType.FLOORS_CLIMBED -> FloorsClimbedRecord::class
        HealthRecordType.VO2_MAX -> Vo2MaxRecord::class
        HealthRecordType.HYDRATION -> HydrationRecord::class
        HealthRecordType.BODY_FAT -> BodyFatRecord::class
        HealthRecordType.LEAN_BODY_MASS -> LeanBodyMassRecord::class
    }

    private fun appLabel(packageName: String): String = try {
        val info = context.packageManager.getApplicationInfo(packageName, 0)
        context.packageManager.getApplicationLabel(info).toString()
    } catch (_: PackageManager.NameNotFoundException) {
        when {
            packageName == "android" || packageName.startsWith("com.android.healthconnect.phone.") -> "Votre téléphone"
            else -> packageName
        }
    }

    private fun canonicalLabel(category: String, resolvedLabel: String): String = when (category) {
        "WITHINGS" -> "Withings"
        "GOOGLE_FIT" -> "Google Fit"
        "BASIC_FIT" -> "Basic-Fit"
        "RENAISSANCE" -> "Projet Renaissance"
        "DEVICE" -> "Votre téléphone"
        else -> resolvedLabel
    }

    private fun metadataTime(record: Record): Pair<Instant, Instant> = record.metadata.lastModifiedTime.let { it to it }
    private fun client(): HealthConnectClient = HealthConnectClient.getOrCreate(context)
}
