package fr.projetrenaissance.domain

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

enum class HealthRecordType {
    STEPS,
    DISTANCE,
    ACTIVE_CALORIES,
    TOTAL_CALORIES,
    HEART_RATE,
    RESTING_HEART_RATE,
    HEART_RATE_VARIABILITY_RMSSD,
    WEIGHT,
    SLEEP_SESSION,
    EXERCISE_SESSION,
    SPEED,
    FLOORS_CLIMBED,
    VO2_MAX,
    HYDRATION,
    BODY_FAT,
    LEAN_BODY_MASS,
}

enum class HealthSourceCategory { WITHINGS, GOOGLE_FIT, BASIC_FIT, RENAISSANCE, DEVICE, OTHER }
enum class HealthRecordingMethod { MEASURED, AUTOMATIC, MANUAL, UNKNOWN }
enum class HealthDeviceKind { WATCH, SENSOR, PHONE, OTHER, UNKNOWN }

data class HealthSample(
    val healthConnectId: String,
    val type: HealthRecordType,
    val startTimeMillis: Long,
    val endTimeMillis: Long = startTimeMillis,
    val value: Double? = null,
    val sourcePackage: String,
    val sourceLabel: String,
    val sourceCategory: HealthSourceCategory,
    val recordingMethod: HealthRecordingMethod = HealthRecordingMethod.UNKNOWN,
    val deviceKind: HealthDeviceKind = HealthDeviceKind.UNKNOWN,
    val lastModifiedMillis: Long = startTimeMillis,
)

object HealthSourceClassifier {
    fun classify(packageName: String, appLabel: String?, ownPackage: String): HealthSourceCategory {
        if (packageName == ownPackage) return HealthSourceCategory.RENAISSANCE
        val evidence = "$packageName ${appLabel.orEmpty()}".lowercase()
        return when {
            "withings" in evidence -> HealthSourceCategory.WITHINGS
            "google fit" in evidence || "com.google.android.apps.fitness" in evidence -> HealthSourceCategory.GOOGLE_FIT
            "basic-fit" in evidence || "basic fit" in evidence -> HealthSourceCategory.BASIC_FIT
            packageName == "android" || packageName.startsWith("com.android.healthconnect.phone.") -> HealthSourceCategory.DEVICE
            else -> HealthSourceCategory.OTHER
        }
    }
}

object HealthDeduplication {
    fun selectPreferred(
        samples: List<HealthSample>,
        priorities: Map<HealthRecordType, List<HealthSourceCategory>>,
    ): List<HealthSample> {
        return samples.groupBy { it.type }.flatMap { (type, sameType) ->
            val active = mutableListOf<MutableList<HealthSample>>()
            val completed = mutableListOf<List<HealthSample>>()
            sameType.sortedWith(compareBy(HealthSample::startTimeMillis, HealthSample::healthConnectId)).forEach { sample ->
                val expired = active.filterNot { couldStillMatch(it, sample) }
                completed += expired
                active.removeAll(expired.toSet())
                val group = active.firstOrNull { members -> members.any { equivalent(it, sample) } }
                if (group == null) active += mutableListOf(sample) else group += sample
            }
            completed += active
            completed.mapNotNull { group ->
                group.maxWithOrNull(
                    compareBy<HealthSample> { qualityScore(it, priorities[type].orEmpty()) }
                        .thenBy { it.lastModifiedMillis }
                        .thenBy { it.healthConnectId },
                )
            }
        }.sortedBy { it.startTimeMillis }
    }

    fun equivalent(first: HealthSample, second: HealthSample): Boolean {
        if (first.type != second.type || first.sourcePackage == second.sourcePackage) {
            return false
        }
        return when (first.type) {
            HealthRecordType.STEPS,
            HealthRecordType.DISTANCE,
            HealthRecordType.ACTIVE_CALORIES,
            HealthRecordType.TOTAL_CALORIES,
            HealthRecordType.FLOORS_CLIMBED,
            HealthRecordType.SPEED,
            -> overlapMillis(first, second) > 0

            HealthRecordType.EXERCISE_SESSION -> {
                abs(first.startTimeMillis - second.startTimeMillis) <= 10 * 60_000L && overlapRatio(first, second) >= 0.70
            }

            HealthRecordType.SLEEP_SESSION -> overlapRatio(first, second) >= 0.70
            HealthRecordType.WEIGHT,
            HealthRecordType.BODY_FAT,
            HealthRecordType.LEAN_BODY_MASS,
            -> abs(first.startTimeMillis - second.startTimeMillis) <= 5 * 60_000L && valuesClose(first, second, 0.02)

            HealthRecordType.HEART_RATE,
            HealthRecordType.RESTING_HEART_RATE,
            HealthRecordType.HEART_RATE_VARIABILITY_RMSSD,
            -> abs(first.startTimeMillis - second.startTimeMillis) <= 5_000L && valuesClose(first, second, 0.01)

            HealthRecordType.HYDRATION -> abs(first.startTimeMillis - second.startTimeMillis) <= 2 * 60_000L && valuesClose(first, second, 0.01)
            HealthRecordType.VO2_MAX -> abs(first.startTimeMillis - second.startTimeMillis) <= 5 * 60_000L && valuesClose(first, second, 0.02)
        }
    }

