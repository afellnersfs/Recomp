package com.arie.recomp.widgets

import android.content.Context
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.arie.recomp.MainActivity
import com.arie.recomp.data.ExerciseCatalog
import com.arie.recomp.data.Graph
import com.arie.recomp.health.HealthConnectManager
import java.time.LocalDate
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private val WidgetBg = Color(0xFF141926)
private val Accent = Color(0xFF2E5CFF)
private val Track = Color(0xFF2A2E36)
private val Dim = Color(0xFF9BA1AC)

private fun GlanceModifier.widgetBg(): GlanceModifier {
    val m = background(ColorProvider(WidgetBg))
    return if (Build.VERSION.SDK_INT >= 31) m.cornerRadius(16.dp) else m
}

private fun title(size: Int = 20) =
    TextStyle(color = ColorProvider(Color.White), fontSize = size.sp, fontWeight = FontWeight.Bold)

private fun caption() = TextStyle(color = ColorProvider(Dim), fontSize = 11.sp)

/** Health Connect numbers cached for widgets, refreshed by [WidgetUpdateWorker]. */
object WidgetSnapshot {
    private const val PREFS = "widget_snapshot"

    fun save(context: Context, steps: Long?, cals: Double?) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putLong("steps", steps ?: -1L)
            .putLong("cals", cals?.toLong() ?: -1L)
            .putString("day", LocalDate.now().toString())
            .apply()
    }

    data class Data(val steps: Long?, val cals: Long?)

    fun load(context: Context): Data {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (p.getString("day", "") != LocalDate.now().toString()) return Data(null, null)
        return Data(
            p.getLong("steps", -1L).takeIf { it >= 0 },
            p.getLong("cals", -1L).takeIf { it >= 0 }
        )
    }
}

object Widgets {
    suspend fun refreshAll(context: Context) {
        WaterWidget().updateAll(context)
        StatsWidget().updateAll(context)
        StreakWidget().updateAll(context)
    }

    fun refreshAllAsync(context: Context) {
        val app = context.applicationContext
        CoroutineScope(Dispatchers.Default).launch { runCatching { refreshAll(app) } }
    }
}

// ---------- Water: tap anywhere = +1 cup ----------

class WaterWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        Graph.init(context)
        val cups = Graph.nutrition.cupsTodayOnce()
        val goal = Graph.settings.current().waterGoalCups
        provideContent {
            Column(
                modifier = GlanceModifier.fillMaxSize().widgetBg().padding(12.dp)
                    .clickable(actionRunCallback<LogWaterAction>()),
                verticalAlignment = Alignment.CenterVertically,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("💧 $cups / $goal", style = title())
                Spacer(GlanceModifier.height(8.dp))
                LinearProgressIndicator(
                    progress = if (goal > 0) (cups.toFloat() / goal).coerceIn(0f, 1f) else 0f,
                    modifier = GlanceModifier.fillMaxWidth().height(6.dp),
                    color = ColorProvider(Accent),
                    backgroundColor = ColorProvider(Track)
                )
                Spacer(GlanceModifier.height(6.dp))
                Text("TAP FOR +1 CUP", style = caption())
            }
        }
    }
}

class LogWaterAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        Graph.init(context)
        Graph.nutrition.addWater(1)
        WaterWidget().updateAll(context)
    }
}

class WaterWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = WaterWidget()
}

// ---------- Steps & calories from Health Connect ----------

class StatsWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = WidgetSnapshot.load(context)
        provideContent {
            Row(
                modifier = GlanceModifier.fillMaxSize().widgetBg().padding(12.dp)
                    .clickable(actionStartActivity<MainActivity>()),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = GlanceModifier.defaultWeight()) {
                    Text("👟 ${data.steps?.let { "%,d".format(it) } ?: "—"}", style = title(18))
                    Text("steps", style = caption())
                    Spacer(GlanceModifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = ((data.steps ?: 0L).toFloat() / 10_000f).coerceIn(0f, 1f),
                        modifier = GlanceModifier.fillMaxWidth().height(4.dp),
                        color = ColorProvider(Accent),
                        backgroundColor = ColorProvider(Track)
                    )
                }
                Spacer(GlanceModifier.width(16.dp))
                Column(modifier = GlanceModifier.defaultWeight()) {
                    Text("🔥 ${data.cals?.let { "%,d".format(it) } ?: "—"}", style = title(18))
                    Text("calories burned", style = caption())
                }
            }
        }
    }
}

class StatsWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = StatsWidget()
}

// ---------- Streak & latest PRs ----------

class StreakWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        Graph.init(context)
        val stats = Graph.workouts.weekStats()
        val prs = Graph.db.workoutDao().prsOnce().sortedByDescending { it.achievedAt }.take(2)
        provideContent {
            Column(
                modifier = GlanceModifier.fillMaxSize().widgetBg().padding(12.dp)
                    .clickable(actionStartActivity<MainActivity>())
            ) {
                Text("🔥 ${stats.streakWeeks}-week streak", style = title(16))
                Text("${stats.doneThisWeek}/${stats.planned} workouts this week", style = caption())
                Spacer(GlanceModifier.height(6.dp))
                if (prs.isEmpty()) {
                    Text("PRs will show up here", style = caption())
                } else {
                    prs.forEach { pr ->
                        val name = ExerciseCatalog.get(pr.exerciseId).name
                        Text(
                            "🏆 $name  ${trim(pr.weightLbs)}×${pr.reps}",
                            style = TextStyle(color = ColorProvider(Accent), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }
    }

    private fun trim(v: Double): String =
        if (v == v.toLong().toDouble()) v.toLong().toString() else v.toString()
}

class StreakWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = StreakWidget()
}

// ---------- Periodic refresh ----------

class WidgetUpdateWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        Graph.init(applicationContext)
        val snap = HealthConnectManager(applicationContext).todaySnapshot()
        if (snap.hasPermissions) {
            WidgetSnapshot.save(applicationContext, snap.steps, snap.caloriesKcal)
        }
        runCatching { Widgets.refreshAll(applicationContext) }
        return Result.success()
    }

    companion object {
        fun schedule(context: Context) {
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "widget_update",
                ExistingPeriodicWorkPolicy.UPDATE,
                PeriodicWorkRequestBuilder<WidgetUpdateWorker>(30, TimeUnit.MINUTES).build()
            )
        }
    }
}
