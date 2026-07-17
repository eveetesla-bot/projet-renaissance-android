package fr.projetrenaissance

import android.graphics.BitmapFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import fr.projetrenaissance.domain.ExerciseMediaCatalog
import fr.projetrenaissance.domain.ExerciseMediaCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MachineAssetsDeviceTest {
    @Test
    fun twelveRealisticPrimaryAssetsAndThumbnailsAreReadable() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals(12, ExerciseMediaCatalog.all.size)

        ExerciseMediaCatalog.all.forEach { media ->
            val primary = media.assets.single { it.category == ExerciseMediaCategory.PRIMARY_VISUAL }
            listOf(primary.localPath to (1200 to 800), primary.localThumbnailPath to (600 to 400)).forEach { (name, dimensions) ->
                val resourceId = context.resources.getIdentifier(requireNotNull(name), "drawable", context.packageName)
                assertTrue("Ressource absente pour ${media.exerciseId}: $name", resourceId != 0)
                val bitmap = BitmapFactory.decodeResource(context.resources, resourceId)
                assertNotNull("Image illisible pour ${media.exerciseId}: $name", bitmap)
                assertEquals(dimensions.first, bitmap.width)
                assertEquals(dimensions.second, bitmap.height)
            }
        }
    }
}
