package fr.projetrenaissance

import android.app.Application
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import fr.projetrenaissance.data.AppDatabase
import fr.projetrenaissance.data.RenaissanceRepository
import fr.projetrenaissance.data.UserPreferencesRepository
import fr.projetrenaissance.data.health.HealthConnectGateway
import fr.projetrenaissance.data.health.HealthDataRepository

class RenaissanceApplication : Application() {
    val container by lazy {
        val database = Room.databaseBuilder(
            this,
            AppDatabase::class.java,
            "renaissance.db",
        ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4).build()
        val healthGateway = HealthConnectGateway(this)
        val preferences = UserPreferencesRepository(this)
        AppContainer(
            context = this,
            repository = RenaissanceRepository(database),
            preferences = preferences,
            healthGateway = healthGateway,
            healthData = HealthDataRepository(database, healthGateway, preferences),
        )
    }
}

internal val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """CREATE TABLE IF NOT EXISTS `health_records` (`profileId` TEXT NOT NULL, `healthConnectId` TEXT NOT NULL, `recordType` TEXT NOT NULL, `startTime` INTEGER NOT NULL, `endTime` INTEGER NOT NULL, `value` REAL, `secondaryValue` REAL, `payloadJson` TEXT NOT NULL, `sourcePackage` TEXT NOT NULL, `sourceLabel` TEXT NOT NULL, `sourceCategory` TEXT NOT NULL, `deviceManufacturer` TEXT, `deviceModel` TEXT, `deviceType` INTEGER, `recordingMethod` INTEGER NOT NULL, `lastModifiedAt` INTEGER NOT NULL, `importedAt` INTEGER NOT NULL, `dedupeKey` TEXT NOT NULL, `isPreferred` INTEGER NOT NULL, PRIMARY KEY(`profileId`, `healthConnectId`))""",
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_health_records_profileId_recordType_startTime` ON `health_records` (`profileId`, `recordType`, `startTime`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_health_records_profileId_sourcePackage` ON `health_records` (`profileId`, `sourcePackage`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_health_records_profileId_dedupeKey` ON `health_records` (`profileId`, `dedupeKey`)")
        db.execSQL(
            """CREATE TABLE IF NOT EXISTS `health_sync_state` (`profileId` TEXT NOT NULL, `recordType` TEXT NOT NULL, `changesToken` TEXT, `lastAttemptAt` INTEGER, `lastSuccessAt` INTEGER, `lastError` TEXT, `permissionState` TEXT NOT NULL, PRIMARY KEY(`profileId`, `recordType`))""",
        )
    }
}

internal val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `set_logs` ADD COLUMN `isTest` INTEGER NOT NULL DEFAULT 0")
    }
}

internal val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `set_logs` ADD COLUMN `sessionId` TEXT NOT NULL DEFAULT ''")
        db.execSQL(
            """CREATE TABLE IF NOT EXISTS `workout_sessions` (`id` TEXT NOT NULL, `profileId` TEXT NOT NULL, `templateId` TEXT NOT NULL, `length` TEXT NOT NULL, `startedAt` INTEGER NOT NULL, `currentExerciseIndex` INTEGER NOT NULL, `note` TEXT NOT NULL, `painReported` INTEGER NOT NULL, `timerEndsAt` INTEGER NOT NULL, `status` TEXT NOT NULL, PRIMARY KEY(`id`))""",
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_workout_sessions_profileId_templateId_status` ON `workout_sessions` (`profileId`, `templateId`, `status`)")
    }
}

data class AppContainer(
    val context: Application,
    val repository: RenaissanceRepository,
    val preferences: UserPreferencesRepository,
    val healthGateway: HealthConnectGateway,
    val healthData: HealthDataRepository,
)
