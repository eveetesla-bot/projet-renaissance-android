package fr.projetrenaissance.presentation

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import fr.projetrenaissance.data.BodyMetricEntity
import fr.projetrenaissance.data.ExerciseEntity
import fr.projetrenaissance.data.ProfileEntity
import fr.projetrenaissance.data.WorkoutTemplateEntity
import fr.projetrenaissance.data.HealthRecordEntity
import fr.projetrenaissance.data.health.HealthConnectAvailability
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Person
import fr.projetrenaissance.domain.SessionLength
import fr.projetrenaissance.domain.LoadHistoryPoint
import fr.projetrenaissance.domain.BiologicalSex
import fr.projetrenaissance.domain.ProgramGoal
import fr.projetrenaissance.domain.TrainingProfile
import fr.projetrenaissance.domain.WorkoutCoach
import fr.projetrenaissance.domain.WorkoutCoachContext
import fr.projetrenaissance.domain.ExerciseMediaCatalog
import fr.projetrenaissance.domain.CompletedProgramSlot
import fr.projetrenaissance.domain.ProgramProgress
import fr.projetrenaissance.domain.ProgramSetRecord
import fr.projetrenaissance.domain.ProgramTemplateRef
import fr.projetrenaissance.domain.readinessEstimateRange
import fr.projetrenaissance.domain.readinessReliabilityLabel
import java.util.Locale
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private object Routes {
    const val HOME = "home"
    const val PROGRAM = "program"
    const val TRACKING = "tracking"
    const val NUTRITION = "nutrition"
    const val PROFILE = "profile"
    const val LIBRARY = "library"
    const val TIMER = "timer"
    const val HEALTH = "health"
    const val MEDIA = "media/{exerciseId}/{view}"
    const val WORKOUT = "workout/{templateId}"
    fun workout(id: String) = "workout/$id"
    fun media(id: String, view: ExerciseMediaView = ExerciseMediaView.MOVEMENT) = "media/$id/${view.name}"
}

private data class MainDestination(val route: String, val label: String, val icon: ImageVector)

private val mainDestinations = listOf(
    MainDestination(Routes.HOME, "Accueil", Icons.Filled.Home),
    MainDestination(Routes.PROGRAM, "Programme", Icons.Filled.FitnessCenter),
    MainDestination(Routes.TRACKING, "Suivi", Icons.Filled.Insights),
    MainDestination(Routes.NUTRITION, "Nutrition", Icons.Filled.Restaurant),
    MainDestination(Routes.PROFILE, "Profil", Icons.Filled.Person),
)

@Composable
fun RenaissanceApp(viewModel: AppViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val appContext = LocalContext.current
    val lockedToSonia = appContext.packageName.endsWith(".soniatest")
    var requestingBackgroundHealth by remember { mutableStateOf(false) }
    val healthPermissionLauncher = rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract(),
    ) { granted ->
        viewModel.onHealthPermissionsResult(granted)
        if (requestingBackgroundHealth) {
            viewModel.setHealthBackgroundSync(HealthPermission.PERMISSION_READ_HEALTH_DATA_IN_BACKGROUND in granted)
            requestingBackgroundHealth = false
        }
    }
    if (uiState.loading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }
    if (uiState.preferences.activeProfileId == null || uiState.profile == null) {
        if (!lockedToSonia) {
            ProfileSelection(viewModel::selectProfile)
        } else {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        }
        return
    }
    val activeProfile = uiState.profile ?: return
    if (!uiState.preferences.onboardingCompleted) {
        StartupHealthOnboarding(
            profile = activeProfile,
            state = uiState,
            onRequestPermissions = { healthPermissionLauncher.launch(viewModel.healthPermissions()) },
            onSync = viewModel::syncHealth,
            onFinish = viewModel::completeOnboarding,
        )
        return
    }

    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val route = backStack?.destination?.route
    val showBottomBar = route in mainDestinations.map { it.route }

    Scaffold(
        bottomBar = {
            if (showBottomBar) MainNavigationBar(navController, route)
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier.padding(padding),
        ) {
            composable(Routes.HOME) {
                HomeScreen(uiState, navController, viewModel::saveCheckIn)
            }
            composable(Routes.PROGRAM) {
                ProgramScreen(uiState.templates, uiState.setLogs) { navController.navigate(Routes.workout(it)) }
            }
            composable(Routes.TRACKING) { TrackingScreen(uiState) }
            composable(Routes.NUTRITION) { NutritionScreen(uiState.profile) }
            composable(Routes.PROFILE) {
                ProfileScreen(
                    state = uiState,
                    onChangeProfile = viewModel::selectProfile,
                    onSound = viewModel::setSound,
                    onVibration = viewModel::setVibration,
                    onClearance = viewModel::setClearance,
                    onExportPayload = viewModel::exportPayload,
                    onImportPayload = viewModel::importPayload,
                    onHealth = { navController.navigate(Routes.HEALTH) },
                    onResetToday = viewModel::resetToday,
                    onResetProfile = viewModel::resetLocalProfile,
                    onResetAll = viewModel::resetAll,
                    profileLocked = lockedToSonia,
                )
            }
            composable(Routes.LIBRARY) { ExerciseLibraryScreen(uiState.exercises, activeProfile.id) { navController.navigate(Routes.media(it)) } }
            composable(Routes.TIMER) { TimerScreen(viewModel) }
            composable(Routes.HEALTH) {
                HealthScreen(
                    state = uiState,
                    onBack = { navController.popBackStack() },
                    onRequestPermissions = { healthPermissionLauncher.launch(viewModel.healthPermissions()) },
                    onSync = viewModel::syncHealth,
                    onBackground = { enabled ->
                        if (enabled) {
                            requestingBackgroundHealth = true
                            healthPermissionLauncher.launch(viewModel.healthPermissions(includeBackground = true))
                        } else viewModel.setHealthBackgroundSync(false)
                    },
                    onDelete = viewModel::deleteImportedHealthData,
                    onResetProfile = viewModel::resetLocalProfile,
                    onPriority = viewModel::setHealthPriority,
                    onManageAccess = {
                        val intent = android.content.Intent("android.health.connect.action.MANAGE_HEALTH_PERMISSIONS")
                            .putExtra(android.content.Intent.EXTRA_PACKAGE_NAME, appContext.packageName)
                        appContext.startActivity(intent)
                    },
                )
            }
            composable(Routes.MEDIA) { entry ->
                val id = entry.arguments?.getString("exerciseId").orEmpty()
                uiState.exercises.firstOrNull { it.id == id }?.let { exercise ->
                    val view = entry.arguments?.getString("view")
                        ?.let { runCatching { ExerciseMediaView.valueOf(it) }.getOrNull() }
                        ?: ExerciseMediaView.MOVEMENT
                    ExerciseMediaScreen(
                        exercise = exercise,
                        isSonia = activeProfile.id == "sonia",
                        initialView = view,
                        profileId = activeProfile.id,
                        userPhotoUri = uiState.preferences.machinePhotoUris[id],
                        onUserPhotoChanged = { uri -> viewModel.setMachinePhoto(id, uri) },
                        onUserPhotoRemoved = { viewModel.removeMachinePhoto(id) },
                        onBack = { navController.popBackStack() },
                    )
                }
            }
            composable(Routes.WORKOUT) { entry ->
                val templateId = entry.arguments?.getString("templateId").orEmpty()
                WorkoutScreen(
                    templateId = templateId,
                    profile = activeProfile,
                    viewModel = viewModel,
                    onClose = { navController.popBackStack() },
                    onMedia = { id, view -> navController.navigate(Routes.media(id, view)) },
                )
            }
        }
    }
}

