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
}
