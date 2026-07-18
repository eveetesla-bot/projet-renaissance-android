package fr.projetrenaissance.presentation

import android.graphics.BitmapFactory
import android.net.Uri
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddAPhoto
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import fr.projetrenaissance.R
import fr.projetrenaissance.data.ExerciseEntity
import fr.projetrenaissance.domain.ExerciseMediaCatalog
import fr.projetrenaissance.domain.ExerciseMediaCategory
import fr.projetrenaissance.domain.ExerciseAssetType
import kotlinx.coroutines.delay
import java.io.File
import java.io.FileOutputStream
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ExerciseMediaThumbnail(exerciseId: String, profileId: String? = null, onOpen: () -> Unit) {
    if (ExerciseMediaCatalog.forExercise(exerciseId) == null && exerciseArtFor(exerciseId, profileId) == null) return
    Card(
        onClick = onOpen,
        modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(3.dp),
    ) {
        Column {
            PrimaryMediaImage(exerciseId, Modifier.fillMaxWidth().aspectRatio(3f / 2f), thumbnail = true, profileId = profileId)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Mouvement réaliste · hors connexion", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(12.dp))
                Text("OUVRIR", color = Copper, fontWeight = FontWeight.Bold)
            }
        }
    }
}

private data class ExerciseArt(@DrawableRes val full: Int, @DrawableRes val thumb: Int)

// Pack d'illustrations éditoriales personnalisées (variante par profil :
// Gérard tatouage avant-bras gauche, Sonia épaule droite neutre).
private val gerardArt = mapOf(
    "leg_press" to ExerciseArt(R.drawable.ex_g_leg_press, R.drawable.ex_g_leg_press_t),
    "chest_press" to ExerciseArt(R.drawable.ex_g_chest_press, R.drawable.ex_g_chest_press_t),
    "seated_row" to ExerciseArt(R.drawable.ex_g_seated_row, R.drawable.ex_g_seated_row_t),
    "leg_curl" to ExerciseArt(R.drawable.ex_g_leg_curl, R.drawable.ex_g_leg_curl_t),
    "lateral_raise" to ExerciseArt(R.drawable.ex_g_lateral_raise, R.drawable.ex_g_lateral_raise_t),
    "triceps_rope" to ExerciseArt(R.drawable.ex_g_triceps_rope, R.drawable.ex_g_triceps_rope_t),
    "calf_press" to ExerciseArt(R.drawable.ex_g_calf_press, R.drawable.ex_g_calf_press_t),
    "hip_thrust" to ExerciseArt(R.drawable.ex_g_hip_thrust, R.drawable.ex_g_hip_thrust_t),
    "lat_pulldown" to ExerciseArt(R.drawable.ex_g_lat_pulldown, R.drawable.ex_g_lat_pulldown_t),
    "incline_press" to ExerciseArt(R.drawable.ex_g_incline_press, R.drawable.ex_g_incline_press_t),
    "leg_extension" to ExerciseArt(R.drawable.ex_g_leg_extension, R.drawable.ex_g_leg_extension_t),
    "reverse_fly" to ExerciseArt(R.drawable.ex_g_reverse_fly, R.drawable.ex_g_reverse_fly_t),
    "biceps_curl" to ExerciseArt(R.drawable.ex_g_biceps_curl, R.drawable.ex_g_biceps_curl_t),
    "dead_bug" to ExerciseArt(R.drawable.ex_g_dead_bug, R.drawable.ex_g_dead_bug_t),
    "chest_row" to ExerciseArt(R.drawable.ex_g_chest_row, R.drawable.ex_g_chest_row_t),
    "shoulder_press" to ExerciseArt(R.drawable.ex_g_shoulder_press, R.drawable.ex_g_shoulder_press_t),
    "back_extension" to ExerciseArt(R.drawable.ex_g_back_extension, R.drawable.ex_g_back_extension_t),
)

private val soniaArt = mapOf(
    "bike" to ExerciseArt(R.drawable.ex_s_bike, R.drawable.ex_s_bike_t),
    "leg_press" to ExerciseArt(R.drawable.ex_s_leg_press, R.drawable.ex_s_leg_press_t),
    "leg_curl" to ExerciseArt(R.drawable.ex_s_leg_curl, R.drawable.ex_s_leg_curl_t),
    "abductors" to ExerciseArt(R.drawable.ex_s_abductors, R.drawable.ex_s_abductors_t),
    "calf_press" to ExerciseArt(R.drawable.ex_s_calf_press, R.drawable.ex_s_calf_press_t),
    "dead_bug" to ExerciseArt(R.drawable.ex_s_dead_bug, R.drawable.ex_s_dead_bug_t),
    "glute_bridge" to ExerciseArt(R.drawable.ex_s_glute_bridge, R.drawable.ex_s_glute_bridge_t),
    "leg_extension" to ExerciseArt(R.drawable.ex_s_leg_extension, R.drawable.ex_s_leg_extension_t),
    "adductors" to ExerciseArt(R.drawable.ex_s_adductors, R.drawable.ex_s_adductors_t),
    "step_up" to ExerciseArt(R.drawable.ex_s_step_up, R.drawable.ex_s_step_up_t),
    "reverse_crunch" to ExerciseArt(R.drawable.ex_s_reverse_crunch, R.drawable.ex_s_reverse_crunch_t),
    "breathing_reset" to ExerciseArt(R.drawable.ex_s_breathing_reset, R.drawable.ex_s_breathing_reset_t),
    "chest_row" to ExerciseArt(R.drawable.ex_s_chest_row, R.drawable.ex_s_chest_row_t),
    "hip_thrust" to ExerciseArt(R.drawable.ex_s_hip_thrust, R.drawable.ex_s_hip_thrust_t),
    "chest_press" to ExerciseArt(R.drawable.ex_s_chest_press, R.drawable.ex_s_chest_press_t),
)

