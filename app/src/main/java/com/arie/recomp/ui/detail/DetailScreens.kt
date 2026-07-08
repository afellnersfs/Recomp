package com.arie.recomp.ui.detail

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.arie.recomp.data.Graph
import com.arie.recomp.data.Settings
import com.arie.recomp.health.HealthConnectManager
import com.arie.recomp.health.SleepNight
import com.arie.recomp.ui.BarChart
import com.arie.recomp.ui.GlassCard
import com.arie.recomp.ui.HourlyBars
import com.arie.recomp.ui.HrBandChart
import com.arie.recomp.ui.Hypnogram
import com.arie.recomp.ui.LineChart
import com.arie.recomp.ui.SectionLabel
import com.arie.recomp.ui.Segmented
import com.arie.recomp.ui.home.toSpans
import com.arie.recomp.ui.theme.AccentActivity
import com.arie.recomp.ui.theme.AccentHeart
import com.arie.recomp.ui.theme.AccentSleep
import com.arie.recomp.ui.theme.AccentWeight
import com.arie.recomp.ui.theme.StageAwake
import com.arie.recomp.ui.theme.StageDeep
import com.arie.recomp.ui.theme.StageLight
import com.arie.recomp.ui.theme.StageRem
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.launch

@Composable
private fun DetailColumn(title: String, content: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(horizontal = 18.dp)
            .padding(top = 12.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(title, style = MaterialTheme.typography.headlineLarge)
        content()
    }
}

// ---------------- Sleep ----------------

@Composable
fun SleepDetailScreen() {
    val context = LocalContext.current
    val hc = remember { HealthConnectManager(context) }
    var nights by remember { mutableStateOf<List<SleepNight>>(emptyList()) }
    LaunchedEffect(Unit) { nights = hc.sleepHistory(14) }
    val night = nights.maxByOrNull { it.endMs }
    val scores by Graph.db.scoreDao().all().collectAsState(initial = emptyList())

    DetailColumn("Sleep") {
        GlassCard {
            SectionLabel("Last night")
            Spacer(Modifier.height(8.dp))
            if (night == null) {
                Text(
                    "No sleep data yet — make sure Fitbit is syncing to Health Connect.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    "${night.totalMin / 60}h ${night.totalMin % 60}m asleep",
                    style = MaterialTheme.typography.headlineMedium,
                    color = AccentSleep
                )
                Spacer(Modifier.height(12.dp))
                Hypnogram(night.toSpans())
                Spacer(Modifier.height(14.dp))
                StageRow(StageAwake, "Awake", night.awakeMin, night)
                StageRow(StageRem, "REM", night.remMin, night)
                StageRow(StageLight, "Light", night.lightMin, night)
                StageRow(StageDeep, "Deep", night.deepMin, night)
            }
        }

        GlassCard {
            SectionLabel("Last 14 nights")
            Spacer(Modifier.height(8.dp))
            val dayFmt = DateTimeFormatter.ofPattern("E")
            BarChart(
                nights.takeLast(14).map { n ->
                    val d = Instant.ofEpochMilli(n.endMs).atZone(ZoneId.systemDefault())
                    d.format(dayFmt).take(2) to n.totalMin / 60.0
                },
                color = AccentSleep
            )
        }

        GlassCard {
            SectionLabel("Sleep score trend")
            Spacer(Modifier.height(8.dp))
            LineChart(
                scores.filter { it.sleepScore != null }
                    .map { s ->
                        java.time.LocalDate.parse(s.day)
                            .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() to
                            s.sleepScore!!.toDouble()
                    },
                color = AccentSleep,
                valueFormatter = { "${it.toInt()}" }
            )
        }
    }
}

