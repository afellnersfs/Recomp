package com.arie.recomp.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        WorkoutSession::class, SetLog::class, PersonalRecord::class,
        BodyMetric::class, ProgressPhoto::class, WaterLog::class, CalorieLog::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun workoutDao(): WorkoutDao
    abstract fun bodyDao(): BodyDao
    abstract fun nutritionDao(): NutritionDao
}
