package fr.projetrenaissance.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.Index
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "profiles", primaryKeys = ["id"])
data class ProfileEntity(
    val id: String,
    val displayName: String,
    val age: Int,
    val goal: String,
    val targetWeight: String,
    val healthNotes: String,
)

@Entity(tableName = "exercises", primaryKeys = ["id"])
data class ExerciseEntity(
    val id: String,
    val name: String,
    val muscles: String,
    val machineSetup: String,
    val commonErrors: String,
    val alternative: String,
    val shoulderLoad: String = "NONE",
    val requiresClearance: Boolean = false,
)

@Entity(tableName = "workout_templates", primaryKeys = ["id"])
data class WorkoutTemplateEntity(
    val id: String,
    val profileId: String,
    val weekFrom: Int,
    val weekTo: Int,
    val code: String,
    val title: String,
    val intent: String,
)

@Entity(tableName = "workout_exercises", primaryKeys = ["templateId", "position"])
data class WorkoutExerciseEntity(
    val templateId: String,
    val position: Int,
    val exerciseId: String,
    val sets: Int,
    val reps: String,
    val restSeconds: Int,
    val tempo: String,
    val rpe: String,
)

@Entity(tableName = "body_metrics", primaryKeys = ["id"])
data class BodyMetricEntity(
    val id: String,
    val profileId: String,
    val recordedAt: Long,
    val weightKg: Double,
)

@Entity(tableName = "daily_check_ins", primaryKeys = ["id"])
data class DailyCheckInEntity(
    val id: String,
    val profileId: String,
    val recordedAt: Long,
    val energy: Int,
    val sleep: Int,
    val mood: Int,
    val pain: Int,
)

@Entity(tableName = "set_logs", primaryKeys = ["id"])
data class SetLogEntity(
    val id: String,
    val profileId: String,
    val templateId: String,
    val exerciseId: String,
    val setNumber: Int,
    val reps: Int,
    val loadKg: Double,
    val rpe: Int,
    val completedAt: Long,
    val isTest: Boolean = false,
    val sessionId: String = "",
)

@Entity(
    tableName = "workout_sessions",
    indices = [
        Index(value = ["profileId", "templateId", "status"]),
    ],
)
data class WorkoutSessionEntity(
    @androidx.room.PrimaryKey val id: String,
    val profileId: String,
    val templateId: String,
    val length: String,
    val startedAt: Long,
    val currentExerciseIndex: Int,
    val note: String,
    val painReported: Boolean,
    val timerEndsAt: Long,
    val status: String,
)

@Entity(
    tableName = "health_records",
    primaryKeys = ["profileId", "healthConnectId"],
    indices = [
        Index(value = ["profileId", "recordType", "startTime"]),
        Index(value = ["profileId", "sourcePackage"]),
        Index(value = ["profileId", "dedupeKey"]),
    ],
)
data class HealthRecordEntity(
    val profileId: String,
    val healthConnectId: String,
    val recordType: String,
    val startTime: Long,
    val endTime: Long,
    val value: Double?,
    val secondaryValue: Double?,
    val payloadJson: String,
    val sourcePackage: String,
    val sourceLabel: String,
    val sourceCategory: String,
    val deviceManufacturer: String?,
    val deviceModel: String?,
    val deviceType: Int?,
    val recordingMethod: Int,
    val lastModifiedAt: Long,
    val importedAt: Long,
    val dedupeKey: String,
    val isPreferred: Boolean,
)

@Entity(tableName = "health_sync_state", primaryKeys = ["profileId", "recordType"])
data class HealthSyncStateEntity(
    val profileId: String,
    val recordType: String,
    val changesToken: String?,
    val lastAttemptAt: Long?,
    val lastSuccessAt: Long?,
    val lastError: String?,
    val permissionState: String,
)

data class PlannedExercise(
    val position: Int,
    val sets: Int,
    val reps: String,
    val restSeconds: Int,
    val tempo: String,
    val rpe: String,
    val exercise: ExerciseEntity,
)