@Composable
private fun MainNavigationBar(navController: NavHostController, currentRoute: String?) {
    NavigationBar {
        mainDestinations.forEach { destination ->
            NavigationBarItem(
                selected = currentRoute == destination.route,
                onClick = {
                    navController.navigate(destination.route) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = { Icon(destination.icon, contentDescription = destination.label) },
                label = { Text(destination.label) },
            )
        }
    }
}

@Composable
private fun ProfileSelection(onSelect: (String) -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().background(WarmBackground).padding(24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text("PROJET", style = MaterialTheme.typography.labelLarge, color = Copper)
        Text("RENAISSANCE", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Black, color = Navy)
        Text("Choisissez votre espace personnel", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(32.dp))
        ProfileCard("Gérard", "Masse musculaire sèche · 69–70 kg", "Taekwondo & kung-fu", onClick = { onSelect("gerard") })
        Spacer(Modifier.height(16.dp))
        ProfileCard("Sonia", "Tonicité · jambes · fessiers · mobilité", "Parcours protecteur de l'épaule", onClick = { onSelect("sonia") })
        Spacer(Modifier.height(24.dp))
        MedicalNotice()
    }
}

@Composable
private fun StartupHealthOnboarding(
    profile: ProfileEntity,
    state: AppUiState,
    onRequestPermissions: () -> Unit,
    onSync: () -> Unit,
    onFinish: () -> Unit,
) {
    var step by remember(profile.id) { mutableIntStateOf(0) }
    Column(
        modifier = Modifier.fillMaxSize().background(WarmBackground).padding(24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text("PROJET RENAISSANCE", style = MaterialTheme.typography.labelLarge, color = Copper)
        Text(
            if (step == 0) "Bienvenue ${profile.displayName}" else "Vos données corporelles",
            style = MaterialTheme.typography.headlineLarge,
            color = DeepNavy,
        )
        Spacer(Modifier.height(14.dp))
        PremiumSurfaceCard(tone = if (step == 0) PremiumTone.SAGE else PremiumTone.NAVY) {
            if (step == 0) {
                Text("Un démarrage simple et personnel", style = MaterialTheme.typography.titleLarge)
                Text("L’application utilise uniquement les données locales du profil ${profile.displayName}. Elle prépare ensuite le plan du jour avec un niveau de confiance explicite.")
                Text("1 · Profil choisi\n2 · Autorisations santé\n3 · Première synchronisation\n4 · Préparation et plan du jour")
                Button(onClick = { step = 1 }, modifier = Modifier.fillMaxWidth()) { Text("CONTINUER") }
            } else {
                Text("HEALTH CONNECT", style = MaterialTheme.typography.labelLarge, color = if (state.preferences.healthSyncEnabled) Sage else Copper)
                Text("Sommeil, fréquence cardiaque au repos et VFC permettent de personnaliser la préparation. Vous gardez le contrôle des autorisations.")
                state.healthMessage?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                if (!state.preferences.healthSyncEnabled) {
                    Button(
                        onClick = onRequestPermissions,
                        enabled = state.healthAvailability == HealthConnectAvailability.AVAILABLE,
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("AUTORISER LES DONNÉES SANTÉ") }
                    OutlinedButton(onClick = onFinish, modifier = Modifier.fillMaxWidth()) {
                        Text("CONTINUER SANS DONNÉES SANTÉ")
                    }
                } else {
                    Button(onClick = onSync, modifier = Modifier.fillMaxWidth()) { Text("SYNCHRONISER MAINTENANT") }
                    Text("La préparation se construit avec vos données disponibles. Les premières journées conservent une confiance réduite.", style = MaterialTheme.typography.bodySmall)
                    Button(onClick = onFinish, modifier = Modifier.fillMaxWidth()) { Text("CRÉER MON PLAN DU JOUR") }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        MedicalNotice()
    }
}

@Composable
private fun ProfileCard(name: String, goal: String, detail: String, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Paper), border = BorderStroke(1.dp, WarmLine), elevation = CardDefaults.cardElevation(0.dp)) {
        Column(Modifier.padding(22.dp)) {
            Text(name, style = MaterialTheme.typography.headlineMedium, color = Navy, fontWeight = FontWeight.Bold)
            Text(goal, style = MaterialTheme.typography.titleMedium)
            Text(detail, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))
            Text("OUVRIR LE PROFIL  →", color = Copper, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun HomeScreen(state: AppUiState, nav: NavHostController, onCheckIn: (Int, Int, Int, Int) -> Unit) {
    val profile = state.profile ?: return
    val next = state.templates.firstOrNull()
    val readiness = state.dailyHealth.readiness
    val readinessRange = readinessEstimateRange(readiness.score, readiness.confidence)
    val readinessValue = when {
        readiness.score == null -> "—"
        readiness.confidence < 85 && readinessRange != null -> "${readinessRange.first}–${readinessRange.last}"
        else -> "${readiness.score}"
    }
    val sessionsDone = state.dailyHealth.sessionsThisWeek.coerceAtMost(3)
    val today = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.FRANCE)).uppercase(Locale.FRANCE)
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            EditorialPageHeader(
                eyebrow = today,
                title = "Salut ${profile.displayName}",
                subtitle = if (profile.id == "sonia") "Avancer avec douceur et précision." else "La régularité transforme chaque séance en progrès.",
                trailing = {
                    Box(Modifier.size(50.dp).background(PaleCopper, CircleShape), contentAlignment = Alignment.Center) {
                        Text(profile.displayName.take(1), color = Copper, style = MaterialTheme.typography.titleLarge)
                    }
                },
            )
        }
        // Héro : la séance du jour et son action principale, en tête.
        item {
            val startOfDay = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val doneToday = state.setLogs.any { !it.isTest && it.completedAt >= startOfDay }
            PremiumSurfaceCard(tone = PremiumTone.NAVY) {
                Text("SÉANCE DU JOUR", style = MaterialTheme.typography.labelLarge, color = Color(0xFFF0997B))
                Text(next?.title ?: "Programme prêt", style = MaterialTheme.typography.headlineMedium, color = Color.White)
                Text(next?.intent.orEmpty(), color = OnNavy.copy(alpha = .72f))
                if (doneToday) {
                    Text(
                        "✓ Séance enregistrée aujourd’hui — bien joué !",
                        color = Color(0xFF9FE1CB),
                        fontWeight = FontWeight.Bold,
                    )
                }
                Spacer(Modifier.height(14.dp))
                Button(
                    onClick = { next?.let { nav.navigate(Routes.workout(it.id)) } },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (doneToday) Sage else Copper,
                        contentColor = Color.White,
                    ),
                ) { Text(if (doneToday) "REVOIR OU CONTINUER LA SÉANCE" else "▶  DÉMARRER LA SÉANCE") }
            }
        }
        // Tuiles de stats clés.
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PremiumMetricCard("Préparation", readinessValue, readinessReliabilityLabel(readiness.confidence), Copper, Modifier.weight(1f))
                PremiumMetricCard("Semaine", "$sessionsDone / 3", "séances", Sage, Modifier.weight(1f))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                QuickAction("Chronomètre", "Repos configurable", Modifier.weight(1f)) { nav.navigate(Routes.TIMER) }
                QuickAction("Bibliothèque", "Exercices et réglages", Modifier.weight(1f)) { nav.navigate(Routes.LIBRARY) }
            }
        }
        item { ReadinessCard(state) }
        item { DailyForm(profile.id == "sonia", state.latestCheckIn, onCheckIn) }
        item { HealthDashboardCard(state) { nav.navigate(Routes.HEALTH) } }
        if (profile.id == "sonia") item { SoniaSafetyCard() }
    }
}

@Composable
private fun DailyForm(isSonia: Boolean, latest: fr.projetrenaissance.data.DailyCheckInEntity?, onSave: (Int, Int, Int, Int) -> Unit) {
    var energy by remember { mutableIntStateOf(3) }
    var sleep by remember { mutableIntStateOf(3) }
    var mood by remember { mutableIntStateOf(3) }
    var pain by remember { mutableIntStateOf(0) }
    var expanded by remember { mutableStateOf(false) }
    PremiumSurfaceCard(tone = PremiumTone.SAGE) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("ÉTAT DE FORME", style = MaterialTheme.typography.labelLarge, color = Sage, fontWeight = FontWeight.Bold)
                Text(latest?.let { "Énergie ${it.energy}/5 · sommeil ${it.sleep}/5" } ?: "Aucune évaluation enregistrée")
            }
            TextButton(onClick = { expanded = !expanded }) { Text(if (expanded) "Réduire" else "Évaluer") }
        }
        if (expanded) {
            ScoreSlider("Énergie", energy, 1f..5f) { energy = it }
            ScoreSlider("Sommeil", sleep, 1f..5f) { sleep = it }
            ScoreSlider("Humeur", mood, 1f..5f) { mood = it }
            ScoreSlider(if (isSonia) "Douleur / épaule" else "Douleur", pain, 0f..10f) { pain = it }
            Button(onClick = { onSave(energy, sleep, mood, pain); expanded = false }, modifier = Modifier.fillMaxWidth()) { Text("ENREGISTRER") }
        }
    }
}

@Composable
private fun ReadinessCard(state: AppUiState) {
    val result = state.dailyHealth.readiness
    var details by remember { mutableStateOf(false) }
    val estimate = readinessEstimateRange(result.score, result.confidence)
    val reliability = readinessReliabilityLabel(result.confidence)
    val estimatedDisplay = estimate?.takeIf { result.confidence < 85 }?.let { "${it.first}–${it.last}" }
    val classification = when (result.classification) {
            fr.projetrenaissance.domain.ReadinessClassification.GOOD ->
                if (result.confidence >= 85) "Bonne préparation" else "Préparation favorable estimée"
            fr.projetrenaissance.domain.ReadinessClassification.CORRECT -> "Préparation correcte"
            fr.projetrenaissance.domain.ReadinessClassification.PARTIAL -> "Récupération partielle"
            fr.projetrenaissance.domain.ReadinessClassification.WEAK -> "Récupération faible"
            else -> "Synchronisez ou renseignez votre état du jour"
        }
    PremiumSurfaceCard {
        Text("PRÉPARATION DU JOUR", style = MaterialTheme.typography.labelLarge, color = Copper)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(18.dp), verticalAlignment = Alignment.CenterVertically) {
            ReadinessGauge(result.score, estimatedDisplay)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(classification, style = MaterialTheme.typography.headlineSmall, color = DeepNavy)
                if (result.score != null) {
                    Text(reliability, color = if (result.confidence >= 60) Sage else Copper, fontWeight = FontWeight.Bold)
                    Text("Confiance ${result.confidence} %", style = MaterialTheme.typography.bodySmall, color = SoftGray)
                }
                Text("Calcul local et personnel", style = MaterialTheme.typography.bodySmall, color = SoftGray)
            }
        }
        result.factors.take(3).forEach { factor ->
            Row(Modifier.fillMaxWidth().background(WarmBackground, RoundedCornerShape(14.dp)).padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(factor.explanation, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = Ink)
                Text("${factor.score}", color = Copper, fontWeight = FontWeight.Black)
            }
        }
        CoachTipCard(result.recommendation, title = "Conseil du jour")
        TextButton(onClick = { details = !details }, modifier = Modifier.align(Alignment.End)) { Text(if (details) "Masquer le détail" else "Voir le détail") }
        if (details) {
            result.factors.drop(3).forEach { factor -> DetailLine(factor.explanation, "${factor.score}/100") }
            if (result.missingInputs.isNotEmpty()) Text("Données manquantes : ${result.missingInputs.joinToString()}", style = MaterialTheme.typography.bodySmall, color = SoftGray)
            Text(
                "Le score agrège les facteurs disponibles. La confiance mesure leur couverture et leur fraîcheur ; lorsque la confiance baisse, la fourchette affichée s’élargit sans modifier le score calculé.",
                style = MaterialTheme.typography.bodySmall,
                color = SoftGray,
            )
        }
    }
}

@Composable
private fun ScoreSlider(label: String, value: Int, range: ClosedFloatingPointRange<Float>, onValue: (Int) -> Unit) {
    Text("$label : $value", fontWeight = FontWeight.Medium)
    Slider(value = value.toFloat(), onValueChange = { onValue(it.toInt()) }, valueRange = range, steps = (range.endInclusive - range.start - 1).toInt())
}

