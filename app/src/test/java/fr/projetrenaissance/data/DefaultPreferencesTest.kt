package fr.projetrenaissance.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DefaultPreferencesTest {
    @Test fun `clean install has no active profile health priority or clearance`() {
        val defaults = UserPreferences()

        assertNull(defaults.activeProfileId)
        assertTrue(defaults.sourcePriorities.isEmpty())
        assertTrue(defaults.machinePhotoUris.isEmpty())
        assertFalse(defaults.healthSyncEnabled)
        assertFalse(defaults.healthBackgroundSyncEnabled)
        assertFalse(defaults.healthWriteEnabled)
        assertFalse(defaults.soniaMedicalClearance)
    }
}
