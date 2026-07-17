package fr.projetrenaissance.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExerciseMediaCatalogTest {
    @Test fun `catalog contains the twelve realistic offline primary visuals`() {
        val expected = setOf("bike", "leg_press", "chest_press", "seated_row", "leg_curl", "lateral_raise", "calf_press", "hip_thrust", "leg_extension", "abductors", "dead_bug", "reverse_crunch")
        assertEquals(expected, ExerciseMediaCatalog.all.map { it.exerciseId }.toSet())
        assertTrue(ExerciseMediaCatalog.all.all { it.guidedIllustration.startPosition.isNotBlank() })
        assertTrue(ExerciseMediaCatalog.all.all { it.machine.verificationStatus == MediaVerificationStatus.VERIFIED })
        assertTrue(ExerciseMediaCatalog.all.all {
            it.machine.localResource == "primary_${it.exerciseId}" &&
                it.machine.userPhotoUri == null &&
                it.machine.source == "Rendu original quasi-photoréaliste Projet Renaissance"
        })
        assertTrue(ExerciseMediaCatalog.all.all { media ->
            media.assets.map { it.category }.toSet() == ExerciseMediaCategory.entries.toSet()
        })
        assertTrue(ExerciseMediaCatalog.all.all { media ->
            media.assets.single { it.category == ExerciseMediaCategory.PRIMARY_VISUAL }.let {
                it.mediaType == ExerciseAssetType.LOCAL_IMAGE &&
                    it.localPath == "primary_${media.exerciseId}" &&
                    it.localThumbnailPath == "thumb_${media.exerciseId}" &&
                    it.isVerified && it.canUseOffline && it.isPrimaryVisual
            }
        })
        assertTrue(ExerciseMediaCatalog.all.all { it.verifiedVideoUrl == null && it.videoReference == null })
        assertTrue(ExerciseMediaCatalog.all.all { media ->
            media.assets.single { it.category == ExerciseMediaCategory.VIDEO }.let {
                it.mediaType == ExerciseAssetType.PLACEHOLDER && !it.isVerified && it.externalUrl == null
            }
        })
        assertTrue(ExerciseMediaCatalog.all.all { it.accessibilityDescription.isNotBlank() })
    }

    @Test fun `floor exercises never claim a machine photo`() {
        val floorIds = setOf("dead_bug", "reverse_crunch")
        ExerciseMediaCatalog.all.forEach { media ->
            val machine = media.assets.single { it.category == ExerciseMediaCategory.MACHINE_VISUAL }
            if (media.exerciseId in floorIds) {
                assertEquals(ExerciseAssetType.PLACEHOLDER, machine.mediaType)
                assertNull(machine.localPath)
            } else {
                assertEquals(ExerciseAssetType.LOCAL_IMAGE, machine.mediaType)
                assertTrue(machine.isMachinePhoto)
            }
        }
    }

    @Test fun `unknown exercise never receives an invented media url`() {
        assertNull(ExerciseMediaCatalog.forExercise("unknown"))
    }
}
