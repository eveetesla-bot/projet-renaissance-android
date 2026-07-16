package fr.projetrenaissance.domain

enum class SessionLength(val maxExercises: Int, val setReduction: Int) {
    FULL(Int.MAX_VALUE, 0),
    MIN_30(5, 0),
    MIN_20(4, 1),
    MINIMAL(3, 1),
}

data class SafetyExercise(
    val shoulderLoad: String,
    val requiresClearance: Boolean,
)

object SafetyRules {
    fun allowedForSonia(exercise: SafetyExercise, medicalClearance: Boolean): Boolean {
        if (exercise.shoulderLoad == "OVERHEAD") return false
        return medicalClearance || !exercise.requiresClearance
    }
}

object GerardNutritionRules {
    private val forbiddenTerms = listOf(
        "whey",
        "caséine",
        "caseine",
        "lait de vache",
        "fromage blanc",
        "yaourt au lait de vache",
    )

    fun isSafeSuggestion(text: String): Boolean {
        val normalized = text.lowercase()
        return forbiddenTerms.none(normalized::contains)
    }
}

object TimerMath {
    fun remainingSeconds(endsAtMillis: Long, nowMillis: Long): Int =
        ((endsAtMillis - nowMillis).coerceAtLeast(0) / 1_000L).toInt()

    fun adjust(endsAtMillis: Long, deltaSeconds: Int, nowMillis: Long): Long =
        (endsAtMillis + deltaSeconds * 1_000L).coerceAtLeast(nowMillis)
}