/**
 * Illustration de l'exercice pour le profil actif : variante Sonia pour son
 * espace, variante Gérard sinon ; repli sur la variante de l'autre profil
 * quand une seule existe. Null si aucune carte du pack ne couvre l'exercice.
 */
@DrawableRes
fun exerciseArtFor(exerciseId: String, profileId: String?, thumbnail: Boolean = false): Int? {
    val art = if (profileId == "sonia") soniaArt[exerciseId] ?: gerardArt[exerciseId]
    else gerardArt[exerciseId] ?: soniaArt[exerciseId]
    return art?.let { if (thumbnail) it.thumb else it.full }
}

@DrawableRes
fun primaryDrawableFor(exerciseId: String, thumbnail: Boolean = false): Int = when (exerciseId) {
    "bike" -> if (thumbnail) R.drawable.thumb_bike else R.drawable.primary_bike
    "leg_press" -> if (thumbnail) R.drawable.thumb_leg_press else R.drawable.primary_leg_press
    "chest_press" -> if (thumbnail) R.drawable.thumb_chest_press else R.drawable.primary_chest_press
    "seated_row" -> if (thumbnail) R.drawable.thumb_seated_row else R.drawable.primary_seated_row
    "leg_curl" -> if (thumbnail) R.drawable.thumb_leg_curl else R.drawable.primary_leg_curl
    "lateral_raise" -> if (thumbnail) R.drawable.thumb_lateral_raise else R.drawable.primary_lateral_raise
    "calf_press" -> if (thumbnail) R.drawable.thumb_calf_press else R.drawable.primary_calf_press
    "hip_thrust" -> if (thumbnail) R.drawable.thumb_hip_thrust else R.drawable.primary_hip_thrust
    "leg_extension" -> if (thumbnail) R.drawable.thumb_leg_extension else R.drawable.primary_leg_extension
    "abductors" -> if (thumbnail) R.drawable.thumb_abductors else R.drawable.primary_abductors
    "dead_bug" -> if (thumbnail) R.drawable.thumb_dead_bug else R.drawable.primary_dead_bug
    "reverse_crunch" -> if (thumbnail) R.drawable.thumb_reverse_crunch else R.drawable.primary_reverse_crunch
    else -> if (thumbnail) R.drawable.thumb_bike else R.drawable.primary_bike
}

@Composable
fun PrimaryMediaImage(
    exerciseId: String,
    modifier: Modifier = Modifier,
    thumbnail: Boolean = false,
    profileId: String? = null,
) {
    val media = ExerciseMediaCatalog.forExercise(exerciseId)
    val drawable = exerciseArtFor(exerciseId, profileId, thumbnail)
        ?: primaryDrawableFor(exerciseId, thumbnail)
    Image(
        painter = painterResource(drawable),
        contentDescription = media?.accessibilityDescription,
        modifier = modifier,
        contentScale = ContentScale.Crop,
    )
}

@Composable
fun MachineAssetImage(
    exerciseId: String,
    modifier: Modifier = Modifier,
    userPhotoUri: String? = ExerciseMediaCatalog.forExercise(exerciseId)?.machine?.userPhotoUri,
) {
    val media = ExerciseMediaCatalog.forExercise(exerciseId)
    val context = LocalContext.current
    val userPhoto = remember(userPhotoUri) {
        userPhotoUri?.let { value ->
            runCatching {
                val uri = Uri.parse(value)
                val bitmap = if (uri.scheme == "file") BitmapFactory.decodeFile(uri.path)
                else context.contentResolver.openInputStream(uri)?.use(BitmapFactory::decodeStream)
                bitmap?.asImageBitmap()
            }.getOrNull()
        }
    }
    if (userPhoto != null) {
        Image(
            bitmap = userPhoto,
            contentDescription = media?.let { "Photo personnelle de ${it.machine.genericName}" },
            modifier = modifier,
            contentScale = ContentScale.Crop,
        )
    } else {
        // Pas de doublon du visuel « mouvement » : tant qu'aucune photo
        // personnelle n'est ajoutée, on affiche une invite dédiée. La vue
        // machine sert à photographier l'appareil réel de sa salle.
        MachinePhotoPlaceholder(modifier)
    }
}