@Composable
private fun ProgramScreen(templates: List<WorkoutTemplateEntity>, setLogs: List<fr.projetrenaissance.data.SetLogEntity>, onWorkout: (String) -> Unit) {
    val completed = ProgramProgress.completedSlots(
        templates.map { ProgramTemplateRef(it.id, it.weekFrom, it.weekTo) },
        setLogs.filter { !it.isTest }.map { ProgramSetRecord(it.templateId, it.completedAt) },
    )
    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { EditorialPageHeader("Parcours personnel", "Programme 12 semaines", "Trois phases de quatre semaines : ancrer, construire, puis intensifier et consolider.") }
        templates.groupBy { it.weekFrom to it.weekTo }.forEach { (weeks, sessions) ->
            item {
                val title = sessions.firstOrNull()?.title?.substringBefore(" ·") ?: "Phase"
                val tone = when (weeks.first) { 1 -> PremiumTone.SAGE; 5 -> PremiumTone.COPPER; else -> PremiumTone.NAVY }
                val done = completed.count { slot -> sessions.any { it.id == slot.templateId } }
                val total = sessions.size * (weeks.second - weeks.first + 1)
                PremiumSurfaceCard(tone = tone) {
                    Text("SEMAINES ${weeks.first}–${weeks.second}", style = MaterialTheme.typography.labelLarge, color = if (tone == PremiumTone.NAVY) Color.White.copy(.7f) else Copper)
                    Text(title, style = MaterialTheme.typography.headlineMedium, color = if (tone == PremiumTone.NAVY) Color.White else DeepNavy)
                    Text(sessions.firstOrNull()?.intent.orEmpty(), color = if (tone == PremiumTone.NAVY) Color.White.copy(.78f) else SoftGray)
                    LinearProgressIndicator(
                        progress = { done / total.coerceAtLeast(1).toFloat() },
                        modifier = Modifier.fillMaxWidth().height(7.dp),
                        color = if (tone == PremiumTone.NAVY) Color.White else Sage,
                        trackColor = if (tone == PremiumTone.NAVY) Color.White.copy(.2f) else WarmLine,
                    )
                    Text("$done / $total séances enregistrées", style = MaterialTheme.typography.bodySmall, color = if (tone == PremiumTone.NAVY) Color.White.copy(.7f) else SoftGray)
                    (weeks.first..weeks.second).forEach { week ->
                        Text("SEMAINE $week", style = MaterialTheme.typography.labelLarge, color = if (tone == PremiumTone.NAVY) Color.White.copy(.65f) else Sage)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            sessions.sortedBy { it.code }.forEach { session ->
                                val isCompleted = CompletedProgramSlot(session.id, week) in completed
                                FilterChip(
                                    selected = isCompleted,
                                    onClick = { onWorkout(session.id) },
                                    label = { Text("${session.code}${if (isCompleted) " ✓" else ""}") },
                                )
                            }
                        }
                        Text("Durée au choix · adaptation disponible · bilan à compléter", style = MaterialTheme.typography.bodySmall, color = if (tone == PremiumTone.NAVY) Color.White.copy(.65f) else SoftGray)
                    }
                    CoachTipCard(
                        when (weeks.first) {
                            1 -> "Commence par rendre les réglages et les trajectoires reproductibles."
                            5 -> "Ajoute du volume seulement si la technique reste stable."
                            else -> "Progresse avec précision, puis allège en semaine 12 pour le bilan."
                        },
                        "Conseil du guide",
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun WorkoutScreen(
    templateId: String,
    profile: ProfileEntity,
    viewModel: AppViewModel,
    onClose: () -> Unit,
    onMedia: (String, ExerciseMediaView) -> Unit,
) {
    val workout by viewModel.workout.collectAsStateWithLifecycle()
    val appState by viewModel.uiState.collectAsStateWithLifecycle()
    val recentPain = appState.latestCheckIn
        ?.takeIf { System.currentTimeMillis() - it.recordedAt <= 36 * 60 * 60 * 1_000L }
        ?.pain
    val coachContext = WorkoutCoachContext(
        readinessScore = appState.dailyHealth.readiness.score,
        readinessConfidence = appState.dailyHealth.readiness.confidence,
        pain = recentPain,
    )
    val sessionRecommendation = WorkoutCoach.recommendSession(coachContext)
    var selectedLength by remember(templateId) { mutableStateOf(sessionRecommendation.length) }
    var confirmRestart by remember(templateId) { mutableStateOf(false) }
    LaunchedEffect(workout.restartVersion) {
        if (workout.restartVersion > 0) confirmRestart = false
    }
    LaunchedEffect(templateId, selectedLength) { viewModel.startWorkout(templateId, selectedLength) }
    val current = workout.exercises.getOrNull(workout.currentExerciseIndex)
    val exerciseHistory = current?.let { plan ->
        appState.setLogs.filter { !it.isTest && it.exerciseId == plan.exercise.id }
    }.orEmpty()
    val previousSet = exerciseHistory.maxByOrNull { it.completedAt }
    val bestLoad = exerciseHistory.maxOfOrNull { it.loadKg }
    // Morphologie : mesures réelles (balance Withings via Health Connect) en
    // priorité — masse maigre, masse grasse, poids. Le poids cible du profil ne
    // sert que de repli, signalé comme tel dans l'explication.
    val preferredRecords = appState.healthRecords.filter { it.isPreferred }
    val measuredLeanMass = preferredRecords.filter { it.recordType == "LEAN_BODY_MASS" && it.value != null }.maxByOrNull { it.startTime }?.value
    val measuredBodyFat = preferredRecords.filter { it.recordType == "BODY_FAT" && it.value != null }.maxByOrNull { it.startTime }?.value
    val measuredWeight = appState.dailyHealth.weight?.value
    val trainingProfile = TrainingProfile(
        sex = if (profile.id == "sonia") BiologicalSex.FEMALE else BiologicalSex.MALE,
        ageYears = profile.age,
        bodyweightKg = measuredWeight ?: parseBodyweight(profile.targetWeight),
        leanMassKg = measuredLeanMass,
        bodyFatPercent = measuredBodyFat,
        weightIsMeasured = measuredWeight != null,
        goal = if (profile.id == "sonia") ProgramGoal.TONE_MOBILITY else ProgramGoal.LEAN_MASS,
    )
    val allHistory = appState.setLogs.filter { !it.isTest }
    val historyByExercise = allHistory.groupBy { it.exerciseId }
        .mapValues { (_, logs) -> logs.map { LoadHistoryPoint(it.loadKg, it.rpe, it.completedAt, it.reps) } }
    val personalIndex = WorkoutCoach.performanceIndex(trainingProfile, historyByExercise)
    val programWeek = allHistory.minOfOrNull { it.completedAt }?.let { first ->
        (((System.currentTimeMillis() - first) / (7L * 24 * 60 * 60 * 1000)).toInt() + 1).coerceIn(1, 12)
    }
    val targetRpe = current?.rpe?.let { Regex("""\d+""").find(it)?.value?.toIntOrNull() } ?: 7
    val loadRecommendation = current?.let { plan ->
        if (exerciseHistory.isEmpty()) {
            // Aucun historique sur cet exercice : référence statistique recalée
            // par l'index personnel et la forme du jour (modifiable ci-dessous).
            WorkoutCoach.recommendStartingLoad(plan.exercise.id, trainingProfile, coachContext, personalIndex, programWeek)
        } else {
            WorkoutCoach.recommendLoad(
                exerciseId = plan.exercise.id,
                history = exerciseHistory.map { LoadHistoryPoint(it.loadKg, it.rpe, it.completedAt, it.reps) },
                context = coachContext,
                targetRpe = targetRpe,
                goal = trainingProfile.goal,
            )
        }
    }
    val repetitionRecommendation = current?.let {
        WorkoutCoach.recommendRepetitions(
            it.reps,
            selectedLength,
            appState.dailyHealth.readiness.score,
            lastAchievedReps = previousSet?.reps,
        )
    }
    var reps by remember(current?.exercise?.id, selectedLength, workout.restartVersion) {
        mutableIntStateOf(repetitionRecommendation?.value ?: 10)
    }
    var rpe by remember(current?.exercise?.id, workout.restartVersion) {
        mutableIntStateOf(current?.rpe?.let { Regex("""\d+""").find(it)?.value?.toIntOrNull() } ?: 7)
    }
    var load by remember(current?.exercise?.id, loadRecommendation?.valueKg, workout.restartVersion) {
        mutableStateOf(loadRecommendation?.valueKg?.let { if (it % 1.0 == 0.0) it.toInt().toString() else it.toString() }.orEmpty())
    }
    var detailsExpanded by remember(current?.exercise?.id, workout.restartVersion) { mutableStateOf(false) }
    var demoExpanded by remember(current?.exercise?.id, workout.restartVersion) { mutableStateOf(true) }

    val totalCompletedSets = workout.completedSets.values.sum()
    LazyColumn(contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        stickyHeader {
            Column(Modifier.fillMaxWidth().background(WarmBackground).padding(bottom = 8.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onClose) {
                        Text(if (totalCompletedSets > 0 && !workout.sessionCompleted) "← Pause" else "← Quitter")
                    }
                    SourceBadge("Séance ${selectedLength.label()}")
                }
                Text("PROGRESSION", style = MaterialTheme.typography.labelLarge, color = Copper)
                LinearProgressIndicator(
                    progress = { (workout.currentExerciseIndex + 1).toFloat() / workout.exercises.size.coerceAtLeast(1) },
                    modifier = Modifier.fillMaxWidth().height(7.dp),
                    color = Copper,
                    trackColor = WarmLine,
                )
                Text(
                    if (workout.sessionCompleted) "Séance terminée"
                    else "Mouvement ${workout.currentExerciseIndex + 1} sur ${workout.exercises.size.coerceAtLeast(1)} · quitter met simplement en pause, la reprise est possible toute la journée",
                    style = MaterialTheme.typography.bodySmall,
                    color = SoftGray,
                )
            }
        }
        if (workout.sessionCompleted) {
            item {
                PremiumSurfaceCard(tone = PremiumTone.NAVY) {
                    Text("SÉANCE TERMINÉE", style = MaterialTheme.typography.labelLarge, color = Color(0xFFF0997B))
                    Text("Bien joué ${profile.displayName} !", style = MaterialTheme.typography.headlineMedium, color = Color.White)
                    Text(
                        "$totalCompletedSets série(s) enregistrée(s) · ${workout.completedSets.size} exercice(s) travaillé(s) · ${sessionDurationLabel(workout.startedAtMillis)}",
                        color = OnNavy.copy(alpha = .78f),
                    )
                    Text(
                        "Les charges et répétitions validées nourrissent le coach : la prochaine séance s’adaptera à ce que vous avez réellement fait.",
                        style = MaterialTheme.typography.bodySmall,
                        color = OnNavy.copy(alpha = .6f),
                    )
                    Spacer(Modifier.height(14.dp))
                    Button(
                        onClick = onClose,
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Copper, contentColor = Color.White),
                    ) { Text("RETOUR À L'ACCUEIL") }
                }
            }
            item { CoachTipCard("Hydratation, étirements doux et un vrai repas dans les deux heures : la progression se construit aussi après la séance.", "Après l’effort") }
        } else {
        item {
            PremiumSurfaceCard(tone = PremiumTone.SAGE) {
                Text("COACH PERSONNEL", style = MaterialTheme.typography.labelLarge, color = Sage, fontWeight = FontWeight.Bold)
                Text("Format conseillé : ${sessionRecommendation.length.label()}", style = MaterialTheme.typography.titleLarge, color = DeepNavy)
                Text(sessionRecommendation.explanation, color = SoftGray)
                Text("La durée utilise la préparation, sa confiance et la douleur récente. La charge utilise uniquement l’historique réel, le RPE et la récupération.", style = MaterialTheme.typography.bodySmall, color = SoftGray)
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SessionLength.entries.forEach { length ->
                    FilterChip(
                        selected = selectedLength == length,
                        onClick = { selectedLength = length },
                        enabled = workout.completedSets.isEmpty(),
                        label = { Text(length.label()) },
                    )
                }
            }
        }
        item {
            if (confirmRestart) {
                PremiumSurfaceCard(tone = PremiumTone.COPPER) {
                    Text("Recommencer cette séance ?", style = MaterialTheme.typography.titleMedium, color = DeepNavy, fontWeight = FontWeight.Bold)
                    Text("Seules les séries enregistrées depuis l’ouverture de cette séance seront effacées. L’historique antérieur reste intact.", color = SoftGray)
                    Button(
                        onClick = viewModel::restartWorkout,
                        enabled = !workout.resetInProgress,
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(if (workout.resetInProgress) "REDÉMARRAGE…" else "CONFIRMER LE REDÉMARRAGE") }
                    TextButton(onClick = { confirmRestart = false }) { Text("Annuler") }
                }
            } else {
                OutlinedButton(onClick = { confirmRestart = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("RECOMMENCER LA SÉANCE")
                }
            }
        }
        workout.restartMessage?.let { message ->
            item {
                PremiumSurfaceCard(tone = if (message.startsWith("Échec")) PremiumTone.COPPER else PremiumTone.SAGE) {
                    Text(message, color = DeepNavy, fontWeight = FontWeight.Bold)
                    TextButton(onClick = viewModel::clearRestartMessage) { Text("FERMER") }
                }
            }
        }
        if (current == null) {
            item { PremiumCard { Text("Préparation de la séance…") } }
        } else {
            item {
                Text("FOCUS DU MOUVEMENT", style = MaterialTheme.typography.labelLarge, color = Copper)
                Text(current.exercise.name, style = MaterialTheme.typography.headlineLarge, color = DeepNavy)
                Text(current.exercise.muscles, style = MaterialTheme.typography.titleMedium, color = Sage)
                if (profile.id == "sonia" && current.exercise.shoulderLoad != "NONE") AssistChip(onClick = {}, label = { Text("ÉPAULE · ADAPTER") })
                TextButton(onClick = { demoExpanded = !demoExpanded }) { Text(if (demoExpanded) "Masquer l’aperçu" else "Voir le mouvement") }
                if (demoExpanded) {
                    Text("Visuel réaliste local · touchez pour ouvrir la fiche complète", style = MaterialTheme.typography.bodySmall, color = SoftGray)
                    ExerciseMediaThumbnail(current.exercise.id, profile.id) { onMedia(current.exercise.id, ExerciseMediaView.MOVEMENT) }
                    MachineAssetThumbnail(
                        current.exercise.id,
                        appState.preferences.machinePhotoUris[current.exercise.id],
                    ) { onMedia(current.exercise.id, ExerciseMediaView.MACHINE) }
                }
                val media = ExerciseMediaCatalog.forExercise(current.exercise.id)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    OutlinedButton(
                        onClick = { onMedia(current.exercise.id, ExerciseMediaView.MACHINE) },
                        modifier = Modifier.weight(1f),
                    ) { Text("VOIR LA MACHINE") }
                    OutlinedButton(
                        onClick = { onMedia(current.exercise.id, ExerciseMediaView.VIDEO) },
                        enabled = media?.videoReference != null,
                        modifier = Modifier.weight(1f),
                    ) { Text(if (media?.videoReference == null) "VIDÉO INDISP." else "VOIR LA VIDÉO") }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    OutlinedButton(
                        onClick = { onMedia(current.exercise.id, ExerciseMediaView.BOOK) },
                        modifier = Modifier.weight(1f),
                    ) { Text("VOIR DANS LE LIVRE") }
                    Button(
                        onClick = { onMedia(current.exercise.id, ExerciseMediaView.MOVEMENT) },
                        modifier = Modifier.weight(1f),
                    ) { Text("VOIR LE MOUVEMENT") }
                }
            }
            item { PrescriptionCard(current.sets, current.reps, current.tempo, current.rpe, current.restSeconds) }
            item {
                PremiumSurfaceCard {
                    val completedCount = workout.completedSets[current.exercise.id] ?: 0
                    Text(
                        if (completedCount >= current.sets) "EXERCICE TERMINÉ · ${current.sets} / ${current.sets}"
                        else "SÉRIE ${completedCount + 1} / ${current.sets}",
                        style = MaterialTheme.typography.labelLarge,
                        color = if (completedCount >= current.sets) Sage else Copper,
                    )
                    if (completedCount == 0) {
                        Text(
                            "Réglez la machine, effectuez votre série avec les valeurs proposées (modifiables), puis validez : le chrono de repos se lance automatiquement.",
                            style = MaterialTheme.typography.bodySmall,
                            color = SoftGray,
                        )
                    }
                    loadRecommendation?.let { recommendation ->
                        Text(
                            recommendation.valueKg?.let { "Charge conseillée : ${if (it % 1.0 == 0.0) it.toInt() else it} kg" }
                                ?: "Calibration de charge nécessaire",
                            style = MaterialTheme.typography.titleMedium,
                            color = if (recommendation.requiresCalibration) Copper else Sage,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(recommendation.explanation, style = MaterialTheme.typography.bodySmall, color = SoftGray)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(previousSet?.loadKg?.let { "Précédente : $it kg" } ?: "Aucune charge précédente", style = MaterialTheme.typography.bodySmall, color = SoftGray)
                        Text(bestLoad?.let { "Meilleure : $it kg" } ?: "Aucune meilleure charge", style = MaterialTheme.typography.bodySmall, color = SoftGray)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NumberStepper(repetitionRecommendation?.unitLabel ?: "Rép.", reps, { reps = (reps - 1).coerceAtLeast(0) }, { reps++ }, Modifier.weight(1f))
                        NumberStepper("RPE", rpe, { rpe = (rpe - 1).coerceAtLeast(1) }, { rpe = (rpe + 1).coerceAtMost(10) }, Modifier.weight(1f))
                    }
                    if (repetitionRecommendation?.unitLabel == "Min.") {
                        Text("Charge externe : 0 kg · ajustez la résistance pour respecter le RPE cible.", color = SoftGray)
                    } else {
                        OutlinedTextField(
                            value = load,
                            onValueChange = { candidate -> if (candidate.matches(Regex("""\d{0,3}([.,]\d{0,2})?"""))) load = candidate.replace(',', '.') },
                            label = { Text("Charge (kg)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                        )
                        previousSet?.let { previous ->
                            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                                val base = previous.loadKg.toInt()
                                listOf((base - 5).coerceAtLeast(0), base, base + 2, base + 5).distinct().forEach { value ->
                                    AssistChip(onClick = { load = value.toString() }, label = { Text("$value kg") })
                                }
                            }
                        }
                    }
                    if (load.isBlank()) Text("Saisissez 0 uniquement si la charge réelle est de 0 kg.", style = MaterialTheme.typography.bodySmall, color = SoftGray)
                    Button(
                        onClick = { load.toDoubleOrNull()?.let { viewModel.completeSet(reps, it, rpe) } },
                        enabled = load.toDoubleOrNull() != null && completedCount < current.sets,
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                    ) {
                        Text(if (completedCount >= current.sets) "TOUTES LES SÉRIES SONT FAITES" else "SÉRIE TERMINÉE")
                    }
                    OutlinedButton(onClick = viewModel::reportPain, modifier = Modifier.fillMaxWidth()) { Text("DOULEUR OU GÊNE") }
                    OutlinedTextField(
                        value = workout.note,
                        onValueChange = viewModel::updateWorkoutNote,
                        label = { Text("Note de séance") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                    )
                }
            }
            if (workout.timerRunning) item { CompactTimer(workout.timerSeconds, viewModel::adjustTimer, viewModel::stopTimer) }
            if (workout.painReported) item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                    Column(Modifier.padding(18.dp)) {
                        Text("Arrêtez ce mouvement", fontWeight = FontWeight.Bold)
                        Text("Ne forcez pas. Utilisez seulement une alternative indolore et autorisée : ${current.exercise.alternative}.")
                    }
                }
            }
            item {
                TextButton(onClick = { detailsExpanded = !detailsExpanded }, modifier = Modifier.fillMaxWidth()) {
                    Text(if (detailsExpanded) "Masquer les consignes" else "Réglage, erreurs et alternative")
                }
                if (detailsExpanded) {
                    PremiumSurfaceCard(tone = PremiumTone.COPPER) {
                        DetailLine("Réglage", current.exercise.machineSetup)
                        HorizontalDivider(Modifier.padding(vertical = 8.dp))
                        DetailLine("Erreurs à éviter", current.exercise.commonErrors)
                        HorizontalDivider(Modifier.padding(vertical = 8.dp))
                        DetailLine("Machine occupée", current.exercise.alternative)
                    }
                    Spacer(Modifier.height(8.dp))
                    CoachTipCard("Reste concentré sur le tempo et la qualité de la trajectoire.", "Note du coach")
                }
            }
            item {
                val isLastExercise = workout.currentExerciseIndex >= workout.exercises.lastIndex
                if (isLastExercise) {
                    Button(
                        onClick = viewModel::finishWorkout,
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Copper, contentColor = Color.White),
                    ) { Text("TERMINER LA SÉANCE ✓") }
                } else {
                    Button(onClick = viewModel::nextExercise, modifier = Modifier.fillMaxWidth().height(54.dp), colors = ButtonDefaults.buttonColors(containerColor = Sage, contentColor = Color.White)) {
                        Text("EXERCICE SUIVANT  →")
                    }
                    if (totalCompletedSets > 0) {
                        TextButton(onClick = viewModel::finishWorkout, modifier = Modifier.fillMaxWidth()) {
                            Text("Terminer la séance maintenant")
                        }
                    }
                }
            }
        }
        }
    }
}

@Composable
private fun PrescriptionCard(sets: Int, reps: String, tempo: String, rpe: String, rest: Int) {
    Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = DeepNavy), elevation = CardDefaults.cardElevation(2.dp)) {
        Row(Modifier.fillMaxWidth().padding(18.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            PrescriptionValue("Séries", "$sets")
            PrescriptionValue("Rép.", reps)
            PrescriptionValue("Tempo", tempo)
            PrescriptionValue("RPE", rpe)
            PrescriptionValue("Repos", "${rest}s")
        }
    }
}

@Composable
private fun PrescriptionValue(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = Color.White.copy(alpha = .7f), fontSize = 11.sp)
        Text(value, color = Color.White, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun NumberStepper(label: String, value: Int, minus: () -> Unit, plus: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(label, fontWeight = FontWeight.Medium)
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = minus) { Text("−", fontSize = 24.sp) }
            Text("$value", fontSize = 22.sp, fontWeight = FontWeight.Bold)
            TextButton(onClick = plus) { Text("+", fontSize = 22.sp) }
        }
    }
}

