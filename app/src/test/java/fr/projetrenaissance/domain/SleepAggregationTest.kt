package fr.projetrenaissance.domain

import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SleepAggregationTest {
    private val zone = ZoneId.of("Europe/Paris")
    private fun at(day: Int, hour: Int, minute: Int = 0): Long =
        ZonedDateTime.of(2026, 7, day, hour, minute, 0, 0, zone).toInstant().toEpochMilli()

    @Test fun `complete night crossing midnight is aggregated`() {
        val result = SleepAggregation.latestNight(
            listOf(SleepSegment("n", at(14, 23), at(15, 7), "withings", "Withings", 2)),
            at(15, 9), zone,
        )!!
        assertEquals(480, result.sleepDurationMinutes)
        assertEquals(SleepQuality.COMPLETE, result.quality)
    }

    @Test fun `fragmented night excludes awake gap`() {
        val result = SleepAggregation.latestNight(
            listOf(
                SleepSegment("a", at(14, 22, 40), at(15, 1, 30), "withings", "Withings"),
                SleepSegment("b", at(15, 2, 5), at(15, 6, 40), "withings", "Withings"),
            ), at(15, 9), zone,
        )!!
        assertEquals(445, result.sleepDurationMinutes)
        assertEquals(35, result.awakeGapMinutes)
    }

    @Test fun `day nap is separate from latest night`() {
        val result = SleepAggregation.latestNight(
            listOf(
                SleepSegment("night", at(14, 23), at(15, 7), "withings", "Withings"),
                SleepSegment("nap", at(15, 14), at(15, 14, 45), "withings", "Withings"),
            ), at(15, 16), zone,
        )!!
        assertEquals(480, result.sleepDurationMinutes)
        assertEquals(45, result.naps.single().durationMinutes)
    }

    @Test fun `preferred source wins duplicate night`() {
        val result = SleepAggregation.latestNight(
            listOf(
                SleepSegment("fit", at(14, 23), at(15, 7), "fit", "Google Fit", 1),
                SleepSegment("withings", at(14, 23, 5), at(15, 7, 5), "withings", "Withings", 2),
            ), at(15, 9), zone,
        )!!
        assertEquals("Withings", result.sourceLabel)
        assertEquals(listOf("withings"), result.segmentIds)
    }

    @Test fun `overlapping fragments are not double counted`() {
        val result = SleepAggregation.latestNight(
            listOf(
                SleepSegment("a", at(14, 23), at(15, 3), "withings", "Withings"),
                SleepSegment("b", at(15, 2), at(15, 7), "withings", "Withings"),
            ), at(15, 9), zone,
        )!!
        assertEquals(480, result.sleepDurationMinutes)
    }

    @Test fun `stale night is labelled and has zero confidence`() {
        val result = SleepAggregation.latestNight(
            listOf(SleepSegment("old", at(12, 23), at(13, 7), "fit", "Google Fit")),
            at(15, 9), zone,
        )!!
        assertEquals(SleepQuality.STALE, result.quality)
        assertEquals(0, result.confidence)
    }

    @Test fun `absence returns null`() {
        assertNull(SleepAggregation.latestNight(emptyList(), at(15, 9), zone))
    }
}
