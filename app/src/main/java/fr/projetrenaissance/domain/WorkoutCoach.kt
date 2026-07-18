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
    val reps: Int = 0,
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

enum class BiologicalSex { MALE, FEMALE }

/** Objectif du programme complet : il oriente la progression proposée. */
enum class ProgramGoal {
    /** Gérard : construire de la masse sèche → priorité à la montée de charge. */
    LEAN_MASS,

    /** Sonia : tonicité et mobilité → priorité aux répétitions, charges modérées. */
    TONE_MOBILITY,
}

/**
 * Morphologie servant de base aux références statistiques. Les mesures réelles
 * (balance Withings via Health Connect) priment toujours :
 * - [leanMassKg] : masse maigre mesurée — meilleure base de calcul ;
 * - [bodyFatPercent] : à défaut, la masse maigre est déduite du poids mesuré ;
 * - [bodyweightKg] : poids mesuré, ou repli sur le poids cible du profil
 *   ([weightIsMeasured] = false, signalé à l'utilisateur).
 * Sans aucune donnée de poids, aucune charge n'est inventée.
 */
data class TrainingProfile(
    val sex: BiologicalSex,
    val ageYears: Int,
    val bodyweightKg: Double?,
    val leanMassKg: Double? = null,
    val bodyFatPercent: Double? = null,
    val weightIsMeasured: Boolean = true,
    val goal: ProgramGoal = ProgramGoal.LEAN_MASS,
)

object WorkoutCoach {
    private val noExternalLoadExercises = setOf(
        "bike",
        "dead_bug",
        "reverse_crunch",
        "glute_bridge",
        "step_up",
        "breathing_reset",
    )

    // Références statistiques : charge de travail (~10 répétitions) exprimée en
    // fraction du poids de corps pour un homme adulte de niveau débutant à
    // intermédiaire. Valeurs prudentes, non médicales ; elles servent de point
    // de départ que l'index personnel recale ensuite sur les vraies séries.
    private val bodyweightLoadCoefficient = mapOf(
        "leg_press" to 0.90,
        "calf_press" to 0.90,
        "hip_thrust" to 0.60,
        "lat_pulldown" to 0.55,
        "seated_row" to 0.45,
        "chest_row" to 0.45,
        "chest_press" to 0.45,
        "incline_press" to 0.40,
        "leg_extension" to 0.35,
        "leg_curl" to 0.30,
        "abductors" to 0.30,
        "adductors" to 0.30,
        "shoulder_press" to 0.30,
        "triceps_rope" to 0.20,
        "biceps_curl" to 0.18,
        "reverse_fly" to 0.12,
        "lateral_raise" to 0.10,
        "back_extension" to 0.10,
    )

    // Fraction de masse maigre typique servant à convertir les coefficients
    // « poids de corps » en coefficients « masse maigre » (référence homme).
    private const val REFERENCE_LEAN_FRACTION = 0.75

    private val lowerBodyExercises = setOf(
        "leg_press", "calf_press", "leg_extension", "leg_curl", "hip_thrust",
        "abductors", "adductors", "back_extension", "step_up", "glute_bridge",
    )

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

    /**
     * Référence statistique de charge pour cet exercice et cette morphologie
     * (sans la forme du jour ni l'historique). Null si aucune donnée de poids.
     */
    fun baselineLoad(exerciseId: String, profile: TrainingProfile): Double? {
        if (exerciseId in noExternalLoadExercises) return 0.0
        val coefficient = bodyweightLoadCoefficient[exerciseId] ?: 0.35
        val ageFactor = if (profile.ageYears <= 35) 1.0
            else (1.0 - (profile.ageYears - 35) * 0.006).coerceAtLeast(0.80)
        val leanMass = profile.leanMassKg
            ?: profile.bodyFatPercent?.let { fat ->
                profile.bodyweightKg?.times(1.0 - fat.coerceIn(5.0, 60.0) / 100.0)
            }
        if (leanMass != null) {
            // Base composition corporelle mesurée : l'écart homme/femme restant
            // porte surtout sur le haut du corps, à masse maigre égale.
            val sexFactor = when {
                profile.sex == BiologicalSex.MALE -> 1.0
                exerciseId in lowerBodyExercises -> 0.95
                else -> 0.78
            }
            return leanMass / REFERENCE_LEAN_FRACTION * coefficient * sexFactor * ageFactor
        }
        val bodyweight = profile.bodyweightKg ?: return null
        val sexFactor = when {
            profile.sex == BiologicalSex.MALE -> 1.0
            exerciseId in lowerBodyExercises -> 0.80
            else -> 0.62
        }
        val measurementCaution = if (profile.weightIsMeasured) 1.0 else 0.9
        return bodyweight * coefficient * sexFactor * ageFactor * measurementCaution
    }

