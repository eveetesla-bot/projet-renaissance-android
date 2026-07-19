package fr.projetrenaissance.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HealthRulesTest {
    private val priorities = mapOf(
        HealthRecordType.STEPS to listOf(HealthSourceCategory.WITHINGS, HealthSourceCategory.GOOGLE_FIT),
        HealthRecordType.EXERCISE_SESSION to listOf(HealthSourceCategory.BASIC_FIT, HealthSourceCategory.RENAISSANCE),
    )

    @Test
    fun `Withings et Google Fit ne doublent pas les memes pas`() {
        val result = HealthDeduplication.selectPreferred(
            listOf(
                sample("w", HealthRecordType.STEPS, HealthSourceCategory.WITHINGS, 1_000.0, end = 60_000),
                sample("g", HealthRecordType.STEPS, HealthSourceCategory.GOOGLE_FIT, 1_020.0, end = 60_000),
            ), priorities,
        )
        assertEquals(1, result.size)
        assertEquals(HealthSourceCategory.WITHINGS, result.single().sourceCategory)
    }

    @Test
    fun `les donnees uniquement Withings sont conservees`() {
        val only = sample("w", HealthRecordType.WEIGHT, HealthSourceCategory.WITHINGS, 68.2)
        assertEquals(listOf(only), HealthDeduplication.selectPreferred(listOf(only), priorities))
    }

    @Test
    fun `Basic Fit absent ne cree aucune attribution Basic Fit`() {
        val category = HealthSourceClassifier.classify("com.example.unknown", "Mon sport", "fr.projetrenaissance")
        assertEquals(HealthSourceCategory.OTHER, category)
    }

    @Test
    fun `une seance prouvee Basic Fit est reconnue`() {
        val category = HealthSourceClassifier.classify("com.vendor.app", "Basic-Fit", "fr.projetrenaissance")
        assertEquals(HealthSourceCategory.BASIC_FIT, category)
    }

    @Test
    fun `les permissions partielles limitent les types lus`() {
        val requested = setOf(HealthRecordType.STEPS, HealthRecordType.SLEEP_SESSION, HealthRecordType.WEIGHT)
        val granted = setOf(HealthRecordType.STEPS, HealthRecordType.WEIGHT)
        assertEquals(granted, HealthSyncReducer.readableTypes(granted, requested))
    }

    @Test
    fun `une permission revoquee disparait des types lisibles`() {
        val requested = setOf(HealthRecordType.STEPS, HealthRecordType.SLEEP_SESSION)
        val afterRevocation = HealthSyncReducer.readableTypes(setOf(HealthRecordType.STEPS), requested)
        assertFalse(HealthRecordType.SLEEP_SESSION in afterRevocation)
    }

    @Test
    fun `deux seances proches mais peu recouvertes restent distinctes`() {
        val first = sample("a", HealthRecordType.EXERCISE_SESSION, HealthSourceCategory.WITHINGS, start = 0, end = 30 * 60_000)
        val second = sample("b", HealthRecordType.EXERCISE_SESSION, HealthSourceCategory.BASIC_FIT, start = 9 * 60_000, end = 39 * 60_000)
        assertTrue(HealthDeduplication.equivalent(first, second))
        val separate = second.copy(startTimeMillis = 10 * 60_000, endTimeMillis = 70 * 60_000)
        assertFalse(HealthDeduplication.equivalent(first, separate))
    }

    @Test
    fun `une suppression Health Connect retire le record local`() {
        val initial = listOf(sample("keep", HealthRecordType.WEIGHT, HealthSourceCategory.WITHINGS), sample("delete", HealthRecordType.WEIGHT, HealthSourceCategory.WITHINGS))
        val result = HealthSyncReducer.apply(initial, listOf(HealthChange.Delete("delete")))
        assertEquals(listOf("keep"), result.map { it.healthConnectId })
    }

    @Test
    fun `le changement de source prioritaire change le gagnant`() {
        val records = listOf(
            sample("w", HealthRecordType.STEPS, HealthSourceCategory.WITHINGS, end = 60_000),
            sample("g", HealthRecordType.STEPS, HealthSourceCategory.GOOGLE_FIT, end = 60_000),
        )
        val googleFirst = mapOf(HealthRecordType.STEPS to listOf(HealthSourceCategory.GOOGLE_FIT, HealthSourceCategory.WITHINGS))
        assertEquals(HealthSourceCategory.GOOGLE_FIT, HealthDeduplication.selectPreferred(records, googleFirst).single().sourceCategory)
    }

    @Test
    fun `une mesure d appareil d une autre source ne bat pas la priorite Withings`() {
        // Poids : la balance Withings (autrefois classée OTHER) contre une
        // « mesure montre » d'une autre application. La source prioritaire
        // doit gagner, quelle que soit la méthode de l'autre source.
        val withingsScale = sample("w", HealthRecordType.WEIGHT, HealthSourceCategory.WITHINGS, 68.2)
        val otherWatch = sample("o", HealthRecordType.WEIGHT, HealthSourceCategory.GOOGLE_FIT, 68.4)
            .copy(recordingMethod = HealthRecordingMethod.MEASURED, deviceKind = HealthDeviceKind.WATCH)
        val result = HealthDeduplication.selectPreferred(
            listOf(withingsScale, otherWatch),
            mapOf(HealthRecordType.WEIGHT to listOf(HealthSourceCategory.WITHINGS, HealthSourceCategory.GOOGLE_FIT)),
        )
        assertEquals(HealthSourceCategory.WITHINGS, result.single().sourceCategory)
    }

    @Test(timeout = 5_000)
    fun `la deduplication reste rapide sur dix mille intervalles`() {
        val records = (0 until 5_000).flatMap { index ->
            val start = index * 60_000L
            listOf(
                sample("w$index", HealthRecordType.STEPS, HealthSourceCategory.WITHINGS, 100.0, start, start + 60_000L),
                sample("g$index", HealthRecordType.STEPS, HealthSourceCategory.GOOGLE_FIT, 101.0, start, start + 60_000L),
            )
        }
        val result = HealthDeduplication.selectPreferred(records, priorities)
        assertEquals(5_000, result.size)
        assertTrue(result.all { it.sourceCategory == HealthSourceCategory.WITHINGS })
    }

    private fun sample(
        id: String,
        type: HealthRecordType,
        source: HealthSourceCategory,
        value: Double? = null,
        start: Long = 0,
        end: Long = start,
    ) = HealthSample(
        healthConnectId = id,
        type = type,
        startTimeMillis = start,
        endTimeMillis = end,
        value = value,
        sourcePackage = source.name.lowercase(),
        sourceLabel = source.name,
        sourceCategory = source,
    )
}
