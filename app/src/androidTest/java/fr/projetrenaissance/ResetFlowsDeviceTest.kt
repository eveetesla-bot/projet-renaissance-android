package fr.projetrenaissance

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import fr.projetrenaissance.data.AppDatabase
import fr.projetrenaissance.data.BodyMetricEntity
import fr.projetrenaissance.data.DailyCheckInEntity
import fr.projetrenaissance.data.HealthRecordEntity
import fr.projetrenaissance.data.RenaissanceRepository
import fr.projetrenaissance.data.SetLogEntity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.ZoneId

@RunWith(AndroidJUnit4::class)
class ResetFlowsDeviceTest {
    @Test fun dayResetKeepsOlderHistoryAndHealth() = runBlocking {
        withDatabase { database ->
            val repository = RenaissanceRepository(database)
            repository.seedIfNeeded()
            val dao = database.dao()
            val zone = ZoneId.systemDefault()
            val today = LocalDate.now().atStartOfDay(zone).toInstant().toEpochMilli() + 1_000L
            val old = LocalDate.now().minusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() + 1_000L
            dao.insertSetLogs(listOf(set("today", today), set("old", old)))
            dao.insertCheckIns(listOf(
                DailyCheckInEntity("today-check", "gerard", today, 3, 3, 3, 0),
                DailyCheckInEntity("old-check", "gerard", old, 3, 3, 3, 0),
            ))
            dao.upsertHealthRecords(listOf(healthRecord()))

            repository.resetToday("gerard", today)

            assertEquals(listOf("old"), dao.allSetLogs().map { it.id })
            assertEquals(listOf("old-check"), dao.allCheckIns().map { it.id })
            assertEquals(1, dao.healthRecords("gerard").size)
        }
    }

    @Test fun profileResetNeverTouchesOtherProfile() = runBlocking {
        withDatabase { database ->
            val repository = RenaissanceRepository(database)
            repository.seedIfNeeded()
            database.dao().insertBodyMetrics(listOf(
                BodyMetricEntity("g", "gerard", 1L, 68.0),
                BodyMetricEntity("s", "sonia", 1L, 58.0),
            ))
            repository.resetProfileData("gerard")
            assertEquals(listOf("s"), database.dao().allBodyMetrics().map { it.id })
        }
    }

    @Test fun totalResetRemovesLocalDataAndEditorialSeedCanBeRestored() = runBlocking {
        withDatabase { database ->
            val repository = RenaissanceRepository(database)
            repository.seedIfNeeded()
            database.dao().insertSetLog(set("local", 1L))
            repository.resetAllData()
            assertEquals(0, database.dao().profileCount())
            assertEquals(0, database.dao().allSetLogs().size)
            repository.seedIfNeeded()
            assertEquals(2, database.dao().profileCount())
            assertEquals(0, database.dao().allSetLogs().size)
        }
    }

    private fun set(id: String, time: Long) =
        SetLogEntity(id, "gerard", "gerard_0_A", "leg_press", 1, 10, 40.0, 7, time)

    private fun healthRecord() = HealthRecordEntity(
        "gerard", "health", "STEPS", 1L, 2L, 100.0, null, "{}", "source",
        "Withings", "WITHINGS", null, null, null, 1, 2L, 2L, "key", true,
    )

    private suspend fun withDatabase(block: suspend (AppDatabase) -> Unit) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        try {
            block(database)
        } finally {
            database.close()
        }
    }
}
