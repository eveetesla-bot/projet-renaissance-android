package fr.projetrenaissance.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("renaissance_preferences")
private val mediaExerciseIds = listOf(
    "bike", "leg_press", "chest_press", "seated_row", "leg_curl", "lateral_raise",
    "calf_press", "hip_thrust", "leg_extension", "abductors", "dead_bug", "reverse_crunch",
)

data class UserPreferences(
    val activeProfileId: String? = null,
    val defaultRestSeconds: Int = 90,
    val soundEnabled: Boolean = false,
    val vibrationEnabled: Boolean = true,
    val soniaMedicalClearance: Boolean = false,
    val healthSyncEnabled: Boolean = false,
    val healthBackgroundSyncEnabled: Boolean = false,
    val healthWriteEnabled: Boolean = false,
    val sourcePriorities: Map<String, String> = emptyMap(),
    val machinePhotoUris: Map<String, String> = emptyMap(),
    val onboardingCompleted: Boolean = false,
    val lastHealthSyncAt: Long = 0L,
)

class UserPreferencesRepository(private val context: Context) {
    private object Keys {
        val profile = stringPreferencesKey("active_profile")
        val rest = intPreferencesKey("default_rest")
        val sound = booleanPreferencesKey("sound")
        val vibration = booleanPreferencesKey("vibration")
        val clearance = booleanPreferencesKey("sonia_clearance")
        val healthSync = booleanPreferencesKey("health_sync")
        val healthBackgroundSync = booleanPreferencesKey("health_background_sync")
        val healthWrite = booleanPreferencesKey("health_write")
        val onboardingCompleted = booleanPreferencesKey("onboarding_completed")
        val lastHealthSyncAt = androidx.datastore.preferences.core.longPreferencesKey("last_health_sync_at")
    }

    val preferences: Flow<UserPreferences> = context.dataStore.data.map { values ->
        val activeProfile = values[Keys.profile]
        UserPreferences(
            activeProfileId = activeProfile,
            defaultRestSeconds = values[Keys.rest] ?: 90,
            soundEnabled = values[Keys.sound] ?: false,
            vibrationEnabled = values[Keys.vibration] ?: true,
            soniaMedicalClearance = values[Keys.clearance] ?: false,
            healthSyncEnabled = values[Keys.healthSync] ?: false,
            healthBackgroundSyncEnabled = values[Keys.healthBackgroundSync] ?: false,
            healthWriteEnabled = values[Keys.healthWrite] ?: false,
            onboardingCompleted = values[Keys.onboardingCompleted] ?: false,
            lastHealthSyncAt = values[Keys.lastHealthSyncAt] ?: 0L,
            sourcePriorities = if (activeProfile == null) emptyMap() else listOf("SLEEP_SESSION", "STEPS", "EXERCISE_SESSION").mapNotNull { type ->
                values[stringPreferencesKey("health_priority_${activeProfile}_$type")]?.substringBefore(',')?.let { type to it }
            }.toMap(),
            machinePhotoUris = if (activeProfile == null) emptyMap() else mediaExerciseIds.mapNotNull { exerciseId ->
                values[stringPreferencesKey("machine_photo_${activeProfile}_$exerciseId")]?.let { exerciseId to it }
            }.toMap(),
        )
    }

    suspend fun selectProfile(id: String) = context.dataStore.edit { it[Keys.profile] = id }
    suspend fun setSound(enabled: Boolean) = context.dataStore.edit { it[Keys.sound] = enabled }
    suspend fun setVibration(enabled: Boolean) = context.dataStore.edit { it[Keys.vibration] = enabled }
    suspend fun setClearance(enabled: Boolean) = context.dataStore.edit { it[Keys.clearance] = enabled }
    suspend fun setHealthSync(enabled: Boolean) = context.dataStore.edit { it[Keys.healthSync] = enabled }
    suspend fun setHealthBackgroundSync(enabled: Boolean) = context.dataStore.edit { it[Keys.healthBackgroundSync] = enabled }
    suspend fun setHealthWrite(enabled: Boolean) = context.dataStore.edit { it[Keys.healthWrite] = enabled }
    suspend fun setOnboardingCompleted(completed: Boolean) = context.dataStore.edit { it[Keys.onboardingCompleted] = completed }
    suspend fun setLastHealthSyncAt(timestamp: Long) = context.dataStore.edit { it[Keys.lastHealthSyncAt] = timestamp }

    fun sourcePriority(profileId: String, recordType: String): Flow<List<String>> {
        val key = stringPreferencesKey("health_priority_${profileId}_$recordType")
        return context.dataStore.data.map { values -> values[key]?.split(',')?.filter(String::isNotBlank).orEmpty() }
    }

    suspend fun setSourcePriority(profileId: String, recordType: String, sources: List<String>) {
        val key = stringPreferencesKey("health_priority_${profileId}_$recordType")
        context.dataStore.edit { it[key] = sources.joinToString(",") }
    }

    suspend fun setMachinePhotoUri(profileId: String, exerciseId: String, uri: String?) {
        require(exerciseId in mediaExerciseIds) { "Exercice média inconnu : $exerciseId" }
        val key = stringPreferencesKey("machine_photo_${profileId}_$exerciseId")
        context.dataStore.edit { values ->
            if (uri.isNullOrBlank()) values.remove(key) else values[key] = uri
        }
    }

    suspend fun resetLocalProfile(profileId: String) {
        context.dataStore.edit { values ->
            listOf("SLEEP_SESSION", "STEPS", "EXERCISE_SESSION").forEach { type ->
                values.remove(stringPreferencesKey("health_priority_${profileId}_$type"))
            }
            mediaExerciseIds.forEach { exerciseId ->
                values.remove(stringPreferencesKey("machine_photo_${profileId}_$exerciseId"))
            }
            values.remove(Keys.healthSync)
            values.remove(Keys.healthBackgroundSync)
            values.remove(Keys.healthWrite)
            values.remove(Keys.onboardingCompleted)
            values.remove(Keys.lastHealthSyncAt)
            if (profileId == "sonia") values.remove(Keys.clearance)
        }
    }

    suspend fun resetToday(profileId: String) {
        context.dataStore.edit { values ->
            values.remove(stringPreferencesKey("today_state_$profileId"))
            values.remove(stringPreferencesKey("today_plan_$profileId"))
        }
    }

    suspend fun resetAll() {
        context.dataStore.edit { it.clear() }
    }
}
