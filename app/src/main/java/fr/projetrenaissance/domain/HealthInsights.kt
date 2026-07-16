package fr.projetrenaissance.domain

import fr.projetrenaissance.data.DailyCheckInEntity
import fr.projetrenaissance.data.HealthRecordEntity
import fr.projetrenaissance.data.SetLogEntity
import java.time.Instant
import java.time.ZoneId

data class MeasuredValue(
    val value: Double,
    val unit: String,
    val source: String,
    val measuredAt: Long,
    val status: String,
    val confidence: Int,
    val syncedAt: Long,
)

data class DailyHealthSummary(
    val readiness: DailyReadinessResult,
    val sleepNight: AggregatedSleepNight?,
    val stepsToday: MeasuredValue?,
    val restingHeartRate: MeasuredValue?,
    val weight: MeasuredValue?,
    val sessionsThisWeek: Int,
    val baselines: PersonalBaselineSummary,
)

data class PersonalBaselineSummary(
    val sleepSamples: Int,
    val sleepReferenceMinutes: Int?,
    val restingHeartRateSamples: Int,
    val restingHeartRateReference: Double?,
    val hrvSamples: Int,
    val hrvReference: Double?,
) {
    companion object {
        const val SLEEP_REQUIRED = 4
        const val RESTING_HEART_RATE_REQUIRED = 3
        const val HRV_REQUIRED = 4
    }
}

private fun progressiveBaselineConfidence(sampleCount: Int, activationSamples: Int): Int {
    if (sampleCount < activationSamples) return (20 + sampleCount * 10).coerceAtMost(50)
    val progress = (sampleCount - activationSamples).toDouble() / (14 - activationSamples)
    return (55 + progress.coerceIn(0.0, 1.0) * 35).toInt()
}

private fun median(values: List<Int>): Int? = values.takeIf { it.isNotEmpty() }
    ?.sorted()
    ?.let { sorted ->
        val middle = sorted.size / 2
        if (sorted.size % 2 == 0) (sorted[middle - 1] + sorted[middle]) / 2 else sorted[middle]
    }

private fun averageOrNull(values: List<Double>, required: Int): Double? =
    values.takeIf { it.size >= required }?.average()

object HealthInsights {
    private const val HOUR = 3_600_000L
    private const val DAY = 24 * HOUR

