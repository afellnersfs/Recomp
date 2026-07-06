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
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class HealthSnapshot(
    val steps: Long? = null,
    val caloriesKcal: Double? = null,
    val sleepMinutes: Long? = null,
    val restingHr: Long? = null,
    val available: Boolean = false,
    val hasPermissions: Boolean = false
) {
    val sleepPoor: Boolean get() = sleepMinutes != null && sleepMinutes < 6 * 60
}

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

    fun requestPermissionContract() = PermissionController.createRequestPermissionResultContract()

    suspend fun hasAllPermissions(): Boolean = runCatching {
        client.permissionController.getGrantedPermissions().containsAll(permissions)
    }.getOrDefault(false)

    suspend fun todaySnapshot(): HealthSnapshot {
        if (!isAvailable()) return HealthSnapshot(available = false)
        if (!hasAllPermissions()) return HealthSnapshot(available = true, hasPermissions = false)

        val zone = ZoneId.systemDefault()
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

        // Last night's sleep: sessions between yesterday 18:00 and today 12:00.
        var sleepMin: Long? = null
        runCatching {
            val from = LocalDate.now().minusDays(1).atTime(18, 0).atZone(zone).toInstant()
            val to = LocalDate.now().atTime(12, 0).atZone(zone).toInstant()
            val sessions = client.readRecords(
                ReadRecordsRequest(SleepSessionRecord::class, TimeRangeFilter.between(from, to))
            ).records
            if (sessions.isNotEmpty()) {
                sleepMin = sessions.sumOf { Duration.between(it.startTime, it.endTime).toMinutes() }
            }
        }

        // Most recent resting HR within the last week.
        var rhr: Long? = null
        runCatching {
            val recs = client.readRecords(
                ReadRecordsRequest(
                    RestingHeartRateRecord::class,
                    TimeRangeFilter.between(now.minus(Duration.ofDays(7)), now)
                )
            ).records
            rhr = recs.maxByOrNull { it.time }?.beatsPerMinute
        }

        return HealthSnapshot(steps, cals, sleepMin, rhr, available = true, hasPermissions = true)
    }
}
