package com.arie.recomp.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        WorkoutSession::class, SetLog::class, PersonalRecord::class,
        BodyMetric::class, ProgressPhoto::class, WaterLog::class, CalorieLog::class,
        DailyScore::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun workoutDao(): WorkoutDao
    abstract fun bodyDao(): BodyDao
    abstract fun nutritionDao(): NutritionDao
    abstract fun scoreDao(): ScoreDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `daily_score` (" +
                        "`day` TEXT NOT NULL, " +
                        "`sleepScore` INTEGER, " +
                        "`readiness` INTEGER, " +
                        "`updatedAt` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`day`))"
                )
            }
        }
    }
}
