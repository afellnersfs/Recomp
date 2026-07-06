package com.arie.recomp.ui.settings

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.arie.recomp.R
import com.arie.recomp.backup.ExportImport
import com.arie.recomp.data.Graph
import com.arie.recomp.data.Settings
import com.arie.recomp.health.HealthConnectManager
import com.arie.recomp.notifications.ReminderScheduler
import com.arie.recomp.ui.AppCard
import com.arie.recomp.ui.SectionLabel
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settings by Graph.settings.flow.collectAsState(initial = Settings())
    var showPrivacy by remember { mutableStateOf(false) }

    fun save(reschedule: Boolean = false, transform: (Settings) -> Settings) {
        scope.launch {
            Graph.settings.update(transform)
            if (reschedule) {
                ReminderScheduler.scheduleWorkoutReminder(context)
                ReminderScheduler.scheduleWaterReminder(context)
            }
        }
    }

    fun toast(msg: String) = Toast.makeText(context, msg, Toast.LENGTH_LONG).show()

    val hc = remember { HealthConnectManager(context) }
    val hcLauncher = rememberLauncherForActivityResult(hc.requestPermissionContract()) { }
    var hcConnected by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { hcConnected = hc.isAvailable() && hc.hasAllPermissions() }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) scope.launch {
            toast(if (ExportImport.exportTo(context, uri)) "Backup exported ✓" else "Export failed")
        }
    }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) scope.launch {
            toast(if (ExportImport.importFrom(context, uri)) "Backup restored ✓" else "Import failed — is this a Recomp backup?")
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineLarge)

        // ---- Training ----
        AppCard {
            SectionLabel("Training")
            Spacer(Modifier.height(12.dp))

            Text("Session length", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 8.dp)) {
                listOf(30, 40, 50, 60).forEach { min ->
                    FilterChip(
                        selected = settings.sessionMinutes == min,
                        onClick = { save { it.copy(sessionMinutes = min) } },
                        label = { Text("$min min") }
                    )
                }
            }

            StepperRow(
                label = "Rest timer",
                value = "${settings.restSeconds}s",
                onDec = { save { it.copy(restSeconds = (it.restSeconds - 15).coerceAtLeast(30)) } },
                onInc = { save { it.copy(restSeconds = (it.restSeconds + 15).coerceAtMost(300)) } }
            )

            Spacer(Modifier.height(8.dp))
            Text("Training days", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(vertical = 8.dp)) {
                // ISO day numbers, shown Sun..Sat
                listOf(7 to "Su", 1 to "Mo", 2 to "Tu", 3 to "We", 4 to "Th", 5 to "Fr", 6 to "Sa")
                    .forEach { (day, label) ->
                        FilterChip(
                            selected = day in settings.trainingDays,
                            onClick = {
                                save(reschedule = true) { s ->
                                    val days = if (day in s.trainingDays) s.trainingDays - day
                                    else s.trainingDays + day
                                    s.copy(
                                        trainingDays = days,
                                        plannedDaysPerWeek = days.size.coerceIn(1, 7)
                                    )
                                }
                            },
                            label = { Text(label) }
                        )
                    }
            }

            SwitchRow(
                label = "Workout reminder",
                checked = settings.reminderEnabled,
                onChange = { on -> save(reschedule = true) { it.copy(reminderEnabled = on) } }
            )
            if (settings.reminderEnabled) {
                StepperRow(
                    label = "Reminder time",
                    value = "%d:%02d".format(settings.reminderHour, settings.reminderMinute),
                    onDec = {
                        save(reschedule = true) {
                            val total = (it.reminderHour * 60 + it.reminderMinute - 30 + 1440) % 1440
                            it.copy(reminderHour = total / 60, reminderMinute = total % 60)
                        }
                    },
                    onInc = {
                        save(reschedule = true) {
                            val total = (it.reminderHour * 60 + it.reminderMinute + 30) % 1440
                            it.copy(reminderHour = total / 60, reminderMinute = total % 60)
                        }
                    }
                )
            }
        }

        // ---- Equipment ----
        AppCard {
            SectionLabel("Equipment")
            Spacer(Modifier.height(8.dp))
            Text(
                "Smith machine + bench press barbell + dumbbells. The program only uses these.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            StepperRow(
                label = "Heaviest dumbbell",
                value = "${settings.dumbbellMaxLbs} lb",
                onDec = { save { it.copy(dumbbellMaxLbs = (it.dumbbellMaxLbs - 2.5).coerceAtLeast(5.0)) } },
                onInc = { save { it.copy(dumbbellMaxLbs = it.dumbbellMaxLbs + 2.5) } }
            )
            SwitchRow(
                label = "I have a free barbell now",
                checked = settings.hasFreeBarbell,
                onChange = { on -> save { it.copy(hasFreeBarbell = on) } }
            )
        }

        // ---- Nutrition ----
        AppCard {
            SectionLabel("Nutrition & water")
            Spacer(Modifier.height(8.dp))
            StepperRow(
                label = "Daily calorie target",
                value = "%,d".format(settings.calorieTarget),
                onDec = { save { it.copy(calorieTarget = (it.calorieTarget - 50).coerceAtLeast(1000)) } },
                onInc = { save { it.copy(calorieTarget = it.calorieTarget + 50) } }
            )
            StepperRow(
                label = "Water goal (cups)",
                value = "${settings.waterGoalCups}",
                onDec = { save { it.copy(waterGoalCups = (it.waterGoalCups - 1).coerceAtLeast(4)) } },
                onInc = { save { it.copy(waterGoalCups = (it.waterGoalCups + 1).coerceAtMost(20)) } }
            )
            SwitchRow(
                label = "Water reminders (daytime)",
                checked = settings.waterReminderEnabled,
                onChange = { on -> save(reschedule = true) { it.copy(waterReminderEnabled = on) } }
            )
        }

        // ---- Shabbat mode ----
        AppCard {
            SectionLabel("Shabbat mode")
            Spacer(Modifier.height(8.dp))
            Text(
                "All notifications and reminders are silenced from Friday evening through Saturday night, every week.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            SwitchRow(
                label = "Shabbat mode",
                checked = settings.shabbatEnabled,
                onChange = { on -> save(reschedule = true) { it.copy(shabbatEnabled = on) } }
            )
            if (settings.shabbatEnabled) {
                SwitchRow(
                    label = "Use local sunset times",
                    checked = settings.shabbatUseSunset,
                    onChange = { on -> save(reschedule = true) { it.copy(shabbatUseSunset = on) } }
                )
                if (settings.shabbatUseSunset) {
                    var lat by remember(settings.latitude) { mutableStateOf(settings.latitude.toString()) }
                    var lon by remember(settings.longitude) { mutableStateOf(settings.longitude.toString()) }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = lat, onValueChange = { lat = it },
                            label = { Text("Latitude") }, singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = lon, onValueChange = { lon = it },
                            label = { Text("Longitude") }, singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    TextButton(onClick = {
                        val la = lat.toDoubleOrNull()
                        val lo = lon.toDoubleOrNull()
                        if (la != null && lo != null) {
                            save(reschedule = true) { it.copy(latitude = la, longitude = lo) }
                            toast("Location saved — sunset times active")
                        } else toast("Enter valid coordinates (e.g. 40.09, -74.22)")
                    }) { Text("Save location") }
                    Text(
                        "Window: candle lighting (sunset − 18 min) Friday until sunset + 42 min Saturday.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    var start by remember(settings.shabbatStart) { mutableStateOf(settings.shabbatStart) }
                    var end by remember(settings.shabbatEnd) { mutableStateOf(settings.shabbatEnd) }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = start, onValueChange = { start = it },
                            label = { Text("Friday start (HH:mm)") }, singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = end, onValueChange = { end = it },
                            label = { Text("Saturday end (HH:mm)") }, singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    TextButton(onClick = {
                        val ok = Regex("^\\d{1,2}:\\d{2}$").matches(start.trim()) &&
                            Regex("^\\d{1,2}:\\d{2}$").matches(end.trim())
                        if (ok) {
                            save(reschedule = true) {
                                it.copy(shabbatStart = start.trim(), shabbatEnd = end.trim())
                            }
                            toast("Shabbat times saved")
                        } else toast("Use HH:mm, e.g. 17:45")
                    }) { Text("Save times") }
                }
            }
        }

        // ---- Health Connect ----
        AppCard {
            SectionLabel("Health Connect (Fitbit)")
            Spacer(Modifier.height(8.dp))
            Text(
                if (hcConnected) "Connected ✓ — steps, sleep, heart rate and calories are flowing in."
                else "Not connected yet.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    if (hc.isAvailable()) hcLauncher.launch(hc.permissions)
                    else toast("Install/enable Health Connect first")
                }) { Text(if (hcConnected) "Re-check" else "Connect") }
                OutlinedButton(onClick = {
                    runCatching {
                        context.startActivity(Intent("androidx.health.ACTION_HEALTH_CONNECT_SETTINGS"))
                    }.onFailure { toast("Couldn't open Health Connect") }
                }) { Text("Open Health Connect") }
            }
            TextButton(onClick = { showPrivacy = true }) { Text("How your health data is used") }
        }

        // ---- Backup ----
        AppCard {
            SectionLabel("Backup")
            Spacer(Modifier.height(8.dp))
            Text(
                "Your data is backed up to your Google Drive automatically by Android " +
                    "(Settings > Google > Backup) and restored when you set up a new phone. " +
                    "You can also export a file manually.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { exportLauncher.launch("recomp-backup.json") }) { Text("Export") }
                OutlinedButton(onClick = {
                    importLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                }) { Text("Restore from file") }
            }
        }

        Spacer(Modifier.height(8.dp))
    }

    if (showPrivacy) {
        AlertDialog(
            onDismissRequest = { showPrivacy = false },
            title = { Text("Your data stays on your phone") },
            text = { Text(stringResource(R.string.health_privacy)) },
            confirmButton = { TextButton(onClick = { showPrivacy = false }) { Text("Got it") } }
        )
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun StepperRow(label: String, value: String, onDec: () -> Unit, onInc: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        FilledTonalIconButton(onClick = onDec, modifier = Modifier.size(40.dp)) {
            Icon(Icons.Filled.Remove, contentDescription = "decrease")
        }
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 12.dp)
        )
        FilledTonalIconButton(onClick = onInc, modifier = Modifier.size(40.dp)) {
            Icon(Icons.Filled.Add, contentDescription = "increase")
        }
    }
}
