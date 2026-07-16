package fr.projetrenaissance

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import fr.projetrenaissance.data.AppDatabase
import fr.projetrenaissance.data.RenaissanceRepository
import fr.projetrenaissance.data.SetLogEntity
import fr.projetrenaissance.data.WorkoutSessionEntity
import fr.projetrenaissance.domain.SessionLength
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WorkoutRestartDeviceTest {
    @Test fun emptySessionRestartsCleanly() = runBlocking {
        withRepository { repository, database ->
            val session = session("empty", note = "à effacer")
            database.dao().upsertWorkoutSession(session)
            assertEquals(0, repository.restartWorkoutAtomically(session))
            assertReset(database, session.id)
        }
    }

    @Test fun oneSetIsDeleted() = runBlocking {
        withRepository { repository, database ->
            val session = session("one")
            database.dao().upsertWorkoutSession(session)
            database.dao().insertSetLog(set("one-set", session.id))
            assertEquals(1, repository.restartWorkoutAtomically(session))
            assertReset(database, session.id)
        }
    }

    @Test fun multipleSetsAndTheirRpeAreDeleted() = runBlocking {
        withRepository { repository, database ->
            val session = session("multi")
            database.dao().upsertWorkoutSession(session)
            database.dao().insertSetLogs((1..4).map { set("set-$it", session.id, rpe = 5 + it) })
            assertEquals(4, repository.restartWorkoutAtomically(session))
            assertTrue(database.dao().setLogsForSession(session.id).isEmpty())
        }
    }

    @Test fun runningRestAndNoteAreReset() = runBlocking {
        withRepository { repository, database ->
            val session = session("timer", note = "commentaire", timerEndsAt = Long.MAX_VALUE)
            database.dao().upsertWorkoutSession(session)
            repository.restartWorkoutAtomically(session)
            assertReset(database, session.id)
        }
    }

    @Test fun closingAndReopeningResumesTheSameSession() = runBlocking {
        withRepository { repository, _ ->
            val first = repository.openOrResumeWorkout("gerard", "gerard_0_A", SessionLength.FULL)
            val reopened = repository.openOrResumeWorkout("gerard", "gerard_0_A", SessionLength.FULL)
            assertEquals(first.session.id, reopened.session.id)
        }
    }

    @Test fun olderHistoryAndOtherProfilesRemainUntouched() = runBlocking {
        withRepository { repository, database ->
            val current = session("current")
            database.dao().upsertWorkoutSession(current)
            database.dao().insertSetLogs(
                listOf(
                    set("current-set", current.id),
                    set("old-history", "older-session"),
                    set("sonia-history", "sonia-session", profileId = "sonia"),
                ),
            )
            repository.restartWorkoutAtomically(current)
            assertEquals(setOf("old-history", "sonia-history"), database.dao().allSetLogs().map { it.id }.toSet())
        }
    }

    private suspend fun assertReset(database: AppDatabase, sessionId: String) {
        assertTrue(database.dao().setLogsForSession(sessionId).isEmpty())
        val state = database.dao().activeWorkoutSession("gerard", "gerard_0_A")!!
        assertEquals(0, state.currentExerciseIndex)
        assertEquals("", state.note)
        assertEquals(0L, state.timerEndsAt)
        assertEquals(false, state.painReported)
    }

    private fun session(id: String, note: String = "", timerEndsAt: Long = 0L) = WorkoutSessionEntity(
        id = id,
        profileId = "gerard",
        templateId = "gerard_0_A",
        length = SessionLength.FULL.name,
        startedAt = 1L,
        currentExerciseIndex = 2,
        note = note,
        painReported = true,
        timerEndsAt = timerEndsAt,
        status = "ACTIVE",
    )

    private fun set(id: String, sessionId: String, rpe: Int = 7, profileId: String = "gerard") = SetLogEntity(
        id, profileId, if (profileId == "gerard") "gerard_0_A" else "sonia_0_A",
        "leg_press", 1, 10, 40.0, rpe, 2L, sessionId = sessionId,
    )

    private suspend fun withRepository(block: suspend (RenaissanceRepository, AppDatabase) -> Unit) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        try {
            val repository = RenaissanceRepository(database)
            repository.seedIfNeeded()
            block(repository, database)
        } finally {
            database.close()
        }
    }
}
