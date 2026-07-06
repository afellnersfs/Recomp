package com.arie.recomp.ui.workout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.arie.recomp.data.ExerciseCatalog
import com.arie.recomp.data.Graph
import com.arie.recomp.data.WorkoutRepository
import com.arie.recomp.ui.AppCard
import com.arie.recomp.ui.SectionLabel
import com.arie.recomp.ui.theme.Accent

@Composable
fun SummaryScreen(nav: NavHostController, sessionId: Long) {
    val summary by produceState<WorkoutRepository.Summary?>(initialValue = null, sessionId) {
        value = Graph.workouts.summary(sessionId)
    }

    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(24.dp))
        Text("Workout complete 💪", style = MaterialTheme.typography.headlineLarge, textAlign = TextAlign.Center)
        summary?.let { s ->
            Text(
                s.templateName,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
            Spacer(Modifier.height(24.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SummaryStat("Time", "${s.durationMin}m", Modifier.weight(1f))
                SummaryStat("Volume", "%,d lb".format(s.totalVolume.toLong()), Modifier.weight(1f))
                SummaryStat("Sets", "${s.setCount}", Modifier.weight(1f))
            }

            Spacer(Modifier.height(16.dp))

            if (s.prExerciseIds.isNotEmpty()) {
                AppCard {
                    SectionLabel("PRs hit today")
                    Spacer(Modifier.height(8.dp))
                    s.prExerciseIds.forEach { id ->
                        Text(
                            "🏆 ${ExerciseCatalog.get(id).name}",
                            style = MaterialTheme.typography.titleMedium,
                            color = Accent,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }
            }
        }

        Spacer(Modifier.weight(1f))

        OutlinedButton(
            onClick = { nav.navigate("cooldown") },
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) { Text("COOL-DOWN STRETCH (OPTIONAL)") }
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = { nav.popBackStack("home", false) },
            modifier = Modifier.fillMaxWidth().height(64.dp)
        ) { Text("DONE", style = MaterialTheme.typography.titleLarge) }
    }
}

@Composable
private fun SummaryStat(label: String, value: String, modifier: Modifier = Modifier) {
    AppCard(modifier) {
        Text(value, style = MaterialTheme.typography.headlineMedium)
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
