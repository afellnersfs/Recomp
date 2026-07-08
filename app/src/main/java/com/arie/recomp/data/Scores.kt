package com.arie.recomp.data

import com.arie.recomp.data.db.DailyScore
import com.arie.recomp.health.HealthConnectManager
import com.arie.recomp.health.SleepNight
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * In-app computed scores (Fitbit Premium's insights stay in Fitbit's app,
 * so we derive our own from the raw Health Connect data).
 */
object Scores {

    /**
     * 0–100: duration vs target (40%), deep+REM share (30%),
     * efficiency (20%), schedule consistency vs 14-day median (10%).
     */
    fun sleepScore(night: SleepNight, history: List<SleepNight>, targetMin: Long = 480): Int {
        val duration = (night.totalMin.toDouble() / targetMin).coerceIn(0.0, 1.0)

        val asleep = night.totalMin.coerceAtLeast(1)
        val deepRemShare = (night.deepMin + night.remMin).toDouble() / asleep
        val deepRem = (deepRemShare / 0.40).coerceIn(0.0, 1.0)   // ~40% deep+REM = ideal

        val inBedMin = ((night.endMs - night.startMs) / 60_000).coerceAtLeast(1)
        val efficiencyRaw = night.totalMin.toDouble() / inBedMin
        val efficiency = ((efficiencyRaw - 0.60) / 0.35).coerceIn(0.0, 1.0)  // 95%+ = full marks

        val consistency = run {
            val bedtimes = history.filter { it.startMs != night.startMs }.map { bedMinuteOfDay(it.startMs) }
            if (bedtimes.size < 3) 0.7 else {
                val median = bedtimes.sorted()[bedtimes.size / 2]
                val diff = abs(bedMinuteOfDay(night.startMs) - median)
                (1.0 - diff / 120.0).coerceIn(0.0, 1.0)          // 2h off schedule = 0
            }
        }

        return (100 * (0.40 * duration + 0.30 * deepRem + 0.20 * efficiency + 0.10 * consistency))
            .roundToInt().coerceIn(0, 100)
    }

    /** Minute-of-day shifted by 12h so bedtimes around midnight don't wrap. */
    private fun bedMinuteOfDay(ms: Long): Int {
        val t = Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault())
        return (t.hour * 60 + t.minute + 12 * 60) % (24 * 60)
    }

    /**
     * 0–100: resting HR vs 30-day baseline (40%), last night's sleep score (40%),
     * yesterday's training load vs weekly average (20%).
     */
    fun readiness(
        rhrLatest: Double?,
        rhrBaseline: Double?,
        sleepScore: Int?,
        loadYesterday: Double,
        loadWeekAvg: Double
    ): Int {
        val rhr = if (rhrLatest == null || rhrBaseline == null) 0.7 else {
            val delta = rhrLatest - rhrBaseline    // elevated RHR = worse recovery
            (0.75 - delta * 0.08).coerceIn(0.0, 1.0)
        }
        val sleep = (sleepScore?.toDouble() ?: 70.0) / 100.0
        val load = when {
            loadWeekAvg <= 0.0 -> 0.85
            loadYesterday / loadWeekAvg > 1.5 -> 0.45   // big session yesterday
            loadYesterday / loadWeekAvg > 0.8 -> 0.70
            else -> 1.0                                  // rested
        }
        return (100 * (0.40 * rhr + 0.40 * sleep + 0.20 * load)).roundToInt().coerceIn(0, 100)
    }

    fun verdict(readiness: Int, nextWorkoutName: String): String = when {
        readiness >= 75 -> "Good day to push $nextWorkoutName — chase the PRs."
        readiness >= 50 -> "Solid. Train $nextWorkoutName as planned."
        else -> "Take it lighter today — drop a set per exercise or go 10% lighter."
    }
}

/** Computes today's scores from Health Connect + training log and persists them. */
object DailyScoreCompute {

    suspend fun computeAndPersist(
        hc: HealthConnectManager,
        night: SleepNight?,
        nextWorkoutName: String
    ): Triple<Int?, Int?, String> {
        val history = if (night != null) hc.sleepHistory(14) else emptyList()
        val sleepScore = night?.let { Scores.sleepScore(it, history) }

        val rhrTrend = hc.restingHrTrend(30)
        val baseline = rhrTrend.map { it.second }.takeIf { it.isNotEmpty() }?.average()
        val latest = rhrTrend.lastOrNull()?.second

        val sets = Graph.db.workoutDao().allSetsOnce()
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now()
        fun dayVolume(d: LocalDate): Double {
            val start = d.atStartOfDay(zone).toInstant().toEpochMilli()
            val end = d.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
            return sets.filter { it.loggedAt in start until end }.sumOf { it.weightLbs * it.reps }
        }
        val loadYesterday = dayVolume(today.minusDays(1))
        val loadAvg = (1..7).map { dayVolume(today.minusDays(it.toLong())) }.average()

        val readiness = if (sleepScore == null && latest == null) null
        else Scores.readiness(latest, baseline, sleepScore, loadYesterday, loadAvg)
        val verdict = readiness?.let { Scores.verdict(it, nextWorkoutName) } ?: ""

        if (sleepScore != null || readiness != null) {
            Graph.db.scoreDao().upsert(
                DailyScore(todayKey(), sleepScore, readiness, System.currentTimeMillis())
            )
        }
        return Triple(sleepScore, readiness, verdict)
    }
}
