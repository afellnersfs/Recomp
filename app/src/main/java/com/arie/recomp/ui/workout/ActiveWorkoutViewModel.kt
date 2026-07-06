package com.arie.recomp.ui.workout

import android.app.Application
import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arie.recomp.data.Equipment
import com.arie.recomp.data.Exercise
import com.arie.recomp.data.ExerciseCatalog
import com.arie.recomp.data.Graph
import com.arie.recomp.data.Program
import com.arie.recomp.data.Suggestion
import com.arie.recomp.widgets.Widgets
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ActiveWorkoutViewModel(app: Application) : AndroidViewModel(app) {

    data class LoggedSet(val weight: Double, val reps: Int, val isPr: Boolean)

    data class ExerciseSlot(
        val exercise: Exercise,
        val suggestion: Suggestion,
        val logged: List<LoggedSet> = emptyList(),
        val skipped: Boolean = false
    )

    var sessionId by mutableStateOf<Long?>(null)
        private set
    var templateName by mutableStateOf("")
        private set
    var slots by mutableStateOf(listOf<ExerciseSlot>())
        private set
    var current by mutableIntStateOf(0)
        private set
    var weight by mutableDoubleStateOf(0.0)
        private set
    var reps by mutableIntStateOf(0)
        private set
    var restRemaining by mutableIntStateOf(0)
        private set
    var restTotal by mutableIntStateOf(75)
        private set
    var prFlash by mutableStateOf(false)
        private set

    private var restJob: Job? = null

    val allDone: Boolean get() = slots.isNotEmpty() && current >= slots.size
    val currentSlot: ExerciseSlot? get() = slots.getOrNull(current)

    fun start() {
        if (sessionId != null) return
        viewModelScope.launch {
            Graph.init(getApplication())
            val settings = Graph.settings.current()
            restTotal = settings.restSeconds
            val session = Graph.workouts.startSession()
            sessionId = session.id
            val template = Program.byId(session.templateId)
            templateName = template.name
            slots = Program.exerciseIdsFor(template, settings.sessionMinutes).map { id ->
                val ex = ExerciseCatalog.get(id)
                ExerciseSlot(ex, Graph.workouts.suggestionFor(ex, session.id))
            }
            applySuggestion(0)
        }
    }

    private fun applySuggestion(index: Int) {
        val slot = slots.getOrNull(index) ?: return
        weight = slot.suggestion.weightLbs
        reps = slot.suggestion.reps
    }

    private fun weightStep(): Double =
        if (currentSlot?.exercise?.equipment == Equipment.DUMBBELL) 2.5 else 5.0

    fun adjustWeight(up: Boolean) {
        val step = weightStep()
        weight = (weight + if (up) step else -step).coerceAtLeast(0.0)
    }

    fun adjustReps(up: Boolean) {
        val step = if (currentSlot?.exercise?.isTimed == true) 5 else 1
        reps = (reps + if (up) step else -step).coerceAtLeast(1)
    }

    fun logSet() {
        val id = sessionId ?: return
        val slot = currentSlot ?: return
        val w = weight
        val r = reps
        viewModelScope.launch {
            val isPr = Graph.workouts.logSet(id, slot.exercise, slot.logged.size + 1, w, r)
            val updated = slot.copy(logged = slot.logged + LoggedSet(w, r, isPr))
            slots = slots.toMutableList().also { it[current] = updated }
            if (isPr) {
                prFlash = true
                viewModelScope.launch { delay(3000); prFlash = false }
            }
            if (updated.logged.size >= Program.SETS_PER_EXERCISE) nextExercise() else startRest()
        }
    }

    fun nextExercise() {
        restJob?.cancel()
        restRemaining = 0
        if (current < slots.size - 1) {
            current += 1
            applySuggestion(current)
        } else {
            current = slots.size   // finished the list
        }
    }

    fun skipExercise() {
        slots = slots.toMutableList().also { it[current] = it[current].copy(skipped = true) }
        nextExercise()
    }

    private fun startRest() {
        restJob?.cancel()
        restRemaining = restTotal
        restJob = viewModelScope.launch {
            while (restRemaining > 0) {
                delay(1000)
                restRemaining -= 1
            }
            alert()
        }
    }

    fun addRest(seconds: Int) {
        if (restRemaining > 0) restRemaining += seconds
    }

    fun skipRest() {
        restJob?.cancel()
        restRemaining = 0
    }

    fun finish(onFinished: (Long) -> Unit) {
        val id = sessionId ?: return
        restJob?.cancel()
        viewModelScope.launch {
            Graph.workouts.finishSession(id)
            Widgets.refreshAllAsync(getApplication())
            onFinished(id)
        }
    }

    /** Rest over: vibrate + short beep. */
    private fun alert() {
        val context = getApplication<Application>()
        runCatching {
            val vibrator = if (Build.VERSION.SDK_INT >= 31) {
                (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager)
                    .defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
            vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 250, 120, 250), -1))
        }
        runCatching {
            val tone = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 85)
            tone.startTone(ToneGenerator.TONE_PROP_BEEP2, 400)
            viewModelScope.launch {
                delay(700)
                runCatching { tone.release() }
            }
        }
    }
}
