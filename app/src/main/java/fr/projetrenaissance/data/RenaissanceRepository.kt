package fr.projetrenaissance.data

import androidx.room.withTransaction
import fr.projetrenaissance.domain.SafetyExercise
import fr.projetrenaissance.domain.SafetyRules
import fr.projetrenaissance.domain.SessionLength
import java.util.UUID
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.flow.Flow
import org.json.JSONArray
import org.json.JSONObject

class RenaissanceRepository(private val database: AppDatabase) {
    private val dao = database.dao()

    suspend fun seedIfNeeded() {
        database.withTransaction {
            dao.deleteLegacyDemoMetrics()
            dao.deleteKnownTimerValidationSet(
                startMillis = 1_784_132_100_000L,
                endMillis = 1_784_132_700_000L,
            )
            dao.deleteMarkedTestSets()
            // Le contenu éditorial (profils, exercices, séances) est réécrit à
            // chaque lancement : il est idempotent et permet aux installations
            // existantes de recevoir les corrections de programme sans
            // réinstallation. Les données utilisateur vivent dans d'autres tables.
            dao.insertProfiles(DemoData.profiles)
            dao.insertExercises(DemoData.exercises)
            // Les gabarits sont entièrement réécrits (et les anciens supprimés)
            // pour que les installations existantes suivent la structure de
            // phases du livre. Les journaux d'entraînement ne sont pas touchés.
            dao.deleteEveryWorkoutExercise()
            dao.deleteEveryWorkoutTemplate()
            dao.insertTemplates(DemoData.templates)
            dao.insertWorkoutExercises(DemoData.workoutExercises)
        }
    }

    fun profile(id: String): Flow<ProfileEntity?> = dao.observeProfile(id)
    fun templates(profileId: String): Flow<List<WorkoutTemplateEntity>> = dao.observeTemplates(profileId)
    fun exercises(): Flow<List<ExerciseEntity>> = dao.observeExercises()
    fun metrics(profileId: String): Flow<List<BodyMetricEntity>> = dao.observeMetrics(profileId)
    fun latestCheckIn(profileId: String): Flow<DailyCheckInEntity?> = dao.observeLatestCheckIn(profileId)
    fun checkIns(profileId: String): Flow<List<DailyCheckInEntity>> = dao.observeCheckIns(profileId)
    fun setLogs(profileId: String): Flow<List<SetLogEntity>> = dao.observeSetLogs(profileId)

    suspend fun plannedExercises(
        templateId: String,
        length: SessionLength,
        isSonia: Boolean,
        medicalClearance: Boolean,
    ): List<PlannedExercise> {
        val rows = dao.workoutRows(templateId)
        val exercises = dao.exercisesByIds(rows.map { it.exerciseId }).associateBy { it.id }
        return rows.mapNotNull { row ->
            val exercise = exercises[row.exerciseId] ?: return@mapNotNull null
            val allowed = !isSonia || SafetyRules.allowedForSonia(
                SafetyExercise(exercise.shoulderLoad, exercise.requiresClearance),
                medicalClearance,
            )
            if (!allowed) null else PlannedExercise(
                position = row.position,
                sets = (row.sets - length.setReduction).coerceAtLeast(1),
                reps = row.reps,
                restSeconds = row.restSeconds,
                tempo = row.tempo,
                rpe = row.rpe,
                exercise = exercise,
            )
        }.take(length.maxExercises)
    }

    suspend fun saveCheckIn(profileId: String, energy: Int, sleep: Int, mood: Int, pain: Int) {
        dao.insertCheckIn(
            DailyCheckInEntity(
                id = UUID.randomUUID().toString(),
                profileId = profileId,
                recordedAt = System.currentTimeMillis(),
                energy = energy,
                sleep = sleep,
                mood = mood,
                pain = pain,
            ),
        )
    }

    suspend fun saveSet(
        profileId: String,
        templateId: String,
        exerciseId: String,
        setNumber: Int,
        reps: Int,
        loadKg: Double,
        rpe: Int,
        isTest: Boolean = false,
        sessionId: String = "",
    ) {
        dao.insertSetLog(
            SetLogEntity(
                id = if (isTest) "test:${UUID.randomUUID()}" else UUID.randomUUID().toString(),
                profileId = profileId,
                templateId = templateId,
                exerciseId = exerciseId,
                setNumber = setNumber,
                reps = reps,
                loadKg = loadKg,
                rpe = rpe,
                completedAt = System.currentTimeMillis(),
                isTest = isTest,
                sessionId = sessionId,
            ),
        )
    }

