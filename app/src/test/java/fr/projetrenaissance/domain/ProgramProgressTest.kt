package fr.projetrenaissance.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgramProgressTest {
    private val phaseA = ProgramTemplateRef("gerard_0_A", 1, 3)

    @Test fun `sets from one workout complete only one week`() {
        val slots = ProgramProgress.completedSlots(
            listOf(phaseA),
            listOf(
                ProgramSetRecord(phaseA.id, 1_000),
                ProgramSetRecord(phaseA.id, 2_000),
                ProgramSetRecord(phaseA.id, 3_000),
            ),
        )

        assertEquals(setOf(CompletedProgramSlot(phaseA.id, 1)), slots)
    }

    @Test fun `separate recorded workouts fill weeks once`() {
        val fiveHours = 5 * 60 * 60 * 1_000L
        val slots = ProgramProgress.completedSlots(
            listOf(phaseA),
            listOf(
                ProgramSetRecord(phaseA.id, 1_000),
                ProgramSetRecord(phaseA.id, 1_000 + fiveHours),
            ),
        )

        assertEquals(
            setOf(
                CompletedProgramSlot(phaseA.id, 1),
                CompletedProgramSlot(phaseA.id, 2),
            ),
            slots,
        )
        assertTrue(CompletedProgramSlot(phaseA.id, 3) !in slots)
    }

    @Test fun `records never overflow template week range`() {
        val records = (0..5).map { ProgramSetRecord(phaseA.id, it * 5L * 60 * 60 * 1_000) }
        assertEquals(3, ProgramProgress.completedSlots(listOf(phaseA), records).size)
    }
}
