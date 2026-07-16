package fr.projetrenaissance.domain

enum class ExerciseMediaType { GUIDED_ILLUSTRATION, MACHINE_ILLUSTRATION, MACHINE_PHOTO, VERIFIED_EXTERNAL_VIDEO, BOOK_SHEET }
enum class MediaVerificationStatus { VERIFIED, TO_VALIDATE, MISSING }

data class GuidedMovement(
    val startPosition: String,
    val middlePosition: String,
    val endPosition: String,
    val trajectory: String,
    val mainPivot: String,
    val supports: String,
    val recommendedRange: String,
    val breathing: String,
    val effortZone: String,
    val commonSuccess: String,
)

data class MachineMedia(
    val expectedType: String,
    val genericName: String,
    val possibleVariants: String,
    val localResource: String?,
    val userPhotoUri: String? = null,
    val source: String?,
    val verificationStatus: MediaVerificationStatus,
    val adjustmentLandmarks: String,
)

data class ExerciseMedia(
    val exerciseId: String,
    val guidedIllustration: GuidedMovement,
    val machine: MachineMedia,
    val verifiedVideoUrl: String?,
    val videoSource: String?,
    val bookLocator: String,
    val verificationDate: String,
    val accessibilityDescription: String,
)

object ExerciseMediaCatalog {
    private fun guided(
        start: String,
        middle: String,
        end: String,
        trajectory: String,
        pivot: String,
        supports: String,
        range: String,
        effort: String,
    ) = GuidedMovement(
        startPosition = start,
        middlePosition = middle,
        endPosition = end,
        trajectory = trajectory,
        mainPivot = pivot,
        supports = supports,
        recommendedRange = range,
        breathing = "Inspirer au retour, expirer pendant l’effort sans bloquer la respiration.",
        effortZone = effort,
        commonSuccess = "Appuis stables, trajectoire lente et retour entièrement contrôlé.",
    )

    private fun machine(
        exerciseId: String,
        expected: String,
        generic: String,
        variants: String,
        landmarks: String,
    ) = MachineMedia(
        expectedType = expected,
        genericName = generic,
        possibleVariants = variants,
        localResource = "machine_$exerciseId",
        userPhotoUri = null,
        source = "Illustration vectorielle originale Projet Renaissance",
        verificationStatus = MediaVerificationStatus.VERIFIED,
        adjustmentLandmarks = landmarks,
    )

