package fr.projetrenaissance

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import fr.projetrenaissance.data.UserPreferencesRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UserMachinePhotoPreferencesDeviceTest {
    @Test
    fun mediaResetRulesAreProfileScoped() = runBlocking {
        val testContext = InstrumentationRegistry.getInstrumentation().targetContext
        val preferences = UserPreferencesRepository(testContext)
        preferences.resetAll()

        preferences.selectProfile("gerard")
        preferences.setMachinePhotoUri("gerard", "leg_press", "file:///gerard/leg_press.jpg")
        preferences.setMachinePhotoUri("sonia", "leg_press", "content://sonia/leg_press")
        preferences.resetToday("gerard")
        assertEquals("file:///gerard/leg_press.jpg", preferences.preferences.first().machinePhotoUris["leg_press"])

        preferences.resetLocalProfile("gerard")
        assertTrue(preferences.preferences.first().machinePhotoUris.isEmpty())

        preferences.selectProfile("sonia")
        assertEquals("content://sonia/leg_press", preferences.preferences.first().machinePhotoUris["leg_press"])

        preferences.resetAll()
        assertTrue(preferences.preferences.first().machinePhotoUris.isEmpty())
    }
}