@Composable
private fun MachinePhotoPlaceholder(modifier: Modifier = Modifier) {
    Box(modifier.background(SoftSage), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(24.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.AddAPhoto,
                contentDescription = null,
                tint = Sage,
                modifier = Modifier.size(44.dp),
            )
            Text(
                "Photographiez la machine de votre salle",
                style = MaterialTheme.typography.titleMedium,
                color = DeepNavy,
                textAlign = TextAlign.Center,
            )
            Text(
                "Votre photo remplacera cet emplacement et vous aidera à retrouver le bon appareil.",
                style = MaterialTheme.typography.bodySmall,
                color = SoftGray,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
fun MachineAssetThumbnail(exerciseId: String, userPhotoUri: String? = null, onOpen: () -> Unit) {
    val media = ExerciseMediaCatalog.forExercise(exerciseId) ?: return
    Card(onClick = onOpen, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
        Column(Modifier.padding(10.dp)) {
            MachineAssetImage(exerciseId, Modifier.fillMaxWidth().aspectRatio(3f / 2f).clip(RoundedCornerShape(18.dp)), userPhotoUri)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(media.machine.genericName, style = MaterialTheme.typography.bodySmall, color = SoftGray)
                Text("VOIR LA MACHINE", color = Copper, fontWeight = FontWeight.Bold)
            }
        }
    }
}

enum class ExerciseMediaView { MOVEMENT, MACHINE, VIDEO, BOOK }

@Composable
fun ExercisePreviewCard(exercise: ExerciseEntity, isSonia: Boolean, onOpen: () -> Unit) {
    val profileId = if (isSonia) "sonia" else "gerard"
    if (ExerciseMediaCatalog.forExercise(exercise.id) == null && exerciseArtFor(exercise.id, profileId) == null) return
    Card(
        onClick = onOpen,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Paper),
        elevation = CardDefaults.cardElevation(2.dp),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            PrimaryMediaImage(exercise.id, Modifier.fillMaxWidth().aspectRatio(3f / 2f).clip(RoundedCornerShape(18.dp)), thumbnail = true, profileId = profileId)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text(exercise.name, style = MaterialTheme.typography.titleLarge, color = DeepNavy)
                    Text(exercise.muscles, style = MaterialTheme.typography.bodyMedium, color = Sage)
                }
                SourceBadge("Commun")
            }
            if (isSonia && exercise.shoulderLoad != "NONE") {
                AssistChip(onClick = {}, label = { Text("ÉPAULE · PRUDENCE") })
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Visuel réaliste original · hors connexion", style = MaterialTheme.typography.bodySmall, color = SoftGray)
                Text("VOIR LA FICHE  →", style = MaterialTheme.typography.labelLarge, color = Copper)
            }
        }
    }
}

