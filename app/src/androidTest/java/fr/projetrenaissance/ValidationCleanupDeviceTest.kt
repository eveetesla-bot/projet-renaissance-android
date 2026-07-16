package fr.projetrenaissance

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import fr.projetrenaissance.data.AppDatabase
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ValidationCleanupDeviceTest {
    @Test
    fun knownTimerValidationSetIsRemovedWithoutDeletingOtherSets() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = Room.databaseBuilder(context, AppDatabase::class.java, "renaissance.db")
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
            .build()
        val candidates = database.dao().allSetLogs().filter {
            it.profileId == "gerard" &&
                it.loadKg == 0.0 &&
                it.reps == 10 &&
                it.rpe == 7 &&
                it.completedAt in 1_784_132_100_000L..1_784_132_700_000L
        }
        assertEquals(0, candidates.size)
        assertEquals(0, database.dao().allSetLogs().count { it.isTest })
        database.close()
    }
}
