package com.arie.recomp.ui.home

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.arie.recomp.data.Graph
import com.arie.recomp.data.Program
import com.arie.recomp.data.Settings
import com.arie.recomp.data.WorkoutRepository
import com.arie.recomp.data.WorkoutTemplate
import com.arie.recomp.data.ExerciseCatalog
import com.arie.recomp.health.HealthConnectManager
import com.arie.recomp.health.HealthSnapshot
import com.arie.recomp.ui.AppCard
import com.arie.recomp.ui.ProgressRing
import com.arie.recomp.ui.SectionLabel
import com.arie.recomp.ui.openUrl
import com.arie.recomp.ui.theme.Accent
import com.arie.recomp.update.UpdateChecker
import com.arie.recomp.widgets.Widgets
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(nav: NavHostController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settings by Graph.settings.flow.collectAsState(initial = Settings())
    val cups by Graph.nutrition.cupsToday().collectAsState(initial = 0)
    val sessions by Graph.workouts.completedSessions.collectAsState(initial = emptyList())

    val hc = remember { HealthConnectManager(context) }
    var snapshot by remember { mutableStateOf(HealthSnapshot()) }
    val hcLauncher = rememberLauncherForActivityResult(hc.requestPermissionContract()) {
        scope.launch { snapshot = hc.todaySnapshot() }
    }
    LaunchedEffect(Unit) { snapshot = hc.todaySnapshot() }

    var updateVersion by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) { updateVersion = UpdateChecker.newerVersion() }

    var week by remember { mutableStateOf(WorkoutRepository.WeekStats(0, 3, 0)) }
    var nextTemplate by remember { mutableStateOf<WorkoutTemplate?>(null) }
    LaunchedEffect(sessions) {
        week = Graph.workouts.weekStats()
        nextTemplate = Graph.workouts.nextTemplate()
    }

    val greeting = when (LocalTime.now().hour) {
        in 0..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        else -> "Good evening"
    }
    val todayIsTraining = LocalDate.now().dayOfWeek.value in settings.trainingDays

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        updateVersion?.let { v ->
            AppCard(onClick = { openUrl(context, UpdateChecker.DOWNLOAD_URL) }) {
                Text("⬆️  Update available — v$v", style = MaterialTheme.typography.titleMedium, color = Accent)
                Text(
                    "Tap to download, then open the file to install. All your data stays.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Column {
            Text(greeting, style = MaterialTheme.typography.headlineLarge)
            Text(
                "🔥 ${week.streakWeeks}-week streak  •  ${week.doneThisWeek}/${week.planned} workouts this week",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Today's plan — front and center
        AppCard {
            SectionLabel(if (todayIsTraining) "Today's workout" else "Rest day")
            nextTemplate?.let { t ->
                Text(t.name, style = MaterialTheme.typography.headlineMedium)
                Text(
                    Program.exerciseIdsFor(t, settings.sessionMinutes)
                        .joinToString("  •  ") { ExerciseCatalog.get(it).name },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            Spacer(Modifier.height(16.dp))
            if (todayIsTraining) {
                Button(
                    onClick = { nav.navigate("warmup") },
                    modifier = Modifier.fillMaxWidth().height(64.dp)
                ) { Text("START WORKOUT", style = MaterialTheme.typography.titleLarge) }
            } else {
                OutlinedButton(
                    onClick = { nav.navigate("warmup") },
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) { Text("TRAIN ANYWAY") }
            }
        }

        // Recovery hint from last night's sleep
        if (snapshot.sleepPoor) {
            AppCard {
                val m = snapshot.sleepMinutes ?: 0
                Text(
                    "😴 Short night (${m / 60}h ${m % 60}m). Consider going lighter today — " +
                        "drop a set per exercise or take 10% off the bar.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        // Fitbit stats via Health Connect
        Column {
            SectionLabel("Today — Fitbit via Health Connect")
            Spacer(Modifier.height(8.dp))
            when {
                !snapshot.available -> AppCard {
                    Text(
                        "Health Connect isn't set up on this phone.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    TextButton(onClick = {
                        openUrl(context, "market://details?id=com.google.android.apps.healthdata")
                    }) { Text("Get Health Connect") }
                }
                !snapshot.hasPermissions -> AppCard {
                    Text(
                        "Connect Health Connect to see steps, sleep, calories and heart rate here.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = { hcLauncher.launch(hc.permissions) }) { Text("Connect") }
                }
                else -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        HealthStat("Steps", snapshot.steps?.let { "%,d".format(it) } ?: "—", Modifier.weight(1f))
                        HealthStat("Calories", snapshot.caloriesKcal?.let { "%,d".format(it.toLong()) } ?: "—", Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        HealthStat(
                            "Sleep",
                            snapshot.sleepMinutes?.let { "${it / 60}h ${it % 60}m" } ?: "—",
                            Modifier.weight(1f)
                        )
                        HealthStat("Resting HR", snapshot.restingHr?.let { "$it bpm" } ?: "—", Modifier.weight(1f))
                    }
                }
            }
        }

        // Water ring — tap to add a cup
        AppCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ProgressRing(
                    progress = if (settings.waterGoalCups > 0) cups.toFloat() / settings.waterGoalCups else 0f,
                    modifier = Modifier.clickable {
                        scope.launch {
                            Graph.nutrition.addWater(1)
                            Widgets.refreshAllAsync(context)
                        }
                    }
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("$cups", style = MaterialTheme.typography.headlineMedium)
                        Text(
                            "of ${settings.waterGoalCups}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(Modifier.padding(8.dp))
                Column {
                    Text("Water", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Tap the ring for +1 cup",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun HealthStat(label: String, value: String, modifier: Modifier = Modifier) {
    AppCard(modifier) {
        Text(value, style = MaterialTheme.typography.headlineMedium)
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
