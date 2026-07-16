package fr.projetrenaissance.domain

import kotlin.math.roundToInt

data class WorkoutCoachContext(
    val readinessScore: Int?,
    val readinessConfidence: Int,
    val pain: Int?,
)

data class SessionRecommendation(
    val length: SessionLength,
    val explanation: String,
)

data class LoadHistoryPoint(
    val loadKg: Double,
    val rpe: Int,
    val completedAt: Long,
)

data class LoadRecommendation(
    val valueKg: Double?,
    val explanation: String,
    val requiresCalibration: Boolean,
)

data class RepetitionRecommendation(
    val value: Int,
    val unitLabel: String,
)

object WorkoutCoach {
    private val noExternalLoadExercises = setOf("bike", "dead_bug", "reverse_crunch")

    fun recommendSession(context: WorkoutCoachContext): SessionRecommendation = when {
        (context.pain ?: 0) >= 7 -> SessionRecommendation(
            SessionLength.MINIMAL,
            "Douleur élevée déclarée : format minimal, sans forcer et avec arrêt au moindre signal.",
        )
        context.readinessScore == null -> SessionRecommendation(
            SessionLength.MIN_20,
            "Références encore incomplètes : démarrage progressif de 20 minutes.",
        )
        context.readinessScore < 40 -> SessionRecommendation(
            SessionLength.MINIMAL,
            "Récupération faible : conserver seulement l’essentiel.",
        )
        context.readinessScore < 60 || (context.pain ?: 0) >= 4 -> SessionRecommendation(
            SessionLength.MIN_20,
            "Récupération partielle ou gêne présente : volume réduit.",
        )
        context.readinessScore < 80 || context.readinessConfidence < 60 -> SessionRecommendation(
            SessionLength.MIN_30,
            "Préparation correcte mais prudence utile : format de 30 minutes.",
        )
        else -> SessionRecommendation(
            SessionLength.FULL,
            "Préparation favorable : séance complète, à confirmer pendant l’échauffement.",
        )
    }

    fun recommendLoad(
        exerciseId: String,
        history: List<LoadHistoryPoint>,
        context: WorkoutCoachContext,
    ): LoadRecommendation {
        if (exerciseId in noExternalLoadExercises && history.isEmpty()) {
            return LoadRecommendation(
                valueKg = 0.0,
                explanation = "Aucune charge externe prévue. Réglez l’effort avec le tempo ou la résistance selon le RPE.",
                requiresCalibration = false,
            )
        }
        val previous = history.maxByOrNull { it.completedAt } ?: return LoadRecommendation(
            valueKg = null,
            explanation = "Première utilisation : choisissez une charge volontairement légère, puis ajustez pour atteindre le RPE cible avec une technique stable.",
            requiresCalibration = true,
        )
        val healthFactor = when {
            (context.pain ?: 0) >= 7 -> .80
            context.readinessScore == null -> .90
            context.readinessScore < 40 -> .80
            context.readinessScore < 60 || (context.pain ?: 0) >= 4 -> .90
            context.readinessScore < 80 || context.readinessConfidence < 60 -> .95
            else -> 1.0
        }
        val rpeFactor = when {
            previous.rpe >= 9 -> .90
            previous.rpe >= 8 -> .95
            else -> 1.0
        }
        val factor = minOf(healthFactor, rpeFactor)
        val suggested = roundToHalfKg(previous.loadKg * factor)
        val explanation = when {
            suggested < previous.loadKg -> "Charge précédente réduite selon la récupération ou le dernier RPE. Modifiable après l’échauffement."
            else -> "Charge précédente maintenue. L’application ne l’augmente qu’après validation réelle du RPE et de la technique."
        }
        return LoadRecommendation(suggested, explanation, false)
    }

    fun recommendRepetitions(prescription: String, sessionLength: SessionLength): RepetitionRecommendation {
        val numbers = Regex("""\d+""").findAll(prescription).map { it.value.toInt() }.toList()
        val minimum = numbers.firstOrNull() ?: 10
        val maximum = numbers.getOrNull(1) ?: minimum
        val timed = prescription.contains("min", ignoreCase = true)
        val value = if (timed) {
            val factor = when (sessionLength) {
                SessionLength.FULL -> 1.0
                SessionLength.MIN_30 -> .85
                SessionLength.MIN_20 -> .70
                SessionLength.MINIMAL -> .55
            }
            (minimum * factor).roundToInt().coerceAtLeast(3)
        } else if (sessionLength == SessionLength.FULL) {
            ((minimum + maximum) / 2.0).roundToInt()
        } else {
            minimum
        }
        return RepetitionRecommendation(value, if (timed) "Min." else "Rép.")
    }

    private fun roundToHalfKg(value: Double): Double =
        (value.coerceAtLeast(0.0) * 2.0).roundToInt() / 2.0
}
