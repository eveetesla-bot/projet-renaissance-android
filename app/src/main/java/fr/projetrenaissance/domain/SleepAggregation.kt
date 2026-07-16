package fr.projetrenaissance.domain

import java.time.Instant
import java.time.ZoneId
import kotlin.math.max
import kotlin.math.min

data class SleepSegment(
    val id: String,
    val startMillis: Long,
    val endMillis: Long,
    val sourcePackage: String,
    val sourceLabel: String,
    val priority: Int = 0,
    val lastModifiedMillis: Long = endMillis,
)

enum class SleepQuality { COMPLETE, PARTIAL, STALE }

data class SleepNap(val startMillis: Long, val endMillis: Long, val durationMinutes: Int)

data class AggregatedSleepNight(
    val bedtimeMillis: Long,
    val wakeTimeMillis: Long,
    val sleepDurationMinutes: Int,
    val elapsedWindowMinutes: Int,
    val awakeGapMinutes: Int,
    val sourceLabel: String,
    val sourcePackage: String,
    val segmentIds: List<String>,
    val quality: SleepQuality,
    val confidence: Int,
    val naps: List<SleepNap>,
)

object SleepAggregation {
    private const val MINUTE = 60_000L
    private const val HOUR = 60 * MINUTE

    fun latestNight(
        input: List<SleepSegment>,
        nowMillis: Long,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): AggregatedSleepNight? {
        val valid = input.filter { it.endMillis > it.startMillis && it.endMillis - it.startMillis <= 24 * HOUR }
        if (valid.isEmpty()) return null
        val preferred = deduplicate(valid)
        val episodes = preferred.groupBy { it.sourcePackage }.values
            .flatMap { mergeSourceFragments(it) }
            .filter { it.end <= nowMillis }
        val naps = episodes.filter { episode ->
            val minutes = episode.duration / MINUTE
            val midpointHour = Instant.ofEpochMilli((episode.start + episode.end) / 2).atZone(zoneId).hour
            minutes in 20 until 180 && midpointHour in 9 until 20
        }.map { SleepNap(it.start, it.end, (it.duration / MINUTE).toInt()) }
        val night = episodes.filter { episode ->
            val midpointHour = Instant.ofEpochMilli((episode.start + episode.end) / 2).atZone(zoneId).hour
            val nocturnal = midpointHour >= 22 || midpointHour < 8
            nocturnal && episode.duration >= 20 * MINUTE
        }.maxByOrNull { it.end } ?: return null
        val stale = nowMillis - night.end > 36 * HOUR
        val slept = (night.duration / MINUTE).toInt()
        val elapsed = ((night.end - night.start) / MINUTE).toInt()
        val quality = when {
            stale -> SleepQuality.STALE
            slept < 180 -> SleepQuality.PARTIAL
            else -> SleepQuality.COMPLETE
        }
        return AggregatedSleepNight(
            bedtimeMillis = night.start,
            wakeTimeMillis = night.end,
            sleepDurationMinutes = slept,
            elapsedWindowMinutes = elapsed,
            awakeGapMinutes = (elapsed - slept).coerceAtLeast(0),
            sourceLabel = night.segments.first().sourceLabel,
            sourcePackage = night.segments.first().sourcePackage,
            segmentIds = night.segments.map { it.id },
            quality = quality,
            confidence = when (quality) {
                SleepQuality.COMPLETE -> if (night.segments.size > 1) 85 else 90
                SleepQuality.PARTIAL -> 60
                SleepQuality.STALE -> 0
            },
            naps = naps,
        )
    }

    private fun deduplicate(items: List<SleepSegment>): List<SleepSegment> {
        val sorted = items.sortedWith(compareBy(SleepSegment::startMillis, SleepSegment::id))
        val used = mutableSetOf<String>()
        return buildList {
            sorted.forEach { candidate ->
                if (candidate.id in used) return@forEach
                val group = sorted.filter { other ->
                    other.id !in used && (candidate.sourcePackage == other.sourcePackage || equivalent(candidate, other))
                }.filter { it.sourcePackage != candidate.sourcePackage || it.id == candidate.id }
                val winner = group.maxWithOrNull(compareBy<SleepSegment> { it.priority }.thenBy { it.lastModifiedMillis }) ?: candidate
                add(winner)
                group.forEach { used += it.id }
            }
        }
    }

    private fun equivalent(a: SleepSegment, b: SleepSegment): Boolean {
        if (a.sourcePackage == b.sourcePackage) return false
        val overlap = (min(a.endMillis, b.endMillis) - max(a.startMillis, b.startMillis)).coerceAtLeast(0)
        val shortest = min(a.endMillis - a.startMillis, b.endMillis - b.startMillis)
        val closeBounds = kotlin.math.abs(a.startMillis - b.startMillis) <= 30 * MINUTE &&
            kotlin.math.abs(a.endMillis - b.endMillis) <= 30 * MINUTE
        return (shortest > 0 && overlap.toDouble() / shortest >= .70) || closeBounds
    }

    private data class Episode(val start: Long, val end: Long, val duration: Long, val segments: List<SleepSegment>)

    private fun mergeSourceFragments(items: List<SleepSegment>): List<Episode> {
        if (items.isEmpty()) return emptyList()
        val result = mutableListOf<Episode>()
        var group = mutableListOf(items.minBy { it.startMillis })
        items.sortedBy { it.startMillis }.drop(1).forEach { segment ->
            if (segment.startMillis - group.maxOf { it.endMillis } <= 90 * MINUTE) group += segment
            else {
                result += episode(group)
                group = mutableListOf(segment)
            }
        }
        result += episode(group)
        return result
    }

    private fun episode(segments: List<SleepSegment>): Episode {
        val ordered = segments.sortedBy { it.startMillis }
        var union = 0L
        var start = ordered.first().startMillis
        var end = ordered.first().endMillis
        ordered.drop(1).forEach { segment ->
            if (segment.startMillis <= end) end = max(end, segment.endMillis)
            else {
                union += end - start
                start = segment.startMillis
                end = segment.endMillis
            }
        }
        union += end - start
        return Episode(ordered.first().startMillis, ordered.maxOf { it.endMillis }, union, ordered)
    }
}
