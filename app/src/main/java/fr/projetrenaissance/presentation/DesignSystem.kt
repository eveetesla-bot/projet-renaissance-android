package fr.projetrenaissance.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.util.Locale
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

enum class PremiumTone { PAPER, SAGE, COPPER, NAVY }

data class CalendarPoint(val date: LocalDate, val value: Float?)

@Composable
fun PremiumSurfaceCard(
    modifier: Modifier = Modifier,
    tone: PremiumTone = PremiumTone.PAPER,
    content: @Composable ColumnScope.() -> Unit,
) {
    val container = when (tone) {
        PremiumTone.PAPER -> Paper
        PremiumTone.SAGE -> SoftSage
        PremiumTone.COPPER -> PaleCopper
        PremiumTone.NAVY -> DeepNavy
    }
    // Couleur de texte par défaut associée au fond : sur le fond sombre NAVY,
    // les textes sans couleur explicite doivent rester lisibles (clair sur
    // sombre) plutôt que de retomber sur onSurface (sombre sur sombre).
    val contentColor = if (tone == PremiumTone.NAVY) OnNavy else Ink
    val border = if (tone == PremiumTone.PAPER) BorderStroke(1.dp, WarmLine) else null
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = container, contentColor = contentColor),
        border = border,
        elevation = CardDefaults.cardElevation(defaultElevation = if (tone == PremiumTone.NAVY) 3.dp else 0.dp),
    ) { Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp), content = content) }
}

@Composable
fun EditorialPageHeader(eyebrow: String, title: String, subtitle: String, trailing: (@Composable () -> Unit)? = null) {
    Column(Modifier.fillMaxWidth().padding(top = 4.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.size(width = 26.dp, height = 5.dp).background(Copper, RoundedCornerShape(50)))
                Text(eyebrow.uppercase(Locale.getDefault()), style = MaterialTheme.typography.labelLarge, color = Copper)
            }
            trailing?.invoke()
        }
        Text(title, style = MaterialTheme.typography.displaySmall, color = DeepNavy)
        Text(subtitle, style = MaterialTheme.typography.bodyLarge, color = SoftGray)
    }
}

@Composable
fun RenaissanceInsightCard(title: String, text: String, tone: PremiumTone = PremiumTone.SAGE) {
    PremiumSurfaceCard(tone = tone) {
        Text(title.uppercase(Locale.getDefault()), style = MaterialTheme.typography.labelLarge, color = if (tone == PremiumTone.NAVY) Color.White.copy(.72f) else Sage)
        Text(text, style = MaterialTheme.typography.bodyLarge, color = if (tone == PremiumTone.NAVY) Color.White else Ink)
    }
}

@Composable
fun CoachTipCard(text: String, title: String = "Conseil Renaissance") {
    Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = PaleCopper)) {
        Row(Modifier.fillMaxWidth().padding(18.dp), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.Top) {
            Box(Modifier.size(34.dp).background(Copper, CircleShape), contentAlignment = Alignment.Center) {
                Text("R", color = Color.White, fontWeight = FontWeight.Black)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title.uppercase(Locale.getDefault()), style = MaterialTheme.typography.labelLarge, color = Copper)
                Text(text, style = MaterialTheme.typography.bodyLarge, color = Ink)
            }
        }
    }
}

@Composable
fun SourceBadge(label: String) = SmallBadge(label, SoftSage, Sage)

@Composable
fun ConfidenceBadge(value: Int) = SmallBadge("Confiance $value %", PaleCopper, Copper)

@Composable
fun TrendChip(values: List<Float>) {
    val delta = values.takeIf { it.size >= 2 }?.let { it.last() - it.first() }
    val (label, background, foreground) = when {
        delta == null -> Triple("Tendance indisponible", Color(0xFFF0EEE9), SoftGray)
        kotlin.math.abs(delta) < .01f -> Triple("Stable", SoftSage, Sage)
        delta > 0 -> Triple("En hausse", SoftSage, Sage)
        else -> Triple("En baisse", PaleCopper, Copper)
    }
    SmallBadge(label, background, foreground)
}