    private fun qualityScore(sample: HealthSample, priority: List<HealthSourceCategory>): Int {
        val measuredDevice = sample.recordingMethod == HealthRecordingMethod.MEASURED &&
            sample.deviceKind in setOf(HealthDeviceKind.WATCH, HealthDeviceKind.SENSOR)
        val priorityIndex = priority.indexOf(sample.sourceCategory)
        val sourceScore = if (priorityIndex >= 0) 500 - priorityIndex else 0
        val methodScore = when (sample.recordingMethod) {
            HealthRecordingMethod.MEASURED -> 120
            HealthRecordingMethod.AUTOMATIC -> 80
            HealthRecordingMethod.MANUAL -> 20
            HealthRecordingMethod.UNKNOWN -> 0
        }
        return (if (measuredDevice) 1_000 else 0) + sourceScore + methodScore
    }

    private fun couldStillMatch(group: List<HealthSample>, sample: HealthSample): Boolean {
        val latestStart = group.maxOf { it.startTimeMillis }
        val latestEnd = group.maxOf { it.endTimeMillis }
        return when (sample.type) {
            HealthRecordType.STEPS,
            HealthRecordType.DISTANCE,
            HealthRecordType.ACTIVE_CALORIES,
            HealthRecordType.TOTAL_CALORIES,
            HealthRecordType.FLOORS_CLIMBED,
            HealthRecordType.SPEED,
            HealthRecordType.SLEEP_SESSION,
            -> latestEnd > sample.startTimeMillis

            HealthRecordType.EXERCISE_SESSION -> latestEnd > sample.startTimeMillis && sample.startTimeMillis - latestStart <= 10 * 60_000L
            HealthRecordType.HEART_RATE,
            HealthRecordType.RESTING_HEART_RATE,
            HealthRecordType.HEART_RATE_VARIABILITY_RMSSD,
            -> sample.startTimeMillis - latestStart <= 5_000L

            HealthRecordType.WEIGHT,
            HealthRecordType.BODY_FAT,
            HealthRecordType.LEAN_BODY_MASS,
            HealthRecordType.VO2_MAX,
            -> sample.startTimeMillis - latestStart <= 5 * 60_000L

            HealthRecordType.HYDRATION -> sample.startTimeMillis - latestStart <= 2 * 60_000L
        }
    }

    private fun overlapMillis(a: HealthSample, b: HealthSample): Long =
        (min(a.endTimeMillis, b.endTimeMillis) - max(a.startTimeMillis, b.startTimeMillis)).coerceAtLeast(0)

    private fun overlapRatio(a: HealthSample, b: HealthSample): Double {
        val shortest = min(a.endTimeMillis - a.startTimeMillis, b.endTimeMillis - b.startTimeMillis)
        return if (shortest <= 0) 0.0 else overlapMillis(a, b).toDouble() / shortest
    }

    private fun valuesClose(a: HealthSample, b: HealthSample, tolerance: Double): Boolean {
        val left = a.value ?: return false
        val right = b.value ?: return false
        val scale = max(max(abs(left), abs(right)), 1.0)
        return abs(left - right) / scale <= tolerance
    }
}

sealed interface HealthChange {
    data class Upsert(val sample: HealthSample) : HealthChange
    data class Delete(val healthConnectId: String) : HealthChange
}

object HealthSyncReducer {
    fun apply(current: List<HealthSample>, changes: List<HealthChange>): List<HealthSample> {
        val records = current.associateByTo(linkedMapOf()) { it.healthConnectId }
        changes.forEach { change ->
            when (change) {
                is HealthChange.Upsert -> records[change.sample.healthConnectId] = change.sample
                is HealthChange.Delete -> records.remove(change.healthConnectId)
            }
        }
        return records.values.sortedBy { it.startTimeMillis }
    }

    fun readableTypes(granted: Set<HealthRecordType>, requested: Set<HealthRecordType>): Set<HealthRecordType> =
        granted.intersect(requested)
}