    suspend fun resetProfileData(profileId: String) = database.withTransaction {
        dao.deleteBodyMetrics(profileId)
        dao.deleteCheckIns(profileId)
        dao.deleteSetLogs(profileId)
        dao.deleteAllHealthRecords(profileId)
        dao.deleteHealthSyncStates(profileId)
        dao.deleteWorkoutSessions(profileId)
    }

    suspend fun resetToday(profileId: String, now: Long = System.currentTimeMillis()) = database.withTransaction {
        val zone = ZoneId.systemDefault()
        val day = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
        val start = day.atStartOfDay(zone).toInstant().toEpochMilli()
        val end = day.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
        dao.deleteSetLogsInWindow(profileId, start, end)
        dao.deleteCheckInsInWindow(profileId, start, end)
        dao.deleteBodyMetricsInWindow(profileId, start, end)
        dao.deleteWorkoutSessionsInWindow(profileId, start, end)
    }

    suspend fun resetAllData() = database.withTransaction {
        dao.deleteEveryWorkoutSession()
        dao.deleteEverySetLog()
        dao.deleteEveryCheckIn()
        dao.deleteEveryBodyMetric()
        dao.deleteEveryHealthRecord()
        dao.deleteEveryHealthSyncState()
        dao.deleteEveryWorkoutExercise()
        dao.deleteEveryWorkoutTemplate()
        dao.deleteEveryExercise()
        dao.deleteEveryProfile()
    }

    suspend fun openOrResumeWorkout(
        profileId: String,
        templateId: String,
        length: SessionLength,
    ): WorkoutSessionSnapshot = database.withTransaction {
        val existing = dao.activeWorkoutSession(profileId, templateId)
        val session = if (existing != null && existing.length == length.name) {
            existing
        } else {
            if (existing != null) dao.upsertWorkoutSession(existing.copy(status = "ABANDONED"))
            val created = WorkoutSessionEntity(
                id = UUID.randomUUID().toString(),
                profileId = profileId,
                templateId = templateId,
                length = length.name,
                startedAt = System.currentTimeMillis(),
                currentExerciseIndex = 0,
                note = "",
                painReported = false,
                timerEndsAt = 0L,
                status = "ACTIVE",
            )
            dao.upsertWorkoutSession(created)
            created
        }
        WorkoutSessionSnapshot(session, dao.setLogsForSession(session.id))
    }

    suspend fun updateWorkoutSession(session: WorkoutSessionEntity) {
        dao.upsertWorkoutSession(session)
    }

    suspend fun restartWorkoutAtomically(session: WorkoutSessionEntity): Int = database.withTransaction {
        val deleted = dao.deleteWorkoutSetsBySession(session.id)
        dao.upsertWorkoutSession(
            session.copy(
                startedAt = System.currentTimeMillis(),
                currentExerciseIndex = 0,
                note = "",
                painReported = false,
                timerEndsAt = 0L,
                status = "ACTIVE",
            ),
        )
        deleted
    }

    suspend fun deleteWorkoutProgress(
        profileId: String,
        templateId: String,
        startedAt: Long,
        endedAt: Long,
    ): Int = dao.deleteWorkoutSetsInWindow(profileId, templateId, startedAt, endedAt)

    suspend fun exportJson(preferences: UserPreferences): String {
        val root = JSONObject()
            .put("schemaVersion", 1)
            .put("exportedAt", System.currentTimeMillis())
            .put("preferences", JSONObject()
                .put("activeProfileId", preferences.activeProfileId)
                .put("defaultRestSeconds", preferences.defaultRestSeconds)
                .put("soundEnabled", preferences.soundEnabled)
                .put("vibrationEnabled", preferences.vibrationEnabled)
                .put("soniaMedicalClearance", preferences.soniaMedicalClearance))

        root.put("bodyMetrics", JSONArray().apply {
            dao.allBodyMetrics().forEach { item ->
                put(JSONObject()
                    .put("id", item.id)
                    .put("profileId", item.profileId)
                    .put("recordedAt", item.recordedAt)
                    .put("weightKg", item.weightKg))
            }
        })
        root.put("dailyCheckIns", JSONArray().apply {
            dao.allCheckIns().forEach { item ->
                put(JSONObject()
                    .put("id", item.id)
                    .put("profileId", item.profileId)
                    .put("recordedAt", item.recordedAt)
                    .put("energy", item.energy)
                    .put("sleep", item.sleep)
                    .put("mood", item.mood)
                    .put("pain", item.pain))
            }
        })
        root.put("setLogs", JSONArray().apply {
            dao.allSetLogs().forEach { item ->
                put(JSONObject()
                    .put("id", item.id)
                    .put("profileId", item.profileId)
                    .put("templateId", item.templateId)
                    .put("exerciseId", item.exerciseId)
                    .put("setNumber", item.setNumber)
                    .put("reps", item.reps)
                    .put("loadKg", item.loadKg)
                    .put("rpe", item.rpe)
                    .put("completedAt", item.completedAt)
                    .put("isTest", item.isTest))
            }
        })
        return root.toString(2)
    }

