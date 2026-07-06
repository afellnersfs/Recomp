package com.arie.recomp.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.arie.recomp.MainActivity
import com.arie.recomp.R
import com.arie.recomp.data.Graph
import com.arie.recomp.data.Settings
import java.time.Duration
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object Channels {
    const val REMINDERS = "reminders"

    fun ensure(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(
            NotificationChannel(REMINDERS, "Reminders", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Workout and water reminders"
            }
        )
    }
}

object Notifier {
    fun show(context: Context, id: Int, title: String, text: String) {
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return
        val open = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, Channels.REMINDERS)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        runCatching { NotificationManagerCompat.from(context).notify(id, notification) }
    }
}

object ReminderScheduler {

    fun scheduleAllAsync(context: Context) {
        val app = context.applicationContext
        CoroutineScope(Dispatchers.Default).launch {
            Graph.init(app)
            scheduleWorkoutReminder(app)
            scheduleWaterReminder(app)
        }
    }

    suspend fun scheduleWorkoutReminder(context: Context) {
        val wm = WorkManager.getInstance(context)
        val s = Graph.settings.current()
        if (!s.reminderEnabled || s.trainingDays.isEmpty()) {
            wm.cancelUniqueWork("workout_reminder")
            return
        }
        val now = ZonedDateTime.now()
        val next = nextReminderTime(now, s) ?: return
        val delayMs = Duration.between(now, next).toMillis().coerceAtLeast(60_000)
        val req = OneTimeWorkRequestBuilder<WorkoutReminderWorker>()
            .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
            .build()
        wm.enqueueUniqueWork("workout_reminder", ExistingWorkPolicy.REPLACE, req)
    }

    /** Next configured training-day reminder that does not fall inside the Shabbat window. */
    fun nextReminderTime(now: ZonedDateTime, s: Settings): ZonedDateTime? {
        for (i in 0..14) {
            val date = now.toLocalDate().plusDays(i.toLong())
            if (date.dayOfWeek.value !in s.trainingDays) continue
            val candidate = date.atTime(s.reminderHour, s.reminderMinute).atZone(now.zone)
            if (!candidate.isAfter(now)) continue
            if (s.shabbatEnabled && Shabbat.isInWindow(candidate, s)) continue
            return candidate
        }
        return null
    }

    suspend fun scheduleWaterReminder(context: Context) {
        val wm = WorkManager.getInstance(context)
        val s = Graph.settings.current()
        if (!s.waterReminderEnabled) {
            wm.cancelUniqueWork("water_reminder")
            return
        }
        val req = PeriodicWorkRequestBuilder<WaterReminderWorker>(2, TimeUnit.HOURS).build()
        wm.enqueueUniquePeriodicWork("water_reminder", ExistingPeriodicWorkPolicy.UPDATE, req)
    }
}

class WorkoutReminderWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        Graph.init(applicationContext)
        val s = Graph.settings.current()
        val now = ZonedDateTime.now()
        val quiet = s.shabbatEnabled && Shabbat.isInWindow(now, s)
        if (s.reminderEnabled && !quiet && now.dayOfWeek.value in s.trainingDays) {
            val template = Graph.workouts.nextTemplate()
            Notifier.show(
                applicationContext, 1001,
                "Time to train 💪",
                "${template.name} tonight — about ${s.sessionMinutes} min. You've got this."
            )
        }
        ReminderScheduler.scheduleWorkoutReminder(applicationContext)
        return Result.success()
    }
}

class WaterReminderWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        Graph.init(applicationContext)
        val s = Graph.settings.current()
        val now = ZonedDateTime.now()
        val quiet = s.shabbatEnabled && Shabbat.isInWindow(now, s)
        val daytime = now.hour in 10..19
        if (s.waterReminderEnabled && !quiet && daytime) {
            val cups = Graph.nutrition.cupsTodayOnce()
            if (cups < s.waterGoalCups) {
                Notifier.show(
                    applicationContext, 1002,
                    "Water check 💧",
                    "$cups of ${s.waterGoalCups} cups so far today."
                )
            }
        }
        return Result.success()
    }
}

/** Reminders use one-shot WorkManager jobs, so re-arm them after a reboot. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Channels.ensure(context)
            ReminderScheduler.scheduleAllAsync(context)
        }
    }
}
