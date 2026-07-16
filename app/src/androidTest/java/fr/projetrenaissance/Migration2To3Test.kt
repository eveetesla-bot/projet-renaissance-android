package fr.projetrenaissance

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import fr.projetrenaissance.data.AppDatabase
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class Migration2To3Test {
    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory(),
    )

    @Test
    fun migration2To3_marksExistingSetsAsReal() {
        helper.createDatabase(TEST_DATABASE, 2).apply {
            execSQL(
                """INSERT INTO set_logs
                   (id, profileId, templateId, exerciseId, setNumber, reps, loadKg, rpe, completedAt)
                   VALUES ('real', 'gerard', 'template', 'leg_press', 1, 10, 40.0, 7, 1)""",
            )
            close()
        }
        helper.runMigrationsAndValidate(TEST_DATABASE, 3, true, MIGRATION_2_3).use { database ->
            database.query("SELECT isTest FROM set_logs WHERE id = 'real'").use { cursor ->
                cursor.moveToFirst()
                assertEquals(0, cursor.getInt(0))
            }
        }
    }

    private companion object {
        const val TEST_DATABASE = "migration-2-3-test"
    }
}
