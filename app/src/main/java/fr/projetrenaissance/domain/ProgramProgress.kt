package fr.projetrenaissance.domain

data class ProgramTemplateRef(
    val id: String,
    val weekFrom: Int,
    val weekTo: Int,
)

data class ProgramSetRecord(
    val templateId: String,
    val completedAt: Long,
)

data class CompletedProgramSlot(
    val templateId: String,
    val week: Int,
)

object ProgramProgress {
    private const val SESSION_GAP_MILLIS = 4 * 60 * 60 * 1_000L

    /**
     * Set logs belonging to the same template and separated by less than four
     * hours are one recorded workout. Recorded workouts are assigned in order
     * to the weeks covered by their template, never duplicated across weeks.
     */
    fun completedSlots(
        templates: List<ProgramTemplateRef>,
        records: List<ProgramSetRecord>,
    ): Set<CompletedProgramSlot> = buildSet {
        templates.forEach { template ->
            val times = records.asSequence()
                .filter { it.templateId == template.id }
                .map { it.completedAt }
                .sorted()
                .toList()
            val workoutCount = times.fold(0 to null as Long?) { (count, previous), time ->
                val startsNewWorkout = previous == null || time - previous > SESSION_GAP_MILLIS
                (count + if (startsNewWorkout) 1 else 0) to time
            }.first
            (template.weekFrom..template.weekTo).take(workoutCount).forEach { week ->
                add(CompletedProgramSlot(template.id, week))
            }
        }
    }
}
