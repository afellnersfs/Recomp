package com.arie.recomp.ui.workout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.arie.recomp.data.Routines
import com.arie.recomp.ui.SectionLabel
import com.arie.recomp.ui.openUrl
import kotlinx.coroutines.delay

/** 5-minute guided warm-up before a workout, or cool-down stretches after. */
@Composable
fun RoutineScreen(nav: NavHostController, warmup: Boolean) {
    val steps = if (warmup) Routines.warmup else Routines.cooldown
    val context = LocalContext.current
    var index by remember { mutableIntStateOf(0) }
    var remaining by remember { mutableIntStateOf(steps.first().seconds) }
    var paused by remember { mutableStateOf(false) }
    val done = index >= steps.size

    LaunchedEffect(index, paused) {
        if (index >= steps.size) return@LaunchedEffect
        while (!paused && remaining > 0) {
            delay(1000)
            remaining -= 1
        }
        if (!paused && remaining <= 0) {
            if (index < steps.size - 1) {
                index += 1
                remaining = steps[index].seconds
            } else {
                index = steps.size
            }
        }
    }

    fun leave() {
        if (warmup) {
            nav.navigate("workout") { popUpTo("home") }
        } else {
            nav.popBackStack("home", false)
        }
    }

    Column(
        Modifier.fillMaxSize().statusBarsPadding().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SectionLabel(if (warmup) "Warm-up" else "Cool-down")
        Spacer(Modifier.height(32.dp))

        if (done) {
            Text("Done! 🔥", style = MaterialTheme.typography.headlineLarge)
            Spacer(Modifier.height(8.dp))
            Text(
                if (warmup) "You're warm. Let's lift." else "Nice work. Go eat something.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Text(
                steps[index].name,
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(16.dp))
            Text(
                "%d:%02d".format(remaining / 60, remaining % 60),
                style = MaterialTheme.typography.displayLarge
            )
            Spacer(Modifier.height(8.dp))
            steps.getOrNull(index + 1)?.let {
                Text("Next: ${it.name}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(24.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedButton(onClick = { paused = !paused }, modifier = Modifier.height(56.dp)) {
                    Text(if (paused) "RESUME" else "PAUSE")
                }
                OutlinedButton(
                    onClick = {
                        if (index < steps.size - 1) {
                            index += 1
                            remaining = steps[index].seconds
                        } else index = steps.size
                    },
                    modifier = Modifier.height(56.dp)
                ) { Text("NEXT") }
            }
        }

        TextButton(onClick = {
            openUrl(context, if (warmup) Routines.WARMUP_VIDEO else Routines.COOLDOWN_VIDEO)
        }) {
            Text("▶ Watch the ${if (warmup) "5-min warm-up" else "stretch"} video")
        }

        Spacer(Modifier.weight(1f))

        Button(
            onClick = { leave() },
            modifier = Modifier.fillMaxWidth().height(72.dp)
        ) {
            Text(
                if (warmup) "START WORKOUT" else "DONE",
                style = MaterialTheme.typography.titleLarge
            )
        }
        if (warmup && !done) {
            TextButton(onClick = { leave() }, modifier = Modifier.padding(top = 4.dp)) {
                Text("Skip warm-up", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