@Composable
private fun StageRow(color: Color, name: String, minutes: Long, night: SleepNight) {
    val inBed = ((night.endMs - night.startMs) / 60_000).coerceAtLeast(1)
    Row(
        Modifier.fillMaxWidth().padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Canvas(Modifier.size(10.dp)) { drawCircle(color) }
        Spacer(Modifier.width(10.dp))
        Text(name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Text(
            "${minutes / 60}h ${minutes % 60}m",
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(Modifier.width(10.dp))
        Text(
            "${minutes * 100 / inBed}%",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ---------------- Heart ----------------

@Composable
fun HeartDetailScreen() {
    val context = LocalContext.current
    val hc = remember { HealthConnectManager(context) }
    var hourly by remember { mutableStateOf<List<Pair<Long, Long>?>>(emptyList()) }
    var resting by remember { mutableStateOf<List<Pair<Long, Double>>>(emptyList()) }
    LaunchedEffect(Unit) {
        hourly = hc.hourlyHeartRate()
        resting = hc.restingHrTrend(30)
    }

    DetailColumn("Heart rate") {
        GlassCard {
            SectionLabel("Today — hourly range")
            Spacer(Modifier.height(8.dp))
            HrBandChart(
                hourly = hourly,
                restingHr = resting.lastOrNull()?.second?.toLong(),
                color = AccentHeart
            )
        }
        GlassCard {
            SectionLabel("Resting HR — 30 days")
            Spacer(Modifier.height(8.dp))
            LineChart(resting, color = AccentHeart, valueFormatter = { "${it.toInt()} bpm" })
        }
    }
}

// ---------------- Steps ----------------

@Composable
fun StepsDetailScreen() {
    val context = LocalContext.current
    val hc = remember { HealthConnectManager(context) }
    var hourly by remember { mutableStateOf<List<Double>>(emptyList()) }
    var daily by remember { mutableStateOf<List<Pair<Long, Double>>>(emptyList()) }
    var range by remember { mutableIntStateOf(0) }
    LaunchedEffect(range) {
        hourly = hc.hourlySteps()
        daily = hc.dailySteps(if (range == 0) 7 else 30)
    }

    DetailColumn("Steps") {
        GlassCard {
            SectionLabel("Today by hour")
            Spacer(Modifier.height(8.dp))
            HourlyBars(hourly, color = AccentActivity)
        }
        GlassCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SectionLabel("Daily totals")
                Spacer(Modifier.weight(1f))
                Segmented(listOf("7 days", "30 days"), range, { range = it })
            }
            Spacer(Modifier.height(8.dp))
            val fmt = DateTimeFormatter.ofPattern("M/d")
            BarChart(
                daily.map { (t, v) ->
                    Instant.ofEpochMilli(t).atZone(ZoneId.systemDefault()).format(fmt) to v
                }.let { if (range == 1) it.filterIndexed { i, _ -> i % 5 == 0 || i == it.lastIndex } else it },
                color = AccentActivity
            )
        }
    }
}

// ---------------- Weight ----------------

@Composable
fun WeightDetailScreen() {
    val scope = rememberCoroutineScope()
    val metrics by Graph.body.metrics.collectAsState(initial = emptyList())
    val settings by Graph.settings.flow.collectAsState(initial = Settings())
    val weights = metrics.filter { it.type == "Weight" }
    var input by remember { mutableStateOf("") }
    var goalInput by remember { mutableStateOf("") }

    DetailColumn("Weight") {
        GlassCard {
            SectionLabel("Log today")
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    label = { Text("lbs") },
                    singleLine = true
                )
                Button(
                    onClick = {
                        input.toDoubleOrNull()?.let { v ->
                            scope.launch {
                                Graph.body.addMetric("Weight", v)
                                input = ""
                            }
                        }
                    },
                    modifier = Modifier.height(56.dp)
                ) { Text("ADD") }
            }
        }

        GlassCard {
            SectionLabel("Trend — 7-day average")
            Spacer(Modifier.height(8.dp))
            LineChart(
                weights.map { it.recordedAt to it.value },
                color = AccentWeight,
                movingAverage = true,
                goal = settings.goalWeightLbs.takeIf { it > 0 }
            )
        }

        GlassCard {
            SectionLabel("Goal weight (draws a line on the chart)")
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = goalInput,
                    onValueChange = { goalInput = it },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    label = {
                        Text(if (settings.goalWeightLbs > 0) "Current: ${settings.goalWeightLbs}" else "No goal set")
                    },
                    singleLine = true
                )
                Button(
                    onClick = {
                        goalInput.toDoubleOrNull()?.let { v ->
                            scope.launch {
                                Graph.settings.update { it.copy(goalWeightLbs = v) }
                                goalInput = ""
                            }
                        }
                    },
                    modifier = Modifier.height(56.dp)
                ) { Text("SET") }
            }
        }
    }
}