    suspend fun importJson(payload: String): ImportSummary {
        val root = JSONObject(payload)
        require(root.getInt("schemaVersion") == 1) { "Version JSON non prise en charge" }
        val validProfiles = setOf("gerard", "sonia")

        fun JSONArray.objects(): List<JSONObject> = (0 until length()).map(::getJSONObject)

        val metrics = root.optJSONArray("bodyMetrics")?.objects().orEmpty().map { item ->
            val profileId = item.getString("profileId").also { require(it in validProfiles) }
            BodyMetricEntity(item.getString("id"), profileId, item.getLong("recordedAt"), item.getDouble("weightKg"))
        }
        val checkIns = root.optJSONArray("dailyCheckIns")?.objects().orEmpty().map { item ->
            val profileId = item.getString("profileId").also { require(it in validProfiles) }
            DailyCheckInEntity(
                item.getString("id"), profileId, item.getLong("recordedAt"),
                item.getInt("energy").coerceIn(1, 5), item.getInt("sleep").coerceIn(1, 5),
                item.getInt("mood").coerceIn(1, 5), item.getInt("pain").coerceIn(0, 10),
            )
        }
        val sets = root.optJSONArray("setLogs")?.objects().orEmpty().map { item ->
            val profileId = item.getString("profileId").also { require(it in validProfiles) }
            SetLogEntity(
                item.getString("id"), profileId, item.getString("templateId"), item.getString("exerciseId"),
                item.getInt("setNumber").coerceAtLeast(1), item.getInt("reps").coerceAtLeast(0),
                item.getDouble("loadKg").coerceAtLeast(0.0), item.getInt("rpe").coerceIn(1, 10),
                item.getLong("completedAt"), item.optBoolean("isTest", false),
            )
        }

        database.withTransaction {
            if (metrics.isNotEmpty()) dao.insertBodyMetrics(metrics)
            if (checkIns.isNotEmpty()) dao.insertCheckIns(checkIns)
            if (sets.isNotEmpty()) dao.insertSetLogs(sets)
        }
        return ImportSummary(metrics.size, checkIns.size, sets.size)
    }
}

data class ImportSummary(val metrics: Int, val checkIns: Int, val sets: Int)

data class WorkoutSessionSnapshot(
    val session: WorkoutSessionEntity,
    val setLogs: List<SetLogEntity>,
)

private object DemoData {
    val profiles = listOf(
        ProfileEntity(
            id = "gerard",
            displayName = "Gérard",
            age = 52,
            goal = "Construire une masse musculaire sèche sans perdre explosivité et mobilité.",
            targetWeight = "69–70 kg",
            healthNotes = "Allergie permanente aux protéines de lait de vache.",
        ),
        ProfileEntity(
            id = "sonia",
            displayName = "Sonia",
            age = 51,
            goal = "Tonicité des cuisses, fessiers et sangle abdominale, avec mobilité.",
            targetWeight = "Tendance et bien-être avant le chiffre",
            healthNotes = "Capsulite épaule droite et migraines. Aucun mouvement au-dessus de la tête.",
        ),
    )