@Composable
private fun TimerScreen(viewModel: AppViewModel) {
    val workout by viewModel.workout.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text("CHRONOMÈTRE", style = MaterialTheme.typography.labelLarge, color = Copper, fontWeight = FontWeight.Bold)
        Text(formatSeconds(workout.timerSeconds), fontSize = 72.sp, fontWeight = FontWeight.Black, color = Navy)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = { viewModel.adjustTimer(-15) }) { Text("− 15 s") }
            OutlinedButton(onClick = { viewModel.adjustTimer(15) }) { Text("+ 15 s") }
        }
        Spacer(Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = { viewModel.startTimer(60) }) { Text("60 s") }
            Button(onClick = { viewModel.startTimer(90) }) { Text("90 s") }
            Button(onClick = { viewModel.startTimer(120) }) { Text("120 s") }
        }
        TextButton(onClick = viewModel::stopTimer) { Text("Arrêter") }
    }
}

@Composable
private fun CompactTimer(seconds: Int, adjust: (Int) -> Unit, stop: () -> Unit) {
    Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Copper), elevation = CardDefaults.cardElevation(2.dp)) {
        Row(Modifier.fillMaxWidth().padding(18.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = { adjust(-15) }, colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White), border = BorderStroke(1.dp, Color.White.copy(alpha = .7f))) { Text("−15") }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("REPOS", color = Color.White.copy(alpha = .85f), fontWeight = FontWeight.Bold)
                Text(formatSeconds(seconds), color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.Black)
                TextButton(onClick = stop) { Text("Passer", color = Color.White) }
            }
            OutlinedButton(onClick = { adjust(15) }, colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White), border = BorderStroke(1.dp, Color.White.copy(alpha = .7f))) { Text("+15") }
        }
    }
}

