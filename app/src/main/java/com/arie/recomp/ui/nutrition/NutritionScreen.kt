package com.arie.recomp.ui.nutrition

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.arie.recomp.data.Graph
import com.arie.recomp.data.Settings
import com.arie.recomp.ui.AppCard
import com.arie.recomp.ui.ProgressRing
import com.arie.recomp.ui.SectionLabel
import com.arie.recomp.ui.theme.Accent
import com.arie.recomp.widgets.Widgets
import kotlinx.coroutines.launch

@Composable
fun NutritionScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settings by Graph.settings.flow.collectAsState(initial = Settings())
    val cups by Graph.nutrition.cupsToday().collectAsState(initial = 0)
    val entries by Graph.nutrition.caloriesToday().collectAsState(initial = emptyList())
    val consumed = entries.sumOf { it.calories }

    var calInput by remember { mutableStateOf("") }
    var labelInput by remember { mutableStateOf("") }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Fuel", style = MaterialTheme.typography.headlineLarge)

        // Calories
        AppCard {
            SectionLabel("Calories today")
            Spacer(Modifier.height(8.dp))
            Text(
                "%,d / %,d".format(consumed, settings.calorieTarget),
                style = MaterialTheme.typography.headlineLarge
            )
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = {
                    if (settings.calorieTarget > 0)
                        (consumed.toFloat() / settings.calorieTarget).coerceIn(0f, 1f)
                    else 0f
                },
                modifier = Modifier.fillMaxWidth().height(10.dp),
                color = Accent,
                trackColor = MaterialTheme.colorScheme.outline
            )
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = calInput,
                    onValueChange = { calInput = it },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    label = { Text("kcal") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = labelInput,
                    onValueChange = { labelInput = it },
                    modifier = Modifier.weight(1.4f),
                    label = { Text("Label (optional)") },
                    singleLine = true
                )
            }
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    calInput.toIntOrNull()?.let { c ->
                        scope.launch {
                            Graph.nutrition.addCalories(c, labelInput.trim())
                            calInput = ""
                            labelInput = ""
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) { Text("ADD") }
        }

        // Today's entries
        if (entries.isNotEmpty()) {
            AppCard {
                SectionLabel("Logged today")
                Spacer(Modifier.height(4.dp))
                entries.forEach { e ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            e.label.ifBlank { "—" },
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f)
                        )
                        Text("${e.calories} kcal", style = MaterialTheme.typography.titleMedium)
                        IconButton(onClick = { scope.launch { Graph.nutrition.deleteCalories(e) } }) {
                            Icon(
                                Icons.Filled.Delete, contentDescription = "Delete",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Water
        AppCard {
            SectionLabel("Water")
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                ProgressRing(
                    progress = if (settings.waterGoalCups > 0) cups.toFloat() / settings.waterGoalCups else 0f
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
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            scope.launch {
                                Graph.nutrition.addWater(1)
                                Widgets.refreshAllAsync(context)
                            }
                        },
                        modifier = Modifier.height(56.dp)
                    ) { Text("+1 CUP 💧") }
                    OutlinedButton(onClick = {
                        scope.launch {
                            Graph.nutrition.undoWater()
                            Widgets.refreshAllAsync(context)
                        }
                    }) { Text("Undo") }
                }
            }
        }

        Spacer(Modifier.height(8.dp))
    }
}