    val exercises = listOf(
        ExerciseEntity("bike", "Vélo", "Jambes, système cardio-respiratoire", "Selle au niveau de la hanche, genou légèrement fléchi en bas.", "Résistance trop forte dès le départ.", "Marche confortable"),
        ExerciseEntity("leg_press", "Presse à cuisses", "Quadriceps, fessiers, adducteurs", "Dossier incliné, bassin stable, pieds largeur d'épaules.", "Décoller le bassin, rentrer les genoux, verrouiller brutalement.", "Goblet squat vers banc"),
        ExerciseEntity("chest_press", "Développé poitrine machine", "Pectoraux, triceps", "Poignées à hauteur de poitrine, omoplates stables.", "Épaules en avant, rebond, amplitude forcée.", "Pompes inclinées", "MODERATE", true),
        ExerciseEntity("seated_row", "Rowing assis poulie", "Dos, biceps", "Prise neutre, poitrine haute, épaules basses.", "Tirer avec l'élan, avancer le cou.", "Rowing poitrine appuyée", "MODERATE", true),
        ExerciseEntity("leg_curl", "Leg curl assis", "Ischio-jambiers", "Axe aligné avec le genou, coussin au-dessus des chevilles.", "Donner un élan, cambrer, laisser la pile claquer.", "Leg curl couché"),
        ExerciseEntity("lateral_raise", "Élévations latérales", "Deltoïdes", "Charge légère et bras dans une amplitude confortable.", "Hausser les épaules, lancer la charge.", "Machine latérale", "MODERATE", true),
        ExerciseEntity("calf_press", "Mollets à la presse", "Mollets", "Avant-pied stable sur la plateforme.", "Rebondir en bas, raccourcir l'amplitude.", "Mollets debout"),
        ExerciseEntity("hip_thrust", "Hip thrust guidé", "Grand fessier, ischio-jambiers", "Coussin au pli des hanches, tibias presque verticaux en haut.", "Cambrer, pousser sur la pointe, ouvrir les côtes.", "Pont fessier au sol", "LOW", false),
        ExerciseEntity("leg_extension", "Leg extension", "Quadriceps", "Axe aligné au genou, dossier soutenant le bassin.", "Lancer la charge, décoller les hanches.", "Presse pieds bas"),
        ExerciseEntity("abductors", "Abducteurs machine", "Moyen et petit fessier", "Dos soutenu, pieds placés, amplitude confortable.", "Rebondir, avancer la tête.", "Mini-band assis"),
        ExerciseEntity("dead_bug", "Dead bug bras au sol", "Sangle abdominale", "Bas du dos stable, bras détendus au sol.", "Creuser le dos, accélérer.", "Expiration et rétroversion"),
        ExerciseEntity("reverse_crunch", "Reverse crunch", "Sangle abdominale", "Bassin stable, mouvement lent.", "Prendre de l'élan, tirer sur la nuque.", "Glissement de talons"),
        // Fiches ajoutées pour la fidélité au livre (chapitres 16 à 18).
        ExerciseEntity("triceps_rope", "Extension triceps à la corde", "Triceps", "Coudes fixés le long du corps, poulie haute, corde tenue prise neutre.", "Écarter les coudes, se pencher pour pousser avec le poids du corps.", "Machine triceps", "LOW", false),
        ExerciseEntity("lat_pulldown", "Tirage vertical prise neutre", "Grand dorsal, biceps", "Cuisses calées, prise neutre, tirer vers le haut de la poitrine.", "Tirer derrière la nuque, se balancer, épaules vers les oreilles.", "Tractions assistées", "MODERATE", true),
        ExerciseEntity("incline_press", "Développé incliné machine", "Haut des pectoraux, triceps", "Dossier incliné, poignées au niveau du haut de la poitrine, omoplates posées.", "Décoller les épaules, cambrer exagérément, amplitude douloureuse.", "Haltères prise neutre", "MODERATE", true),
        ExerciseEntity("reverse_fly", "Reverse fly machine", "Arrière d'épaule, haut du dos", "Poitrine contre le support, bras presque tendus, ouvrir vers l'arrière.", "Charger trop lourd, hausser les épaules, donner de l'élan.", "Oiseau poulie", "MODERATE", true),
        ExerciseEntity("biceps_curl", "Curl biceps à la poulie", "Biceps", "Coudes le long du corps, poulie basse, montée contrôlée.", "Balancer le buste, avancer les coudes.", "Curl machine", "LOW", false),
        ExerciseEntity("chest_row", "Rowing poitrine appuyée", "Dos, arrière d'épaule, biceps", "Poitrine posée sur le support, tirer les coudes vers l'arrière sans élan.", "Décoller la poitrine, tirer avec le bas du dos.", "Rowing assis poulie", "MODERATE", true),
        ExerciseEntity("shoulder_press", "Développé épaules machine", "Deltoïdes, triceps", "Poignées au niveau des oreilles, dos plaqué, trajectoire guidée.", "Cambrer, verrouiller brutalement, descendre trop bas si l'épaule tire.", "Landmine press", "OVERHEAD", true),
        ExerciseEntity("back_extension", "Extension lombaire à 45°", "Chaîne postérieure, lombaires", "Bassin au bord du coussin, descendre dos neutre puis remonter sans hyperextension.", "Monter en arc de cercle exagéré, prendre de l'élan.", "Bird-dog"),
        ExerciseEntity("glute_bridge", "Pont fessier au sol", "Grand fessier, ischio-jambiers", "Allongée, pieds près des fessiers, monter le bassin sans cambrer.", "Pousser avec les lombaires, écarter les genoux.", "Hip thrust machine si installation indolore"),
        ExerciseEntity("step_up", "Step-up bas", "Quadriceps, fessiers, équilibre", "Marche basse et stable, monter par la jambe d'appui sans se hisser du bras.", "S'aider du bras, marche trop haute, genou qui rentre.", "Assis-debout sur banc"),
        ExerciseEntity("adductors", "Adducteurs machine", "Adducteurs", "Dos soutenu, amplitude confortable, serrer sans à-coup.", "Forcer l'amplitude, rebondir.", "Ballon entre les genoux"),
        ExerciseEntity("breathing_reset", "Respiration et bassin", "Diaphragme, plancher pelvien, sangle profonde", "Assise ou allongée, expiration longue puis rétroversion douce du bassin.", "Respirer vite, crisper les épaules.", "Assise calme sur banc"),
    )

