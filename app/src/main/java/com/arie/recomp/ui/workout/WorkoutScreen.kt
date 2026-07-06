package com.arie.recomp.ui.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.arie.recomp.ui.AppCard
import com.arie.recomp.ui.SectionLabel
import com.arie.recomp.ui.BigStepper
import com.arie.recomp.ui.fmtLbs
import com.arie.recomp.ui.openVideo
import com.arie.recomp.ui.theme.Accent
import com.arie.recomp.ui.theme.Bg

@Composable
fun WorkoutScreen(nav: NavHostController, vm: ActiveWorkoutViewModel = viewModel()) {
    val context = LocalContext.current
    LaunchedEffect(Unit) { vm.start() }

    fun goToSummary() = vm.finish { id ->
        nav.navigate("summary/$id") { popUpTo("home") }
    }

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(vm.templateName, style = MaterialTheme.typography.headlineMedium)
                    if (vm.slots.isNotEmpty() && !vm.allDone) {
                        Text(
                            "Exercise ${vm.current + 1} of ${vm.slots.size}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(onClick = { goToSummary() }) {
                    Icon(
                        Icons.Filled.Close, contentDescription = "Finish workout",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (vm.prFlash) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "🏆 NEW PR!",
                    style = MaterialTheme.typography.titleLarge,
                    color = Accent,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(Modifier.height(12.dp))

            val slot = vm.currentSlot
            when {
                vm.slots.isEmpty() -> {
                    Text(
                        "Loading…",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                vm.allDone -> {
                    AppCard {
                        Text("That's everything 🎉", style = MaterialTheme.typography.headlineMedium)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Log the workout to see your summary and PRs.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = { goToSummary() },
                            modifier = Modifier.fillMaxWidth().height(64.dp)
                        ) { Text("FINISH WORKOUT", style = MaterialTheme.typography.labelLarge) }
                    }
                }
                slot != null -> {
                    val ex = slot.exercise

                    // Exercise header: name + big play button opens the demo video
                    AppCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(ex.name, style = MaterialTheme.typography.titleLarge)
                                Text(
                                    slot.suggestion.note,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Accent
                                )
                            }
                            Surface(
                                onClick = { openVideo(context, ex) },
                                shape = CircleShape,
                                color = Accent,
                                modifier = Modifier.size(64.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Filled.PlayArrow,
                                        contentDescription = "Watch demo video",
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(40.dp)
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        ex.cues.forEach { cue ->
                            Text("•  $cue", style = MaterialTheme.typography.bodyMedium)
                        }
                        Spacer(Modifier.height(6.dp))
                        ex.mistakes.forEach { m ->
                            Text(
                                "✕  $m",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // Sets already logged
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        repeat(com.arie.recomp.data.Program.SETS_PER_EXERCISE) { i ->
                            val logged = slot.logged.getOrNull(i)
                            Surface(
                                shape = MaterialTheme.shapes.medium,
                                color = if (logged != null) MaterialTheme.colorScheme.surfaceVariant
                                else MaterialTheme.colorScheme.surface
                            ) {
                                Text(
                                    when {
                                        logged == null -> "Set ${i + 1}"
                                        ex.isTimed -> "${logged.reps}s ✓"
                                        logged.isPr -> "${fmtLbs(logged.weight)}×${logged.reps} 🏆"
                                        else -> "${fmtLbs(logged.weight)}×${logged.reps} ✓"
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    color = if (logged != null) MaterialTheme.colorScheme.onSurface
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // Weight & reps — huge steppers
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        if (!ex.isTimed && ex.startingWeightLbs >= 0 && ex.equipment != com.arie.recomp.data.Equipment.BODYWEIGHT) {
                            BigStepper(
                                value = fmtLbs(vm.weight),
                                label = "lbs",
                                onDec = { vm.adjustWeight(false) },
                                onInc = { vm.adjustWeight(true) }
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        BigStepper(
                            value = "${vm.reps}",
                            label = if (ex.isTimed) "seconds" else "reps",
                            onDec = { vm.adjustReps(false) },
                            onInc = { vm.adjustReps(true) }
                        )
                    }

                    Spacer(Modifier.height(20.dp))

                    Button(
                        onClick = { vm.logSet() },
                        modifier = Modifier.fillMaxWidth().height(72.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Accent)
                    ) {
                        Text(
                            "LOG SET ${slot.logged.size + 1}",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }

                    TextButton(
                        onClick = { vm.skipExercise() },
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                    ) {
                        Text("Skip exercise", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        // Rest timer overlay — auto-starts after each logged set
        if (vm.restRemaining > 0) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Bg.copy(alpha = 0.97f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    SectionLabel("Rest")
                    Text(
                        "%d:%02d".format(vm.restRemaining / 60, vm.restRemaining % 60),
                        style = MaterialTheme.typography.displayLarge
                    )
                    vm.currentSlot?.let { s ->
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Next: set ${s.logged.size + 1} of ${s.exercise.name}",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.height(24.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        OutlinedButton(
                            onClick = { vm.addRest(30) },
                            modifier = Modifier.height(56.dp)
                        ) { Text("+30s") }
                        Button(
                            onClick = { vm.skipRest() },
                            modifier = Modifier.height(56.dp)
                        ) { Text("SKIP — I'M READY") }
                    }
                }
            }
        }
    }
}
