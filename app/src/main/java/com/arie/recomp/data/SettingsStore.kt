package com.arie.recomp.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

data class Settings(
    val sessionMinutes: Int = 30,
    val restSeconds: Int = 75,
    val plannedDaysPerWeek: Int = 3,
    val trainingDays: Set<Int> = setOf(7, 2, 4),   // ISO: Mon=1..Sun=7 -> Sun/Tue/Thu
    val reminderEnabled: Boolean = true,
    val reminderHour: Int = 18,
    val reminderMinute: Int = 30,
    val waterGoalCups: Int = 8,
    val waterReminderEnabled: Boolean = false,
    val calorieTarget: Int = 2000,
    val shabbatEnabled: Boolean = true,
    val shabbatUseSunset: Boolean = false,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val shabbatStart: String = "17:45",            // Friday, HH:mm (fixed-time fallback)
    val shabbatEnd: String = "21:30",              // Saturday, HH:mm
    val dumbbellMaxLbs: Double = 15.0,
    val hasFreeBarbell: Boolean = false,
    val goalWeightLbs: Double = 0.0,               // 0 = no goal band on the weight chart
    val onboarded: Boolean = false
)

class SettingsStore(private val context: Context) {

    private object K {
        val sessionMinutes = intPreferencesKey("sessionMinutes")
        val restSeconds = intPreferencesKey("restSeconds")
        val plannedDaysPerWeek = intPreferencesKey("plannedDaysPerWeek")
        val trainingDays = stringPreferencesKey("trainingDays")
        val reminderEnabled = booleanPreferencesKey("reminderEnabled")
        val reminderHour = intPreferencesKey("reminderHour")
        val reminderMinute = intPreferencesKey("reminderMinute")
        val waterGoalCups = intPreferencesKey("waterGoalCups")
        val waterReminderEnabled = booleanPreferencesKey("waterReminderEnabled")
        val calorieTarget = intPreferencesKey("calorieTarget")
        val shabbatEnabled = booleanPreferencesKey("shabbatEnabled")
        val shabbatUseSunset = booleanPreferencesKey("shabbatUseSunset")
        val latitude = doublePreferencesKey("latitude")
        val longitude = doublePreferencesKey("longitude")
        val shabbatStart = stringPreferencesKey("shabbatStart")
        val shabbatEnd = stringPreferencesKey("shabbatEnd")
        val dumbbellMaxLbs = doublePreferencesKey("dumbbellMaxLbs")
        val hasFreeBarbell = booleanPreferencesKey("hasFreeBarbell")
        val goalWeightLbs = doublePreferencesKey("goalWeightLbs")
        val onboarded = booleanPreferencesKey("onboarded")
    }

    val flow: Flow<Settings> = context.dataStore.data.map { p ->
        val d = Settings()
        Settings(
            sessionMinutes = p[K.sessionMinutes] ?: d.sessionMinutes,
            restSeconds = p[K.restSeconds] ?: d.restSeconds,
            plannedDaysPerWeek = p[K.plannedDaysPerWeek] ?: d.plannedDaysPerWeek,
            trainingDays = (p[K.trainingDays] ?: "7,2,4")
                .split(",").filter { it.isNotBlank() }.map { it.toInt() }.toSet(),
            reminderEnabled = p[K.reminderEnabled] ?: d.reminderEnabled,
            reminderHour = p[K.reminderHour] ?: d.reminderHour,
            reminderMinute = p[K.reminderMinute] ?: d.reminderMinute,
            waterGoalCups = p[K.waterGoalCups] ?: d.waterGoalCups,
            waterReminderEnabled = p[K.waterReminderEnabled] ?: d.waterReminderEnabled,
            calorieTarget = p[K.calorieTarget] ?: d.calorieTarget,
            shabbatEnabled = p[K.shabbatEnabled] ?: d.shabbatEnabled,
            shabbatUseSunset = p[K.shabbatUseSunset] ?: d.shabbatUseSunset,
            latitude = p[K.latitude] ?: d.latitude,
            longitude = p[K.longitude] ?: d.longitude,
            shabbatStart = p[K.shabbatStart] ?: d.shabbatStart,
            shabbatEnd = p[K.shabbatEnd] ?: d.shabbatEnd,
            dumbbellMaxLbs = p[K.dumbbellMaxLbs] ?: d.dumbbellMaxLbs,
            hasFreeBarbell = p[K.hasFreeBarbell] ?: d.hasFreeBarbell,
            goalWeightLbs = p[K.goalWeightLbs] ?: d.goalWeightLbs,
            onboarded = p[K.onboarded] ?: d.onboarded
        )
    }

    suspend fun current(): Settings = flow.first()

    suspend fun update(block: (Settings) -> Settings) {
        val next = block(current())
        context.dataStore.edit { p ->
            p[K.sessionMinutes] = next.sessionMinutes
            p[K.restSeconds] = next.restSeconds
            p[K.plannedDaysPerWeek] = next.plannedDaysPerWeek
            p[K.trainingDays] = next.trainingDays.joinToString(",")
            p[K.reminderEnabled] = next.reminderEnabled
            p[K.reminderHour] = next.reminderHour
            p[K.reminderMinute] = next.reminderMinute
            p[K.waterGoalCups] = next.waterGoalCups
            p[K.waterReminderEnabled] = next.waterReminderEnabled
            p[K.calorieTarget] = next.calorieTarget
            p[K.shabbatEnabled] = next.shabbatEnabled
            p[K.shabbatUseSunset] = next.shabbatUseSunset
            p[K.latitude] = next.latitude
            p[K.longitude] = next.longitude
            p[K.shabbatStart] = next.shabbatStart
            p[K.shabbatEnd] = next.shabbatEnd
            p[K.dumbbellMaxLbs] = next.dumbbellMaxLbs
            p[K.hasFreeBarbell] = next.hasFreeBarbell
            p[K.goalWeightLbs] = next.goalWeightLbs
            p[K.onboarded] = next.onboarded
        }
    }
}