    /**
     * Index de performance personnel : où l'utilisateur se situe par rapport à
     * la référence statistique, mesuré sur toutes ses vraies séries. 1,0 = dans
     * la moyenne ; 1,2 = 20 % au-dessus. Sert à recaler les propositions des
     * exercices jamais pratiqués. Null tant qu'aucune série exploitable.
     */
    fun performanceIndex(
        profile: TrainingProfile,
        historyByExercise: Map<String, List<LoadHistoryPoint>>,
    ): Double? {
        val ratios = historyByExercise.mapNotNull { (exerciseId, points) ->
            val baseline = baselineLoad(exerciseId, profile) ?: return@mapNotNull null
            if (baseline <= 0.0) return@mapNotNull null
            val best = points.maxOfOrNull { it.loadKg } ?: return@mapNotNull null
            if (best <= 0.0) null else best / baseline
        }.sorted()
        if (ratios.isEmpty()) return null
        val median = ratios[ratios.size / 2]
        return median.coerceIn(0.5, 1.75)
    }

    /**
     * Charge proposée pour un exercice déjà pratiqué. Adaptation dans les deux
     * sens : réduction si récupération faible ou dernier RPE trop haut,
     * progression si le dernier effort était nettement sous la cible et que la
     * forme du jour le permet — au rythme dicté par l'objectif du programme.
     */
    fun recommendLoad(
        exerciseId: String,
        history: List<LoadHistoryPoint>,
        context: WorkoutCoachContext,
        targetRpe: Int = 7,
        goal: ProgramGoal = ProgramGoal.LEAN_MASS,
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
        val healthFactor = dailyLoadFactor(context)
        val rpeFactor = when {
            previous.rpe >= 9 -> .90
            previous.rpe >= 8 -> .95
            else -> 1.0
        }
        val progressionFactor = if (
            healthFactor >= 1.0 && previous.rpe in 1..(targetRpe - 2)
        ) {
            when (goal) {
                ProgramGoal.LEAN_MASS -> 1.05
                ProgramGoal.TONE_MOBILITY -> 1.025
            }
        } else 1.0
        val factor = minOf(healthFactor, rpeFactor) * progressionFactor
        val suggested = roundToHalfKg(previous.loadKg * factor)
        val explanation = when {
            suggested > previous.loadKg -> "Dernier effort nettement sous la cible et bonne forme du jour : légère progression proposée, conforme à l’objectif du programme."
            suggested < previous.loadKg -> "Charge précédente réduite selon la récupération ou le dernier RPE. Modifiable après l’échauffement."
            else -> "Charge précédente maintenue. L’application ne l’augmente qu’après validation réelle du RPE et de la technique."
        }
        return LoadRecommendation(suggested, explanation, false)
    }