    // Édition premium du livre : trois phases de quatre semaines, chacune avec
    // ses propres tableaux de séances (plus aucune dérivation mécanique).
    private val phases = listOf(
        Triple(1..4, "Ancrer", "Installer les réglages et sortir de chaque séance avec une répétition propre en réserve."),
        Triple(5..8, "Construire", "Ajouter du volume utile sans précipiter."),
        Triple(9..12, "Intensifier", "Double progression, petits records de répétitions, puis bilan du cycle."),
    )

    private val gerardTitles = listOf(
        mapOf("A" to "Ancrer les bases", "B" to "Construire le dos et l’arrière du corps", "C" to "Accumuler du travail utile"),
        mapOf("A" to "Ajouter du volume", "B" to "Épaissir sans précipiter", "C" to "Tolérer davantage de travail"),
        mapOf("A" to "Intensifier avec précision", "B" to "Faire monter les repères", "C" to "Consolider le cycle"),
    )
    private val soniaTitles = mapOf(
        "A" to "Cuisses et stabilité",
        "B" to "Fessiers et mobilité",
        "C" to "Endurance musculaire",
    )

    val templates = buildList {
        phases.forEachIndexed { phaseIndex, (weeks, _, intent) ->
            listOf("A", "B", "C").forEach { code ->
                add(WorkoutTemplateEntity("gerard_${phaseIndex}_$code", "gerard", weeks.first, weeks.last, code, "${gerardTitles[phaseIndex].getValue(code)} · Séance $code", intent))
                add(
                    WorkoutTemplateEntity(
                        "sonia_${phaseIndex}_$code", "sonia", weeks.first, weeks.last, code,
                        "${soniaTitles.getValue(code)} · Séance $code",
                        "Bas du corps, épaule droite neutre ; le haut du corps n’apparaît qu’après validation médicale.",
                    ),
                )
            }
        }
    }

    private data class Prescription(val exercise: String, val sets: Int, val reps: String, val rest: Int, val tempo: String, val rpe: String)

