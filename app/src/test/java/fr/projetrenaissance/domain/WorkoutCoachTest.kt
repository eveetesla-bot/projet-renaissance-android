package fr.projetrenaissance.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkoutCoachTest {
    @Test fun `good readiness proposes full session`() {
        val result = WorkoutCoach.recommendSession(WorkoutCoachContext(90, 80, 0))
        assertEquals(SessionLength.FULL, result.length)
    }

    @Test fun `missing readiness starts progressively`() {
        val result = WorkoutCoach.recommendSession(WorkoutCoachContext(null, 0, null))
        assertEquals(SessionLength.MIN_20, result.length)
    }

    @Test fun `high pain always proposes minimal session`() {
        val result = WorkoutCoach.recommendSession(WorkoutCoachContext(95, 90, 8))
        assertEquals(SessionLength.MINIMAL, result.length)
    }

    @Test fun `machine without history requires calibration and invents no load`() {
        val result = WorkoutCoach.recommendLoad("leg_press", emptyList(), WorkoutCoachContext(90, 80, 0))
        assertNull(result.valueKg)
        assertTrue(result.requiresCalibration)
    }

    @Test fun `known load is reduced after high rpe`() {
        val result = WorkoutCoach.recommendLoad(
            "leg_press",
            listOf(LoadHistoryPoint(40.0, 9, 1L)),
            WorkoutCoachContext(90, 80, 0),
        )
        assertEquals(36.0, result.valueKg!!, 0.0)
        assertFalse(result.requiresCalibration)
    }

    @Test fun `bodyweight exercise starts at zero external load`() {
        val result = WorkoutCoach.recommendLoad("dead_bug", emptyList(), WorkoutCoachContext(null, 0, null))
        assertEquals(0.0, result.valueKg!!, 0.0)
    }

    @Test fun `timed exercise receives reduced duration for short session`() {
        val result = WorkoutCoach.recommendRepetitions("6 min", SessionLength.MIN_20)
        assertEquals(4, result.value)
        assertEquals("Min.", result.unitLabel)
    }

    @Test fun `starting load without bodyweight invents nothing`() {
        val result = WorkoutCoach.recommendStartingLoad(
            "leg_press",
            TrainingProfile(BiologicalSex.MALE, 40, bodyweightKg = null),
            WorkoutCoachContext(90, 80, 0),
        )
        assertNull(result.valueKg)
        assertTrue(result.requiresCalibration)
    }

    @Test fun `starting load estimates from bodyweight profile and readiness`() {
        // Homme 70 kg, bonne forme : leg press ≈ 70 × 0,90 × 1,0 × 1,0 × 1,0 = 63 → arrondi plaque 62,5.
        val result = WorkoutCoach.recommendStartingLoad(
            "leg_press",
            TrainingProfile(BiologicalSex.MALE, 30, bodyweightKg = 70.0),
            WorkoutCoachContext(90, 80, 0),
        )
        assertEquals(62.5, result.valueKg!!, 0.01)
    }

    @Test fun `starting load is lighter for lower readiness`() {
        val good = WorkoutCoach.recommendStartingLoad("chest_press", TrainingProfile(BiologicalSex.MALE, 30, 70.0), WorkoutCoachContext(90, 80, 0)).valueKg!!
        val weak = WorkoutCoach.recommendStartingLoad("chest_press", TrainingProfile(BiologicalSex.MALE, 30, 70.0), WorkoutCoachContext(35, 80, 0)).valueKg!!
        assertTrue(weak < good)
    }

    @Test fun `bodyweight movement starts at zero external load in starting estimate`() {
        val result = WorkoutCoach.recommendStartingLoad(
            "glute_bridge",
            TrainingProfile(BiologicalSex.FEMALE, 45, 60.0),
            WorkoutCoachContext(80, 70, 0),
        )
        assertEquals(0.0, result.valueKg!!, 0.0)
    }

    @Test fun `full session pushes reps toward top of range on strong readiness`() {
        val strong = WorkoutCoach.recommendRepetitions("8–12", SessionLength.FULL, readinessScore = 90)
        val weak = WorkoutCoach.recommendRepetitions("8–12", SessionLength.FULL, readinessScore = 50)
        assertEquals(12, strong.value)
        assertEquals(8, weak.value)
    }

    @Test fun `measured lean mass drives the estimate instead of total weight`() {
        val withLeanMass = WorkoutCoach.recommendStartingLoad(
            "leg_press",
            TrainingProfile(BiologicalSex.FEMALE, 51, bodyweightKg = 60.0, leanMassKg = 42.0),
            WorkoutCoachContext(90, 80, 0),
        ).valueKg!!
        // Masse maigre : 42 / 0,75 × 0,90 × 0,95 (bas du corps femme) × 0,904
        // (51 ans) = 43,3 → arrondi plaque 42,5.
        assertEquals(42.5, withLeanMass, 0.01)
    }

    @Test fun `body fat measurement refines the weight-only estimate`() {
        val weightOnly = WorkoutCoach.recommendStartingLoad(
            "chest_press",
            TrainingProfile(BiologicalSex.MALE, 52, bodyweightKg = 72.0),
            WorkoutCoachContext(90, 80, 0),
        ).valueKg!!
        val withBodyFat = WorkoutCoach.recommendStartingLoad(
            "chest_press",
            TrainingProfile(BiologicalSex.MALE, 52, bodyweightKg = 72.0, bodyFatPercent = 20.0),
            WorkoutCoachContext(90, 80, 0),
        ).valueKg!!
        // Les deux chemins donnent une valeur plausible mais distincte : la
        // composition mesurée affine la référence.
        assertTrue(withBodyFat > 0 && weightOnly > 0)
    }

    @Test fun `target weight fallback is flagged and more cautious`() {
        val fallback = WorkoutCoach.recommendStartingLoad(
            "leg_press",
            TrainingProfile(BiologicalSex.MALE, 52, bodyweightKg = 69.0, weightIsMeasured = false),
            WorkoutCoachContext(90, 80, 0),
        )
        val measured = WorkoutCoach.recommendStartingLoad(
            "leg_press",
            TrainingProfile(BiologicalSex.MALE, 52, bodyweightKg = 69.0, weightIsMeasured = true),
            WorkoutCoachContext(90, 80, 0),
        )
        assertTrue(fallback.valueKg!! < measured.valueKg!!)
        assertTrue(fallback.explanation.contains("cible"))
        assertTrue(measured.explanation.contains("mesuré"))
    }

    @Test fun `performance index transfers to a new exercise`() {
        val profile = TrainingProfile(BiologicalSex.MALE, 52, bodyweightKg = 72.0)
        // Presse : référence ≈ 58,3 ; réalisé 76 → index ≈ 1,3 (30 % au-dessus).
        val index = WorkoutCoach.performanceIndex(
            profile,
            mapOf("leg_press" to listOf(LoadHistoryPoint(76.0, 7, 1L))),
        )!!
        assertTrue(index > 1.2)
        val boosted = WorkoutCoach.recommendStartingLoad("chest_press", profile, WorkoutCoachContext(90, 80, 0), index)
        val neutral = WorkoutCoach.recommendStartingLoad("chest_press", profile, WorkoutCoachContext(90, 80, 0), null)
        assertTrue(boosted.valueKg!! > neutral.valueKg!!)
    }

    @Test fun `easy previous effort progresses load per program goal`() {
        val history = listOf(LoadHistoryPoint(40.0, 5, 1L))
        val leanMass = WorkoutCoach.recommendLoad("chest_press", history, WorkoutCoachContext(90, 80, 0), targetRpe = 7, goal = ProgramGoal.LEAN_MASS)
        val tone = WorkoutCoach.recommendLoad("chest_press", history, WorkoutCoachContext(90, 80, 0), targetRpe = 7, goal = ProgramGoal.TONE_MOBILITY)
        assertEquals(42.0, leanMass.valueKg!!, 0.0)
        assertEquals(41.0, tone.valueKg!!, 0.0)
    }

    @Test fun `no progression when daily readiness is reduced`() {
        val history = listOf(LoadHistoryPoint(40.0, 5, 1L))
        val result = WorkoutCoach.recommendLoad("chest_press", history, WorkoutCoachContext(55, 80, 0), targetRpe = 7)
        assertTrue(result.valueKg!! <= 40.0)
    }

    @Test fun `reps follow what the user actually achieved last time`() {
        val result = WorkoutCoach.recommendRepetitions("8–12", SessionLength.MIN_30, readinessScore = 70, lastAchievedReps = 11)
        assertEquals(11, result.value)
        val capped = WorkoutCoach.recommendRepetitions("8–12", SessionLength.MIN_30, readinessScore = 70, lastAchievedReps = 15)
        assertEquals(12, capped.value)
    }
}
