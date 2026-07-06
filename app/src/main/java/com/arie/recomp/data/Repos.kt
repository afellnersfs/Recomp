package com.arie.recomp.data

import android.content.Context
import androidx.room.Room
import com.arie.recomp.data.db.AppDatabase
import com.arie.recomp.data.db.BodyDao
import com.arie.recomp.data.db.BodyMetric
import com.arie.recomp.data.db.CalorieLog
import com.arie.recomp.data.db.NutritionDao
import com.arie.recomp.data.db.PersonalRecord
import com.arie.recomp.data.db.ProgressPhoto
import com.arie.recomp.data.db.SetLog
import com.arie.recomp.data.db.WaterLog
import com.arie.recomp.data.db.WorkoutDao
import com.arie.recomp.data.db.WorkoutSession
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.WeekFields

/** Tiny service locator — this is a single-user personal app, no DI framework needed. */
object Graph {
    lateinit var appContext: Context
        private set
    lateinit var db: AppDatabase
        private set
    lateinit var settings: SettingsStore
        private set
    lateinit var workouts: WorkoutRepository
        private set
    lateinit var nutrition: NutritionRepository
        private set
    lateinit var body: BodyRepository
        private set

    private var initialized = false

    @Synchronized
    fun init(context: Context) {
        if (initialized) return
        appContext = context.applicationContext
        db = Room.databaseBuilder(appContext, AppDatabase::class.java, "recomp.db").build()
        settings = SettingsStore(appContext)
        workouts = WorkoutRepository(db.workoutDao(), settings)
        nutrition = NutritionRepository(db.nutritionDao())
        body = BodyRepository(db.bodyDao(), appContext)
        initialized = true
    }
}

fun todayKey(): String = LocalDate.now().toString()

class WorkoutRepository(private val dao: WorkoutDao, private val settingsStore: SettingsStore) {

    val completedSessions = dao.completedSessions()
    val allSets = dao.allSets()
    val prs = dao.prs()

    suspend fun nextTemplate(): WorkoutTemplate = Program.templateByCount(dao.completedCount())

    suspend fun startSession(): WorkoutSession {
        val template = nextTemplate()
        val s = WorkoutSession(templateId = template.id, startedAt = System.currentTimeMillis())
        val id = dao.insertSession(s)
        dao.deleteAbandoned(exceptId = id)
        return s.copy(id = id)
    }

    suspend fun suggestionFor(ex: Exercise, currentSessionId: Long): Suggestion {
        val settings = settingsStore.current()
        val last = dao.lastSessionSets(ex.id, currentSessionId)
        return Progression.suggest(ex, last, settings.dumbbellMaxLbs)
    }

    /** Logs the set; returns true when it beats the existing PR (est. 1RM). */
    suspend fun logSet(sessionId: Long, ex: Exercise, setNumber: Int, weight: Double, reps: Int): Boolean {
        dao.insertSet(
            SetLog(
                sessionId = sessionId, exerciseId = ex.id, setNumber = setNumber,
                weightLbs = weight, reps = reps, loggedAt = System.currentTimeMillis()
            )
        )
        if (ex.isTimed || weight <= 0) return false
        val e1rm = Progression.epley(weight, reps)
        val existing = dao.pr(ex.id)
        if (existing == null || e1rm > existing.estOneRm) {
            dao.upsertPr(PersonalRecord(ex.id, weight, reps, e1rm, System.currentTimeMillis()))
            return existing != null   // very first log isn't celebrated as a PR
        }
        return false
    }

    suspend fun finishSession(sessionId: Long) {
        val s = dao.session(sessionId) ?: return
        dao.updateSession(s.copy(endedAt = System.currentTimeMillis()))
    }

    data class Summary(
        val templateName: String,
        val durationMin: Long,
        val totalVolume: Double,
        val setCount: Int,
        val prExerciseIds: List<String>
    )

    suspend fun summary(sessionId: Long): Summary? {
        val session = dao.session(sessionId) ?: return null
        val sets = dao.setsForSession(sessionId)
        val end = session.endedAt ?: System.currentTimeMillis()
        val prIds = dao.prsOnce()
            .filter { it.achievedAt in session.startedAt..end }
            .map { it.exerciseId }
        return Summary(
            templateName = Program.byId(session.templateId).name,
            durationMin = (end - session.startedAt) / 60000,
            totalVolume = sets.sumOf { it.weightLbs * it.reps },
            setCount = sets.size,
            prExerciseIds = prIds
        )
    }

    data class WeekStats(val doneThisWeek: Int, val planned: Int, val streakWeeks: Int)

    suspend fun weekStats(): WeekStats {
        val planned = settingsStore.current().plannedDaysPerWeek
        val sessions = dao.allSessionsOnce().filter { it.endedAt != null }
        val wf = WeekFields.ISO
        fun weekKey(d: LocalDate) = d.get(wf.weekBasedYear()) to d.get(wf.weekOfWeekBasedYear())
        fun dateOf(t: Long) = Instant.ofEpochMilli(t).atZone(ZoneId.systemDefault()).toLocalDate()

        val byWeek = sessions.groupBy { weekKey(dateOf(it.startedAt)) }.mapValues { it.value.size }
        val today = LocalDate.now()
        val done = byWeek[weekKey(today)] ?: 0

        var streak = 0
        var cursor = today.minusWeeks(1)
        while ((byWeek[weekKey(cursor)] ?: 0) >= planned) {
            streak++
            cursor = cursor.minusWeeks(1)
        }
        if (done >= planned) streak++
        return WeekStats(done, planned, streak)
    }
}

class NutritionRepository(private val dao: NutritionDao) {

    fun cupsToday() = dao.cupsForDay(todayKey())
    suspend fun cupsTodayOnce() = dao.cupsForDayOnce(todayKey())
    fun caloriesToday() = dao.caloriesForDay(todayKey())

    suspend fun addWater(cups: Int = 1) {
        dao.insertWater(WaterLog(cups = cups, day = todayKey(), loggedAt = System.currentTimeMillis()))
    }

    suspend fun undoWater() = dao.undoLastWater(todayKey())

    suspend fun addCalories(calories: Int, label: String) {
        dao.insertCalories(
            CalorieLog(calories = calories, label = label, day = todayKey(), loggedAt = System.currentTimeMillis())
        )
    }

    suspend fun deleteCalories(entry: CalorieLog) = dao.deleteCalories(entry)
}

class BodyRepository(private val dao: BodyDao, private val context: Context) {

    val metrics = dao.metrics()
    val photos = dao.photos()

    val photosDir: File
        get() = File(context.filesDir, "photos").apply { mkdirs() }

    fun photoFile(name: String) = File(photosDir, name)

    suspend fun addMetric(type: String, value: Double) {
        dao.insertMetric(BodyMetric(type = type, value = value, recordedAt = System.currentTimeMillis()))
    }

    suspend fun deleteMetric(m: BodyMetric) = dao.deleteMetric(m)

    suspend fun registerPhoto(fileName: String) {
        dao.insertPhoto(ProgressPhoto(fileName = fileName, takenAt = System.currentTimeMillis()))
    }

    suspend fun deletePhoto(p: ProgressPhoto) {
        photoFile(p.fileName).delete()
        dao.deletePhoto(p)
    }
}
