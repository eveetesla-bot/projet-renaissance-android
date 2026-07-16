package fr.projetrenaissance.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RulesTest {
    @Test
    fun `Gerard suggestions reject every explicit cow milk protein source`() {
        listOf(
            "Ajouter de la whey après la séance",
            "Prendre de la caséine le soir",
            "Un yaourt au lait de vache",
            "Manger du fromage blanc",
        ).forEach { suggestion ->
            assertFalse(suggestion, GerardNutritionRules.isSafeSuggestion(suggestion))
        }
        assertTrue(GerardNutritionRules.isSafeSuggestion("Mélange pois-riz garanti compatible"))
    }

    @Test
    fun `Sonia never sees overhead exercise`() {
        val overhead = SafetyExercise(shoulderLoad = "OVERHEAD", requiresClearance = true)
        assertFalse(SafetyRules.allowedForSonia(overhead, medicalClearance = false))
        assertFalse(SafetyRules.allowedForSonia(overhead, medicalClearance = true))
    }

    @Test
    fun `Sonia clearance unlocks only conditional non overhead exercise`() {
        val row = SafetyExercise(shoulderLoad = "MODERATE", requiresClearance = true)
        assertFalse(SafetyRules.allowedForSonia(row, medicalClearance = false))
        assertTrue(SafetyRules.allowedForSonia(row, medicalClearance = true))
    }

    @Test
    fun `timer uses absolute end time and never becomes negative`() {
        assertEquals(45, TimerMath.remainingSeconds(55_000, 10_000))
        assertEquals(0, TimerMath.remainingSeconds(5_000, 10_000))
        assertEquals(25_000, TimerMath.adjust(10_000, 15, 5_000))
        assertEquals(5_000, TimerMath.adjust(10_000, -15, 5_000))
    }

    @Test
    fun `short variants progressively reduce the session`() {
        assertEquals(Int.MAX_VALUE, SessionLength.FULL.maxExercises)
        assertEquals(5, SessionLength.MIN_30.maxExercises)
        assertEquals(4, SessionLength.MIN_20.maxExercises)
        assertEquals(3, SessionLength.MINIMAL.maxExercises)
        assertEquals(1, SessionLength.MINIMAL.setReduction)
    }
}

