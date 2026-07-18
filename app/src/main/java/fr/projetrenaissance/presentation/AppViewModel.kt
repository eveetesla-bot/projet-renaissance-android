package fr.projetrenaissance.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import fr.projetrenaissance.AppContainer
import fr.projetrenaissance.data.BodyMetricEntity
import fr.projetrenaissance.data.DailyCheckInEntity
import fr.projetrenaissance.data.ExerciseEntity
import fr.projetrenaissance.data.PlannedExercise
import fr.projetrenaissance.data.ProfileEntity
import fr.projetrenaissance.data.UserPreferences
import fr.projetrenaissance.data.WorkoutTemplateEntity
import fr.projetrenaissance.data.HealthRecordEntity
import fr.projetrenaissance.data.HealthSyncStateEntity
import fr.projetrenaissance.data.SetLogEntity
import fr.projetrenaissance.data.WorkoutSessionEntity
import fr.projetrenaissance.data.health.HealthConnectAvailability
import fr.projetrenaissance.data.health.HealthConnectSyncWorker
import androidx.health.connect.client.permission.HealthPermission
import fr.projetrenaissance.domain.SessionLength
import fr.projetrenaissance.domain.TimerMath
import fr.projetrenaissance.domain.HealthRecordType
import fr.projetrenaissance.domain.HealthSourceCategory
import fr.projetrenaissance.domain.DailyHealthSummary
import fr.projetrenaissance.domain.HealthInsights
import kotlinx.coroutines.Job
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import android.net.Uri
import java.io.File

data class AppUiState(
    val loading: Boolean = true,
    val preferences: UserPreferences = UserPreferences(),
    val profile: ProfileEntity? = null,
    val templates: List<WorkoutTemplateEntity> = emptyList(),
    val exercises: List<ExerciseEntity> = emptyList(),
    val metrics: List<BodyMetricEntity> = emptyList(),
    val latestCheckIn: DailyCheckInEntity? = null,
    val checkIns: List<DailyCheckInEntity> = emptyList(),
    val setLogs: List<SetLogEntity> = emptyList(),
    val healthAvailability: HealthConnectAvailability = HealthConnectAvailability.UNAVAILABLE,
    val healthRecords: List<HealthRecordEntity> = emptyList(),
    val healthSyncStates: List<HealthSyncStateEntity> = emptyList(),
    val healthMessage: String? = null,
    val dailyHealth: DailyHealthSummary = HealthInsights.calculate(emptyList(), emptyList(), emptyList(), 0L),
)

data class ActiveWorkoutState(
    val sessionId: String = "",
    val templateId: String = "",
    val length: SessionLength = SessionLength.FULL,
    val exercises: List<PlannedExercise> = emptyList(),
    val currentExerciseIndex: Int = 0,
    val completedSets: Map<String, Int> = emptyMap(),
    val timerSeconds: Int = 0,
    val timerRunning: Boolean = false,
    val timerEndsAtMillis: Long = 0L,
    val painReported: Boolean = false,
    val startedAtMillis: Long = 0L,
    val restartVersion: Int = 0,
    val note: String = "",
    val restartMessage: String? = null,
    val resetInProgress: Boolean = false,
    val sessionCompleted: Boolean = false,
)