    val all = listOf(
        ExerciseMedia(
            "bike",
            guided("Bassin centré sur la selle.", "Pédale à mi-course, genou aligné.", "Jambe presque tendue en bas.", "Rotation régulière du pédalier.", "Hanche et genou.", "Selle, pieds dans les pédales, mains légères.", "Genou encore légèrement fléchi au point bas.", "Poussée fluide du pied."),
            machine("bike", "Vélo cardio assis", "Vélo ergométrique", "Droit, semi-allongé", "Hauteur et recul de selle, sangles de pédales, niveau de résistance."),
            null, null, "Livre · fiche Vélo", "2026-07-16", "Trois positions du pédalage sur vélo, buste stable.",
        ),
        ExerciseMedia(
            "leg_press",
            guided("Pieds à plat, genoux fléchis.", "Plateforme poussée à mi-course.", "Jambes tendues sans verrouiller.", "Poussée oblique guidée.", "Genou et hanche.", "Bassin et dos au dossier, deux pieds sur la plateforme.", "Descendre sans décoller le bassin.", "Pousser la plateforme avec tout le pied."),
            machine("leg_press", "Presse inclinée ou horizontale", "Presse à cuisses", "À charge guidée, à disques, horizontale", "Inclinaison du dossier, butée de départ, placement des pieds, poignées latérales."),
            null, null, "Livre · fiche Presse à cuisses", "2026-07-16", "Départ, descente contrôlée et poussée à la presse à cuisses.",
        ),
        ExerciseMedia(
            "chest_press",
            guided("Poignées près de la poitrine.", "Coudes en extension progressive.", "Bras presque tendus.", "Poussée horizontale.", "Coude et épaule.", "Dos au dossier, pieds au sol.", "Retour avant que l’épaule ne parte vers l’avant.", "Pousser les poignées devant soi."),
            machine("chest_press", "Presse poitrine assise", "Chest press", "Convergente, horizontale, prise neutre", "Hauteur du siège, dossier, position de départ, poignées."),
            null, null, "Livre · fiche Développé poitrine", "2026-07-16", "Départ, poussée et retour contrôlé à la machine poitrine.",
        ),
        ExerciseMedia(
            "seated_row",
            guided("Bras tendus, épaules basses.", "Coudes tirés près du corps.", "Poignées proches du buste.", "Tirage horizontal vers soi.", "Coude et omoplate.", "Pieds calés, bassin stable, poitrine soutenue si disponible.", "Arrêter avant de basculer le buste.", "Tirer les coudes derrière soi."),
            machine("seated_row", "Rowing à poulie basse", "Rowing assis", "Poulie, convergent, poitrine appuyée", "Repose-pieds, siège, appui poitrine, poignée neutre."),
            null, null, "Livre · fiche Rowing assis", "2026-07-16", "Bras tendus, tirage et retour au rowing assis.",
        ),
        ExerciseMedia(
            "leg_curl",
            guided("Genoux fléchis légèrement, rouleau aux chevilles.", "Talons tirés vers le bas.", "Flexion confortable maximale.", "Arc vers l’arrière et le bas.", "Genou.", "Dos et bassin au dossier, cuisses sous le maintien.", "Ne pas forcer la flexion ni décoller le bassin.", "Ramener les talons sous le siège."),
            machine("leg_curl", "Leg curl assis", "Leg curl", "Assis, couché, debout unilatéral", "Axe du genou, dossier, maintien des cuisses, rouleau des chevilles."),
            null, null, "Livre · fiche Leg curl", "2026-07-16", "Départ, flexion des genoux et retour au leg curl.",
        ),
        ExerciseMedia(
            "lateral_raise",
            guided("Bras près du corps.", "Bras montant sur le côté.", "Amplitude confortable sous l’horizontale.", "Arc latéral contrôlé.", "Épaule.", "Pieds stables, buste vertical.", "S’arrêter avant toute gêne ou haussement d’épaule.", "Éloigner doucement les mains du corps."),
            machine("lateral_raise", "Haltères légers ou machine", "Élévation latérale", "Haltères, poulie basse, machine à coussins", "Charge, hauteur de siège et position des coussins selon la variante."),
            null, null, "Livre · fiche Élévations latérales", "2026-07-16", "Bras bas, élévation confortable et retour lent.",
        ),
        ExerciseMedia(
            "calf_press",
            guided("Talons légèrement descendus.", "Chevilles en position neutre.", "Talons poussés vers le haut.", "Flexion-extension de cheville.", "Cheville.", "Avant-pieds sur la plateforme, bassin au dossier.", "Amplitude confortable sans rebond.", "Pousser la plateforme avec l’avant-pied."),
            machine("calf_press", "Presse à cuisses", "Mollets à la presse", "Presse inclinée, horizontale, machine mollets", "Dossier, butée, placement sécurisé de l’avant-pied."),
            null, null, "Livre · fiche Mollets à la presse", "2026-07-16", "Chevilles fléchies, poussée des mollets et retour.",
        ),
        ExerciseMedia(
            "hip_thrust",
            guided("Bassin bas, haut du dos soutenu.", "Bassin en montée.", "Hanches alignées avec le tronc.", "Translation verticale du bassin.", "Hanche.", "Omoplates sur l’appui, pieds au sol.", "Monter sans cambrer le bas du dos.", "Pousser le sol avec les talons."),
            machine("hip_thrust", "Hip thrust guidé", "Machine hip thrust", "Machine à ceinture, Smith machine, banc", "Appui dorsal, coussin ou ceinture pelvienne, position des pieds."),
            null, null, "Livre · fiche Hip thrust", "2026-07-16", "Bassin bas, extension de hanche et retour contrôlé.",
        ),
        ExerciseMedia(
            "leg_extension",
            guided("Genoux fléchis, rouleau sur les tibias.", "Jambes en extension.", "Genoux presque tendus.", "Arc vers l’avant et le haut.", "Genou.", "Dos et bassin au dossier, mains aux poignées.", "Ne pas verrouiller brutalement.", "Pousser le rouleau avec les tibias."),
            machine("leg_extension", "Leg extension assis", "Leg extension", "Bilatéral, unilatéral", "Axe du genou, dossier, rouleau des tibias, amplitude de départ."),
            null, null, "Livre · fiche Leg extension", "2026-07-16", "Genoux fléchis, extension et retour sans élan.",
        ),
        ExerciseMedia(
            "abductors",
            guided("Genoux rapprochés contre les coussins.", "Ouverture progressive.", "Ouverture confortable.", "Arc latéral des cuisses.", "Hanche.", "Dos au dossier, pieds sur les appuis.", "Ne pas forcer l’ouverture du bassin.", "Pousser les coussins vers l’extérieur."),
            machine("abductors", "Abducteurs assis", "Machine abducteurs", "Dossier droit ou incliné", "Dossier, angle de départ, coussins latéraux, repose-pieds."),
            null, null, "Livre · fiche Abducteurs", "2026-07-16", "Genoux rapprochés, ouverture et retour contrôlé.",
        ),
        ExerciseMedia(
            "dead_bug",
            guided("Dos au sol, hanches et genoux fléchis.", "Bras et jambe opposés s’éloignent.", "Membres proches du sol sans cambrer.", "Allongement diagonal opposé.", "Hanche et épaule.", "Bassin et dos au tapis.", "S’arrêter dès que le bas du dos se creuse.", "Éloigner lentement le talon et la main."),
            machine("dead_bug", "Aucune machine", "Tapis de sol", "Tapis épais, banc plat si validé", "Surface stable, espace libre, tête soutenue si nécessaire."),
            null, null, "Livre · fiche Dead bug", "2026-07-16", "Position neutre, extension opposée et retour du dead bug.",
        ),
        ExerciseMedia(
            "reverse_crunch",
            guided("Dos au sol, genoux ramenés.", "Bassin s’enroule doucement.", "Sacrum légèrement décollé.", "Enroulement court du bassin.", "Bassin et lombaires.", "Dos, bras et tête au tapis.", "Petit mouvement sans élan.", "Ramener le bassin vers les côtes."),
            machine("reverse_crunch", "Aucune machine", "Tapis de sol", "Tapis, banc plat avec appui stable", "Surface antidérapante et espace libre."),
            null, null, "Livre · fiche Reverse crunch", "2026-07-16", "Départ, enroulement du bassin et retour lent.",
        ),
    )

    fun forExercise(exerciseId: String): ExerciseMedia? = all.firstOrNull { it.exerciseId == exerciseId }
}
