package com.arie.recomp.ui.home

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.arie.recomp.data.DailyScoreCompute
import com.arie.recomp.data.Graph
import com.arie.recomp.data.Settings
import com.arie.recomp.data.WorkoutRepository
import com.arie.recomp.health.HealthConnectManager
import com.arie.recomp.health.HealthSnapshot
import com.arie.recomp.health.SleepNight
import com.arie.recomp.ui.GlassCard
import com.arie.recomp.ui.Hypnogram
import com.arie.recomp.ui.ProgressRing
import com.arie.recomp.ui.SectionLabel
import com.arie.recomp.ui.StageSpan
import com.arie.recomp.ui.openUrl
import com.arie.recomp.ui.theme.Accent
import com.arie.recomp.ui.theme.AccentActivity
import com.arie.recomp.ui.theme.AccentHeart
import com.arie.recomp.ui.theme.AccentSleep
import com.arie.recomp.ui.theme.AccentWeight
import com.arie.recomp.ui.theme.OutlineDim
import com.arie.recomp.update.UpdateChecker
import com.arie.recomp.widgets.Widgets
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(nav: NavHostController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settings by Graph.settings.flow.collectAsState(initial = Settings())
    val cups by Graph.nutrition.cupsToday().collectAsState(initial = 0)
    val foodEntries by Graph.nutrition.caloriesToday().collectAsState(initial = emptyList())
    val sessions by Graph.workouts.completedSessions.collectAsState(initial = emptyList())

    val hc = remember { HealthConnectManager(context) }
    var snapshot by remember { mutableStateOf(HealthSnapshot()) }
    var night by remember { mutableStateOf<SleepNight?>(null) }
    var sleepScore by remember { mutableStateOf<Int?>(null) }
    var readiness by remember { mutableStateOf<Int?>(null) }
    var verdict by remember { mutableStateOf("") }
    var updateVersion by remember { mutableStateOf<String?>(null) }
    var refreshing by remember { mutableStateOf(false) }
    var refreshTick by remember { mutableIntStateOf(0) }

    val hcLauncher = rememberLauncherForActivityResult(hc.requestPermissionContract()) {
        refreshTick++
    }

    var week by remember { mutableStateOf(WorkoutRepository.WeekStats(0, 3, 0)) }
    var nextName by remember { mutableStateOf("") }
    LaunchedEffect(sessions) {
        week = Graph.workouts.weekStats()
        nextName = Graph.workouts.nextTemplate().name
    }

    LaunchedEffect(refreshTick) {
        snapshot = hc.todaySnapshot()
        night = hc.lastNight()
        val computed = DailyScoreCompute.computeAndPersist(hc, night, nextName)
        sleepScore = computed.first
        readiness = computed.second
        verdict = computed.third
        refreshing = false
    }
    LaunchedEffect(Unit) { updateVersion = UpdateChecker.newerVersion() }

    val greeting = when (LocalTime.now().hour) {
        in 0..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        else -> "Good evening"
    }
    val todayIsTraining = LocalDate.now().dayOfWeek.value in settings.trainingDays

    PullToRefreshBox(
        isRefreshing = refreshing,
        onRefresh = {
            refreshing = true
            refreshTick++
        },
        modifier = Modifier.fillMaxSize()
    ) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 330.dp),
            modifier = Modifier.fillMaxSize().statusBarsPadding(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 18.dp, end = 18.dp, top = 12.dp, bottom = 120.dp
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column {
                    Text(greeting, style = MaterialTheme.typography.headlineLarge)
                    Text(
                        LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMMM d")) +
                            "   •   🔥 ${week.streakWeeks}-week streak",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            updateVersion?.let { v ->
                item(span = { GridItemSpan(maxLineSpan) }) {
                    GlassCard(onClick = { openUrl(context, UpdateChecker.DOWNLOAD_URL) }) {
                        Text("⬆️  Update available — v$v", style = MaterialTheme.typography.titleMedium, color = Accent)
                        Text(
                            "Tap to download, then open the file to install. All your data stays.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // ---- Hero: daily activity rings ----
            item(span = { GridItemSpan(maxLineSpan) }) {
                GlassCard(onClick = { nav.navigate("steps") }) {
                    SectionLabel("Daily activity")
                    Spacer(Modifier.height(10.dp))
                    if (!snapshot.available || !snapshot.hasPermissions) {
                        HealthConnectPrompt(snapshot, onConnect = { hcLauncher.launch(hc.permissions) })
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TripleRing(
                                fractions = listOf(
                                    (snapshot.steps ?: 0L) / 10_000f,
                                    (snapshot.activeMinutes ?: 0L) / 30f,
                                    ((snapshot.caloriesKcal ?: 0.0) / settings.calorieTarget.coerceAtLeast(1)).toFloat()
                                ),
                                colors = listOf(AccentActivity, Accent, AccentWeight)
                            )
                            Spacer(Modifier.width(20.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                RingLegend(AccentActivity, snapshot.steps?.let { "%,d".format(it) } ?: "—", "steps · 10k goal")
                                RingLegend(Accent, "${snapshot.activeMinutes ?: 0}", "active min · 30 goal")
                                RingLegend(AccentWeight, snapshot.caloriesKcal?.let { "%,d".format(it.toLong()) } ?: "—", "cal burned")
                            }
                        }
                    }
                }
            }

            // ---- Readiness ----
            item {
                GlassCard(onClick = { nav.navigate("sleep") }) {
                    SectionLabel("Readiness")
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ProgressRing(
                            progress = (readiness ?: 0) / 100f,
                            ringSize = 84.dp,
                            strokeWidth = 9.dp,
                            color = scoreColor(readiness)
                        ) {
                            Text(
                                readiness?.toString() ?: "—",
                                style = MaterialTheme.typography.headlineMedium
                            )
                        }
                        Spacer(Modifier.width(14.dp))
                        Text(
                            verdict.ifBlank { "Connect Health Connect to see recovery guidance." },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // ---- Sleep ----
            item {
                GlassCard(onClick = { nav.navigate("sleep") }) {
                    Row {
                        SectionLabel("Sleep")
                        Spacer(Modifier.weight(1f))
                        sleepScore?.let {
                            Text(
                                "$it",
                                style = MaterialTheme.typography.titleMedium,
                                color = AccentSleep
                            )
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    val m = night?.totalMin
                    Text(
                        if (m != null) "${m / 60}h ${m % 60}m" else "—",
                        style = MaterialTheme.typography.headlineMedium,
                        color = AccentSleep
                    )
                    Spacer(Modifier.height(8.dp))
                    Hypnogram(night.toSpans(), compact = true)
                }
            }

            // ---- Heart ----
            item {
                GlassCard(onClick = { nav.navigate("heart") }) {
                    SectionLabel("Heart")
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            snapshot.restingHr?.toString() ?: "—",
                            style = MaterialTheme.typography.headlineMedium,
                            color = AccentHeart
                        )
                        Text(
                            " bpm resting",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                    Text(
                        "Tap for today's 24-hour range",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // ---- Workouts / today's plan ----
            item(span = { GridItemSpan(maxLineSpan) }) {
                GlassCard(onClick = { nav.navigate("workouts") }) {
                    SectionLabel(if (todayIsTraining) "Today's workout" else "Rest day")
                    Spacer(Modifier.height(4.dp))
                    Text(nextName, style = MaterialTheme.typography.headlineMedium)
                    Text(
                        "${week.doneThisWeek} of ${week.planned} workouts this week",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (snapshot.sleepPoor) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "😴 Short night — consider a lighter session.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = AccentSleep
                        )
                    }
                    Spacer(Modifier.height(14.dp))
                    if (todayIsTraining) {
                        Button(
                            onClick = { nav.navigate("warmup") },
                            modifier = Modifier.fillMaxWidth().height(60.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Accent)
                        ) { Text("START WORKOUT", style = MaterialTheme.typography.titleMedium) }
                    } else {
                        OutlinedButton(
                            onClick = { nav.navigate("warmup") },
                            modifier = Modifier.fillMaxWidth().height(52.dp)
                        ) { Text("TRAIN ANYWAY") }
                    }
                }
            }

            // ---- Weight ----
            item {
                val metrics by Graph.body.metrics.collectAsState(initial = emptyList())
                val weights = metrics.filter { it.type == "Weight" }
                GlassCard(onClick = { nav.navigate("weight") }) {
                    SectionLabel("Weight")
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            weights.lastOrNull()?.value?.let { com.arie.recomp.ui.fmtLbs(it) } ?: "—",
                            style = MaterialTheme.typography.headlineMedium,
                            color = AccentWeight
                        )
                        Text(
                            " lb",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                    Text(
                        if (weights.size >= 2) "Tap for the trend and 7-day average"
                        else "Log weight in the detail screen",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // ---- Water ----
            item {
                GlassCard(onClick = { nav.navigate("fuel") }) {
                    SectionLabel("Water & fuel")
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ProgressRing(
                            progress = if (settings.waterGoalCups > 0) cups.toFloat() / settings.waterGoalCups else 0f,
                            ringSize = 76.dp,
                            strokeWidth = 8.dp,
                            color = Accent,
                            modifier = Modifier.clickable {
                                scope.launch {
                                    Graph.nutrition.addWater(1)
                                    Widgets.refreshAllAsync(context)
                                }
                            }
                        ) {
                            Text("$cups", style = MaterialTheme.typography.titleLarge)
                        }
                        Spacer(Modifier.width(14.dp))
                        Column {
                            Text(
                                "of ${settings.waterGoalCups} cups — tap ring for +1",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "🍽 %,d / %,d kcal".format(
                                    foodEntries.sumOf { it.calories }, settings.calorieTarget
                                ),
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HealthConnectPrompt(snapshot: HealthSnapshot, onConnect: () -> Unit) {
    val context = LocalContext.current
    if (!snapshot.available) {
        Text(
            "Health Connect isn't set up on this phone.",
            style = MaterialTheme.typography.bodyMedium
        )
        OutlinedButton(onClick = {
            openUrl(context, "market://details?id=com.google.android.apps.healthdata")
        }, modifier = Modifier.padding(top = 8.dp)) { Text("Get Health Connect") }
    } else {
        Text(
            "Connect Health Connect to see steps, sleep, calories and heart rate.",
            style = MaterialTheme.typography.bodyMedium
        )
        Button(onClick = onConnect, modifier = Modifier.padding(top = 8.dp)) { Text("Connect") }
    }
}

@Composable
private fun RingLegend(color: Color, value: String, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Canvas(Modifier.size(10.dp)) { drawCircle(color) }
        Spacer(Modifier.width(8.dp))
        Column {
            Text(value, style = MaterialTheme.typography.titleMedium)
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** Three concentric activity rings, fill animates on load. */
@Composable
fun TripleRing(
    fractions: List<Float>,
    colors: List<Color>,
    ringSize: Dp = 148.dp
) {
    val animated = fractions.map { f ->
        animateFloatAsState(f.coerceIn(0f, 1f), tween(900), label = "ring").value
    }
    Canvas(Modifier.size(ringSize)) {
        val stroke = 11.dp.toPx()
        repeat(minOf(3, fractions.size)) { i ->
            val inset = stroke * 1.4f * i + stroke / 2
            val arcSize = Size(size.width - inset * 2, size.height - inset * 2)
            val topLeft = Offset(inset, inset)
            drawArc(
                OutlineDim, -90f, 360f, false,
                topLeft = topLeft, size = arcSize,
                style = Stroke(stroke, cap = StrokeCap.Round)
            )
            drawArc(
                colors[i], -90f, 360f * animated[i], false,
                topLeft = topLeft, size = arcSize,
                style = Stroke(stroke, cap = StrokeCap.Round)
            )
        }
    }
}

fun scoreColor(score: Int?): Color = when {
    score == null -> OutlineDim
    score >= 75 -> AccentActivity
    score >= 50 -> AccentWeight
    else -> AccentHeart
}

fun SleepNight?.toSpans(): List<StageSpan> {
    val n = this ?: return emptyList()
    return n.stages.map { s ->
        val row = when (s.type) {
            androidx.health.connect.client.records.SleepSessionRecord.STAGE_TYPE_DEEP -> 3
            androidx.health.connect.client.records.SleepSessionRecord.STAGE_TYPE_REM -> 1
            androidx.health.connect.client.records.SleepSessionRecord.STAGE_TYPE_LIGHT,
            androidx.health.connect.client.records.SleepSessionRecord.STAGE_TYPE_SLEEPING,
            androidx.health.connect.client.records.SleepSessionRecord.STAGE_TYPE_UNKNOWN -> 2
            else -> 0
        }
        StageSpan(s.startMs, s.endMs, row)
    }
}
