package com.arie.recomp.health

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.RestingHeartRateRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.request.AggregateGroupByDurationRequest
import androidx.health.connect.client.request.AggregateGroupByPeriodRequest
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.Period
import java.time.ZoneId

data class HealthSnapshot(
    val steps: Long? = null,
    val caloriesKcal: Double? = null,
    val sleepMinutes: Long? = null,
    val restingHr: Long? = null,
    val activeMinutes: Long? = null,
    val available: Boolean = false,
    val hasPermissions: Boolean = false
) {
    val sleepPoor: Boolean get() = sleepMinutes != null && sleepMinutes < 6 * 60
}

data class SleepStageSpan(val startMs: Long, val endMs: Long, val type: Int)

data class SleepNight(
    val startMs: Long,
    val endMs: Long,
    val stages: List<SleepStageSpan>,
    val totalMin: Long,   // asleep (excludes awake when stage data exists)
    val deepMin: Long,
    val remMin: Long,
    val lightMin: Long,
    val awakeMin: Long
)

/**
 * Reads Fitbit data through Health Connect (Fitbit syncs into Health Connect
 * when enabled in the Fitbit app: Profile > Health Connect > Sync).
 */
class HealthConnectManager(private val context: Context) {

    val permissions: Set<String> = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(HeartRateRecord::class),
        HealthPermission.getReadPermission(RestingHeartRateRecord::class),
        HealthPermission.getReadPermission(SleepSessionRecord::class),
        HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class),
        HealthPermission.getReadPermission(ExerciseSessionRecord::class)
    )

    fun sdkStatus(): Int = HealthConnectClient.getSdkStatus(context)
    fun isAvailable(): Boolean = sdkStatus() == HealthConnectClient.SDK_AVAILABLE

    private val client: HealthConnectClient by lazy { HealthConnectClient.getOrCreate(context) }
    private val zone: ZoneId get() = ZoneId.systemDefault()

    fun requestPermissionContract() = PermissionController.createRequestPermissionResultContract()

    suspend fun hasAllPermissions(): Boolean = runCatching {
        client.permissionController.getGrantedPermissions().containsAll(permissions)
    }.getOrDefault(false)

    suspend fun todaySnapshot(): HealthSnapshot {
        if (!isAvailable()) return HealthSnapshot(available = false)
        if (!hasAllPermissions()) return HealthSnapshot(available = true, hasPermissions = false)

        val startOfDay = LocalDate.now().atStartOfDay(zone).toInstant()
        val now = Instant.now()

        var steps: Long? = null
        var cals: Double? = null
        runCatching {
            val agg = client.aggregate(
                AggregateRequest(
                    metrics = setOf(StepsRecord.COUNT_TOTAL, TotalCaloriesBurnedRecord.ENERGY_TOTAL),
                    timeRangeFilter = TimeRangeFilter.between(startOfDay, now)
                )
            )
            steps = agg[StepsRecord.COUNT_TOTAL]
            cals = agg[TotalCaloriesBurnedRecord.ENERGY_TOTAL]?.inKilocalories
        }

        val night = runCatching { lastNight() }.getOrNull()
        val rhr = runCatching {
            client.readRecords(
                ReadRecordsRequest(
                    RestingHeartRateRecord::class,
                    TimeRangeFilter.between(now.minus(Duration.ofDays(7)), now)
                )
            ).records.maxByOrNull { it.time }?.beatsPerMinute
        }.getOrNull()

        val active = runCatching {
            client.readRecords(
                ReadRecordsRequest(ExerciseSessionRecord::class, TimeRangeFilter.between(startOfDay, now))
            ).records.sumOf { Duration.between(it.startTime, it.endTime).toMinutes() }
        }.getOrNull()

        return HealthSnapshot(
            steps = steps, caloriesKcal = cals,
            sleepMinutes = night?.totalMin, restingHr = rhr, activeMinutes = active,
            available = true, hasPermissions = true
        )
    }

    /** Sleep sessions in the last [days] days, mapped with stage breakdowns. */
    suspend fun sleepHistory(days: Int): List<SleepNight> = runCatching {
        val now = Instant.now()
        client.readRecords(
            ReadRecordsRequest(
                SleepSessionRecord::class,
                TimeRangeFilter.between(now.minus(Duration.ofDays(days.toLong())), now)
            )
        ).records.map { rec ->
            var deep = 0L; var rem = 0L; var light = 0L; var awake = 0L
            val spans = rec.stages.map { st ->
                val mins = Duration.between(st.startTime, st.endTime).toMinutes()
                when (st.stage) {
                    SleepSessionRecord.STAGE_TYPE_DEEP -> deep += mins
                    SleepSessionRecord.STAGE_TYPE_REM -> rem += mins
                    SleepSessionRecord.STAGE_TYPE_AWAKE,
                    SleepSessionRecord.STAGE_TYPE_AWAKE_IN_BED,
                    SleepSessionRecord.STAGE_TYPE_OUT_OF_BED -> awake += mins
                    else -> light += mins
                }
                SleepStageSpan(st.startTime.toEpochMilli(), st.endTime.toEpochMilli(), st.stage)
            }
            val sessionMin = Duration.between(rec.startTime, rec.endTime).toMinutes()
            val asleep = if (spans.isEmpty()) sessionMin else (deep + rem + light)
            SleepNight(
                startMs = rec.startTime.toEpochMilli(),
                endMs = rec.endTime.toEpochMilli(),
                stages = spans,
                totalMin = asleep,
                deepMin = deep, remMin = rem, lightMin = light, awakeMin = awake
            )
        }.sortedBy { it.startMs }
    }.getOrDefault(emptyList())

    /** Longest sleep session that ended since yesterday 18:00. */
    suspend fun lastNight(): SleepNight? {
        val cutoff = LocalDate.now().minusDays(1).atTime(18, 0).atZone(zone).toInstant().toEpochMilli()
        return sleepHistory(2).filter { it.endMs >= cutoff }.maxByOrNull { it.totalMin }
    }

    /** Today's steps in 24 hourly buckets. */
    suspend fun hourlySteps(): List<Double> = runCatching {
        val startOfDay = LocalDate.now().atStartOfDay(zone).toInstant()
        val buckets = client.aggregateGroupByDuration(
            AggregateGroupByDurationRequest(
                metrics = setOf(StepsRecord.COUNT_TOTAL),
                timeRangeFilter = TimeRangeFilter.between(startOfDay, Instant.now()),
                timeRangeSlicer = Duration.ofHours(1)
            )
        )
        val out = MutableList(24) { 0.0 }
        buckets.forEach { b ->
            val hour = b.startTime.atZone(zone).hour
            out[hour] = (b.result[StepsRecord.COUNT_TOTAL] ?: 0L).toDouble()
        }
        out
    }.getOrDefault(emptyList())

    /** Daily step totals for the last [days] days (dayStartMillis to steps). */
    suspend fun dailySteps(days: Int): List<Pair<Long, Double>> = runCatching {
        val start = LocalDate.now().minusDays(days.toLong() - 1).atStartOfDay(zone).toInstant()
        val buckets = client.aggregateGroupByPeriod(
            AggregateGroupByPeriodRequest(
                metrics = setOf(StepsRecord.COUNT_TOTAL),
                timeRangeFilter = TimeRangeFilter.between(
                    start.atZone(zone).toLocalDateTime(),
                    LocalDate.now().plusDays(1).atStartOfDay()
                ),
                timeRangeSlicer = Period.ofDays(1)
            )
        )
        buckets.map { b ->
            b.startTime.atZone(zone).toInstant().toEpochMilli() to
                (b.result[StepsRecord.COUNT_TOTAL] ?: 0L).toDouble()
        }
    }.getOrDefault(emptyList())

    /** Today's heart rate as per-hour (min, max) BPM; null where no samples. */
    suspend fun hourlyHeartRate(): List<Pair<Long, Long>?> = runCatching {
        val startOfDay = LocalDate.now().atStartOfDay(zone).toInstant()
        val records = client.readRecords(
            ReadRecordsRequest(HeartRateRecord::class, TimeRangeFilter.between(startOfDay, Instant.now()))
        ).records
        val byHour = HashMap<Int, MutableList<Long>>()
        records.forEach { rec ->
            rec.samples.forEach { s ->
                byHour.getOrPut(s.time.atZone(zone).hour) { mutableListOf() }.add(s.beatsPerMinute)
            }
        }
        List(24) { h ->
            byHour[h]?.let { it.min() to it.max() }
        }
    }.getOrDefault(emptyList())

    /** Resting HR readings over the last [days] days (timeMillis to bpm). */
    suspend fun restingHrTrend(days: Int): List<Pair<Long, Double>> = runCatching {
        val now = Instant.now()
        client.readRecords(
            ReadRecordsRequest(
                RestingHeartRateRecord::class,
                TimeRangeFilter.between(now.minus(Duration.ofDays(days.toLong())), now)
            )
        ).records
            .sortedBy { it.time }
            .map { it.time.toEpochMilli() to it.beatsPerMinute.toDouble() }
    }.getOrDefault(emptyList())
}