@Composable
private fun TrackingScreen(state: AppUiState) {
    val profile = state.profile
    val health = state.healthRecords.filter { it.isPreferred }
    var period by remember { mutableIntStateOf(7) }
    val healthWeights = health.filter { it.recordType == "WEIGHT" && it.value != null }.sortedBy { it.startTime }
    val sleepRecords = health.filter { it.recordType == "SLEEP_SESSION" }.sortedBy { it.startTime }
    val rhrRecords = health.filter { it.recordType == "RESTING_HEART_RATE" }.sortedBy { it.startTime }
    val stepRecords = health.filter { it.recordType == "STEPS" }.sortedBy { it.startTime }
    val sleepValues = sleepRecords.takeLast(period).map { (it.endTime - it.startTime) / 3_600_000f }
    val zone = ZoneId.systemDefault()
    val periodLabel = if (period == 84) "12 semaines" else "$period jours"
    val lastSync = state.healthSyncStates.mapNotNull { it.lastSuccessAt }.maxOrNull()?.let {
        Instant.ofEpochMilli(it).atZone(zone).format(DateTimeFormatter.ofPattern("d MMM HH:mm", Locale.FRANCE))
    } ?: "jamais"
    val sleepPoints = dailyPoints(period, sleepRecords.map { it.endTime to ((it.endTime - it.startTime) / 3_600_000f) }, true, zone)
    val rhrPoints = dailyPoints(period, rhrRecords.mapNotNull { it.value?.toFloat()?.let { value -> it.startTime to value } }, false, zone)
    val hrvRecords = health.filter { it.recordType == "HEART_RATE_VARIABILITY_RMSSD" }.sortedBy { it.startTime }
    val hrvPoints = dailyPoints(period, hrvRecords.mapNotNull { it.value?.toFloat()?.let { value -> it.startTime to value } }, false, zone)
    val stepPoints = dailyPoints(period, stepRecords.mapNotNull { it.value?.toFloat()?.let { value -> it.startTime to value } }, true, zone)
    val weightPoints = dailyPoints(period, healthWeights.mapNotNull { it.value?.toFloat()?.let { value -> it.startTime to value } }, false, zone)
    val volumePoints = dailyPoints(period, state.setLogs.filter { !it.isTest }.map { it.completedAt to (it.loadKg * it.reps).toFloat() }, true, zone)
    val sessionPairs = state.setLogs.filter { !it.isTest }.distinctBy { it.templateId to Instant.ofEpochMilli(it.completedAt).atZone(zone).toLocalDate() }.map { it.completedAt to 1f }
    val sessionPoints = dailyPoints(period, sessionPairs, true, zone)
    val painPoints = dailyPoints(period, state.checkIns.map { it.recordedAt to it.pain.toFloat() }, false, zone)
    val energyPoints = dailyPoints(period, state.checkIns.map { it.recordedAt to it.energy.toFloat() }, false, zone)
    val readinessPoints = dailyPoints(period, state.dailyHealth.readiness.score?.let { listOf(System.currentTimeMillis() to it.toFloat()) }.orEmpty(), false, zone)
    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            EditorialPageHeader("Journal de progression", "Suivi", "Lire les tendances avec calme, sans juger un chiffre isolé.")
            Row(Modifier.padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(7, 30, 84).forEach { days ->
                    FilterChip(selected = period == days, onClick = { period = days }, label = { Text(if (days == 84) "12 semaines" else "$days jours") })
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                val readiness = state.dailyHealth.readiness
                val readinessRange = readinessEstimateRange(readiness.score, readiness.confidence)
                val readinessValue = when {
                    readiness.score == null -> "—"
                    readiness.confidence < 85 && readinessRange != null -> "${readinessRange.first}–${readinessRange.last}"
                    else -> "${readiness.score} / 100"
                }
                PremiumMetricCard(
                    "Préparation",
                    readinessValue,
                    "${readinessReliabilityLabel(readiness.confidence)} · confiance ${readiness.confidence} %",
                    Copper,
                    Modifier.weight(1f),
                )
                PremiumMetricCard("Poids", state.dailyHealth.weight?.let { String.format(Locale.FRANCE, "%.1f kg", it.value) } ?: "—", profile?.targetWeight.orEmpty(), Sage, Modifier.weight(1f))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PremiumMetricCard("Sommeil", sleepValues.lastOrNull()?.let { String.format(Locale.FRANCE, "%.1f h", it) } ?: "—", sleepRecords.lastOrNull()?.sourceLabel ?: "Source indisponible", Sage, Modifier.weight(1f))
                PremiumMetricCard("Pas", state.dailyHealth.stepsToday?.let { "${it.value.toLong()}" } ?: "—", "aujourd’hui", Copper, Modifier.weight(1f))
            }
        }
        item { PremiumCalendarChartCard("Poids", "Évolution vers le repère personnel", weightPoints, "kg", healthWeights.lastOrNull()?.sourceLabel ?: "Saisie locale", periodLabel, lastSync) }
        item { PremiumCalendarChartCard("Sommeil", "Durée totale par jour", sleepPoints, "h", sleepRecords.lastOrNull()?.sourceLabel ?: "Health Connect", periodLabel, lastSync) }
        item { PremiumCalendarChartCard("FC au repos", "Repère cardiovasculaire quotidien", rhrPoints, "bpm", rhrRecords.lastOrNull()?.sourceLabel ?: "Health Connect", periodLabel, lastSync) }
        item { PremiumCalendarChartCard("VFC RMSSD", "Variabilité cardiaque quotidienne", hrvPoints, "ms", hrvRecords.lastOrNull()?.sourceLabel ?: "Health Connect", periodLabel, lastSync) }
        item { PremiumCalendarChartCard("Pas", "Activité quotidienne enregistrée", stepPoints, "pas", stepRecords.lastOrNull()?.sourceLabel ?: "Health Connect", periodLabel, lastSync) }
        item { PremiumCalendarChartCard("Préparation", "Score personnel calculé", readinessPoints, "/100", "Projet Renaissance", periodLabel, lastSync) }
        item { PremiumCalendarChartCard("Séances", "Séances distinctes terminées", sessionPoints, "séance", "Projet Renaissance", periodLabel, lastSync) }
        item { PremiumCalendarChartCard("Volume", "Charge × répétitions", volumePoints, "kg·rép", "Projet Renaissance", periodLabel, lastSync) }
        item { PremiumCalendarChartCard("Douleur", "Déclaration quotidienne", painPoints, "/10", "Bilan personnel", periodLabel, lastSync) }
        item { PremiumCalendarChartCard("Énergie", "Ressenti quotidien", energyPoints, "/5", "Bilan personnel", periodLabel, lastSync) }
        item { CoachTipCard("Observe plusieurs jours avant de modifier une séance : une tendance est plus utile qu’un point isolé.", "À retenir") }
    }
}

private fun dailyPoints(
    days: Int,
    entries: List<Pair<Long, Float>>,
    sum: Boolean,
    zone: ZoneId,
): List<CalendarPoint> {
    val today = LocalDate.now(zone)
    val grouped = entries.groupBy { Instant.ofEpochMilli(it.first).atZone(zone).toLocalDate() }
    return (days - 1 downTo 0).map { offset ->
        val date = today.minusDays(offset.toLong())
        val values = grouped[date].orEmpty().map { it.second }
        CalendarPoint(date, values.takeIf { it.isNotEmpty() }?.let { if (sum) it.sum() else it.average().toFloat() })
    }
}

