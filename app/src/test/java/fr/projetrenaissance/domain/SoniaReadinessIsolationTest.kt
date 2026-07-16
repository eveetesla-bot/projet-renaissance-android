package fr.projetrenaissance.domain

import fr.projetrenaissance.data.HealthRecordEntity
import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SoniaReadinessIsolationTest {
    private val now = Instant.parse("2026-07-16T07:00:00Z").toEpochMilli()
    private val day = 24 * 60 * 60 * 1_000L

    @Test fun `first sleep day keeps low confidence without personal history`() {
        val result = HealthInsights.calculate(
            records = listOf(sleepRecord("sonia-night-0", 0)),
            checkIns = emptyList(),
            setLogs = emptyList(),
            now = now,
            zone = ZoneId.of("UTC"),
        )

        assertEquals(0, result.baselines.sleepSamples)
        assertNull(result.baselines.sleepReferenceMinutes)
        assertNotNull(result.readiness.score)
        assertTrue(result.readiness.confidence < 20)
        assertEquals("Fiabilité faible", readinessReliabilityLabel(result.readiness.confidence))
    }

    @Test fun `personal sleep rhr and rmssd references activate progressively`() {
        val records = buildList {
            (0..4).forEach { add(sleepRecord("sonia-night-$it", it)) }
            add(valueRecord("rhr-now", "RESTING_HEART_RATE", now - 2 * 60 * 60 * 1_000L, 61.0))
            (2..4).forEach { dayIndex ->
                add(valueRecord("rhr-$dayIndex", "RESTING_HEART_RATE", now - dayIndex * day, 60.0 + dayIndex))
            }
            add(valueRecord("hrv-now", "HEART_RATE_VARIABILITY_RMSSD", now - 2 * 60 * 60 * 1_000L, 38.0))
            (2..5).forEach { dayIndex ->
                add(valueRecord("hrv-$dayIndex", "HEART_RATE_VARIABILITY_RMSSD", now - dayIndex * day, 35.0 + dayIndex))
            }
        }

        val result = HealthInsights.calculate(records, emptyList(), emptyList(), now, ZoneId.of("UTC"))

        assertEquals(4, result.baselines.sleepSamples)
        assertEquals(480, result.baselines.sleepReferenceMinutes)
        assertEquals(3, result.baselines.restingHeartRateSamples)
        assertNotNull(result.baselines.restingHeartRateReference)
        assertEquals(4, result.baselines.hrvSamples)
        assertNotNull(result.baselines.hrvReference)
        assertTrue(result.readiness.factors.any { it.key == "cardiovascular" && it.confidence < 90 })
    }

    @Test fun `Gerard records cannot build Sonia readiness or baselines`() {
        val gerardNights = (0..10).map {
            sleepRecord("gerard-night-$it", it, profileId = "gerard")
        }

        val result = HealthInsights.calculate(
            records = gerardNights,
            checkIns = emptyList(),
            setLogs = emptyList(),
            now = now,
            zone = ZoneId.of("UTC"),
            profileId = "sonia",
        )

        assertNull(result.readiness.score)
        assertEquals(0, result.baselines.sleepSamples)
        assertNull(result.baselines.sleepReferenceMinutes)
    }

    private fun sleepRecord(id: String, daysAgo: Int, profileId: String = "sonia"): HealthRecordEntity {
        val end = now - daysAgo * day - 30 * 60 * 1_000L
        return record(id, "SLEEP_SESSION", end - 8 * 60 * 60 * 1_000L, end, null, profileId)
    }

    private fun valueRecord(id: String, type: String, at: Long, value: Double) =
        record(id, type, at, at, value)

    private fun record(id: String, type: String, start: Long, end: Long, value: Double?, profileId: String = "sonia") = HealthRecordEntity(
        profileId = profileId,
        healthConnectId = id,
        recordType = type,
        startTime = start,
        endTime = end,
        value = value,
        secondaryValue = null,
        payloadJson = "{}",
        sourcePackage = "com.withings.wiscale2",
        sourceLabel = "Withings",
        sourceCategory = "WITHINGS",
        deviceManufacturer = "Withings",
        deviceModel = null,
        deviceType = null,
        recordingMethod = 1,
        lastModifiedAt = end,
        importedAt = now,
        dedupeKey = id,
        isPreferred = true,
    )
}
