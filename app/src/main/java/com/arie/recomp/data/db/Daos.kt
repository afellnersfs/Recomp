package com.arie.recomp.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutDao {
    @Insert suspend fun insertSession(s: WorkoutSession): Long
    @Update suspend fun updateSession(s: WorkoutSession)
    @Query("SELECT * FROM workout_session WHERE id = :id") suspend fun session(id: Long): WorkoutSession?
    @Query("DELETE FROM workout_session WHERE endedAt IS NULL AND id != :exceptId")
    suspend fun deleteAbandoned(exceptId: Long)

    @Query("SELECT * FROM workout_session WHERE endedAt IS NOT NULL ORDER BY startedAt DESC")
    fun completedSessions(): Flow<List<WorkoutSession>>
    @Query("SELECT COUNT(*) FROM workout_session WHERE endedAt IS NOT NULL")
    suspend fun completedCount(): Int
    @Query("SELECT * FROM workout_session ORDER BY startedAt") suspend fun allSessionsOnce(): List<WorkoutSession>

    @Insert suspend fun insertSet(s: SetLog): Long
    @Query("SELECT * FROM set_log WHERE sessionId = :sessionId ORDER BY loggedAt")
    suspend fun setsForSession(sessionId: Long): List<SetLog>

    // All sets of the most recent *finished* session that included this exercise.
    @Query(
        """SELECT * FROM set_log WHERE exerciseId = :exerciseId AND sessionId =
           (SELECT s.sessionId FROM set_log s JOIN workout_session w ON w.id = s.sessionId
            WHERE s.exerciseId = :exerciseId AND w.endedAt IS NOT NULL AND w.id != :excludeSessionId
            ORDER BY s.loggedAt DESC LIMIT 1)
           ORDER BY setNumber"""
    )
    suspend fun lastSessionSets(exerciseId: String, excludeSessionId: Long): List<SetLog>

    @Query("SELECT * FROM set_log ORDER BY loggedAt") fun allSets(): Flow<List<SetLog>>
    @Query("SELECT * FROM set_log ORDER BY loggedAt") suspend fun allSetsOnce(): List<SetLog>

    @Query("SELECT * FROM personal_record") fun prs(): Flow<List<PersonalRecord>>
    @Query("SELECT * FROM personal_record") suspend fun prsOnce(): List<PersonalRecord>
    @Query("SELECT * FROM personal_record WHERE exerciseId = :exerciseId") suspend fun pr(exerciseId: String): PersonalRecord?
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertPr(pr: PersonalRecord)

    // Restore-from-backup helpers
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun restoreSessions(items: List<WorkoutSession>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun restoreSets(items: List<SetLog>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun restorePrs(items: List<PersonalRecord>)
}

@Dao
interface BodyDao {
    @Insert suspend fun insertMetric(m: BodyMetric)
    @Delete suspend fun deleteMetric(m: BodyMetric)
    @Query("SELECT * FROM body_metric ORDER BY recordedAt") fun metrics(): Flow<List<BodyMetric>>
    @Query("SELECT * FROM body_metric ORDER BY recordedAt") suspend fun metricsOnce(): List<BodyMetric>

    @Insert suspend fun insertPhoto(p: ProgressPhoto)
    @Delete suspend fun deletePhoto(p: ProgressPhoto)
    @Query("SELECT * FROM progress_photo ORDER BY takenAt DESC") fun photos(): Flow<List<ProgressPhoto>>

    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun restoreMetrics(items: List<BodyMetric>)
}

@Dao
interface NutritionDao {
    @Insert suspend fun insertWater(w: WaterLog)
    @Query("SELECT COALESCE(SUM(cups), 0) FROM water_log WHERE day = :day")
    fun cupsForDay(day: String): Flow<Int>
    @Query("SELECT COALESCE(SUM(cups), 0) FROM water_log WHERE day = :day")
    suspend fun cupsForDayOnce(day: String): Int
    @Query("DELETE FROM water_log WHERE id = (SELECT id FROM water_log WHERE day = :day ORDER BY loggedAt DESC LIMIT 1)")
    suspend fun undoLastWater(day: String)

    @Insert suspend fun insertCalories(c: CalorieLog)
    @Delete suspend fun deleteCalories(c: CalorieLog)
    @Query("SELECT * FROM calorie_log WHERE day = :day ORDER BY loggedAt DESC")
    fun caloriesForDay(day: String): Flow<List<CalorieLog>>

    @Query("SELECT * FROM water_log") suspend fun allWaterOnce(): List<WaterLog>
    @Query("SELECT * FROM calorie_log") suspend fun allCaloriesOnce(): List<CalorieLog>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun restoreWater(items: List<WaterLog>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun restoreCalories(items: List<CalorieLog>)
}