private fun trendComment(label: String, values: List<Float>): String {
    if (values.size < 3) return "Pas encore assez de données pour une tendance fiable."
    val average = values.average().toFloat()
    val delta = values.last() - average
    val threshold = kotlin.math.abs(average) * .05f
    return when {
        kotlin.math.abs(delta) <= threshold -> "$label stable autour de la moyenne."
        delta > 0 -> "$label au-dessus de la moyenne de la période."
        else -> "$label légèrement sous la moyenne de la période."
    }.replaceFirstChar { it.uppercase() }
}

@Composable
private fun NutritionScreen(profile: ProfileEntity?) {
    val gerard = profile?.id == "gerard"
    var filter by remember { mutableStateOf("Tous") }
    var selected by remember { mutableStateOf<NutritionRecipe?>(null) }
    var favorites by remember { mutableStateOf(setOf<String>()) }
    val visible = nutritionRecipes.filter { filter == "Tous" || (filter == "Favoris" && it.id in favorites) || it.category == filter }
    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { EditorialPageHeader("Cuisine Renaissance", "Nutrition", if (gerard) "Construire sans aucune protéine de lait de vache." else "Énergie régulière, confort et récupération.") }
        item {
            RenaissanceInsightCard(
                if (gerard) "Objectif Gérard" else "Objectif Sonia",
                if (gerard) "Répartir les protéines sur la journée avec des aliments naturellement compatibles et des alternatives végétales vérifiées."
                else "Composer des repas réguliers, digestes et suffisamment nourrissants autour de l’entraînement.",
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Tous", "Repas", "Collation", "Favoris").forEach { value ->
                    FilterChip(selected = filter == value, onClick = { filter = value }, label = { Text(value) })
                }
            }
        }
        selected?.let { recipe ->
            item {
                PremiumSurfaceCard(tone = PremiumTone.COPPER) {
                    TextButton(onClick = { selected = null }) { Text("← Toutes les recettes") }
                    RecipeIllustration(recipe.id, Modifier.fillMaxWidth().height(180.dp))
                    Text(recipe.title, style = MaterialTheme.typography.headlineMedium, color = DeepNavy)
                    SourceBadge(recipe.compatibility)
                    Text("${recipe.minutes} min · ${recipe.portions} portions · ${recipe.protein}", color = SoftGray)
                    Text("Calories non affichées : valeur non vérifiée.", style = MaterialTheme.typography.bodySmall, color = SoftGray)
                    DetailLine("Allergènes", recipe.allergens)
                    DetailLine("Ingrédients", recipe.ingredients.joinToString(" · "))
                    DetailLine("Étapes", recipe.steps.mapIndexed { index, step -> "${index + 1}. $step" }.joinToString("\n"))
                    DetailLine("Variante Gérard", recipe.gerardVariant)
                    DetailLine("Variante Sonia", recipe.soniaVariant)
                    Button(onClick = { favorites = if (recipe.id in favorites) favorites - recipe.id else favorites + recipe.id }) {
                        Text(if (recipe.id in favorites) "RETIRER DES FAVORIS" else "AJOUTER AUX FAVORIS")
                    }
                }
            }
        } ?: run {
            items(visible) { recipe ->
                Card(onClick = { selected = recipe }, shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Paper), border = BorderStroke(1.dp, WarmLine), elevation = CardDefaults.cardElevation(0.dp)) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        RecipeIllustration(recipe.id, Modifier.fillMaxWidth().height(125.dp))
                        Text(recipe.title, style = MaterialTheme.typography.titleLarge, color = DeepNavy)
                        Text("${recipe.minutes} min · ${recipe.portions} portions · ${recipe.protein}", color = SoftGray)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            SourceBadge(recipe.compatibility)
                            Text(if (recipe.id in favorites) "♥ Favori" else "OUVRIR →", color = Copper, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
        item {
            PremiumSurfaceCard(tone = PremiumTone.SAGE) {
                Text("MENU SIMPLE", style = MaterialTheme.typography.labelLarge, color = Sage)
                Text("Choisis une recette principale, ajoute des légumes et adapte la portion de féculents à l’activité.", color = Ink)
                Text("LISTE DE COURSES", style = MaterialTheme.typography.labelLarge, color = Copper)
                Text(nutritionRecipes.flatMap { it.ingredients }.distinct().take(12).joinToString(" · "), style = MaterialTheme.typography.bodyMedium)
            }
        }
        item { CoachTipCard(if (gerard) "« Sans lactose » ne signifie pas sans protéines de lait. Vérifie toujours la liste complète des ingrédients." else "La régularité des repas compte davantage qu’une journée parfaite.", "À retenir") }
    }
}

private data class NutritionRecipe(
    val id: String,
    val title: String,
    val category: String,
    val minutes: Int,
    val portions: Int,
    val protein: String,
    val compatibility: String,
    val allergens: String,
    val ingredients: List<String>,
    val steps: List<String>,
    val gerardVariant: String,
    val soniaVariant: String,
)

private val nutritionRecipes = listOf(
    NutritionRecipe("tofu", "Bol tofu, riz et légumes", "Repas", 25, 2, "≈ 30 g protéines/portion", "Compatible pour les deux", "Soja, sésame", listOf("tofu ferme", "riz", "brocoli", "carotte", "huile de sésame"), listOf("Cuire le riz.", "Dorer le tofu sans produit laitier.", "Ajouter les légumes et assembler."), "Utiliser uniquement un tofu garanti sans trace de protéines de lait.", "Adapter l’assaisonnement et la portion selon le confort digestif."),
    NutritionRecipe("lentils", "Curry doux de lentilles", "Repas", 30, 3, "≈ 22 g protéines/portion", "Compatible pour les deux", "Aucun allergène majeur déclaré", listOf("lentilles", "tomates", "lait de coco", "épinards", "riz"), listOf("Cuire les lentilles.", "Mijoter avec tomates et lait de coco.", "Ajouter les épinards puis servir."), "Vérifier que le lait de coco ne contient aucun dérivé laitier.", "Choisir une version peu épicée les jours sensibles."),
    NutritionRecipe("peas", "Crème pois-banane cacao", "Collation", 5, 1, "≈ 25 g protéines", "Compatible pour les deux", "Pois selon la poudre choisie", listOf("boisson végétale", "banane", "cacao", "protéines de pois certifiées"), listOf("Vérifier l’étiquette de chaque ingrédient.", "Mixer jusqu’à texture homogène.", "Consommer fraîchement préparé."), "Aucune whey, caséine ni dérivé de lait de vache.", "Réduire le cacao si identifié comme déclencheur personnel."),
)

@Composable
private fun RecipeIllustration(id: String, modifier: Modifier) {
    Canvas(modifier) {
        drawRoundRect(SoftSage, size = size, cornerRadius = androidx.compose.ui.geometry.CornerRadius(24f))
        drawCircle(Paper, size.minDimension * .31f, center)
        val colors = if (id == "tofu") listOf(Sage, Copper, Color(0xFFE2B44B)) else if (id == "lentils") listOf(Copper, Color(0xFFD6A94A), Sage) else listOf(Color(0xFFE2B44B), Color(0xFF795548), PaleCopper)
        colors.forEachIndexed { index, color ->
            drawCircle(color, size.minDimension * .10f, Offset(center.x + (index - 1) * size.minDimension * .14f, center.y))
        }
    }
}

@Composable
private fun ExerciseLibraryScreen(exercises: List<ExerciseEntity>, profileId: String, onDemo: (String) -> Unit) {
    var muscleFilter by remember { mutableStateOf("Tous") }
    val visible = exercises.filter { exercise ->
        (muscleFilter == "Tous" || exercise.muscles.contains(muscleFilter, ignoreCase = true)) &&
            !(profileId == "sonia" && exercise.shoulderLoad == "OVERHEAD")
    }
    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { ScreenTitle("Bibliothèque", "Fiches locales issues du livre. Aucun lien vidéo non vérifié.") }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Tous", "Quadriceps", "Fessier", "Dos").forEach { filter ->
                    FilterChip(selected = muscleFilter == filter, onClick = { muscleFilter = filter }, label = { Text(filter) })
                }
            }
        }
        items(visible) { exercise -> ExercisePreviewCard(exercise, profileId == "sonia") { onDemo(exercise.id) } }
    }
}