@Composable
fun ExerciseMediaScreen(
    exercise: ExerciseEntity,
    isSonia: Boolean,
    initialView: ExerciseMediaView,
    profileId: String,
    userPhotoUri: String?,
    onUserPhotoChanged: (String) -> Unit,
    onUserPhotoRemoved: () -> Unit,
    onBack: () -> Unit,
) {
    // Le catalogue est optionnel : les nouveaux exercices sans fiche média
    // détaillée affichent quand même leur illustration et les consignes de
    // la fiche technique (réglage, erreurs, alternative).
    val media = ExerciseMediaCatalog.forExercise(exercise.id)
    if (media == null && exerciseArtFor(exercise.id, profileId) == null) return
    val context = LocalContext.current
    var selectedView by remember(initialView) { mutableStateOf(initialView) }
    var fullScreen by remember { mutableStateOf(false) }
    val machineAsset = media?.assets?.firstOrNull { it.category == ExerciseMediaCategory.MACHINE_VISUAL }
    val hasMachine = machineAsset?.let { it.mediaType != ExerciseAssetType.PLACEHOLDER } ?: true
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        onUserPhotoChanged(uri.toString())
    }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        bitmap ?: return@rememberLauncherForActivityResult
        runCatching {
            val directory = File(context.filesDir, "user_machine_photos/$profileId").apply { mkdirs() }
            val target = File(directory, "${exercise.id}.jpg")
            FileOutputStream(target).use { output -> bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 92, output) }
            onUserPhotoChanged(Uri.fromFile(target).toString())
        }
    }

    if (fullScreen) {
        Dialog(onDismissRequest = { fullScreen = false }) {
            Box(
                Modifier.fillMaxSize().background(Color.Black).clickable { fullScreen = false },
                contentAlignment = Alignment.Center,
            ) {
                if (selectedView == ExerciseMediaView.MACHINE) {
                    MachineAssetImage(exercise.id, Modifier.fillMaxWidth().aspectRatio(3f / 2f), userPhotoUri)
                } else {
                    PrimaryMediaImage(exercise.id, Modifier.fillMaxWidth().aspectRatio(3f / 2f), profileId = profileId)
                }
            }
        }
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { TextButton(onClick = onBack) { Text("← Retour") } }
        item {
            EditorialPageHeader(
                when (selectedView) {
                    ExerciseMediaView.MOVEMENT -> "Voir le mouvement"
                    ExerciseMediaView.MACHINE -> "Voir la machine"
                    ExerciseMediaView.VIDEO -> "Voir la vidéo"
                    ExerciseMediaView.BOOK -> "Voir dans le livre"
                },
                exercise.name,
                exercise.muscles,
            )
            if (isSonia && exercise.shoulderLoad != "NONE") {
                AssistChip(onClick = {}, label = { Text("ÉPAULE · ADAPTER L’AMPLITUDE") })
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    Button(onClick = { selectedView = ExerciseMediaView.MOVEMENT }, modifier = Modifier.weight(1f)) { Text("MOUVEMENT") }
                    OutlinedButton(onClick = { selectedView = ExerciseMediaView.MACHINE }, modifier = Modifier.weight(1f)) { Text("MACHINE") }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    OutlinedButton(
                        onClick = { selectedView = ExerciseMediaView.VIDEO },
                        enabled = media?.videoReference != null,
                        modifier = Modifier.weight(1f),
                    ) { Text(if (media?.videoReference == null) "VIDÉO INDISP." else "VIDÉO") }
                    OutlinedButton(
                        onClick = { selectedView = ExerciseMediaView.BOOK },
                        enabled = media != null,
                        modifier = Modifier.weight(1f),
                    ) { Text("LIVRE") }
                }
            }
        }

        when (selectedView) {
            ExerciseMediaView.MOVEMENT -> {
                item {
                    Card(
                        shape = RoundedCornerShape(28.dp),
                        colors = CardDefaults.cardColors(containerColor = Paper),
                        elevation = CardDefaults.cardElevation(4.dp),
                    ) {
                        Column {
                            PrimaryMediaImage(
                                exercise.id,
                                Modifier.fillMaxWidth().aspectRatio(3f / 2f).clickable { fullScreen = true },
                                profileId = profileId,
                            )
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("ILLUSTRATION ORIGINALE · HORS CONNEXION", style = MaterialTheme.typography.labelLarge, color = Copper)
                                media?.assets?.firstOrNull { it.category == ExerciseMediaCategory.PRIMARY_VISUAL }?.let {
                                    Text(it.subtitle, color = SoftGray)
                                }
                                Text("Touchez l’image pour l’afficher en plein écran.", style = MaterialTheme.typography.bodySmall, color = Sage)
                            }
                        }
                    }
                }
                item {
                    PremiumSurfaceCard {
                        media?.guidedIllustration?.let { guide ->
                            ExerciseSection("Position de départ", guide.startPosition, Copper)
                            ExerciseSection("Trajectoire", guide.trajectory, Navy)
                            ExerciseSection("Appuis", guide.supports, Sage)
                            ExerciseSection("Amplitude conseillée", guide.recommendedRange, Copper)
                            ExerciseSection("Respiration", guide.breathing, Navy)
                        }
                        ExerciseSection("Réglage", exercise.machineSetup, Sage)
                        ExerciseSection("Erreurs à éviter", exercise.commonErrors, Copper)
                        ExerciseSection("Alternative", exercise.alternative, Sage)
                    }
                }
                item { CoachTipCard("Le rendu aide à identifier la posture et l’équipement. Ajuste toujours la machine à ta morphologie et arrête en cas de douleur.", "Conseil Renaissance") }
            }
            ExerciseMediaView.MACHINE -> {
                item {
                    Card(shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = Paper), elevation = CardDefaults.cardElevation(4.dp)) {
                        Column {
                            MachineAssetImage(
                                exercise.id,
                                Modifier.fillMaxWidth().aspectRatio(3f / 2f).clickable { fullScreen = true },
                                userPhotoUri,
                            )
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(media?.machine?.genericName ?: "Machine de la salle", style = MaterialTheme.typography.headlineSmall, color = DeepNavy)
                                Text(
                                    when {
                                        userPhotoUri != null -> "PHOTO PERSONNELLE · PRIORITAIRE"
                                        hasMachine -> "PHOTO DE LA MACHINE À AJOUTER"
                                        else -> "AUCUNE MACHINE REQUISE · EXERCICE AU SOL"
                                    },
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (userPhotoUri != null) Sage else Copper,
                                )
                                Text(if (hasMachine) "Ajoutez la photo de l’appareil de votre salle pour le retrouver facilement d’une séance à l’autre." else "Utilise un tapis stable et un espace dégagé.", color = SoftGray)
                                if (hasMachine) {
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                                        OutlinedButton(onClick = { camera.launch(null) }, modifier = Modifier.weight(1f)) { Text("PRENDRE") }
                                        OutlinedButton(onClick = { picker.launch(arrayOf("image/*")) }, modifier = Modifier.weight(1f)) { Text("CHOISIR") }
                                    }
                                    if (userPhotoUri != null) {
                                        OutlinedButton(onClick = onUserPhotoRemoved, modifier = Modifier.fillMaxWidth()) { Text("SUPPRIMER LA PHOTO PERSONNELLE") }
                                    }
                                }
                                ExerciseSection("Repères de réglage", media?.machine?.adjustmentLandmarks ?: exercise.machineSetup, Sage)
                                media?.machine?.possibleVariants?.let { ExerciseSection("Variantes possibles", it, Copper) }
                                machineAsset?.sourceName?.let { ExerciseSection("Source", it, Navy) }
                            }
                        }
                    }
                }
            }
            ExerciseMediaView.VIDEO -> {
                item {
                    PremiumSurfaceCard(tone = PremiumTone.SAGE) {
                        Text("VIDÉO NON DISPONIBLE", style = MaterialTheme.typography.labelLarge, color = Copper)
                        Text("Aucune vidéo fiable n’a encore été validée pour cet exercice.", style = MaterialTheme.typography.titleMedium, color = DeepNavy)
                        Text("Le bouton reste volontairement désactivé : aucune URL n’est inventée et aucun contenu non vérifié n’est ouvert.", color = SoftGray)
                    }
                }
            }
            ExerciseMediaView.BOOK -> {
                item {
                    PremiumSurfaceCard(tone = PremiumTone.SAGE) {
                        Text("FICHE ÉDITORIALE", style = MaterialTheme.typography.labelLarge, color = Sage)
                        Text(media?.bookLocator ?: "Fiche du livre", style = MaterialTheme.typography.headlineSmall, color = DeepNavy)
                        Text("La fiche locale reprend les consignes du livre utiles pendant la séance.", color = SoftGray)
                        ExerciseSection("Réglage", exercise.machineSetup, Sage)
                        ExerciseSection("Erreurs à éviter", exercise.commonErrors, Copper)
                        ExerciseSection("Alternative", exercise.alternative, Sage)
                    }
                }
            }
        }
        item {
            Text(
                "Média original vérifié le 17 juillet 2026 · aide pédagogique, sans valeur d’avis médical.",
                style = MaterialTheme.typography.bodySmall,
                color = SoftGray,
            )
        }
    }
}