@OptIn(ExperimentalCoroutinesApi::class)
class AppViewModel(private val container: AppContainer) : ViewModel() {
    private val preferences = container.preferences.preferences
    private val activeProfileId = preferences.flatMapLatest { flowOf(it.activeProfileId) }
    private val profile = activeProfileId.flatMapLatest { id ->
        if (id == null) flowOf(null) else container.repository.profile(id)
    }
    private val templates = activeProfileId.flatMapLatest { id ->
        if (id == null) flowOf(emptyList()) else container.repository.templates(id)
    }
    private val metrics = activeProfileId.flatMapLatest { id ->
        if (id == null) flowOf(emptyList()) else container.repository.metrics(id)
    }
    private val latestCheckIn = activeProfileId.flatMapLatest { id ->
        if (id == null) flowOf(null) else container.repository.latestCheckIn(id)
    }
    private val checkIns = activeProfileId.flatMapLatest { id ->
        if (id == null) flowOf(emptyList()) else container.repository.checkIns(id)
    }
    private val setLogs = activeProfileId.flatMapLatest { id ->
        if (id == null) flowOf(emptyList()) else container.repository.setLogs(id)
    }
    private val healthRecords = activeProfileId.flatMapLatest { id ->
        if (id == null) flowOf(emptyList()) else container.healthData.records(id)
    }
    private val healthSyncStates = activeProfileId.flatMapLatest { id ->
        if (id == null) flowOf(emptyList()) else container.healthData.syncStates(id)
    }
    private val healthMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<AppUiState> = combine(
        preferences,
        profile,
        templates,
        container.repository.exercises(),
        metrics,
        latestCheckIn,
        healthRecords,
        healthSyncStates,
        healthMessage,
        checkIns,
        setLogs,
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        AppUiState(
            loading = false,
            preferences = values[0] as UserPreferences,
            profile = values[1] as ProfileEntity?,
            templates = values[2] as List<WorkoutTemplateEntity>,
            exercises = values[3] as List<ExerciseEntity>,
            metrics = values[4] as List<BodyMetricEntity>,
            latestCheckIn = values[5] as DailyCheckInEntity?,
            healthAvailability = container.healthData.availability(),
            healthRecords = values[6] as List<HealthRecordEntity>,
            healthSyncStates = values[7] as List<HealthSyncStateEntity>,
            healthMessage = values[8] as String?,
            checkIns = values[9] as List<DailyCheckInEntity>,
            setLogs = values[10] as List<SetLogEntity>,
            dailyHealth = HealthInsights.calculate(
                values[6] as List<HealthRecordEntity>,
                values[9] as List<DailyCheckInEntity>,
                values[10] as List<SetLogEntity>,
                profileId = (values[1] as ProfileEntity?)?.id,
            ),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppUiState())

    private val _workout = MutableStateFlow(ActiveWorkoutState())
    val workout: StateFlow<ActiveWorkoutState> = _workout
    private var timerJob: Job? = null
    private var lastSetSaveJob: Job? = null
    private var timerEndsAt = 0L

    init {
        viewModelScope.launch {
            container.repository.seedIfNeeded()
            val prefs = container.preferences.preferences.first()
            val profileId = prefs.activeProfileId
            if (profileId != null && prefs.onboardingCompleted && prefs.healthSyncEnabled) {
                val permissionGranted = runCatching {
                    container.healthData.grantedPermissions().intersect(container.healthData.permissions()).isNotEmpty()
                }.getOrDefault(false)
                if (!permissionGranted) {
                    container.preferences.setHealthSync(false)
                    healthMessage.value = "Accès santé à réactiver."
                } else if (System.currentTimeMillis() - prefs.lastHealthSyncAt >= 6 * 60 * 60 * 1_000L) {
                    syncHealthNow(profileId)
                }
            }
        }
        if (container.context.packageName.endsWith(".soniatest")) {
            viewModelScope.launch { container.preferences.selectProfile("sonia") }
        }
    }

    fun selectProfile(id: String) = viewModelScope.launch { container.preferences.selectProfile(id) }
    fun setSound(enabled: Boolean) = viewModelScope.launch { container.preferences.setSound(enabled) }
    fun setVibration(enabled: Boolean) = viewModelScope.launch { container.preferences.setVibration(enabled) }
    fun setClearance(enabled: Boolean) = viewModelScope.launch { container.preferences.setClearance(enabled) }

    fun setMachinePhoto(exerciseId: String, uri: String) = viewModelScope.launch {
        val profileId = uiState.value.profile?.id ?: return@launch
        val previous = uiState.value.preferences.machinePhotoUris[exerciseId]
        if (previous != uri) deleteInternalMachinePhoto(previous)
        container.preferences.setMachinePhotoUri(profileId, exerciseId, uri)
    }

    fun removeMachinePhoto(exerciseId: String) = viewModelScope.launch {
        val profileId = uiState.value.profile?.id ?: return@launch
        deleteInternalMachinePhoto(uiState.value.preferences.machinePhotoUris[exerciseId])
        container.preferences.setMachinePhotoUri(profileId, exerciseId, null)
    }

    fun healthPermissions(includeBackground: Boolean = false): Set<String> = buildSet {
        addAll(container.healthData.permissions())
        if (includeBackground) add(HealthPermission.PERMISSION_READ_HEALTH_DATA_IN_BACKGROUND)
    }

    fun onHealthPermissionsResult(granted: Set<String>) {
        viewModelScope.launch {
            val enabled = granted.intersect(container.healthData.permissions()).isNotEmpty()
            container.preferences.setHealthSync(enabled)
            healthMessage.value = if (enabled) "Autorisations mises à jour." else "Aucune autorisation santé accordée."
            if (enabled) syncHealthNow()
        }
    }

    fun syncHealth() = viewModelScope.launch { syncHealthNow() }

    private suspend fun syncHealthNow() {
        val profileId = uiState.value.profile?.id ?: return
        syncHealthNow(profileId)
    }

    private suspend fun syncHealthNow(profileId: String) {
        healthMessage.value = "Synchronisation en cours…"
        val report = container.healthData.sync(profileId)
        if (report.synchronizedTypes > 0) container.preferences.setLastHealthSyncAt(report.completedAt)
        healthMessage.value = if (report.errors.isEmpty()) {
            "Synchronisation terminée : ${report.imported} importées, ${report.deleted} supprimées."
        } else {
            "Synchronisation partielle : ${report.synchronizedTypes} types, ${report.errors.size} erreur(s)."
        }
    }

    fun setHealthBackgroundSync(enabled: Boolean) = viewModelScope.launch {
        container.preferences.setHealthBackgroundSync(enabled)
        if (enabled) HealthConnectSyncWorker.schedule(container.context) else HealthConnectSyncWorker.cancel(container.context)
        healthMessage.value = if (enabled) "Synchronisation périodique activée." else "Synchronisation périodique désactivée."
    }

    fun deleteImportedHealthData() = viewModelScope.launch {
        val profileId = uiState.value.profile?.id ?: return@launch
        container.healthData.deleteImported(profileId)
        healthMessage.value = "Données santé importées supprimées de ce profil."
    }

    fun resetLocalProfile() = viewModelScope.launch {
        val profileId = uiState.value.profile?.id ?: return@launch
        clearInternalMachinePhotos(profileId)
        container.repository.resetProfileData(profileId)
        container.preferences.resetLocalProfile(profileId)
        HealthConnectSyncWorker.cancel(container.context)
        _workout.value = ActiveWorkoutState()
        healthMessage.value = "Profil local réinitialisé. Aucune donnée n’a été supprimée de Health Connect."
    }

    fun resetToday() = viewModelScope.launch {
        val profileId = uiState.value.profile?.id ?: return@launch
        runCatching {
            lastSetSaveJob?.join()
            container.repository.resetToday(profileId)
            container.preferences.resetToday(profileId)
        }.onSuccess {
            timerJob?.cancel()
            _workout.value = ActiveWorkoutState()
            healthMessage.value = "Journée réinitialisée. L’historique antérieur et les données santé sont conservés."
        }.onFailure {
            healthMessage.value = "La journée n’a pas été réinitialisée : ${it.message}"
        }
    }

    fun resetAll() = viewModelScope.launch {
        runCatching {
            lastSetSaveJob?.join()
            if (container.healthData.availability() == HealthConnectAvailability.AVAILABLE) {
                container.healthData.revokeAllPermissions()
            }
            container.repository.resetAllData()
            container.preferences.resetAll()
            File(container.context.filesDir, "user_machine_photos").deleteRecursively()
            HealthConnectSyncWorker.cancel(container.context)
            container.repository.seedIfNeeded()
            if (container.context.packageName.endsWith(".soniatest")) {
                container.preferences.selectProfile("sonia")
            }
        }.onSuccess {
            timerJob?.cancel()
            _workout.value = ActiveWorkoutState()
            healthMessage.value = "Application remise à zéro."
        }.onFailure {
            healthMessage.value = "La remise à zéro totale a échoué : ${it.message}"
        }
    }

    private fun deleteInternalMachinePhoto(uriValue: String?) {
        val uri = uriValue?.let(Uri::parse) ?: return
        if (uri.scheme != "file") return
        val mediaRoot = File(container.context.filesDir, "user_machine_photos").canonicalFile
        val candidate = uri.path?.let(::File)?.canonicalFile ?: return
        if (candidate.path.startsWith(mediaRoot.path + File.separator)) candidate.delete()
    }

    private fun clearInternalMachinePhotos(profileId: String) {
        val mediaRoot = File(container.context.filesDir, "user_machine_photos").canonicalFile
        val profileDirectory = File(mediaRoot, profileId).canonicalFile
        if (profileDirectory.path.startsWith(mediaRoot.path + File.separator)) profileDirectory.deleteRecursively()
    }

    fun completeOnboarding() = viewModelScope.launch {
        container.preferences.setOnboardingCompleted(true)
    }

    fun setHealthPriority(type: HealthRecordType, source: HealthSourceCategory) = viewModelScope.launch {
        val profileId = uiState.value.profile?.id ?: return@launch
        val changed = container.healthData.setPrimarySource(profileId, type, source)
        healthMessage.value = if (changed) {
            "${source.name} est maintenant prioritaire pour ${type.name}."
        } else {
            "Aucune donnée compatible détectée pour cette source."
        }
    }

    suspend fun exportPayload(): String = container.repository.exportJson(uiState.value.preferences)

    suspend fun importPayload(payload: String): String {
        val result = container.repository.importJson(payload)
        return "Import réussi : ${result.metrics} mesures, ${result.checkIns} bilans, ${result.sets} séries."
    }

    fun saveCheckIn(energy: Int, sleep: Int, mood: Int, pain: Int) {
        val profileId = uiState.value.profile?.id ?: return
        viewModelScope.launch { container.repository.saveCheckIn(profileId, energy, sleep, mood, pain) }
    }

    fun startWorkout(templateId: String, length: SessionLength) {
        val state = uiState.value
        val profileId = state.profile?.id ?: return
        viewModelScope.launch {
            val plan = container.repository.plannedExercises(
                templateId = templateId,
                length = length,
                isSonia = profileId == "sonia",
                medicalClearance = state.preferences.soniaMedicalClearance,
            )
            val snapshot = container.repository.openOrResumeWorkout(profileId, templateId, length)
            val completedSets = snapshot.setLogs
                .filterNot { it.isTest }
                .groupingBy { it.exerciseId }
                .eachCount()
            val now = System.currentTimeMillis()
            _workout.value = ActiveWorkoutState(
                sessionId = snapshot.session.id,
                templateId = templateId,
                length = length,
                exercises = plan,
                currentExerciseIndex = snapshot.session.currentExerciseIndex.coerceIn(0, (plan.size - 1).coerceAtLeast(0)),
                completedSets = completedSets,
                timerSeconds = TimerMath.remainingSeconds(snapshot.session.timerEndsAt, now),
                timerRunning = snapshot.session.timerEndsAt > now,
                timerEndsAtMillis = snapshot.session.timerEndsAt,
                painReported = snapshot.session.painReported,
                startedAtMillis = snapshot.session.startedAt,
                note = snapshot.session.note,
            )
            if (snapshot.session.timerEndsAt > now) startTimer(
                TimerMath.remainingSeconds(snapshot.session.timerEndsAt, now),
            )
        }
    }

    fun completeSet(reps: Int, loadKg: Double, rpe: Int) {
        val current = _workout.value
        val exercise = current.exercises.getOrNull(current.currentExerciseIndex) ?: return
        val completed = current.completedSets[exercise.exercise.id] ?: 0
        val nextCount = (completed + 1).coerceAtMost(exercise.sets)
        _workout.value = current.copy(completedSets = current.completedSets + (exercise.exercise.id to nextCount))
        val profileId = uiState.value.profile?.id ?: return
        val previousSave = lastSetSaveJob
        lastSetSaveJob = viewModelScope.launch {
            previousSave?.join()
            container.repository.saveSet(
                profileId, current.templateId, exercise.exercise.id, nextCount, reps, loadKg, rpe,
                sessionId = current.sessionId,
            )
        }
        if (exercise.restSeconds > 0) startTimer(exercise.restSeconds)
    }

    fun nextExercise() {
        val current = _workout.value
        val updated = current.copy(
            currentExerciseIndex = (current.currentExerciseIndex + 1).coerceAtMost((current.exercises.size - 1).coerceAtLeast(0)),
            painReported = false,
        )
        _workout.value = updated
        persistWorkoutSession(updated)
    }

    /**
     * Clôture explicite de la séance du jour : le chrono est arrêté et la
     * session passe au statut COMPLETED (elle ne sera plus reprise ; un nouveau
     * passage sur la même séance repartira proprement).
     */
    fun finishWorkout() {
        timerJob?.cancel()
        timerEndsAt = 0L
        val updated = _workout.value.copy(
            timerSeconds = 0,
            timerRunning = false,
            timerEndsAtMillis = 0L,
            sessionCompleted = true,
        )
        _workout.value = updated
        persistWorkoutSession(updated)
    }

    fun restartWorkout() {
        val current = _workout.value
        val profileId = uiState.value.profile?.id ?: return
        if (current.sessionId.isBlank() || current.templateId.isBlank()) return
        _workout.value = current.copy(resetInProgress = true, restartMessage = null)
        viewModelScope.launch {
            runCatching {
                lastSetSaveJob?.join()
                container.repository.restartWorkoutAtomically(current.toEntity(profileId))
            }.onSuccess {
                timerJob?.cancel()
                _workout.value = current.copy(
                    currentExerciseIndex = 0,
                    completedSets = emptyMap(),
                    timerSeconds = 0,
                    timerRunning = false,
                    timerEndsAtMillis = 0L,
                    painReported = false,
                    startedAtMillis = System.currentTimeMillis(),
                    restartVersion = current.restartVersion + 1,
                    note = "",
                    restartMessage = "Séance recommencée. Seules les données de cette session ont été effacées.",
                    resetInProgress = false,
                )
            }.onFailure {
                _workout.value = current.copy(
                    restartMessage = "Échec du redémarrage : aucune donnée n’a été modifiée.",
                    resetInProgress = false,
                )
            }
        }
    }

    fun clearRestartMessage() {
        _workout.value = _workout.value.copy(restartMessage = null)
    }

    fun updateWorkoutNote(note: String) {
        val updated = _workout.value.copy(note = note)
        _workout.value = updated
        persistWorkoutSession(updated)
    }

    fun reportPain() {
        val updated = _workout.value.copy(painReported = true)
        _workout.value = updated
        persistWorkoutSession(updated)
    }

    fun startTimer(seconds: Int) {
        timerJob?.cancel()
        timerEndsAt = System.currentTimeMillis() + seconds * 1_000L
        val started = _workout.value.copy(
            timerSeconds = seconds,
            timerRunning = true,
            timerEndsAtMillis = timerEndsAt,
        )
        _workout.value = started
        persistWorkoutSession(started)
        timerJob = viewModelScope.launch {
            while (true) {
                val remaining = TimerMath.remainingSeconds(timerEndsAt, System.currentTimeMillis())
                _workout.value = _workout.value.copy(
                    timerSeconds = remaining,
                    timerRunning = remaining > 0,
                    timerEndsAtMillis = if (remaining > 0) timerEndsAt else 0L,
                )
                if (remaining <= 0) {
                    signalTimerEnd()
                    break
                }
                delay(250)
            }
        }
    }

    /**
     * Fin de repos : vibration et/ou signal sonore selon les préférences.
     * Corrige le fait que les réglages « Vibration du chronomètre » et
     * « Signal sonore » n'étaient reliés à aucun comportement.
     */
    private fun signalTimerEnd() {
        val preferences = uiState.value.preferences
        if (preferences.vibrationEnabled) {
            runCatching {
                val vibrator = if (android.os.Build.VERSION.SDK_INT >= 31) {
                    val manager = container.context.getSystemService(android.content.Context.VIBRATOR_MANAGER_SERVICE)
                        as android.os.VibratorManager
                    manager.defaultVibrator
                } else {
                    @Suppress("DEPRECATION")
                    container.context.getSystemService(android.content.Context.VIBRATOR_SERVICE)
                        as android.os.Vibrator
                }
                vibrator.vibrate(
                    android.os.VibrationEffect.createWaveform(
                        longArrayOf(0, 350, 150, 350, 150, 600),
                        -1,
                    ),
                )
            }
        }
        if (preferences.soundEnabled) {
            viewModelScope.launch {
                runCatching {
                    val tone = android.media.ToneGenerator(android.media.AudioManager.STREAM_NOTIFICATION, 85)
                    tone.startTone(android.media.ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 800)
                    delay(900)
                    tone.release()
                }
            }
        }
    }

    fun adjustTimer(deltaSeconds: Int) {
        if (timerEndsAt <= 0L) return
        val now = System.currentTimeMillis()
        timerEndsAt = TimerMath.adjust(timerEndsAt, deltaSeconds, now)
        val remaining = TimerMath.remainingSeconds(timerEndsAt, now)
        val updated = _workout.value.copy(
            timerSeconds = remaining,
            timerRunning = remaining > 0,
            timerEndsAtMillis = if (remaining > 0) timerEndsAt else 0L,
        )
        _workout.value = updated
        persistWorkoutSession(updated)
    }

    fun stopTimer() {
        timerJob?.cancel()
        timerEndsAt = 0L
        val updated = _workout.value.copy(timerSeconds = 0, timerRunning = false, timerEndsAtMillis = 0L)
        _workout.value = updated
        persistWorkoutSession(updated)
    }

    private fun persistWorkoutSession(state: ActiveWorkoutState) {
        val profileId = uiState.value.profile?.id ?: return
        if (state.sessionId.isBlank()) return
        viewModelScope.launch { container.repository.updateWorkoutSession(state.toEntity(profileId)) }
    }

    private fun ActiveWorkoutState.toEntity(profileId: String) = WorkoutSessionEntity(
        id = sessionId,
        profileId = profileId,
        templateId = templateId,
        length = length.name,
        startedAt = startedAtMillis,
        currentExerciseIndex = currentExerciseIndex,
        note = note,
        painReported = painReported,
        timerEndsAt = if (timerRunning) timerEndsAtMillis else 0L,
        status = if (sessionCompleted) "COMPLETED" else "ACTIVE",
    )

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = AppViewModel(container) as T
        }
    }
}