@Composable
private fun SmallBadge(label: String, background: Color, foreground: Color) {
    Box(Modifier.background(background, RoundedCornerShape(50)).padding(horizontal = 11.dp, vertical = 6.dp)) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = foreground, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun PremiumMetricCard(label: String, value: String, caption: String, accent: Color = Copper, modifier: Modifier = Modifier) {
    Card(modifier, shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Paper), border = BorderStroke(1.dp, WarmLine), elevation = CardDefaults.cardElevation(0.dp)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                Box(Modifier.size(9.dp).background(accent, CircleShape))
                Text(label.uppercase(Locale.getDefault()), style = MaterialTheme.typography.labelLarge, color = accent)
            }
            Text(value, style = MaterialTheme.typography.headlineLarge, color = DeepNavy)
            Text(caption, style = MaterialTheme.typography.bodySmall, color = SoftGray)
        }
    }
}

@Composable
fun ReadinessGauge(score: Int?, displayValue: String? = null, modifier: Modifier = Modifier) {
    val value = score?.coerceIn(0, 100) ?: 0
    val accent = when (value) { in 80..100 -> Sage; in 60..79 -> Copper; in 40..59 -> Color(0xFFC48A45); else -> Color(0xFFA85B52) }
    Box(modifier.size(132.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.matchParentSize().padding(7.dp)) {
            drawArc(WarmLine, -215f, 250f, false, style = Stroke(13f, cap = StrokeCap.Round))
            if (score != null) drawArc(accent, -215f, 250f * value / 100f, false, style = Stroke(13f, cap = StrokeCap.Round))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(displayValue ?: score?.toString() ?: "—", style = if (displayValue != null) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.displaySmall, color = DeepNavy)
            Text(if (displayValue != null) "ESTIMATION" else "SUR 100", style = MaterialTheme.typography.bodySmall, color = SoftGray, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun PremiumChartCard(
    title: String,
    subtitle: String,
    values: List<Float>,
    unit: String,
    source: String,
    period: String,
    comment: String,
) {
    PremiumSurfaceCard {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(title.uppercase(Locale.getDefault()), style = MaterialTheme.typography.labelLarge, color = Sage)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = SoftGray)
            }
            SourceBadge(period)
        }
        if (values.isEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text("Aucune donnée réelle disponible", style = MaterialTheme.typography.titleMedium, color = DeepNavy)
            Text("Pas encore assez de données pour une tendance fiable.", color = SoftGray)
        } else {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                Column {
                    Text("${String.format(Locale.FRANCE, "%.1f", values.last())} $unit", style = MaterialTheme.typography.headlineMedium, color = DeepNavy)
                    Text("Moyenne ${String.format(Locale.FRANCE, "%.1f", values.average())} $unit", style = MaterialTheme.typography.bodySmall, color = SoftGray)
                }
                TrendChip(values)
            }
            PremiumLineChart(values)
            Text(comment, style = MaterialTheme.typography.bodyMedium, color = Ink)
            Text("Source : $source", style = MaterialTheme.typography.bodySmall, color = SoftGray)
        }
    }
}

@Composable
fun PremiumCalendarChartCard(
    title: String,
    subtitle: String,
    points: List<CalendarPoint>,
    unit: String,
    source: String,
    period: String,
    lastSync: String,
) {
    val available = points.filter { it.value != null }
    var selectedIndex by remember { mutableIntStateOf(points.lastIndex.coerceAtLeast(0)) }
    val selected = points.getOrNull(selectedIndex)
    PremiumSurfaceCard {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(title.uppercase(Locale.getDefault()), style = MaterialTheme.typography.labelLarge, color = Sage)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = SoftGray)
            }
            SourceBadge(period)
        }
        if (available.isEmpty()) {
            Text("Aucune donnée réelle disponible", style = MaterialTheme.typography.titleMedium, color = DeepNavy)
            Text("Les jours sans mesure restent visibles et ne valent pas zéro.", color = SoftGray)
        } else {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                Column {
                    Text(
                        selected?.value?.let { "${String.format(Locale.FRANCE, "%.1f", it)} $unit" } ?: "Aucune mesure",
                        style = MaterialTheme.typography.headlineSmall,
                        color = DeepNavy,
                    )
                    Text(selected?.date?.format(DateTimeFormatter.ofPattern("d MMM", Locale.FRANCE)).orEmpty(), color = SoftGray)
                }
                Text("Moy. ${String.format(Locale.FRANCE, "%.1f", available.mapNotNull { it.value }.average())} $unit", style = MaterialTheme.typography.bodySmall, color = SoftGray)
            }
            CalendarLineChart(points, selectedIndex) { selectedIndex = it }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                listOfNotNull(points.firstOrNull(), points.getOrNull(points.size / 2), points.lastOrNull()).forEach {
                    Text(it.date.format(DateTimeFormatter.ofPattern("d MMM", Locale.FRANCE)), style = MaterialTheme.typography.bodySmall, color = SoftGray)
                }
            }
            TrendChip(available.mapNotNull { it.value })
            Text("Source : $source · dernière synchronisation : $lastSync", style = MaterialTheme.typography.bodySmall, color = SoftGray)
        }
    }
}