@Composable
private fun LegacyExerciseMediaScreen(
    exercise: ExerciseEntity,
    isSonia: Boolean,
    initialView: ExerciseMediaView,
    onBack: () -> Unit,
) {
    val media = ExerciseMediaCatalog.forExercise(exercise.id) ?: return
    var selectedView by remember(initialView) { mutableStateOf(initialView) }
    var frame by remember { mutableIntStateOf(0) }
    var playing by remember { mutableStateOf(false) }
    var repeat by remember { mutableStateOf(true) }
    LaunchedEffect(playing, frame, repeat) {
        if (playing) {
            delay(1_300)
            if (frame < 2) frame++ else if (repeat) frame = 0 else playing = false
        }
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { TextButton(onClick = onBack) { Text("← Retour") } }
        item {
            EditorialPageHeader(
                when (selectedView) {
                    ExerciseMediaView.MOVEMENT -> "Fiche mouvement"
                    ExerciseMediaView.MACHINE -> "Vue machine"
                    ExerciseMediaView.VIDEO -> "Vidéo"
                    ExerciseMediaView.BOOK -> "Fiche du livre"
                },
                exercise.name,
                exercise.muscles,
            )
            if (isSonia && exercise.shoulderLoad != "NONE") AssistChip(onClick = {}, label = { Text("ÉPAULE · ADAPTER L’AMPLITUDE") })
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                OutlinedButton(onClick = { selectedView = ExerciseMediaView.MOVEMENT }, modifier = Modifier.weight(1f)) { Text("MOUVEMENT") }
                OutlinedButton(onClick = { selectedView = ExerciseMediaView.MACHINE }, modifier = Modifier.weight(1f)) { Text("MACHINE") }
                OutlinedButton(onClick = { selectedView = ExerciseMediaView.BOOK }, modifier = Modifier.weight(1f)) { Text("LIVRE") }
            }
        }
        if (selectedView == ExerciseMediaView.MACHINE) {
            item {
                Card(shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = Paper)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        MachineAssetImage(exercise.id, Modifier.fillMaxWidth().height(300.dp))
                        Text(media.machine.genericName, style = MaterialTheme.typography.headlineLarge, color = DeepNavy)
                        Text(media.machine.expectedType, style = MaterialTheme.typography.titleMedium, color = Sage)
                        AssistChip(onClick = {}, label = { Text("ILLUSTRATION ORIGINALE") })
                        Text(
                            "Vue générique non liée à une marque. Une future photo personnelle validée pourra remplacer cet affichage sans supprimer l’illustration.",
                            color = SoftGray,
                        )
                        ExerciseSection("Repères de réglage", media.machine.adjustmentLandmarks, Sage)
                        ExerciseSection("Variantes possibles", media.machine.possibleVariants, Copper)
                        ExerciseSection(
                            "Source",
                            media.machine.source ?: "Manquante · photo locale ou source autorisée nécessaire",
                            Navy,
                        )
                    }
                }
            }
            return@LazyColumn
        }
        if (selectedView == ExerciseMediaView.BOOK) {
            item {
                PremiumSurfaceCard(tone = PremiumTone.SAGE) {
                    Text("VOIR DANS LE LIVRE", style = MaterialTheme.typography.labelLarge, color = Sage)
                    Text(media.bookLocator, style = MaterialTheme.typography.headlineSmall, color = DeepNavy)
                    Text("La fiche locale reprend les consignes éditoriales utiles hors connexion. L’ouverture directe du document Word sera ajoutée quand un fichier embarqué et paginé sera validé.", color = SoftGray)
                    ExerciseSection("Réglage", exercise.machineSetup, Sage)
                    ExerciseSection("Erreurs à éviter", exercise.commonErrors, Copper)
                    ExerciseSection("Alternative", exercise.alternative, Sage)
                }
            }
            return@LazyColumn
        }
        item {
            Card(shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = PaleCopper), elevation = CardDefaults.cardElevation(2.dp)) {
                Column(Modifier.padding(14.dp)) {
                    ExerciseMovementCanvas(exercise.id, frame, Modifier.fillMaxWidth().height(330.dp), media.accessibilityDescription)
                    Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("MOUVEMENT GUIDÉ", style = MaterialTheme.typography.labelLarge, color = Copper)
                        Text(
                            listOf("DÉPART", "INTERMÉDIAIRE", "FINALE")[frame],
                            style = MaterialTheme.typography.titleMedium,
                            color = DeepNavy,
                        )
                    }
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                OutlinedButton(onClick = { playing = false; frame = (frame - 1).coerceAtLeast(0) }, modifier = Modifier.weight(1f)) { Text("← IMAGE") }
                Button(onClick = { playing = !playing }, modifier = Modifier.weight(1f)) { Text(if (playing) "PAUSE" else "LECTURE LENTE") }
                OutlinedButton(onClick = { playing = false; frame = (frame + 1).coerceAtMost(2) }, modifier = Modifier.weight(1f)) { Text("IMAGE →") }
            }
            TextButton(onClick = { repeat = !repeat }) { Text(if (repeat) "Répétition activée" else "Répétition désactivée") }
        }
        item {
            PremiumSurfaceCard(tone = PremiumTone.SAGE) {
                Text("ÉQUIPEMENT", style = MaterialTheme.typography.labelLarge, color = Sage)
                MachineAssetImage(exercise.id, Modifier.fillMaxWidth().height(170.dp))
                Text(media.machine.genericName, style = MaterialTheme.typography.titleMedium, color = DeepNavy)
                Text("Illustration originale générique · hors connexion", style = MaterialTheme.typography.bodySmall, color = SoftGray)
            }
        }
        item {
            PremiumSurfaceCard {
                ExerciseSection("Position", listOf(media.guidedIllustration.startPosition, media.guidedIllustration.middlePosition, media.guidedIllustration.endPosition)[frame], Copper)
                ExerciseSection("Trajectoire", media.guidedIllustration.trajectory, Navy)
                ExerciseSection("Pivot principal", media.guidedIllustration.mainPivot, Sage)
                ExerciseSection("Appuis", media.guidedIllustration.supports, Navy)
                ExerciseSection("Amplitude conseillée", media.guidedIllustration.recommendedRange, Copper)
                ExerciseSection("Zone de poussée ou tirage", media.guidedIllustration.effortZone, Sage)
                ExerciseSection("Respiration", media.guidedIllustration.breathing, Navy)
                ExerciseSection("Réglage", exercise.machineSetup, Sage)
                ExerciseSection("Exécution", executionCue(exercise.id), Navy)
                ExerciseSection("Erreurs à éviter", exercise.commonErrors, Copper)
                ExerciseSection("Alternative", exercise.alternative, Sage)
            }
        }
        item { ExecutionComparison(exercise) }
        item { CoachTipCard("Garde une amplitude confortable et une trajectoire reproductible. La qualité du geste passe avant la charge.", "Conseil Renaissance") }
        item { RenaissanceInsightCard("Lien avec la séance", "Retrouve ce mouvement dans ta séance avec les séries, répétitions, tempo et repos adaptés à ton profil.") }
        item { Text("Illustration originale hors ligne · ${media.verificationDate}. Schéma indicatif, sans valeur d’avis médical.", style = MaterialTheme.typography.bodySmall) }
    }
}

