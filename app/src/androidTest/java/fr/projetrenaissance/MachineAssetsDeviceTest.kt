package fr.projetrenaissance

import android.graphics.BitmapFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import fr.projetrenaissance.domain.ExerciseMediaCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MachineAssetsDeviceTest {
    @Test
    fun twelveMachineAssetsAreReadableAndShareTheMobileRatio() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals(12, ExerciseMediaCatalog.all.size)

        ExerciseMediaCatalog.all.forEach { media ->
            val name = requireNotNull(media.machine.localResource)
            val resourceId = context.resources.getIdentifier(name, "drawable", context.packageName)
            assertTrue("Ressource absente pour ${media.exerciseId}", resourceId != 0)
            val bitmap = BitmapFactory.decodeResource(context.resources, resourceId)
            assertNotNull("PNG illisible pour ${media.exerciseId}", bitmap)
            assertEquals(1200, bitmap.width)
            assertEquals(800, bitmap.height)
        }
    }
}
