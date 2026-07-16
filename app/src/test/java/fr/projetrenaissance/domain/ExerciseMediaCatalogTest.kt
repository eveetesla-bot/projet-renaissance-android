package fr.projetrenaissance.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExerciseMediaCatalogTest {
    @Test fun `catalog contains the twelve required offline demonstrations`() {
        val expected = setOf("bike", "leg_press", "chest_press", "seated_row", "leg_curl", "lateral_raise", "calf_press", "hip_thrust", "leg_extension", "abductors", "dead_bug", "reverse_crunch")
        assertEquals(expected, ExerciseMediaCatalog.all.map { it.exerciseId }.toSet())
        assertTrue(ExerciseMediaCatalog.all.all { it.guidedIllustration.startPosition.isNotBlank() })
        assertTrue(ExerciseMediaCatalog.all.all { it.machine.verificationStatus == MediaVerificationStatus.TO_VALIDATE })
        assertTrue(ExerciseMediaCatalog.all.all { it.machine.localResource == null && it.verifiedVideoUrl == null })
        assertTrue(ExerciseMediaCatalog.all.all { it.accessibilityDescription.isNotBlank() })
    }

    @Test fun `unknown exercise never receives an invented media url`() {
        assertNull(ExerciseMediaCatalog.forExercise("unknown"))
    }
}