@Composable
private fun ExerciseSection(title: String, text: String, accent: Color) {
    Text(title.uppercase(), style = MaterialTheme.typography.labelLarge, color = accent)
    Text(text, style = MaterialTheme.typography.bodyLarge, color = Ink)
    Spacer(Modifier.height(6.dp))
}

private fun executionCue(exerciseId: String): String = when (exerciseId) {
    "bike" -> "Pédale avec un rythme fluide, bassin stable et genou aligné avec le pied."
    "dead_bug", "reverse_crunch" -> "Engage la sangle abdominale, ralentis le retour et conserve le bas du dos stable."
    "lateral_raise" -> "Monte sans élan dans l’amplitude confortable, puis contrôle entièrement la descente."
    else -> "Démarre depuis un appui stable, contrôle l’aller et le retour, puis marque brièvement la position forte."
}

@Composable
private fun ExerciseMovementCanvas(exerciseId: String, frame: Int, modifier: Modifier, description: String) {
    Canvas(modifier.semantics { contentDescription = description }) {
        drawRoundRect(Color(0xFFF2EEE6), size = size, cornerRadius = androidx.compose.ui.geometry.CornerRadius(28f))
        drawLine(Color(0xFFD6CFC3), Offset(size.width * .08f, size.height * .84f), Offset(size.width * .92f, size.height * .84f), 5f)
        drawExercise(exerciseId, frame)
        drawMovementGuides(exerciseId, frame)
    }
}

@Composable
private fun ExecutionComparison(exercise: ExerciseEntity) {
    PremiumSurfaceCard(tone = PremiumTone.SAGE) {
        Text("BONNE EXÉCUTION", style = MaterialTheme.typography.labelLarge, color = Sage)
        Text("Appuis stables · trajectoire contrôlée · amplitude confortable.", color = Ink)
        Text("ERREUR FRÉQUENTE", style = MaterialTheme.typography.labelLarge, color = Copper)
        Text(exercise.commonErrors, color = Ink)
        Text(
            "Le schéma local indique la direction générale. Il ne remplace pas une validation biomécanique ou médicale.",
            style = MaterialTheme.typography.bodySmall,
            color = SoftGray,
        )
    }
}