    fun calculate(
        records: List<HealthRecordEntity>,
        checkIns: List<DailyCheckInEntity>,
        setLogs: List<SetLogEntity>,
        now: Long = System.currentTimeMillis(),
        zone: ZoneId = ZoneId.systemDefault(),
        profileId: String? = null,
    ): DailyHealthSummary {
        val isolatedRecords = profileId?.let { id -> records.filter { it.profileId == id } } ?: records
        val isolatedCheckIns = profileId?.let { id -> checkIns.filter { it.profileId == id } } ?: checkIns
        val isolatedSetLogs = profileId?.let { id -> setLogs.filter { it.profileId == id } } ?: setLogs
        val preferred = isolatedRecords.filter { it.isPreferred }
        val sleepSegments = preferred.filter { it.recordType == "SLEEP_SESSION" }.map {
            SleepSegment(it.healthConnectId, it.startTime, it.endTime, it.sourcePackage, it.sourceLabel, sourcePriority(it.sourceCategory), it.lastModifiedAt)
        }
        val sleepNight = SleepAggregation.latestNight(sleepSegments, now, zone)
        val latestSleepIds = sleepNight?.segmentIds.orEmpty().toSet()
        val sleepDurations = preferred.filter {
            it.recordType == "SLEEP_SESSION" &&
                it.healthConnectId !in latestSleepIds &&
                it.endTime in (now - 28 * DAY)..now
        }
            .map { ((it.endTime - it.startTime) / 60_000).toInt() }.filter { it in 180..900 }
        val sleepReference = median(sleepDurations).takeIf {
            sleepDurations.size >= PersonalBaselineSummary.SLEEP_REQUIRED
        }
        val sleepInput = sleepNight?.takeIf { it.quality != SleepQuality.STALE }?.let {
            SleepReadinessInput(
                durationMinutes = it.sleepDurationMinutes,
                personalAverageMinutes = sleepReference,
                confidence = minOf(
                    it.confidence,
                    progressiveBaselineConfidence(sleepDurations.size, PersonalBaselineSummary.SLEEP_REQUIRED),
                ),
            )
        }
        val rhrRecords = preferred.filter { it.recordType == "RESTING_HEART_RATE" && it.value != null }.sortedBy { it.startTime }
        val latestRhr = rhrRecords.lastOrNull()
        val rhrHistory = rhrRecords.filter { it.startTime in (now - 28 * DAY)..(now - DAY) }.mapNotNull { it.value }
        val hrvRecords = preferred.filter { it.recordType == "HEART_RATE_VARIABILITY_RMSSD" && it.value != null }.sortedBy { it.startTime }
        val latestHrv = hrvRecords.lastOrNull()?.takeIf { now - it.startTime <= 36 * HOUR }
        val hrvHistory = hrvRecords.filter { it.startTime in (now - 28 * DAY)..(now - DAY) }.mapNotNull { it.value }
        val rhrReference = averageOrNull(rhrHistory, PersonalBaselineSummary.RESTING_HEART_RATE_REQUIRED)
        val hrvReference = averageOrNull(hrvHistory, PersonalBaselineSummary.HRV_REQUIRED)
        val recentRhr = latestRhr?.takeIf { now - it.startTime <= 36 * HOUR && rhrReference != null }
        val usableHrv = latestHrv?.takeIf { hrvReference != null }
        val cardioConfidence = listOfNotNull(
            recentRhr?.let { progressiveBaselineConfidence(rhrHistory.size, PersonalBaselineSummary.RESTING_HEART_RATE_REQUIRED) },
            usableHrv?.let { progressiveBaselineConfidence(hrvHistory.size, PersonalBaselineSummary.HRV_REQUIRED) },
        ).takeIf { it.isNotEmpty() }?.average()?.toInt() ?: 0
        val cardioInput = if (recentRhr != null || usableHrv != null) CardioReadinessInput(
            restingHeartRate = recentRhr?.value,
            personalAverage = rhrReference,
            ageHours = (now - maxOf(recentRhr?.startTime ?: 0L, usableHrv?.startTime ?: 0L)).toDouble() / HOUR,
            confidence = cardioConfidence,
            hrvRmssd = usableHrv?.value,
            personalAverageHrv = hrvReference,
        ) else null
        val recentExerciseMinutes = preferred.filter { it.recordType == "EXERCISE_SESSION" && it.endTime >= now - 72 * HOUR }.sumOf { it.value ?: ((it.endTime - it.startTime) / 60_000.0) }
        val historicExercise = preferred.filter { it.recordType == "EXERCISE_SESSION" && it.endTime in (now - 14 * DAY)..(now - 3 * DAY) }
            .sumOf { it.value ?: ((it.endTime - it.startTime) / 60_000.0) }
        val loadInput = if (recentExerciseMinutes > 0 || historicExercise > 0) LoadReadinessInput(recentExerciseMinutes, if (historicExercise > 0) historicExercise / 11.0 * 3.0 else null) else null
        val checkIn = isolatedCheckIns.lastOrNull()?.takeIf { now - it.recordedAt <= 36 * HOUR }
        val readiness = DailyReadinessEngine.calculate(ReadinessInputs(
            sleep = sleepInput,
            cardiovascular = cardioInput,
            recentLoad = loadInput,
            pain = checkIn?.pain,
            manual = checkIn?.let { ManualReadinessInput(energy = it.energy, mood = it.mood) },
        ))
        val startOfDay = Instant.ofEpochMilli(now).atZone(zone).toLocalDate().atStartOfDay(zone).toInstant().toEpochMilli()
        val steps = preferred.filter { it.recordType == "STEPS" && it.endTime >= startOfDay && it.startTime <= now && it.value != null }
        val stepValue = steps.sumOf { it.value!! }.takeIf { steps.isNotEmpty() }?.let { value -> measured(value, "pas", steps.maxBy { it.endTime }, now) }
        val rhrValue = latestRhr?.value?.let { measured(it, "bpm", latestRhr, now) }
        val latestWeight = preferred.filter { it.recordType == "WEIGHT" && it.value != null }.maxByOrNull { it.startTime }
        val weight = latestWeight?.value?.let { measured(it, "kg", latestWeight, now) }
        val weekStart = Instant.ofEpochMilli(now).atZone(zone).toLocalDate().minusDays((Instant.ofEpochMilli(now).atZone(zone).dayOfWeek.value - 1).toLong()).atStartOfDay(zone).toInstant().toEpochMilli()
        val healthSessions = preferred.count { it.recordType == "EXERCISE_SESSION" && it.startTime >= weekStart }
        val localSessions = isolatedSetLogs.filter { it.completedAt >= weekStart }.map { it.templateId to Instant.ofEpochMilli(it.completedAt).atZone(zone).toLocalDate() }.distinct().size
        return DailyHealthSummary(
            readiness = readiness,
            sleepNight = sleepNight,
            stepsToday = stepValue,
            restingHeartRate = rhrValue,
            weight = weight,
            sessionsThisWeek = maxOf(healthSessions, localSessions),
            baselines = PersonalBaselineSummary(
                sleepSamples = sleepDurations.size,
                sleepReferenceMinutes = sleepReference,
                restingHeartRateSamples = rhrHistory.size,
                restingHeartRateReference = rhrReference,
                hrvSamples = hrvHistory.size,
                hrvReference = hrvReference,
            ),
        )
    }

    private fun measured(value: Double, unit: String, record: HealthRecordEntity, now: Long) = MeasuredValue(
        value, unit, record.sourceLabel, record.endTime, if (now - record.endTime <= 36 * HOUR) "À jour" else "Ancienne", if (record.isPreferred) 90 else 50, record.importedAt,
    )

    private fun sourcePriority(category: String): Int = when (category) { "WITHINGS" -> 4; "GOOGLE_FIT" -> 3; "BASIC_FIT" -> 2; "RENAISSANCE" -> 5; else -> 1 }
}
