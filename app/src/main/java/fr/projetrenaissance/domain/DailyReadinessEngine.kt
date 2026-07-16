package fr.projetrenaissance.domain

import kotlin.math.ceil
import kotlin.math.roundToInt

data class ReadinessWeights(
    val sleep: Double = .35,
    val cardiovascular: Double = .20,
    val recentLoad: Double = .20,
    val comfort: Double = .15,
    val subjective: Double = .10,
) {
    init { require(kotlin.math.abs(sleep + cardiovascular + recentLoad + comfort + subjective - 1.0) < .001) }
}

data class SleepReadinessInput(val durationMinutes: Int, val personalAverageMinutes: Int?, val regularityMinutes: Int? = null, val confidence: Int = 85)
data class CardioReadinessInput(
    val restingHeartRate: Double? = null,
    val personalAverage: Double? = null,
    val ageHours: Double,
    val confidence: Int = 90,
    val hrvRmssd: Double? = null,
    val personalAverageHrv: Double? = null,
)
data class LoadReadinessInput(val load72Hours: Double, val personalAverage72Hours: Double?, val confidence: Int = 80)
data class ManualReadinessInput(val energy: Int? = null, val fatigue: Int? = null, val mood: Int? = null)
data class ReadinessInputs(
    val sleep: SleepReadinessInput? = null,
    val cardiovascular: CardioReadinessInput? = null,
    val recentLoad: LoadReadinessInput? = null,
    val pain: Int? = null,
    val manual: ManualReadinessInput? = null,
)

enum class ReadinessClassification { GOOD, CORRECT, PARTIAL, WEAK, INSUFFICIENT }
data class ReadinessFactor(val key: String, val score: Int, val configuredWeight: Double, val confidence: Int, val explanation: String)
data class DailyReadinessResult(
    val score: Int?,
    val classification: ReadinessClassification,
    val recommendation: String,
    val confidence: Int,
    val factors: List<ReadinessFactor>,
    val missingInputs: List<String>,
)

object DailyReadinessEngine {
    fun calculate(inputs: ReadinessInputs, weights: ReadinessWeights = ReadinessWeights()): DailyReadinessResult {
        val factors = buildList {
            inputs.sleep?.let { add(ReadinessFactor("sleep", sleepScore(it), weights.sleep, it.confidence, "Sommeil réel agrégé")) }
            inputs.cardiovascular?.takeIf { it.ageHours <= 36 }?.let {
                val explanation = when {
                    it.restingHeartRate != null && it.hrvRmssd != null -> "Récupération cardio (FC repos et VFC RMSSD)"
                    it.hrvRmssd != null -> "VFC RMSSD comparée à la moyenne personnelle"
                    else -> "FC au repos comparée à la moyenne personnelle"
                }
                add(ReadinessFactor("cardiovascular", cardioScore(it), weights.cardiovascular, it.confidence, explanation))
            }
            inputs.recentLoad?.let { add(ReadinessFactor("recentLoad", loadScore(it), weights.recentLoad, it.confidence, "Charge réelle des dernières 72 h")) }
            inputs.pain?.let { add(ReadinessFactor("comfort", painScore(it), weights.comfort, 90, "Douleur déclarée : ${it.coerceIn(0, 10)}/10")) }
            inputs.manual?.let { manualScore(it)?.let { score -> add(ReadinessFactor("subjective", score, weights.subjective, 90, "Ressenti saisi aujourd’hui")) } }
        }
        val availableWeight = factors.sumOf { it.configuredWeight }
        val score = if (availableWeight == 0.0) null else (factors.sumOf { it.score * it.configuredWeight } / availableWeight).roundToInt().coerceIn(0, 100)
        val classification = classify(score)
        val confidence = factors.sumOf { it.configuredWeight * it.confidence }.roundToInt().coerceIn(0, 100)
        val missing = listOf("sleep", "cardiovascular", "recentLoad", "comfort", "subjective") - factors.map { it.key }.toSet()
        val recommendation = when {
            score == null -> "Renseignez votre état du jour ou synchronisez vos données pour obtenir un conseil."
            (inputs.pain ?: 0) >= 7 -> "Douleur élevée : adaptez ou réduisez la séance et demandez conseil si nécessaire."
            score >= 80 && confidence < 60 -> "Données limitées : confirmez votre état à l’échauffement et adaptez si nécessaire."
            score >= 80 && confidence < 85 -> "Séance normale envisagée, à confirmer à l’échauffement."
            score >= 80 -> "Séance normale."
            score >= 60 -> "Séance normale, avec auto-évaluation à l’échauffement."
            score >= 40 -> "Séance courte ou charge réduite de 5 à 10 %."
            else -> "Mobilité, marche ou repos, puis réévaluation."
        }
        return DailyReadinessResult(score, classification, recommendation, confidence, factors, missing)
    }