    fun recommendRepetitions(
        prescription: String,
        sessionLength: SessionLength,
        readinessScore: Int? = null,
        lastAchievedReps: Int? = null,
    ): RepetitionRecommendation {
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
        } else {
            val base = if (sessionLength == SessionLength.FULL && maximum > minimum) {
                // Séance complète : cible placée dans la fourchette selon la
                // préparation du jour (sommeil, cardio de la montre…).
                when {
                    readinessScore == null -> ((minimum + maximum) / 2.0).roundToInt()
                    readinessScore >= 85 -> maximum
                    readinessScore >= 65 -> ((minimum + maximum) / 2.0).roundToInt()
                    else -> minimum
                }
            } else {
                minimum
            }
            // Apprentissage du réel : si l'utilisateur a fait davantage la
            // dernière fois et que la forme du jour est correcte, on ne propose
            // pas moins que son acquis (borné à la fourchette prescrite).
            if (lastAchievedReps != null && lastAchievedReps > base && (readinessScore == null || readinessScore >= 65)) {
                lastAchievedReps.coerceAtMost(maximum)
            } else base
        }
        return RepetitionRecommendation(value, if (timed) "Min." else "Rép.")
    }

    /**
     * Première charge proposée quand aucun historique n'existe pour cet
     * exercice. Combine la référence statistique (composition corporelle, âge,
     * sexe), l'index de performance personnel appris sur les autres exercices,
     * la progression du programme et la forme du jour. Toujours modifiable.
     */
    fun recommendStartingLoad(
        exerciseId: String,
        profile: TrainingProfile,
        context: WorkoutCoachContext,
        performanceIndex: Double? = null,
        programWeek: Int? = null,
    ): LoadRecommendation {
        if (exerciseId in noExternalLoadExercises) {
            return LoadRecommendation(
                valueKg = 0.0,
                explanation = "Aucune charge externe prévue. Réglez l’effort avec le tempo ou la résistance selon le RPE.",
                requiresCalibration = false,
            )
        }
        val baseline = baselineLoad(exerciseId, profile) ?: return LoadRecommendation(
            valueKg = null,
            explanation = "Synchronisez votre balance (Withings) ou renseignez un poids pour une première charge estimée. En attendant : démarrez léger et ajustez au RPE.",
            requiresCalibration = true,
        )
        // Transfert partiel de l'index personnel (prudence : moitié de l'écart).
        val indexFactor = performanceIndex?.let { 1.0 + (it - 1.0) * 0.5 } ?: 1.0
        // Progression attendue du programme selon l'objectif : montée douce de
        // charge pour la masse sèche, stabilité pour tonicité/mobilité (la
        // progression y passe d'abord par les répétitions).
        val weekFactor = when (profile.goal) {
            ProgramGoal.LEAN_MASS -> programWeek?.let { (1.0 + (it - 1).coerceAtLeast(0) * 0.005).coerceAtMost(1.06) } ?: 1.0
            ProgramGoal.TONE_MOBILITY -> 1.0
        }
        val estimate = baseline * indexFactor * weekFactor * dailyLoadFactor(context)
        val rounded = roundToPlate(estimate)
        val weightSource = when {
            profile.leanMassKg != null -> "votre masse maigre mesurée (${format1(profile.leanMassKg)} kg)"
            profile.bodyFatPercent != null && profile.bodyweightKg != null ->
                "votre poids mesuré (${format1(profile.bodyweightKg)} kg) et votre masse grasse mesurée"
            profile.weightIsMeasured && profile.bodyweightKg != null ->
                "votre poids mesuré (${format1(profile.bodyweightKg)} kg)"
            else -> "le poids cible du profil (aucune pesée synchronisée)"
        }
        val indexNote = performanceIndex?.let {
            when {
                it > 1.1 -> " Vos séries vous situent au-dessus de la moyenne : la proposition en tient compte."
                it < 0.9 -> " Vos séries suggèrent un démarrage plus léger que la moyenne : la proposition en tient compte."
                else -> ""
            }
        }.orEmpty()
        return LoadRecommendation(
            valueKg = rounded,
            explanation = "Première charge estimée d’après $weightSource, votre âge et votre forme du jour.$indexNote Volontairement prudente : ajustez après l’échauffement pour atteindre le RPE cible.",
            requiresCalibration = true,
        )
    }

    // Facteur de modulation quotidien commun (0,80 à 1,0) selon la préparation
    // du jour (sommeil, FC repos, VFC…) et la douleur déclarée. Partagé entre
    // charge historique et première charge estimée pour rester cohérent.
    private fun dailyLoadFactor(context: WorkoutCoachContext): Double = when {
        (context.pain ?: 0) >= 7 -> .80
        context.readinessScore == null -> .90
        context.readinessScore < 40 -> .80
        context.readinessScore < 60 || (context.pain ?: 0) >= 4 -> .90
        context.readinessScore < 80 || context.readinessConfidence < 60 -> .95
        else -> 1.0
    }

    private fun roundToHalfKg(value: Double): Double =
        (value.coerceAtLeast(0.0) * 2.0).roundToInt() / 2.0

    // Incréments plausibles de plaques/goupilles de machine (2,5 kg).
    private fun roundToPlate(value: Double): Double =
        (value.coerceAtLeast(0.0) / 2.5).roundToInt() * 2.5

    private fun format1(value: Double): String {
        val rounded = (value * 10).roundToInt() / 10.0
        return if (rounded % 1.0 == 0.0) rounded.toInt().toString() else rounded.toString().replace('.', ',')
    }
}