@Composable
private fun ProfileScreen(
    state: AppUiState,
    onChangeProfile: (String) -> Unit,
    onSound: (Boolean) -> Unit,
    onVibration: (Boolean) -> Unit,
    onClearance: (Boolean) -> Unit,
    onExportPayload: suspend () -> String,
    onImportPayload: suspend (String) -> String,
    onHealth: () -> Unit,
    onResetToday: () -> Unit,
    onResetProfile: () -> Unit,
    onResetAll: () -> Unit,
    profileLocked: Boolean,
) {
    val profile = state.profile ?: return
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var transferMessage by remember { mutableStateOf<String?>(null) }
    var pendingProfile by remember { mutableStateOf<String?>(null) }
    var pendingReset by remember { mutableStateOf<String?>(null) }
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        if (uri != null) scope.launch {
            transferMessage = runCatching {
                val payload = onExportPayload()
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(payload) }
                        ?: error("Impossible d'ouvrir le fichier")
                }
                "Export JSON réussi."
            }.getOrElse { "Échec de l'export : ${it.message}" }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) scope.launch {
            transferMessage = runCatching {
                val payload = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                        ?: error("Impossible d'ouvrir le fichier")
                }
                onImportPayload(payload)
            }.getOrElse { "Échec de l'import : ${it.message}" }
        }
    }
    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { ScreenTitle(profile.displayName, profile.goal) }
        item {
            PremiumCard {
                DetailLine("Âge", "${profile.age} ans")
                DetailLine("Objectif", profile.targetWeight)
                DetailLine("Santé", profile.healthNotes)
            }
        }
        item {
            PremiumSurfaceCard(tone = PremiumTone.COPPER) {
                Text("REMISE À ZÉRO", style = MaterialTheme.typography.labelLarge, color = Copper, fontWeight = FontWeight.Bold)
                Text("Choisissez précisément ce qui doit être effacé. Health Connect ne supprime jamais vos mesures système.", color = SoftGray)
                OutlinedButton(onClick = { pendingReset = "day" }, modifier = Modifier.fillMaxWidth()) {
                    Text("RÉINITIALISER LA JOURNÉE")
                }
                OutlinedButton(onClick = { pendingReset = "profile" }, modifier = Modifier.fillMaxWidth()) {
                    Text("RÉINITIALISER LE PROFIL LOCAL")
                }
                Button(
                    onClick = { pendingReset = "all" },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                ) { Text("RÉINITIALISATION TOTALE") }
                pendingReset?.let { reset ->
                    HorizontalDivider(Modifier.padding(vertical = 8.dp))
                    val explanation = when (reset) {
                        "day" -> "Effacer uniquement le plan, la séance, les réponses et le bilan d’aujourd’hui ? L’historique antérieur est conservé."
                        "profile" -> "Effacer l’historique, les séries, les données santé importées et les préférences locales de ${profile.displayName} ?"
                        else -> "Revenir à la première installation ? Toutes les données locales seront effacées et les autorisations santé seront révoquées, sans supprimer les données de Health Connect."
                    }
                    Text(explanation, color = DeepNavy, fontWeight = FontWeight.Bold)
                    Button(
                        onClick = {
                            when (reset) {
                                "day" -> onResetToday()
                                "profile" -> onResetProfile()
                                else -> onResetAll()
                            }
                            pendingReset = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("CONFIRMER") }
                    TextButton(onClick = { pendingReset = null }) { Text("Annuler") }
                }
                state.healthMessage?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = SoftGray) }
            }
        }
        item {
            PremiumCard {
                Text("SANTÉ ET SYNCHRONISATION", style = MaterialTheme.typography.labelLarge, color = Sage, fontWeight = FontWeight.Bold)
                Text("Health Connect, permissions, sources et diagnostic local.")
                Button(onClick = onHealth, modifier = Modifier.fillMaxWidth()) { Text("OUVRIR HEALTH CONNECT") }
            }
        }
        item {
            PremiumCard {
                Text("PRÉFÉRENCES", style = MaterialTheme.typography.labelLarge, color = Copper, fontWeight = FontWeight.Bold)
                SettingSwitch("Vibration du chronomètre", state.preferences.vibrationEnabled, onVibration)
                SettingSwitch("Signal sonore", state.preferences.soundEnabled, onSound)
                if (profile.id == "sonia") {
                    SettingSwitch("Validation médicale renseignée", state.preferences.soniaMedicalClearance, onClearance)
                    Text("Les mouvements au-dessus de la tête restent désactivés dans cette version.", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        if (!profileLocked) item {
            PremiumCard {
                Text("CHANGER DE PROFIL", style = MaterialTheme.typography.labelLarge, color = Sage, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = { pendingProfile = "gerard" }) { Text("Gérard") }
                    OutlinedButton(onClick = { pendingProfile = "sonia" }) { Text("Sonia") }
                }
                pendingProfile?.let { target ->
                    Text("Confirmer le passage vers ${if (target == "gerard") "Gérard" else "Sonia"} ? Les données restent séparées.", color = Copper)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { onChangeProfile(target); pendingProfile = null }) { Text("CONFIRMER") }
                        TextButton(onClick = { pendingProfile = null }) { Text("Annuler") }
                    }
                }
            }
        }
        item {
            PremiumCard {
                Text("EXPORT / IMPORT JSON", style = MaterialTheme.typography.labelLarge, color = Copper, fontWeight = FontWeight.Bold)
                Text("Sauvegardez localement les mesures, bilans quotidiens et séries réalisées.")
                Button(
                    onClick = { exportLauncher.launch("projet-renaissance-${profile.id}.json") },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("EXPORTER") }
                OutlinedButton(
                    onClick = { importLauncher.launch(arrayOf("application/json", "text/json", "text/plain")) },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("IMPORTER") }
                transferMessage?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
            }
        }
        item { MedicalNotice() }
    }
}

@Composable
private fun HealthDashboardCard(state: AppUiState, onOpen: () -> Unit) {
    PremiumSurfaceCard(tone = PremiumTone.SAGE) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("SANTÉ DU JOUR", style = MaterialTheme.typography.labelLarge, color = Sage)
                Text("Tes repères essentiels", style = MaterialTheme.typography.titleMedium, color = DeepNavy)
            }
            TextButton(onClick = onOpen) { Text("Sources") }
        }
        state.dailyHealth.stepsToday?.let { MeasuredValueCard("Pas aujourd’hui", it) } ?: DetailLine("Pas aujourd’hui", "Indisponible")
        state.dailyHealth.sleepNight?.let { DetailLine("Dernière nuit", "${it.sleepDurationMinutes / 60} h ${it.sleepDurationMinutes % 60} · ${it.sourceLabel}") } ?: DetailLine("Dernière nuit", "Indisponible")
        state.dailyHealth.restingHeartRate?.let { MeasuredValueCard("FC au repos", it) } ?: DetailLine("FC au repos", "Indisponible")
        state.dailyHealth.weight?.let { MeasuredValueCard("Poids", it) } ?: DetailLine("Poids", "Indisponible")
    }
}

@Composable
private fun MeasuredValueCard(label: String, value: fr.projetrenaissance.domain.MeasuredValue) {
    val formatted = if (value.unit == "kg") String.format(Locale.FRANCE, "%.1f", value.value) else value.value.toLong().toString()
    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, fontWeight = FontWeight.Bold)
            Text("$formatted ${value.unit}", color = Navy, fontWeight = FontWeight.Bold)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            SourceBadge(value.source)
            ConfidenceBadge(value.confidence)
        }
    }
}