private fun DrawScope.drawExercise(id: String, frame: Int) {
    val t = frame / 2f
    val w = size.width
    val h = size.height
    val ink = Navy
    val accent = Copper
    val sage = Sage
    val skin = Color(0xFFD9A27E)
    val shorts = Color(0xFF466277)
    fun limb(a: Offset, b: Offset, color: Color = skin, width: Float = w * .032f) {
        drawLine(Color.White.copy(alpha = .68f), a, b, width + w * .012f, StrokeCap.Round)
        drawLine(color, a, b, width, StrokeCap.Round)
    }
    fun torso(a: Offset, b: Offset) {
        drawLine(Color.White.copy(alpha = .72f), a, b, w * .086f, StrokeCap.Round)
        drawLine(ink, a, b, w * .072f, StrokeCap.Round)
    }
    fun joint(p: Offset) {
        drawCircle(Color.White.copy(alpha = .82f), w * .027f, p)
        drawCircle(accent, w * .019f, p)
    }
    fun head(p: Offset) {
        drawCircle(Color.White.copy(alpha = .85f), w * .063f, p)
        drawCircle(skin, w * .055f, p)
        drawArc(ink, 188f, 166f, false, Offset(p.x - w * .052f, p.y - w * .052f), Size(w * .104f, w * .104f), style = Stroke(w * .018f))
    }
    fun shoe(a: Offset, b: Offset) = drawLine(ink, a, b, w * .035f, StrokeCap.Round)
    fun machine(x: Float, y: Float, width: Float, height: Float) {
        drawRoundRect(sage.copy(alpha = .20f), Offset(x, y), Size(width, height), androidx.compose.ui.geometry.CornerRadius(18f))
        drawRoundRect(sage.copy(alpha = .76f), Offset(x, y), Size(width, height), androidx.compose.ui.geometry.CornerRadius(18f), style = Stroke(w * .012f))
    }
    fun pad(x: Float, y: Float, width: Float, height: Float, color: Color = shorts) =
        drawRoundRect(color, Offset(x, y), Size(width, height), androidx.compose.ui.geometry.CornerRadius(16f))

    when (id) {
        "bike" -> {
            val hip = Offset(w * .49f, h * .49f); val pedalAngle = t * 3.14f
            machine(w * .27f, h * .39f, w * .48f, h * .38f)
            drawCircle(sage, w * .13f, Offset(w * .52f, h * .69f), style = Stroke(w * .014f))
            pad(w * .38f, h * .45f, w * .20f, h * .045f)
            head(Offset(w * .45f, h * .24f))
            torso(Offset(w * .45f,h*.31f), hip)
            limb(hip, Offset(w*.52f + cos(pedalAngle.toDouble()).toFloat()*w*.1f,h*.69f + sin(pedalAngle.toDouble()).toFloat()*w*.1f), shorts)
            limb(Offset(w*.45f,h*.34f), Offset(w*.67f,h*.39f))
            joint(hip)
        }
        "leg_press", "calf_press" -> {
            machine(w*.13f,h*.22f,w*.27f,h*.54f)
            machine(w*.67f,h*.20f,w*.16f,h*.58f)
            pad(w*.20f,h*.45f,w*.18f,h*.30f)
            pad(w*.66f,h*.28f,w*.18f,h*.30f, sage)
            val hip=Offset(w*.36f,h*.59f); val knee=Offset(w*(.50f+.10f*t),h*(.62f-.15f*t)); val foot=Offset(w*.69f,h*.53f)
            head(Offset(w*.28f,h*.34f))
            torso(Offset(w*.30f,h*.42f),hip)
            limb(hip,knee,shorts)
            limb(knee,foot)
            shoe(Offset(foot.x-w*.02f,foot.y),Offset(foot.x+w*.04f,foot.y-w*.02f))
            joint(knee)
        }
        "chest_press", "seated_row" -> {
            machine(w*.18f,h*.31f,w*.22f,h*.48f)
            pad(w*.24f,h*.39f,w*.14f,h*.32f)
            val shoulder=Offset(w*.40f,h*.40f); val handX=if(id=="chest_press") w*(.55f+.20f*t) else w*(.72f-.22f*t)
            head(Offset(w*.36f,h*.25f))
            torso(shoulder,Offset(w*.43f,h*.65f))
            limb(Offset(w*.43f,h*.65f),Offset(w*.52f,h*.80f),shorts)
            limb(shoulder,Offset(handX,h*.43f))
            joint(shoulder)
            machine(w*.70f,h*.27f,w*.13f,h*.52f)
            drawLine(accent,Offset(w*.61f,h*.43f),Offset(w*.78f,h*.43f),w*.018f,StrokeCap.Round)
        }
        "leg_curl", "leg_extension", "abductors" -> {
            machine(w*.22f,h*.31f,w*.40f,h*.48f)
            pad(w*.27f,h*.37f,w*.17f,h*.30f)
            head(Offset(w*.40f,h*.23f))
            val hip=Offset(w*.43f,h*.53f)
            torso(Offset(w*.41f,h*.30f),hip)
            if(id=="abductors") {
                val spread=w*(.06f+.12f*t)
                limb(hip,Offset(w*.43f-spread,h*.72f),shorts)
                limb(hip,Offset(w*.43f+spread,h*.72f),shorts)
                drawCircle(sage,w*.035f,Offset(w*.43f-spread,h*.68f))
                drawCircle(sage,w*.035f,Offset(w*.43f+spread,h*.68f))
                joint(hip)
            } else {
                val knee=Offset(w*.57f,h*.59f)
                val foot=if(id=="leg_extension") Offset(w*(.61f+.18f*t),h*(.76f-.25f*t)) else Offset(w*(.75f-.18f*t),h*(.72f-.05f*t))
                limb(hip,knee,shorts)
                limb(knee,foot)
                drawCircle(sage,w*.034f,foot)
                joint(knee)
            }
        }
        "lateral_raise" -> {
            val shoulder=Offset(w*.5f,h*.38f); val angle=.25f+t*1.15f; val c=cos(angle.toDouble()).toFloat(); val s=sin(angle.toDouble()).toFloat()
            head(Offset(w*.5f,h*.22f))
            torso(shoulder,Offset(w*.5f,h*.68f))
            limb(Offset(w*.5f,h*.68f),Offset(w*.42f,h*.83f),shorts)
            limb(Offset(w*.5f,h*.68f),Offset(w*.58f,h*.83f),shorts)
            limb(shoulder,Offset(w*.5f-c*w*.26f,h*.38f+s*w*.22f))
            limb(shoulder,Offset(w*.5f+c*w*.26f,h*.38f+s*w*.22f))
            drawCircle(shorts,w*.028f,Offset(w*.5f-c*w*.26f,h*.38f+s*w*.22f))
            drawCircle(shorts,w*.028f,Offset(w*.5f+c*w*.26f,h*.38f+s*w*.22f))
            joint(shoulder)
        }
        "hip_thrust", "dead_bug", "reverse_crunch" -> {
            machine(w*.12f,h*.59f,w*.34f,h*.12f)
            pad(w*.14f,h*.59f,w*.28f,h*.08f)
            val shoulder=Offset(w*.32f,h*.56f); val hipY=if(id=="hip_thrust") h*(.67f-.18f*t) else h*.65f; val hip=Offset(w*.53f,hipY); val knee=Offset(w*.70f,h*(.59f-.12f*t)); val foot=Offset(w*.79f,h*.78f)
            head(Offset(w*.22f,h*.52f))
            torso(shoulder,hip)
            limb(hip,knee,shorts)
            limb(knee,foot)
            shoe(Offset(foot.x-w*.02f,foot.y),Offset(foot.x+w*.05f,foot.y))
            joint(hip)
            if(id=="dead_bug") { limb(shoulder,Offset(w*(.40f+.28f*t),h*(.38f-.12f*t))); limb(hip,Offset(w*(.62f+.25f*t),h*(.48f-.16f*t))) }
            if(id=="reverse_crunch") drawArc(accent,-20f,220f,false,Offset(w*.40f,h*.40f),Size(w*.34f,h*.35f),style=Stroke(w*.012f))
        }
        else -> {
            head(Offset(w*.5f,h*.22f)); limb(Offset(w*.5f,h*.29f),Offset(w*.5f,h*.63f)); limb(Offset(w*.5f,h*.63f),Offset(w*.40f,h*.82f)); limb(Offset(w*.5f,h*.63f),Offset(w*.60f,h*.82f))
        }
    }
    repeat(3) { index -> drawCircle(if(index==frame) accent else Color.LightGray, w*.012f, Offset(w*(.44f+index*.06f),h*.93f)) }
}

