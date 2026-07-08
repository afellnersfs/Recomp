package com.arie.recomp.ui.trends

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arie.recomp.data.ExerciseCatalog
import com.arie.recomp.data.Graph
import com.arie.recomp.data.Progression
import com.arie.recomp.ui.BarChart
import com.arie.recomp.ui.GlassCard
import com.arie.recomp.ui.LineChart
import com.arie.recomp.ui.SectionLabel
import com.arie.recomp.ui.Segmented
import com.arie.recomp.ui.fmtLbs
import com.arie.recomp.ui.millisToDate
import com.arie.recomp.ui.theme.AccentActivity
import com.arie.recomp.ui.theme.AccentWeight
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun TrendsScreen() {
    val allSets by Graph.workouts.allSets.collectAsState(initial = emptyList())
    val prs by Graph.workouts.prs.collectAsState(initial = emptyList())
    val metrics by Graph.body.metrics.collectAsState(initial = emptyList())
    var range by remember { mutableIntStateOf(0) }   // 0=Week view window, 1=Month, 2=6M
    val rangeDays = listOf(7L, 30L, 182L)[range]

    val exercisesWithData = ExerciseCatalog.all.filter { ex ->
        allSets.any { it.exerciseId == ex.id && it.weightLbs > 0 }
    }
    var selectedId by remember { mutableStateOf<String?>(null) }
    val selected = exercisesWithData.firstOrNull { it.id == selectedId }
        ?: exercisesWithData.firstOrNull()

    val cutoffMs = LocalDate.now().minusDays(rangeDays)
        .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(horizontal = 18.dp)
            .padding(top = 12.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Trends", style = MaterialTheme.typography.headlineLarge, modifier = Modifier.weight(1f))
            Segmented(listOf("W", "M", "6M"), range, { range = it })
        }

        // Strength per exercise
        GlassCard {
            SectionLabel("Strength — estimated 1RM")
            Spacer(Modifier.height(8.dp))
            if (exercisesWithData.isEmpty()) {
                Text(
                    "Log your first workout to see strength trends.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Row(
                    Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    exercisesWithData.forEach { ex ->
                        FilterChip(
                            selected = ex.id == selected?.id,
                            onClick = { selectedId = ex.id },
                            label = { Text(ex.name) }
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                val points = allSets
                    .filter { it.exerciseId == selected?.id && it.weightLbs > 0 }
                    .groupBy { it.sessionId }
                    .map { (_, sets) ->
                        sets.minOf { it.loggedAt } to sets.maxOf { Progression.epley(it.weightLbs, it.reps) }
                    }
                    .sortedBy { it.first }
                LineChart(points, color = AccentActivity)
            }
        }

        // Weekly volume
        GlassCard {
            SectionLabel("Weekly volume (lbs lifted)")
            Spacer(Modifier.height(8.dp))
            val fmt = DateTimeFormatter.ofPattern("M/d")
            val zone = ZoneId.systemDefault()
            val weeksBack = if (range == 2) 25 else 7
            val thisMonday = LocalDate.now().with(DayOfWeek.MONDAY)
            val weeks = (weeksBack downTo 0).map { i ->
                val start = thisMonday.minusWeeks(i.toLong())
                val startMs = start.atStartOfDay(zone).toInstant().toEpochMilli()
                val endMs = start.plusWeeks(1).atStartOfDay(zone).toInstant().toEpochMilli()
                val vol = allSets
                    .filter { it.loggedAt in startMs until endMs }
                    .sumOf { it.weightLbs * it.reps }
                start.format(fmt) to vol
            }
            BarChart(
                weeks.let { if (it.size > 10) it.filterIndexed { i, _ -> i % 3 == 0 || i == it.lastIndex } else it },
                color = AccentActivity
            )
        }

        // Body weight
        GlassCard {
            SectionLabel("Body weight")
            Spacer(Modifier.height(8.dp))
            LineChart(
                metrics.filter { it.type == "Weight" && it.recordedAt >= cutoffMs }
                    .map { it.recordedAt to it.value },
                color = AccentWeight,
                movingAverage = true
            )
        }

        // PR board
        GlassCard {
            SectionLabel("PR board")
            Spacer(Modifier.height(8.dp))
            if (prs.isEmpty()) {
                Text(
                    "Beat a previous best and it lands here. 🏆",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                prs.sortedByDescending { it.achievedAt }.forEach { pr ->
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                ExerciseCatalog.get(pr.exerciseId).name,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                millisToDate(pr.achievedAt).toString(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            "${fmtLbs(pr.weightLbs)}×${pr.reps}",
                            style = MaterialTheme.typography.titleLarge,
                            color = AccentActivity
                        )
                    }
                }
            }
        }
    }
}
