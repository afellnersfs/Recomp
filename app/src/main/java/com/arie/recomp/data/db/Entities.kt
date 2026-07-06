package com.arie.recomp.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "workout_session")
data class WorkoutSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val templateId: String,          // "A" or "B"
    val startedAt: Long,
    val endedAt: Long? = null
)

@Entity(tableName = "set_log")
data class SetLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val exerciseId: String,
    val setNumber: Int,
    val weightLbs: Double,
    val reps: Int,                   // for timed exercises (plank) this is seconds
    val loggedAt: Long
)

@Entity(tableName = "personal_record")
data class PersonalRecord(
    @PrimaryKey val exerciseId: String,
    val weightLbs: Double,
    val reps: Int,
    val estOneRm: Double,
    val achievedAt: Long
)

@Entity(tableName = "body_metric")
data class BodyMetric(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,                // "Weight", "Waist", "Chest", "Arms", or custom
    val value: Double,               // lbs for weight, inches for girths
    val recordedAt: Long
)

@Entity(tableName = "progress_photo")
data class ProgressPhoto(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fileName: String,            // stored under filesDir/photos/
    val takenAt: Long
)

@Entity(tableName = "water_log")
data class WaterLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val cups: Int,
    val day: String,                 // yyyy-MM-dd local date key
    val loggedAt: Long
)

@Entity(tableName = "calorie_log")
data class CalorieLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val calories: Int,
    val label: String,
    val day: String,
    val loggedAt: Long
)
