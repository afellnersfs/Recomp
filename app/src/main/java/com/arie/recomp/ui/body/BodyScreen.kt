package com.arie.recomp.ui.body

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.arie.recomp.data.Graph
import com.arie.recomp.data.db.ProgressPhoto
import com.arie.recomp.ui.AppCard
import com.arie.recomp.ui.LineChart
import com.arie.recomp.ui.SectionLabel
import com.arie.recomp.ui.millisToDate
import com.arie.recomp.ui.theme.Accent
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val defaultTypes = listOf("Weight", "Waist", "Chest", "Arms")

@Composable
fun BodyScreen() {
    var tab by remember { mutableIntStateOf(0) }
    Column(Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = tab, containerColor = MaterialTheme.colorScheme.background) {
            Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Measurements") })
            Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Photos") })
        }
        if (tab == 0) MeasurementsTab() else PhotosTab()
    }
}

@Composable
private fun MeasurementsTab() {
    val scope = rememberCoroutineScope()
    val metrics by Graph.body.metrics.collectAsState(initial = emptyList())
    val types = (defaultTypes + metrics.map { it.type }).distinct()
    var selectedType by remember { mutableStateOf(defaultTypes.first()) }
    var input by remember { mutableStateOf("") }
    var customType by remember { mutableStateOf("") }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            types.forEach { t ->
                FilterChip(
                    selected = t == selectedType,
                    onClick = { selectedType = t },
                    label = { Text(t) }
                )
            }
        }

        AppCard {
            SectionLabel("Log $selectedType")
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    label = { Text(if (selectedType == "Weight") "lbs" else "inches") },
                    singleLine = true
                )
                Button(
                    onClick = {
                        input.toDoubleOrNull()?.let { v ->
                            scope.launch {
                                Graph.body.addMetric(selectedType, v)
                                input = ""
                            }
                        }
                    },
                    modifier = Modifier.height(56.dp)
                ) { Text("ADD") }
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = customType,
                    onValueChange = { customType = it },
                    modifier = Modifier.weight(1f),
                    label = { Text("New measurement (e.g. Thighs)") },
                    singleLine = true
                )
                TextButton(onClick = {
                    if (customType.isNotBlank()) {
                        selectedType = customType.trim()
                        customType = ""
                    }
                }) { Text("Use") }
            }
        }

        AppCard {
            SectionLabel("$selectedType trend")
            Spacer(Modifier.height(8.dp))
            LineChart(metrics.filter { it.type == selectedType }.map { it.recordedAt to it.value })
        }

        AppCard {
            SectionLabel("History")
            Spacer(Modifier.height(4.dp))
            val history = metrics.filter { it.type == selectedType }.sortedByDescending { it.recordedAt }
            if (history.isEmpty()) {
                Text(
                    "Nothing logged yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            history.take(15).forEach { m ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        millisToDate(m.recordedAt).toString(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    Text("${m.value}", style = MaterialTheme.typography.titleMedium)
                    IconButton(onClick = { scope.launch { Graph.body.deleteMetric(m) } }) {
                        Icon(
                            Icons.Filled.Delete, contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PhotosTab() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val photos by Graph.body.photos.collectAsState(initial = emptyList())
    var pendingFile by remember { mutableStateOf<File?>(null) }
    var selected by remember { mutableStateOf(listOf<ProgressPhoto>()) }
    var showCompare by remember { mutableStateOf(false) }

    val takePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        if (ok) pendingFile?.let { f -> scope.launch { Graph.body.registerPhoto(f.name) } }
        pendingFile = null
    }
    val pickPhoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) scope.launch {
            withContext(Dispatchers.IO) {
                val f = Graph.body.photoFile("IMG_${System.currentTimeMillis()}.jpg")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    f.outputStream().use { input.copyTo(it) }
                }
                Graph.body.registerPhoto(f.name)
            }
        }
    }

    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = {
                val f = Graph.body.photoFile("IMG_${System.currentTimeMillis()}.jpg")
                pendingFile = f
                takePicture.launch(
                    FileProvider.getUriForFile(context, "com.arie.recomp.fileprovider", f)
                )
            }, modifier = Modifier.weight(1f).height(56.dp)) { Text("📷 CAMERA") }
            OutlinedButton(onClick = {
                pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }, modifier = Modifier.weight(1f).height(56.dp)) { Text("GALLERY") }
        }
        Text(
            "Photos stay in the app's private storage. Tap two to compare side by side.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (selected.size == 2) {
            Button(onClick = { showCompare = true }, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                Text("COMPARE SELECTED")
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(photos, key = { it.id }) { photo ->
                val isSelected = selected.any { it.id == photo.id }
                Column {
                    AsyncImage(
                        model = Graph.body.photoFile(photo.fileName),
                        contentDescription = "Progress photo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .aspectRatio(0.8f)
                            .clip(RoundedCornerShape(12.dp))
                            .then(
                                if (isSelected) Modifier.border(3.dp, Accent, RoundedCornerShape(12.dp))
                                else Modifier
                            )
                            .clickable {
                                selected = when {
                                    isSelected -> selected.filterNot { it.id == photo.id }
                                    selected.size < 2 -> selected + photo
                                    else -> listOf(photo)
                                }
                            }
                    )
                    Text(
                        millisToDate(photo.takenAt).toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    if (showCompare && selected.size == 2) {
        val pair = selected.sortedBy { it.takenAt }
        AlertDialog(
            onDismissRequest = { showCompare = false },
            confirmButton = {
                TextButton(onClick = { showCompare = false }) { Text("Close") }
            },
            dismissButton = {
                TextButton(onClick = {
                    scope.launch {
                        pair.forEach { Graph.body.deletePhoto(it) }
                        selected = emptyList()
                        showCompare = false
                    }
                }) { Text("Delete both", color = MaterialTheme.colorScheme.error) }
            },
            title = { Text("Then vs. now") },
            text = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    pair.forEach { p ->
                        Column(Modifier.weight(1f)) {
                            AsyncImage(
                                model = Graph.body.photoFile(p.fileName),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.aspectRatio(0.8f).clip(RoundedCornerShape(12.dp))
                            )
                            Text(
                                millisToDate(p.takenAt).toString(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        )
    }
}
