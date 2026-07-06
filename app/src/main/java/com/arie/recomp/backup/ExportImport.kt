package com.arie.recomp.backup

import android.content.Context
import android.net.Uri
import com.arie.recomp.data.Graph
import com.arie.recomp.data.db.BodyMetric
import com.arie.recomp.data.db.CalorieLog
import com.arie.recomp.data.db.PersonalRecord
import com.arie.recomp.data.db.SetLog
import com.arie.recomp.data.db.WaterLog
import com.arie.recomp.data.db.WorkoutSession
import org.json.JSONArray
import org.json.JSONObject

/**
 * Manual JSON export/import through the system file picker. The primary safety
 * net is Android Auto Backup (the Room DB is backed up to the user's Google
 * Drive automatically); this covers moving data by hand. Progress photo files
 * are not included — only the training/body/nutrition data.
 */
object ExportImport {

    suspend fun exportTo(context: Context, uri: Uri): Boolean = try {
        val json = buildJson()
        context.contentResolver.openOutputStream(uri)?.use { out ->
            out.write(json.toString(2).toByteArray(Charsets.UTF_8))
        } != null
    } catch (e: Exception) {
        false
    }

    suspend fun importFrom(context: Context, uri: Uri): Boolean = try {
        val text = context.contentResolver.openInputStream(uri)
            ?.bufferedReader(Charsets.UTF_8)?.readText()
        if (text.isNullOrBlank()) false else {
            restore(JSONObject(text))
            true
        }
    } catch (e: Exception) {
        false
    }

    private suspend fun buildJson(): JSONObject {
        val workoutDao = Graph.db.workoutDao()
        val bodyDao = Graph.db.bodyDao()
        val nutritionDao = Graph.db.nutritionDao()

        return JSONObject().apply {
            put("app", "recomp")
            put("version", 1)
            put("exportedAt", System.currentTimeMillis())
            put("sessions", JSONArray(workoutDao.allSessionsOnce().map { s ->
                JSONObject().put("id", s.id).put("templateId", s.templateId)
                    .put("startedAt", s.startedAt).put("endedAt", s.endedAt ?: JSONObject.NULL)
            }))
            put("sets", JSONArray(workoutDao.allSetsOnce().map { s ->
                JSONObject().put("id", s.id).put("sessionId", s.sessionId)
                    .put("exerciseId", s.exerciseId).put("setNumber", s.setNumber)
                    .put("weightLbs", s.weightLbs).put("reps", s.reps).put("loggedAt", s.loggedAt)
            }))
            put("prs", JSONArray(workoutDao.prsOnce().map { p ->
                JSONObject().put("exerciseId", p.exerciseId).put("weightLbs", p.weightLbs)
                    .put("reps", p.reps).put("estOneRm", p.estOneRm).put("achievedAt", p.achievedAt)
            }))
            put("metrics", JSONArray(bodyDao.metricsOnce().map { m ->
                JSONObject().put("id", m.id).put("type", m.type)
                    .put("value", m.value).put("recordedAt", m.recordedAt)
            }))
            put("water", JSONArray(nutritionDao.allWaterOnce().map { w ->
                JSONObject().put("id", w.id).put("cups", w.cups)
                    .put("day", w.day).put("loggedAt", w.loggedAt)
            }))
            put("calories", JSONArray(nutritionDao.allCaloriesOnce().map { c ->
                JSONObject().put("id", c.id).put("calories", c.calories).put("label", c.label)
                    .put("day", c.day).put("loggedAt", c.loggedAt)
            }))
        }
    }

    private suspend fun restore(json: JSONObject) {
        require(json.optString("app") == "recomp") { "Not a Recomp backup file" }
        val workoutDao = Graph.db.workoutDao()
        val bodyDao = Graph.db.bodyDao()
        val nutritionDao = Graph.db.nutritionDao()

        workoutDao.restoreSessions(json.getJSONArray("sessions").map { o ->
            WorkoutSession(
                id = o.getLong("id"), templateId = o.getString("templateId"),
                startedAt = o.getLong("startedAt"),
                endedAt = if (o.isNull("endedAt")) null else o.getLong("endedAt")
            )
        })
        workoutDao.restoreSets(json.getJSONArray("sets").map { o ->
            SetLog(
                id = o.getLong("id"), sessionId = o.getLong("sessionId"),
                exerciseId = o.getString("exerciseId"), setNumber = o.getInt("setNumber"),
                weightLbs = o.getDouble("weightLbs"), reps = o.getInt("reps"),
                loggedAt = o.getLong("loggedAt")
            )
        })
        workoutDao.restorePrs(json.getJSONArray("prs").map { o ->
            PersonalRecord(
                exerciseId = o.getString("exerciseId"), weightLbs = o.getDouble("weightLbs"),
                reps = o.getInt("reps"), estOneRm = o.getDouble("estOneRm"),
                achievedAt = o.getLong("achievedAt")
            )
        })
        bodyDao.restoreMetrics(json.getJSONArray("metrics").map { o ->
            BodyMetric(
                id = o.getLong("id"), type = o.getString("type"),
                value = o.getDouble("value"), recordedAt = o.getLong("recordedAt")
            )
        })
        nutritionDao.restoreWater(json.getJSONArray("water").map { o ->
            WaterLog(
                id = o.getLong("id"), cups = o.getInt("cups"),
                day = o.getString("day"), loggedAt = o.getLong("loggedAt")
            )
        })
        nutritionDao.restoreCalories(json.getJSONArray("calories").map { o ->
            CalorieLog(
                id = o.getLong("id"), calories = o.getInt("calories"), label = o.getString("label"),
                day = o.getString("day"), loggedAt = o.getLong("loggedAt")
            )
        })
    }

    private inline fun <T> JSONArray.map(transform: (JSONObject) -> T): List<T> =
        (0 until length()).map { transform(getJSONObject(it)) }
}
