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
class SessionChargeCleanupDeviceTest {
    @Test
    fun noMarkedTestSetRemainsWithoutMutatingRealHistory() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = Room.databaseBuilder(context, AppDatabase::class.java, "renaissance.db")
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
            .build()
        assertEquals(0, database.dao().allSetLogs().count { it.isTest })
        database.close()
    }
}