@Composable
private fun CalendarLineChart(points: List<CalendarPoint>, selectedIndex: Int, onSelect: (Int) -> Unit) {
    val values = points.mapNotNull { it.value }
    Canvas(
        Modifier.fillMaxWidth().height(168.dp).padding(top = 12.dp, bottom = 8.dp)
            .pointerInput(points) {
                detectTapGestures { offset ->
                    if (points.isNotEmpty()) onSelect(((offset.x / size.width) * points.lastIndex).roundToInt().coerceIn(0, points.lastIndex))
                }
            },
    ) {
        if (points.isEmpty()) return@Canvas
        val min = values.minOrNull() ?: 0f
        val max = values.maxOrNull() ?: 1f
        val range = (max - min).coerceAtLeast(.5f)
        repeat(3) { index ->
            val y = size.height * index / 2f
            drawLine(WarmLine.copy(alpha = .65f), Offset(0f, y), Offset(size.width, y), 1.5f)
        }
        fun pointAt(index: Int, value: Float): Offset {
            val x = if (points.size == 1) size.width / 2 else index * size.width / points.lastIndex
            return Offset(x, size.height - ((value - min) / range * size.height))
        }
        var previous: Pair<Int, Float>? = null
        points.forEachIndexed { index, point ->
            val value = point.value
            if (value == null) {
                val x = if (points.size == 1) size.width / 2 else index * size.width / points.lastIndex
                drawCircle(WarmLine, 4f, Offset(x, size.height - 2f), style = Stroke(2f))
                previous = null
            } else {
                val current = pointAt(index, value)
                previous?.let { (previousIndex, previousValue) -> drawLine(Sage, pointAt(previousIndex, previousValue), current, 5f, StrokeCap.Round) }
                drawCircle(if (index == selectedIndex) Copper else Sage, if (index == selectedIndex) 8f else 4f, current)
                previous = index to value
            }
        }
        val selected = points.getOrNull(selectedIndex)?.value
        if (selected != null) {
            val location = pointAt(selectedIndex, selected)
            drawLine(Copper.copy(.35f), Offset(location.x, 0f), Offset(location.x, size.height), 2f)
        }
    }
}

@Composable
private fun PremiumLineChart(values: List<Float>) {
    Canvas(Modifier.fillMaxWidth().height(156.dp).padding(top = 12.dp, bottom = 8.dp)) {
        if (values.isEmpty()) return@Canvas
        val min = values.min()
        val max = values.max()
        val range = (max - min).coerceAtLeast(.5f)
        repeat(3) { index ->
            val y = size.height * index / 2f
            drawLine(WarmLine.copy(alpha = .7f), Offset(0f, y), Offset(size.width, y), 1.5f)
        }
        val points = values.mapIndexed { index, value ->
            val x = if (values.size == 1) size.width / 2 else index * size.width / (values.size - 1)
            Offset(x, size.height - ((value - min) / range * size.height))
        }
        val averageY = size.height - (((values.average().toFloat() - min) / range) * size.height)
        drawLine(Copper.copy(alpha = .35f), Offset(0f, averageY), Offset(size.width, averageY), 2f)
        points.zipWithNext().forEach { (a, b) -> drawLine(Sage, a, b, 6f, StrokeCap.Round) }
        points.dropLast(1).forEach { drawCircle(Paper, 6f, it); drawCircle(Sage, 4f, it) }
        drawCircle(PaleCopper, 13f, points.last())
        drawCircle(Copper, 7f, points.last())
    }
}