@Composable
private fun HealthScreen(
    state: AppUiState,
    onBack: () -> Unit,
    onRequestPermissions: () -> Unit,
    onSync: () -> Unit,
    onBackground: (Boolean) -> Unit,
    onDelete: () -> Unit,
    onResetProfile: () -> Unit,
    onPriority: (fr.projetrenaissance.domain.HealthRecordType, fr.projetrenaissance.domain.HealthSourceCategory) -> Unit,
    onManageAccess: () -> Unit,
) {
    var confirmDelete by remember { mutableStateOf(false) }
    var confirmReset by remember { mutableStateOf(false) }
    val availability = when (state.healthAvailability) {
        HealthConnectAvailability.AVAILABLE -> "Disponible"
        HealthConnectAvailability.UPDATE_REQUIRED -> "Mise à jour requise"
        HealthConnectAvailability.UNAVAILABLE -> "Indisponible sur ce téléphone"
    }
    val grantedTypes = state.healthSyncStates.count { it.permissionState == "GRANTED" }
    val deniedTypes = state.healthSyncStates.count { it.permissionState == "NOT_GRANTED" }
    val sourceGroups = state.healthRecords.groupBy { it.sourcePackage }

    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            TextButton(onClick = onBack) { Text("← Retour") }
            ScreenTitle("Santé et synchronisation", "Health Connect est l'unique passerelle. Les données restent locales.")
        }
        item {
            val baselines = state.dailyHealth.baselines
            PremiumCard {
                Text("RÉFÉRENCES PERSONNELLES", style = MaterialTheme.typography.labelLarge, color = Copper, fontWeight = FontWeight.Bold)
                Text("Elles se construisent uniquement avec les données de ${state.profile?.displayName}. La confiance augmente progressivement.", color = SoftGray)
                DetailLine(
                    "Sommeil",
                    baselines.sleepReferenceMinutes?.let {
                        "Active · ${baselines.sleepSamples} nuits · ${it / 60} h ${it % 60} min"
                    } ?: "En construction · ${baselines.sleepSamples}/${fr.projetrenaissance.domain.PersonalBaselineSummary.SLEEP_REQUIRED} nuits",
                )
                DetailLine(
                    "FC au repos",
                    baselines.restingHeartRateReference?.let {
                        "Active · ${baselines.restingHeartRateSamples} valeurs · ${String.format(Locale.FRANCE, "%.1f bpm", it)}"
                    } ?: "En construction · ${baselines.restingHeartRateSamples}/${fr.projetrenaissance.domain.PersonalBaselineSummary.RESTING_HEART_RATE_REQUIRED} valeurs",
                )
                DetailLine(
                    "VFC RMSSD",
                    baselines.hrvReference?.let {
                        "Active · ${baselines.hrvSamples} valeurs · ${String.format(Locale.FRANCE, "%.1f ms", it)}"
                    } ?: "En construction · ${baselines.hrvSamples}/${fr.projetrenaissance.domain.PersonalBaselineSummary.HRV_REQUIRED} valeurs",
                )
                Text("Avant activation d’une référence, le facteur correspondant est absent ou affiché avec une confiance réduite.", style = MaterialTheme.typography.bodySmall, color = SoftGray)
            }
        }
        item {
            PremiumSurfaceCard(tone = PremiumTone.SAGE) {
                Text("PRIORITÉS DES SOURCES", style = MaterialTheme.typography.labelLarge, color = Sage)
                Text("La source choisie gagne lors d’un doublon équivalent. Le choix est conservé par profil.", color = SoftGray)
                listOf(
                    fr.projetrenaissance.domain.HealthRecordType.SLEEP_SESSION to "Sommeil",
                    fr.projetrenaissance.domain.HealthRecordType.STEPS to "Pas",
                    fr.projetrenaissance.domain.HealthRecordType.EXERCISE_SESSION to "Séances",
                ).forEach { (type, label) ->
                    Text(label, style = MaterialTheme.typography.titleMedium, color = DeepNavy)
                    listOf(
                        fr.projetrenaissance.domain.HealthSourceCategory.WITHINGS to "Withings",
                        fr.projetrenaissance.domain.HealthSourceCategory.GOOGLE_FIT to "Google Fit",
                        fr.projetrenaissance.domain.HealthSourceCategory.BASIC_FIT to "Basic-Fit",
                    ).forEach { (source, sourceLabel) ->
                        val compatibleCount = state.healthRecords.count {
                            it.recordType == type.name && it.sourceCategory == source.name
                        }
                        val available = compatibleCount > 0
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            FilterChip(
                                selected = available && (state.preferences.sourcePriorities[type.name] == source.name ||
                                    (state.preferences.sourcePriorities[type.name] == null && source == fr.projetrenaissance.domain.HealthSourceCategory.WITHINGS)),
                                onClick = { onPriority(type, source) },
                                enabled = available,
                                label = { Text(sourceLabel) },
                            )
                            Text(
                                if (available) "$compatibleCount donnée(s)" else "Aucune donnée détectée",
                                style = MaterialTheme.typography.bodySmall,
                                color = SoftGray,
                            )
                        }
                    }
                }
            }
        }
        item {
            PremiumCard {
                Text("ÉTAT HEALTH CONNECT", style = MaterialTheme.typography.labelLarge, color = Copper, fontWeight = FontWeight.Bold)
                DetailLine("Disponibilité", availability)
                DetailLine("Types autorisés", "$grantedTypes / ${fr.projetrenaissance.domain.HealthRecordType.entries.size}")
                if (deniedTypes > 0) DetailLine("Non autorisés", deniedTypes.toString())
                state.healthMessage?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                Button(onClick = onRequestPermissions, enabled = state.healthAvailability == HealthConnectAvailability.AVAILABLE, modifier = Modifier.fillMaxWidth()) {
                    Text("AUTORISER LES DONNÉES SANTÉ")
                }
                OutlinedButton(onClick = onManageAccess, enabled = state.healthAvailability == HealthConnectAvailability.AVAILABLE, modifier = Modifier.fillMaxWidth()) {
                    Text("GÉRER / RÉVOQUER LES AUTORISATIONS")
                }
                OutlinedButton(onClick = onSync, enabled = state.healthAvailability == HealthConnectAvailability.AVAILABLE, modifier = Modifier.fillMaxWidth()) {
                    Text("SYNCHRONISER MAINTENANT")
                }
                SettingSwitch("Synchronisation périodique (12 h)", state.preferences.healthBackgroundSyncEnabled, onBackground)
            }
        }
        item {
            PremiumCard {
                Text("DIAGNOSTIC DES SOURCES", style = MaterialTheme.typography.labelLarge, color = Sage, fontWeight = FontWeight.Bold)
                if (sourceGroups.isEmpty()) {
                    Text("Aucune donnée détectée. Accordez les permissions puis synchronisez.")
                } else {
                    sourceGroups.forEach { (packageName, records) ->
                        val label = records.first().sourceLabel
                        Text(label.uppercase(Locale.getDefault()), fontWeight = FontWeight.Bold, color = Navy)
                        Text(packageName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        records.groupBy { it.recordType }.forEach { (type, values) ->
                            val latest = values.maxByOrNull { it.startTime }
                            DetailLine(healthTypeLabel(type), "${values.size} donnée(s) · dernière ${latest?.let(::healthValue) ?: "—"}")
                        }
                        HorizontalDivider(Modifier.padding(vertical = 8.dp))
                    }
                }
            }
        }
        item {
            PremiumCard {
                Text("HISTORIQUE LOCAL", style = MaterialTheme.typography.labelLarge, color = Copper, fontWeight = FontWeight.Bold)
                Text("${state.healthRecords.size} enregistrement(s), dont ${state.healthRecords.count { !it.isPreferred }} doublon(s) écarté(s) des totaux.")
                state.healthRecords.take(20).forEach { record ->
                    DetailLine(healthTypeLabel(record.recordType), "${healthValue(record)} · ${record.sourceLabel}${if (!record.isPreferred) " · doublon" else ""}")
                }
            }
        }
        item {
            PremiumCard {
                Text("CONFIDENTIALITÉ", style = MaterialTheme.typography.labelLarge, color = Sage, fontWeight = FontWeight.Bold)
                Text("La suppression locale n'efface rien dans Withings, Google Fit, Basic-Fit ou Health Connect.")
                if (confirmDelete) {
                    Text("Confirmer la suppression des données importées de ${state.profile?.displayName} ?", color = Copper, fontWeight = FontWeight.Bold)
                    Button(onClick = { onDelete(); confirmDelete = false }, modifier = Modifier.fillMaxWidth()) { Text("CONFIRMER") }
                    TextButton(onClick = { confirmDelete = false }) { Text("Annuler") }
                } else {
                    OutlinedButton(onClick = { confirmDelete = true }, modifier = Modifier.fillMaxWidth()) { Text("SUPPRIMER LES DONNÉES IMPORTÉES") }
                }
                HorizontalDivider(Modifier.padding(vertical = 12.dp))
                Text("La réinitialisation complète efface de ce téléphone les mesures locales, bilans, séries, imports, états de synchronisation et priorités du profil actif.", style = MaterialTheme.typography.bodySmall, color = SoftGray)
                if (confirmReset) {
                    Text("Confirmer la réinitialisation complète de ${state.profile?.displayName} ? Cette action est irréversible localement.", color = Copper, fontWeight = FontWeight.Bold)
                    Button(
                        onClick = { onResetProfile(); confirmReset = false },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    ) { Text("CONFIRMER LA RÉINITIALISATION") }
                    TextButton(onClick = { confirmReset = false }) { Text("Annuler") }
                } else {
                    OutlinedButton(onClick = { confirmReset = true }, modifier = Modifier.fillMaxWidth()) { Text("RÉINITIALISER LE PROFIL LOCAL") }
                }
            }
        }
        item { MedicalNotice() }
    }
}

private fun healthValue(record: HealthRecordEntity): String {
    val value = record.value ?: return "Disponible"
    return when (record.recordType) {
        "STEPS" -> "${value.toLong()} pas"
        "DISTANCE" -> String.format(Locale.FRANCE, "%.2f km", value / 1_000)
        "ACTIVE_CALORIES", "TOTAL_CALORIES" -> "${value.toLong()} kcal"
        "HEART_RATE", "RESTING_HEART_RATE" -> "${value.toLong()} bpm"
        "HEART_RATE_VARIABILITY_RMSSD" -> String.format(Locale.FRANCE, "%.1f ms", value)
        "WEIGHT", "LEAN_BODY_MASS" -> String.format(Locale.FRANCE, "%.1f kg", value)
        "SLEEP_SESSION" -> String.format(Locale.FRANCE, "%.1f h", value)
        "EXERCISE_SESSION" -> "${value.toLong()} min"
        "HYDRATION" -> String.format(Locale.FRANCE, "%.2f L", value)
        "BODY_FAT" -> String.format(Locale.FRANCE, "%.1f %%", value)
        "SPEED" -> String.format(Locale.FRANCE, "%.1f m/s", value)
        "FLOORS_CLIMBED" -> String.format(Locale.FRANCE, "%.0f étages", value)
        "VO2_MAX" -> String.format(Locale.FRANCE, "%.1f ml/min/kg", value)
        else -> String.format(Locale.FRANCE, "%.1f", value)
    }
}

private fun healthTypeLabel(type: String): String = when (type) {
    "STEPS" -> "Pas"
    "DISTANCE" -> "Distance"
    "ACTIVE_CALORIES" -> "Calories actives"
    "TOTAL_CALORIES" -> "Calories totales"
    "HEART_RATE" -> "Fréquence cardiaque"
    "RESTING_HEART_RATE" -> "FC au repos"
    "HEART_RATE_VARIABILITY_RMSSD" -> "VFC (RMSSD)"
    "WEIGHT" -> "Poids"
    "SLEEP_SESSION" -> "Sommeil"
    "EXERCISE_SESSION" -> "Séance"
    "SPEED" -> "Vitesse"
    "FLOORS_CLIMBED" -> "Étages"
    "VO2_MAX" -> "VO₂ max"
    "HYDRATION" -> "Hydratation"
    "BODY_FAT" -> "Masse grasse"
    "LEAN_BODY_MASS" -> "Masse maigre"
    else -> type
}

@Composable
private fun SettingSwitch(label: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChecked)
    }
}

@Composable
private fun SoniaSafetyCard() {
    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFE8DB))) {
        Column(Modifier.padding(18.dp)) {
            Text("SÉCURITÉ ÉPAULE", color = Copper, fontWeight = FontWeight.Bold)
            Text("Aucun mouvement au-dessus de la tête. Les exercices conditionnels restent masqués sans validation médicale.")
        }
    }
}

@Composable
private fun MedicalNotice() {
    Text(
        "Cette application accompagne l'entraînement mais ne remplace ni diagnostic, ni avis médical ou paramédical.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun TrackingPlaceholder(title: String, body: String) {
    PremiumCard { Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Navy); Text(body) }
}

@Composable
private fun DetailLine(label: String, value: String) {
    Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = Copper, fontWeight = FontWeight.Bold)
    Text(value)
}

@Composable
private fun PremiumCard(content: @Composable ColumnScope.() -> Unit) {
    PremiumSurfaceCard(content = content)
}

@Composable
private fun QuickAction(title: String, subtitle: String, modifier: Modifier, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = modifier, shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Paper), border = BorderStroke(1.dp, WarmLine), elevation = CardDefaults.cardElevation(0.dp)) {
        Column(Modifier.padding(17.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.size(34.dp).background(PaleCopper, RoundedCornerShape(11.dp)), contentAlignment = Alignment.Center) {
                Text(title.take(1), color = Copper, fontWeight = FontWeight.Black)
            }
            Text(title, fontWeight = FontWeight.Bold, color = DeepNavy)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = SoftGray)
        }
    }
}

@Composable
private fun ScreenTitle(title: String, subtitle: String) {
    EditorialPageHeader("Projet Renaissance", title, subtitle)
}

private fun SessionLength.label(): String = when (this) {
    SessionLength.FULL -> "Complet"
    SessionLength.MIN_30 -> "30 min"
    SessionLength.MIN_20 -> "20 min"
    SessionLength.MINIMAL -> "10–15"
}

private fun formatSeconds(seconds: Int): String = "%d:%02d".format(seconds / 60, seconds % 60)

private fun sessionDurationLabel(startedAtMillis: Long): String {
    if (startedAtMillis <= 0L) return "durée inconnue"
    val minutes = ((System.currentTimeMillis() - startedAtMillis) / 60_000L).coerceAtLeast(1)
    return "$minutes min"
}

// Poids de repli quand aucune mesure de montre/pesée n'est disponible : on
// lit le premier nombre du poids-cible du profil (ex. « 69–70 kg » → 69).
private fun parseBodyweight(targetWeight: String): Double? =
    Regex("""\d+([.,]\d+)?""").find(targetWeight)?.value?.replace(',', '.')?.toDoubleOrNull()