@Dao
interface RenaissanceDao {
    @Query("SELECT COUNT(*) FROM profiles")
    suspend fun profileCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfiles(items: List<ProfileEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExercises(items: List<ExerciseEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplates(items: List<WorkoutTemplateEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkoutExercises(items: List<WorkoutExerciseEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBodyMetric(item: BodyMetricEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCheckIn(item: DailyCheckInEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSetLog(item: SetLogEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBodyMetrics(items: List<BodyMetricEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCheckIns(items: List<DailyCheckInEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSetLogs(items: List<SetLogEntity>)

    @Query("SELECT * FROM profiles WHERE id = :id")
    fun observeProfile(id: String): Flow<ProfileEntity?>

    @Query("SELECT * FROM workout_templates WHERE profileId = :profileId ORDER BY weekFrom, code")
    fun observeTemplates(profileId: String): Flow<List<WorkoutTemplateEntity>>

    @Query("SELECT * FROM exercises ORDER BY name")
    fun observeExercises(): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM workout_exercises WHERE templateId = :templateId ORDER BY position")
    suspend fun workoutRows(templateId: String): List<WorkoutExerciseEntity>

    @Query("SELECT * FROM exercises WHERE id IN (:ids)")
    suspend fun exercisesByIds(ids: List<String>): List<ExerciseEntity>

    @Query("SELECT * FROM body_metrics WHERE profileId = :profileId ORDER BY recordedAt")
    fun observeMetrics(profileId: String): Flow<List<BodyMetricEntity>>

    @Query("SELECT * FROM daily_check_ins WHERE profileId = :profileId ORDER BY recordedAt DESC LIMIT 1")
    fun observeLatestCheckIn(profileId: String): Flow<DailyCheckInEntity?>

    @Query("SELECT * FROM daily_check_ins WHERE profileId = :profileId ORDER BY recordedAt")
    fun observeCheckIns(profileId: String): Flow<List<DailyCheckInEntity>>

    @Query("SELECT * FROM set_logs WHERE profileId = :profileId ORDER BY completedAt")
    fun observeSetLogs(profileId: String): Flow<List<SetLogEntity>>

    @Query(
        """DELETE FROM set_logs
           WHERE profileId = 'gerard'
             AND loadKg = 0.0
             AND reps = 10
             AND rpe = 7
             AND completedAt BETWEEN :startMillis AND :endMillis""",
    )
    suspend fun deleteKnownTimerValidationSet(startMillis: Long, endMillis: Long): Int

    @Query("DELETE FROM set_logs WHERE isTest = 1")
    suspend fun deleteMarkedTestSets(): Int

    @Query("DELETE FROM set_logs WHERE id = :id")
    suspend fun deleteSetLogById(id: String): Int

    @Query("SELECT * FROM set_logs WHERE sessionId = :sessionId ORDER BY completedAt")
    suspend fun setLogsForSession(sessionId: String): List<SetLogEntity>

    @Query("DELETE FROM body_metrics WHERE id IN ('g1', 'g2', 'g3', 's1', 's2', 's3')")
    suspend fun deleteLegacyDemoMetrics()

    @Query("DELETE FROM body_metrics WHERE profileId = :profileId")
    suspend fun deleteBodyMetrics(profileId: String): Int

    @Query("DELETE FROM daily_check_ins WHERE profileId = :profileId")
    suspend fun deleteCheckIns(profileId: String): Int

    @Query("DELETE FROM set_logs WHERE profileId = :profileId")
    suspend fun deleteSetLogs(profileId: String): Int

    @Query(
        """DELETE FROM set_logs
           WHERE profileId = :profileId
             AND templateId = :templateId
             AND completedAt BETWEEN :startedAt AND :endedAt""",
    )
    suspend fun deleteWorkoutSetsInWindow(
        profileId: String,
        templateId: String,
        startedAt: Long,
        endedAt: Long,
    ): Int

    @Query("DELETE FROM set_logs WHERE sessionId = :sessionId")
    suspend fun deleteWorkoutSetsBySession(sessionId: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertWorkoutSession(item: WorkoutSessionEntity)

    @Query(
        """SELECT * FROM workout_sessions
           WHERE profileId = :profileId AND templateId = :templateId AND status = 'ACTIVE'
           ORDER BY startedAt DESC LIMIT 1""",
    )
    suspend fun activeWorkoutSession(profileId: String, templateId: String): WorkoutSessionEntity?

    @Query("DELETE FROM workout_sessions WHERE profileId = :profileId")
    suspend fun deleteWorkoutSessions(profileId: String): Int

    @Query("DELETE FROM workout_sessions WHERE profileId = :profileId AND startedAt BETWEEN :startedAt AND :endedAt")
    suspend fun deleteWorkoutSessionsInWindow(profileId: String, startedAt: Long, endedAt: Long): Int

    @Query("DELETE FROM set_logs WHERE profileId = :profileId AND completedAt BETWEEN :startedAt AND :endedAt")
    suspend fun deleteSetLogsInWindow(profileId: String, startedAt: Long, endedAt: Long): Int

    @Query("DELETE FROM daily_check_ins WHERE profileId = :profileId AND recordedAt BETWEEN :startedAt AND :endedAt")
    suspend fun deleteCheckInsInWindow(profileId: String, startedAt: Long, endedAt: Long): Int

    @Query("DELETE FROM body_metrics WHERE profileId = :profileId AND recordedAt BETWEEN :startedAt AND :endedAt")
    suspend fun deleteBodyMetricsInWindow(profileId: String, startedAt: Long, endedAt: Long): Int

    @Query("DELETE FROM body_metrics")
    suspend fun deleteEveryBodyMetric()

    @Query("DELETE FROM daily_check_ins")
    suspend fun deleteEveryCheckIn()

    @Query("DELETE FROM set_logs")
    suspend fun deleteEverySetLog()

    @Query("DELETE FROM workout_sessions")
    suspend fun deleteEveryWorkoutSession()

    @Query("DELETE FROM health_records")
    suspend fun deleteEveryHealthRecord()

    @Query("DELETE FROM health_sync_state")
    suspend fun deleteEveryHealthSyncState()

    @Query("DELETE FROM workout_exercises")
    suspend fun deleteEveryWorkoutExercise()

    @Query("DELETE FROM workout_templates")
    suspend fun deleteEveryWorkoutTemplate()

    @Query("DELETE FROM exercises")
    suspend fun deleteEveryExercise()

    @Query("DELETE FROM profiles")
    suspend fun deleteEveryProfile()

    @Query("SELECT * FROM body_metrics ORDER BY recordedAt")
    suspend fun allBodyMetrics(): List<BodyMetricEntity>

    @Query("SELECT * FROM daily_check_ins ORDER BY recordedAt")
    suspend fun allCheckIns(): List<DailyCheckInEntity>

    @Query("SELECT * FROM set_logs ORDER BY completedAt")
    suspend fun allSetLogs(): List<SetLogEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertHealthRecords(items: List<HealthRecordEntity>)

    @Query("DELETE FROM health_records WHERE profileId = :profileId AND healthConnectId = :healthConnectId")
    suspend fun deleteHealthRecord(profileId: String, healthConnectId: String)

    @Query("DELETE FROM health_records WHERE profileId = :profileId AND sourceCategory != 'RENAISSANCE'")
    suspend fun deleteImportedHealthRecords(profileId: String)

    @Query("DELETE FROM health_records WHERE profileId = :profileId")
    suspend fun deleteAllHealthRecords(profileId: String): Int

    @Query("SELECT * FROM health_records WHERE profileId = :profileId ORDER BY startTime DESC")
    fun observeHealthRecords(profileId: String): Flow<List<HealthRecordEntity>>

    @Query("SELECT * FROM health_records WHERE profileId = :profileId ORDER BY startTime DESC")
    suspend fun healthRecords(profileId: String): List<HealthRecordEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertHealthSyncState(item: HealthSyncStateEntity)

    @Query("SELECT * FROM health_sync_state WHERE profileId = :profileId AND recordType = :recordType")
    suspend fun healthSyncState(profileId: String, recordType: String): HealthSyncStateEntity?

    @Query("SELECT * FROM health_sync_state WHERE profileId = :profileId ORDER BY recordType")
    fun observeHealthSyncStates(profileId: String): Flow<List<HealthSyncStateEntity>>

    @Query("DELETE FROM health_sync_state WHERE profileId = :profileId")
    suspend fun deleteHealthSyncStates(profileId: String)
}

@Database(
    entities = [
        ProfileEntity::class,
        ExerciseEntity::class,
        WorkoutTemplateEntity::class,
        WorkoutExerciseEntity::class,
        BodyMetricEntity::class,
        DailyCheckInEntity::class,
        SetLogEntity::class,
        WorkoutSessionEntity::class,
        HealthRecordEntity::class,
        HealthSyncStateEntity::class,
    ],
    version = 4,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dao(): RenaissanceDao
}