    // Gérard — semaines 1 à 4 : « Ancrer les bases ».
    private val gerardA1 = listOf(
        Prescription("leg_press", 3, "8–10", 120, "3–1–1", "6–7"),
        Prescription("chest_press", 3, "8–10", 120, "3–0–1", "6–7"),
        Prescription("seated_row", 3, "10–12", 120, "2–1–2", "7"),
        Prescription("leg_curl", 3, "10–12", 90, "3–1–1", "7"),
        Prescription("lateral_raise", 2, "12–15", 75, "2–1–2", "7"),
        Prescription("triceps_rope", 2, "10–15", 75, "2–1–2", "7"),
        Prescription("calf_press", 2, "12–15", 60, "2–1–2", "7"),
    )
    private val gerardB1 = listOf(
        Prescription("hip_thrust", 3, "8–12", 120, "2–1–1", "6–7"),
        Prescription("lat_pulldown", 3, "8–12", 120, "2–1–2", "7"),
        Prescription("incline_press", 3, "8–12", 120, "3–0–1", "7"),
        Prescription("leg_extension", 2, "12–15", 90, "2–1–2", "7"),
        Prescription("reverse_fly", 2, "12–15", 75, "2–1–2", "7"),
        Prescription("biceps_curl", 3, "10–12", 75, "2–1–2", "7"),
        Prescription("dead_bug", 2, "6/côté", 60, "lent", "6"),
    )
    private val gerardC1 = listOf(
        Prescription("leg_press", 3, "10–12", 120, "3–1–1", "7"),
        Prescription("chest_row", 3, "10–12", 120, "2–1–2", "7"),
        Prescription("chest_press", 3, "10–12", 120, "3–0–1", "7"),
        Prescription("leg_curl", 3, "12–15", 90, "3–1–1", "7"),
        Prescription("shoulder_press", 2, "8–12", 90, "3–0–1", "6–7"),
        Prescription("biceps_curl", 2, "12", 75, "2–1–2", "7"),
        Prescription("triceps_rope", 2, "12", 75, "2–1–2", "7"),
        Prescription("back_extension", 2, "10–12", 75, "2–1–2", "6"),
    )

    // Gérard — semaines 5 à 8 : « Ajouter du volume ».
    private val gerardA2 = listOf(
        Prescription("leg_press", 4, "8–12", 150, "3–1–1", "7–8"),
        Prescription("chest_press", 4, "8–12", 120, "3–0–1", "7–8"),
        Prescription("seated_row", 4, "8–12", 120, "2–1–2", "7–8"),
        Prescription("leg_curl", 3, "10–15", 90, "3–1–1", "8"),
        Prescription("lateral_raise", 3, "12–18", 75, "2–1–2", "8"),
        Prescription("triceps_rope", 3, "10–15", 75, "2–1–2", "8"),
        Prescription("calf_press", 3, "12–18", 60, "2–1–2", "8"),
    )
    private val gerardB2 = listOf(
        Prescription("hip_thrust", 4, "8–12", 120, "2–1–1", "7–8"),
        Prescription("lat_pulldown", 4, "8–12", 120, "2–1–2", "8"),
        Prescription("incline_press", 3, "8–12", 120, "3–0–1", "8"),
        Prescription("leg_extension", 3, "10–15", 90, "2–1–2", "8"),
        Prescription("reverse_fly", 3, "12–18", 75, "2–1–2", "8"),
        Prescription("biceps_curl", 3, "8–12", 90, "2–1–2", "8"),
        Prescription("dead_bug", 3, "6/côté", 60, "lent", "7"),
    )
    private val gerardC2 = listOf(
        Prescription("leg_press", 4, "10–15", 120, "3–1–1", "8"),
        Prescription("chest_row", 4, "10–15", 120, "2–1–2", "8"),
        Prescription("chest_press", 3, "10–15", 120, "3–0–1", "8"),
        Prescription("leg_curl", 3, "12–15", 90, "3–1–1", "8"),
        Prescription("shoulder_press", 3, "8–12", 120, "3–0–1", "7–8"),
        Prescription("biceps_curl", 3, "10–15", 75, "2–1–2", "8"),
        Prescription("triceps_rope", 3, "10–15", 75, "2–1–2", "8"),
        Prescription("back_extension", 3, "10–15", 75, "2–1–2", "7"),
    )

