package com.arie.recomp.ui.workouts

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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.arie.recomp.data.ExerciseCatalog
import com.arie.recomp.data.Graph
import com.arie.recomp.data.Program
import com.arie.recomp.data.Settings
import com.arie.recomp.data.WorkoutTemplate
import com.arie.recomp.ui.GlassCard
import com.arie.recomp.ui.SectionLabel
import com.arie.recomp.ui.millisToDate
import com.arie.recomp.ui.theme.Accent
import java.time.format.DateTimeFormatter

@Composable
fun WorkoutsScreen(nav: NavHostController) {
    val settings by Graph.settings.flow.collectAsState(initial = Settings())
    val sessions by Graph.workouts.completedSessions.collectAsState(initial = emptyList())
    val allSets by Graph.workouts.allSets.collectAsState(initial = emptyList())

    var nextTemplate by remember { mutableStateOf<WorkoutTemplate?>(null) }
    LaunchedEffect(sessions) { nextTemplate = Graph.workouts.nextTemplate() }

    val volumeBySession = remember(allSets) {
        allSets.groupBy { it.sessionId }
            .mapValues { (_, sets) -> sets.sumOf { it.weightLbs * it.reps } }
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(horizontal = 18.dp)
            .padding(top = 12.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Workouts", style = MaterialTheme.typography.headlineLarge)

        // Next up
        GlassCard {
            SectionLabel("Next up")
            nextTemplate?.let { t ->
                Spacer(Modifier.height(4.dp))
                Text(t.name, style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.height(8.dp))
                Program.exerciseIdsFor(t, settings.sessionMinutes).forEach { id ->
                    Text(
                        "•  ${ExerciseCatalog.get(id).name}",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    "${settings.sessionMinutes} min · ${Program.SETS_PER_EXERCISE} sets each · warm-up first",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(14.dp))
            Button(
                onClick = { nav.navigate("warmup") },
                modifier = Modifier.fillMaxWidth().height(60.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Accent)
            ) { Text("START WORKOUT", style = MaterialTheme.typography.titleMedium) }
        }

        // History
        GlassCard {
            SectionLabel("History")
            Spacer(Modifier.height(4.dp))
            if (sessions.isEmpty()) {
                Text(
                    "Your first session will show up here.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                val fmt = DateTimeFormatter.ofPattern("EEE, MMM d")
                sessions.take(20).forEach { s ->
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                Program.byId(s.templateId).name,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                millisToDate(s.startedAt).format(fmt),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        val mins = s.endedAt?.let { (it - s.startedAt) / 60000 }
                        Text(
                            listOfNotNull(
                                mins?.let { "${it}m" },
                                volumeBySession[s.id]?.let { "%,d lb".format(it.toLong()) }
                            ).joinToString("  ·  "),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
