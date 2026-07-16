package fr.projetrenaissance.presentation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import fr.projetrenaissance.data.ExerciseEntity
import fr.projetrenaissance.domain.ExerciseMediaCatalog
import fr.projetrenaissance.domain.MediaVerificationStatus
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ExerciseMediaThumbnail(exerciseId: String, onOpen: () -> Unit) {
    val media = ExerciseMediaCatalog.forExercise(exerciseId) ?: return
    Card(onClick = onOpen, modifier = Modifier.fillMaxWidth().padding(top = 10.dp)) {
        Column(Modifier.padding(10.dp)) {
            ExerciseMovementCanvas(exerciseId, 1, Modifier.fillMaxWidth().height(105.dp), media.accessibilityDescription)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Démonstration locale · 3 positions", style = MaterialTheme.typography.bodySmall)
                Text("OUVRIR", color = Copper, fontWeight = FontWeight.Bold)
            }
        }
    }
}

enum class ExerciseMediaView { MOVEMENT, MACHINE, BOOK }

@Composable
fun ExercisePreviewCard(exercise: ExerciseEntity, isSonia: Boolean, onOpen: () -> Unit) {
    val media = ExerciseMediaCatalog.forExercise(exercise.id) ?: return
    Card(
        onClick = onOpen,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Paper),
        elevation = CardDefaults.cardElevation(2.dp),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            ExerciseMovementCanvas(exercise.id, 1, Modifier.fillMaxWidth().height(170.dp), media.accessibilityDescription)
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
                Text("3 positions · hors connexion", style = MaterialTheme.typography.bodySmall, color = SoftGray)
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
                        MachineReferenceCanvas(exercise.id, Modifier.fillMaxWidth().height(300.dp))
                        Text(media.machine.genericName, style = MaterialTheme.typography.headlineLarge, color = DeepNavy)
                        Text(media.machine.expectedType, style = MaterialTheme.typography.titleMedium, color = Sage)
                        if (media.machine.localResource == null) {
                            AssistChip(onClick = {}, label = { Text("PHOTO MACHINE À VALIDER") })
                            Text(
                                "Aucune photo réelle et vérifiée n’est encore intégrée. Ce repère dessiné ne remplace pas la photo de la machine présente dans votre salle.",
                                color = SoftGray,
                            )
                        }
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
private fun MachineReferenceCanvas(exerciseId: String, modifier: Modifier) {
    Canvas(modifier.semantics { contentDescription = "Repère dessiné de la machine, photo réelle à valider" }) {
        drawRoundRect(Color(0xFFF2EEE6), size = size, cornerRadius = androidx.compose.ui.geometry.CornerRadius(28f))
        val w = size.width
        val h = size.height
        drawRoundRect(Sage.copy(alpha = .18f), Offset(w * .12f, h * .12f), Size(w * .76f, h * .72f), androidx.compose.ui.geometry.CornerRadius(30f), style = Stroke(7f))
        when (exerciseId) {
            "bike" -> {
                drawCircle(Sage, w * .18f, Offset(w * .48f, h * .62f), style = Stroke(9f))
                drawLine(Navy, Offset(w * .48f, h * .62f), Offset(w * .36f, h * .35f), 11f, StrokeCap.Round)
                drawLine(Navy, Offset(w * .32f, h * .34f), Offset(w * .48f, h * .34f), 13f, StrokeCap.Round)
                drawLine(Navy, Offset(w * .54f, h * .30f), Offset(w * .72f, h * .43f), 11f, StrokeCap.Round)
            }
            "dead_bug", "reverse_crunch" -> {
                drawRoundRect(Sage, Offset(w * .14f, h * .58f), Size(w * .72f, h * .12f), androidx.compose.ui.geometry.CornerRadius(18f))
            }
            else -> {
                drawRoundRect(Navy.copy(alpha = .80f), Offset(w * .18f, h * .22f), Size(w * .16f, h * .50f), androidx.compose.ui.geometry.CornerRadius(18f))
                drawRoundRect(Sage, Offset(w * .39f, h * .48f), Size(w * .28f, h * .12f), androidx.compose.ui.geometry.CornerRadius(18f))
                drawLine(Copper, Offset(w * .32f, h * .30f), Offset(w * .72f, h * .30f), 10f, StrokeCap.Round)
                drawLine(Copper, Offset(w * .72f, h * .30f), Offset(w * .76f, h * .66f), 10f, StrokeCap.Round)
            }
        }
        drawLine(Color(0xFFD6CFC3), Offset(w * .08f, h * .84f), Offset(w * .92f, h * .84f), 5f)
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