    // Gérard — semaines 9 à 12 : « Intensifier avec précision ».
    private val gerardA3 = listOf(
        Prescription("leg_press", 4, "6–10", 180, "3–1–1", "8–9"),
        Prescription("chest_press", 4, "6–10", 150, "3–0–1", "8–9"),
        Prescription("seated_row", 4, "8–12", 120, "2–1–2", "8–9"),
        Prescription("leg_curl", 3, "8–12", 90, "3–1–1", "8"),
        Prescription("lateral_raise", 3, "12–18", 75, "2–1–2", "8"),
        Prescription("triceps_rope", 3, "8–12", 90, "2–1–2", "8"),
        Prescription("calf_press", 3, "10–15", 75, "2–1–2", "8"),
    )
    private val gerardB3 = listOf(
        Prescription("hip_thrust", 4, "6–10", 150, "2–1–1", "8–9"),
        Prescription("lat_pulldown", 4, "6–10", 120, "2–1–2", "8–9"),
        Prescription("incline_press", 4, "8–12", 120, "3–0–1", "8"),
        Prescription("leg_extension", 3, "10–15", 90, "2–1–2", "8"),
        Prescription("reverse_fly", 3, "12–18", 75, "2–1–2", "8"),
        Prescription("biceps_curl", 3, "8–12", 90, "2–1–2", "8–9"),
        Prescription("dead_bug", 3, "8/côté", 60, "lent", "7"),
    )
    private val gerardC3 = listOf(
        Prescription("leg_press", 3, "10–15", 120, "3–1–1", "8"),
        Prescription("chest_row", 3, "8–12", 120, "2–1–2", "8"),
        Prescription("chest_press", 3, "8–12", 120, "3–0–1", "8"),
        Prescription("leg_curl", 3, "10–15", 90, "3–1–1", "8"),
        Prescription("shoulder_press", 3, "8–12", 120, "3–0–1", "8"),
        Prescription("biceps_curl", 2, "10–15", 75, "2–1–2", "8"),
        Prescription("triceps_rope", 2, "10–15", 75, "2–1–2", "8"),
        Prescription("back_extension", 2, "10–15", 75, "2–1–2", "7"),
    )

    // Sonia — mêmes séances toutes phases (progression par répétitions).
    // Les mouvements du haut du corps (rowing appuyé, chest press neutre)
    // portent requiresClearance : ils ne sont proposés qu'après validation
    // médicale, avec un RPE volontairement bas (4–6), conformément au livre.
    private val soniaA = listOf(
        Prescription("bike", 1, "6 min", 0, "souple", "4"),
        Prescription("leg_press", 3, "10–12", 120, "3–1–1", "6–7"),
        Prescription("leg_curl", 3, "10–15", 90, "3–1–1", "7"),
        Prescription("chest_row", 2, "12–15", 120, "2–1–2", "4–6"),
        Prescription("abductors", 3, "12–18", 75, "2–1–2", "7"),
        Prescription("calf_press", 3, "12–18", 60, "2–1–2", "7"),
        Prescription("dead_bug", 2, "6/côté", 60, "lent", "5–6"),
    )
    private val soniaB = listOf(
        Prescription("bike", 1, "6 min", 0, "souple", "4"),
        Prescription("glute_bridge", 3, "10–15", 90, "2–2–1", "6–7"),
        Prescription("leg_extension", 3, "10–15", 90, "2–1–2", "7"),
        Prescription("chest_press", 2, "12–15", 120, "3–0–1", "4–6"),
        Prescription("adductors", 2, "12–18", 75, "2–1–2", "7"),
        Prescription("step_up", 2, "8/côté", 90, "2–1–2", "6"),
        Prescription("reverse_crunch", 2, "8–12", 60, "lent", "6"),
    )
    private val soniaC = listOf(
        Prescription("bike", 1, "8 min", 0, "souple", "4–5"),
        Prescription("leg_press", 3, "12–15", 120, "3–1–1", "7"),
        Prescription("leg_curl", 3, "12–15", 90, "3–1–1", "7"),
        Prescription("abductors", 3, "15–20", 75, "2–1–2", "7"),
        Prescription("glute_bridge", 3, "12–15", 90, "2–2–1", "7"),
        Prescription("breathing_reset", 3, "5 cycles", 45, "lent", "3"),
    )

    private val gerardPlans = listOf(
        mapOf("A" to gerardA1, "B" to gerardB1, "C" to gerardC1),
        mapOf("A" to gerardA2, "B" to gerardB2, "C" to gerardC2),
        mapOf("A" to gerardA3, "B" to gerardB3, "C" to gerardC3),
    )
    private val soniaPlans = mapOf("A" to soniaA, "B" to soniaB, "C" to soniaC)

    val workoutExercises = templates.flatMap { template ->
        val phaseIndex = template.id.split("_")[1].toInt()
        val plan = if (template.profileId == "gerard") gerardPlans[phaseIndex].getValue(template.code)
        else soniaPlans.getValue(template.code)
        plan.mapIndexed { index, item ->
            WorkoutExerciseEntity(
                templateId = template.id,
                position = index + 1,
                exerciseId = item.exercise,
                sets = item.sets,
                reps = item.reps,
                restSeconds = item.rest,
                tempo = item.tempo,
                rpe = item.rpe,
            )
        }
    }

}