private fun DrawScope.drawMovementGuides(id: String, frame: Int) {
    val w = size.width
    val h = size.height
    val alpha = if (frame == 1) .8f else .48f
    val start: Offset
    val end: Offset
    when (id) {
        "leg_press", "calf_press" -> { start = Offset(w * .50f, h * .53f); end = Offset(w * .70f, h * .43f) }
        "chest_press" -> { start = Offset(w * .50f, h * .36f); end = Offset(w * .76f, h * .36f) }
        "seated_row" -> { start = Offset(w * .72f, h * .36f); end = Offset(w * .48f, h * .36f) }
        "leg_extension" -> { start = Offset(w * .58f, h * .68f); end = Offset(w * .78f, h * .48f) }
        "leg_curl" -> { start = Offset(w * .72f, h * .67f); end = Offset(w * .57f, h * .58f) }
        "lateral_raise" -> { start = Offset(w * .68f, h * .58f); end = Offset(w * .78f, h * .34f) }
        "abductors" -> { start = Offset(w * .50f, h * .66f); end = Offset(w * .68f, h * .66f) }
        "hip_thrust", "reverse_crunch" -> { start = Offset(w * .52f, h * .70f); end = Offset(w * .52f, h * .48f) }
        "dead_bug" -> { start = Offset(w * .55f, h * .56f); end = Offset(w * .78f, h * .40f) }
        "bike" -> { start = Offset(w * .47f, h * .66f); end = Offset(w * .60f, h * .58f) }
        else -> { start = Offset(w * .45f, h * .55f); end = Offset(w * .68f, h * .45f) }
    }
    drawLine(Copper.copy(alpha), start, end, 5f, StrokeCap.Round)
    val direction = start - end
    val length = kotlin.math.sqrt((direction.x * direction.x + direction.y * direction.y).toDouble()).toFloat().coerceAtLeast(1f)
    val unit = Offset(direction.x / length, direction.y / length)
    val normal = Offset(-unit.y, unit.x)
    drawLine(Copper.copy(alpha), end, end + unit * 18f + normal * 10f, 5f, StrokeCap.Round)
    drawLine(Copper.copy(alpha), end, end + unit * 18f - normal * 10f, 5f, StrokeCap.Round)
    drawCircle(Copper.copy(alpha = .18f), w * .035f, start)
    drawCircle(Copper.copy(alpha), w * .016f, start, style = Stroke(3f))
    drawLine(Sage.copy(alpha = .55f), Offset(w * .28f, h * .82f), Offset(w * .72f, h * .82f), 7f, StrokeCap.Round)
}
