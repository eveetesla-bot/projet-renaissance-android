package fr.projetrenaissance

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import fr.projetrenaissance.data.AppDatabase
import fr.projetrenaissance.data.BodyMetricEntity
import fr.projetrenaissance.data.DailyCheckInEntity
import fr.projetrenaissance.data.HealthRecordEntity
import fr.projetrenaissance.data.HealthSyncStateEntity
import fr.projetrenaissance.data.RenaissanceRepository
import fr.projetrenaissance.data.SetLogEntity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SoniaProfileIsolationDeviceTest {
    @Test fun cleanSeedContainsEditorialContentButNoPersonalData() = runBlocking {
        withDatabase { database ->
            val repository = RenaissanceRepository(database)
            repository.seedIfNeeded()
            val dao = database.dao()

            assertEquals(2, dao.profileCount())
            assertEquals(0, dao.allBodyMetrics().size)
            assertEquals(0, dao.allCheckIns().size)
            assertEquals(0, dao.allSetLogs().size)
            assertEquals(0, dao.healthRecords("gerard").size)
            assertEquals(0, dao.healthRecords("sonia").size)
        }
    }

    @Test fun resettingSoniaNeverDeletesGerardData() = runBlocking {
        withDatabase { database ->
            val repository = RenaissanceRepository(database)
            repository.seedIfNeeded()
            val dao = database.dao()
            listOf("gerard", "sonia").forEach { profile ->
                dao.insertBodyMetric(BodyMetricEntity("$profile-metric", profile, 1L, 60.0))
                dao.insertCheckIn(DailyCheckInEntity("$profile-check", profile, 1L, 3, 3, 3, 0))
                dao.insertSetLog(SetLogEntity("$profile-set", profile, "${profile}_0_A", "bike", 1, 10, 0.0, 7, 1L))
                dao.upsertHealthRecords(listOf(healthRecord(profile)))
                dao.upsertHealthSyncState(HealthSyncStateEntity(profile, "STEPS", "token", 1L, 1L, null, "GRANTED"))
            }

            repository.resetProfileData("sonia")

            assertEquals(1, dao.allBodyMetrics().count { it.profileId == "gerard" })
            assertEquals(0, dao.allBodyMetrics().count { it.profileId == "sonia" })
            assertEquals(1, dao.allCheckIns().count { it.profileId == "gerard" })
            assertEquals(0, dao.allCheckIns().count { it.profileId == "sonia" })
            assertEquals(1, dao.allSetLogs().count { it.profileId == "gerard" })
            assertEquals(0, dao.allSetLogs().count { it.profileId == "sonia" })
            assertEquals(1, dao.healthRecords("gerard").size)
            assertEquals(0, dao.healthRecords("sonia").size)
            assertEquals(2, dao.profileCount())
        }
    }

    @Test fun restartingWorkoutDeletesOnlyCurrentSessionWindow() = runBlocking {
        withDatabase { database ->
            val repository = RenaissanceRepository(database)
            repository.seedIfNeeded()
            val dao = database.dao()
            dao.insertSetLogs(
                listOf(
                    SetLogEntity("older", "gerard", "gerard_0_A", "leg_press", 1, 10, 30.0, 7, 900L),
                    SetLogEntity("current", "gerard", "gerard_0_A", "leg_press", 1, 10, 35.0, 7, 1_100L),
                    SetLogEntity("other-template", "gerard", "gerard_0_B", "hip_thrust", 1, 10, 35.0, 7, 1_100L),
                    SetLogEntity("sonia", "sonia", "sonia_0_A", "leg_press", 1, 10, 20.0, 7, 1_100L),
                ),
            )

            val deleted = repository.deleteWorkoutProgress("gerard", "gerard_0_A", 1_000L, 1_200L)

            assertEquals(1, deleted)
            assertEquals(setOf("older", "other-template", "sonia"), dao.allSetLogs().map { it.id }.toSet())
        }
    }

    private suspend fun withDatabase(block: suspend (AppDatabase) -> Unit) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        try {
            block(database)
        } finally {
            database.close()
        }
    }

    private fun healthRecord(profileId: String) = HealthRecordEntity(
        profileId = profileId,
        healthConnectId = "$profileId-steps",
        recordType = "STEPS",
        startTime = 1L,
        endTime = 2L,
        value = 100.0,
        secondaryValue = null,
        payloadJson = "{}",
        sourcePackage = "source.$profileId",
        sourceLabel = profileId,
        sourceCategory = "WITHINGS",
        deviceManufacturer = null,
        deviceModel = null,
        deviceType = null,
        recordingMethod = 1,
        lastModifiedAt = 2L,
        importedAt = 2L,
        dedupeKey = "$profileId-key",
        isPreferred = true,
    )
}