    fun classify(score: Int?): ReadinessClassification = when (score) {
        null -> ReadinessClassification.INSUFFICIENT
        in 80..100 -> ReadinessClassification.GOOD
        in 60..79 -> ReadinessClassification.CORRECT
        in 40..59 -> ReadinessClassification.PARTIAL
        else -> ReadinessClassification.WEAK
    }

    private fun sleepScore(input: SleepReadinessInput): Int {
        val reference = input.personalAverageMinutes?.takeIf { it > 0 } ?: 480
        val ratio = input.durationMinutes.toDouble() / reference
        var score = when {
            ratio >= 1 -> 100.0
            ratio >= .85 -> 75 + (ratio - .85) / .15 * 20
            ratio >= .70 -> 50 + (ratio - .70) / .15 * 25
            ratio >= .50 -> 20 + (ratio - .50) / .20 * 30
            else -> ratio / .50 * 20
        }
        input.regularityMinutes?.let { deviation -> score += when {
            deviation <= 30 -> 10
            deviation <= 60 -> 0
            deviation <= 120 -> -10
            else -> -20
        } }
        return score.roundToInt().coerceIn(0, 100)
    }

    private fun cardioScore(input: CardioReadinessInput): Int {
        val rhrScore = input.restingHeartRate?.let { current -> input.personalAverage?.takeIf { it > 0 }?.let { average ->
            val increase = (current / average - 1).coerceAtLeast(0.0)
            when {
                increase <= .03 -> 100
                increase <= .08 -> (100 - (increase - .03) / .05 * 30).roundToInt()
                increase <= .15 -> (70 - (increase - .08) / .07 * 40).roundToInt()
                else -> 10
            }
        } }
        val hrvScore = input.hrvRmssd?.let { current -> input.personalAverageHrv?.takeIf { it > 0 }?.let { average ->
            val ratio = current / average
            when { ratio >= .95 -> 100; ratio >= .80 -> (70 + (ratio - .80) / .15 * 30).roundToInt(); ratio >= .60 -> (30 + (ratio - .60) / .20 * 40).roundToInt(); else -> 20 }
        } }
        return when {
            rhrScore != null && hrvScore != null -> (rhrScore + hrvScore) / 2
            rhrScore != null -> rhrScore
            hrvScore != null -> hrvScore
            else -> 70
        }
    }

    private fun loadScore(input: LoadReadinessInput): Int {
        val average = input.personalAverage72Hours ?: return 70
        if (average <= 0) return 70
        val ratio = input.load72Hours / average
        return when {
            ratio <= 1.2 -> 95
            ratio <= 1.5 -> (85 - (ratio - 1.2) / .3 * 25).roundToInt()
            else -> (60 - (ratio - 1.5) * 30).roundToInt().coerceAtLeast(30)
        }
    }

    private fun painScore(pain: Int): Int = when (pain.coerceIn(0, 10)) { 0 -> 100; in 1..3 -> 80; in 4..6 -> 50; in 7..8 -> 20; else -> 0 }

    private fun manualScore(input: ManualReadinessInput): Int? {
        val scores = listOfNotNull(input.energy?.let(::scaleFive), input.fatigue?.let { 100 - scaleFive(it) }, input.mood?.let(::scaleFive))
        return scores.takeIf { it.isNotEmpty() }?.average()?.roundToInt()
    }

    private fun scaleFive(value: Int): Int = (value.coerceIn(1, 5) - 1) * 25
}

/**
 * Converts the data coverage confidence into an intentionally conservative
 * presentation interval. The score itself is unchanged; lower confidence only
 * widens the interval shown to the user.
 */
fun readinessEstimateRange(score: Int?, confidence: Int): IntRange? {
    score ?: return null
    val boundedConfidence = confidence.coerceIn(0, 100)
    val halfWidth = ceil((100 - boundedConfidence) * 0.15).toInt()
    return (score - halfWidth).coerceAtLeast(0)..(score + halfWidth).coerceAtMost(100)
}

fun readinessReliabilityLabel(confidence: Int): String = when (confidence.coerceIn(0, 100)) {
    in 85..100 -> "Fiabilité élevée"
    in 60..84 -> "Fiabilité moyenne"
    else -> "Fiabilité faible"
}
