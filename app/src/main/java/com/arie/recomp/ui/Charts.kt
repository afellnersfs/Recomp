package com.arie.recomp.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.arie.recomp.ui.theme.Accent
import com.arie.recomp.ui.theme.OutlineDim
import com.arie.recomp.ui.theme.StageAwake
import com.arie.recomp.ui.theme.StageDeep
import com.arie.recomp.ui.theme.StageLight
import com.arie.recomp.ui.theme.StageRem
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val DateFmt = DateTimeFormatter.ofPattern("MMM d")

fun millisToDate(t: Long): LocalDate =
    Instant.ofEpochMilli(t).atZone(ZoneId.systemDefault()).toLocalDate()

@Composable
private fun EmptyChartHint(modifier: Modifier = Modifier) {
    Text(
        "Not enough data yet — keep logging.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(vertical = 24.dp)
    )
}

private fun DrawScope.hairlineGrid(lines: Int = 3) {
    for (i in 0 until lines) {
        val y = size.height * i / (lines - 1).coerceAtLeast(1)
        drawLine(OutlineDim, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
    }
}

/**
 * Trend line chart with gradient area fill, optional 7-point moving average
 * hero (raw points muted), optional goal line, and scrub-to-inspect.
 */
@Composable
fun LineChart(
    points: List<Pair<Long, Double>>,
    modifier: Modifier = Modifier,
    color: Color = Accent,
    movingAverage: Boolean = false,
    goal: Double? = null,
    valueFormatter: (Double) -> String = { fmtLbs(it) }
) {
    if (points.size < 2) {
        EmptyChartHint(modifier)
        return
    }
    var scrubIndex by remember(points) { mutableIntStateOf(-1) }
    val avg = if (movingAverage) points.mapIndexed { i, _ ->
        val from = (i - 6).coerceAtLeast(0)
        points.subList(from, i + 1).map { it.second }.average()
    } else emptyList()

    val allValues = points.map { it.second } + avg + listOfNotNull(goal)
    val minV = allValues.min()
    val maxV = allValues.max()
    val range = (maxV - minV).takeIf { it > 0.0 } ?: 1.0

    val shown = if (scrubIndex in points.indices) points[scrubIndex] else points.last()

    Column(
        modifier
            .fillMaxWidth()
            .semantics {
                contentDescription =
                    "Trend from ${valueFormatter(points.first().second)} to ${valueFormatter(points.last().second)}"
            }
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(valueFormatter(shown.second), style = MaterialTheme.typography.headlineMedium, color = color)
            Spacer(Modifier.width(8.dp))
            Text(
                millisToDate(shown.first).format(DateFmt),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(160.dp)
                .padding(vertical = 8.dp)
                .pointerInput(points) {
                    detectDragGestures(
                        onDragEnd = { scrubIndex = -1 },
                        onDragCancel = { scrubIndex = -1 }
                    ) { change, _ ->
                        val stepX = size.width.toFloat() / (points.size - 1)
                        scrubIndex = (change.position.x / stepX).toInt().coerceIn(points.indices)
                    }
                }
        ) {
            hairlineGrid()
            val w = size.width
            val h = size.height
            val stepX = w / (points.size - 1)
            fun yFor(v: Double) = (h - ((v - minV) / range * h)).toFloat()

            // goal line
            goal?.let {
                drawLine(
                    color.copy(alpha = 0.35f), Offset(0f, yFor(it)), Offset(w, yFor(it)),
                    strokeWidth = 2f
                )
            }

            val rawAlpha = if (movingAverage) 0.30f else 1f
            val path = Path()
            points.forEachIndexed { i, (_, v) ->
                val x = stepX * i
                val y = yFor(v)
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            // gradient area fill under the raw line
            val fill = Path().apply {
                addPath(path)
                lineTo(w, h)
                lineTo(0f, h)
                close()
            }
            drawPath(
                fill,
                brush = Brush.verticalGradient(
                    listOf(color.copy(alpha = 0.22f * rawAlpha), Color.Transparent)
                )
            )
            drawPath(
                path, color = color.copy(alpha = rawAlpha),
                style = Stroke(3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
            if (movingAverage) {
                val avgPath = Path()
                avg.forEachIndexed { i, v ->
                    val x = stepX * i
                    val y = yFor(v)
                    if (i == 0) avgPath.moveTo(x, y) else avgPath.lineTo(x, y)
                }
                drawPath(
                    avgPath, color = color,
                    style = Stroke(3.5f.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
            }
            // scrub indicator
            if (scrubIndex in points.indices) {
                val x = stepX * scrubIndex
                drawLine(Color.White.copy(alpha = 0.4f), Offset(x, 0f), Offset(x, h), 2f)
                drawCircle(color, 6.dp.toPx(), Offset(x, yFor(points[scrubIndex].second)))
            } else {
                drawCircle(color, 5.dp.toPx(), Offset(w, yFor(points.last().second)))
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                millisToDate(points.first().first).format(DateFmt),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                millisToDate(points.last().first).format(DateFmt),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** Bars with rounded tops and labels underneath. */
@Composable
fun BarChart(
    values: List<Pair<String, Double>>,
    modifier: Modifier = Modifier,
    color: Color = Accent
) {
    if (values.isEmpty() || values.all { it.second == 0.0 }) {
        EmptyChartHint(modifier)
        return
    }
    val maxV = values.maxOf { it.second }.takeIf { it > 0.0 } ?: 1.0
    Row(modifier.fillMaxWidth().height(170.dp), verticalAlignment = Alignment.Bottom) {
        values.forEach { (label, v) ->
            Column(
                Modifier.weight(1f).padding(horizontal = 3.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Column(Modifier.height(140.dp).fillMaxWidth()) {
                    val frac = (v / maxV).toFloat().coerceIn(0.02f, 1f)
                    if (frac < 1f) Box(Modifier.weight(1f - frac))
                    Box(
                        Modifier
                            .weight(frac)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                            .background(
                                Brush.verticalGradient(
                                    listOf(color, color.copy(alpha = 0.55f))
                                )
                            )
                    )
                }
                Text(
                    label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/** 24 slim hourly bars for today's steps. */
@Composable
fun HourlyBars(values: List<Double>, modifier: Modifier = Modifier, color: Color = Accent) {
    if (values.isEmpty() || values.all { it == 0.0 }) {
        EmptyChartHint(modifier)
        return
    }
    val maxV = values.max().takeIf { it > 0.0 } ?: 1.0
    Column(modifier.fillMaxWidth()) {
        Canvas(Modifier.fillMaxWidth().height(120.dp)) {
            val n = values.size
            val gap = 3.dp.toPx()
            val barW = (size.width - gap * (n - 1)) / n
            values.forEachIndexed { i, v ->
                val bh = ((v / maxV) * size.height).toFloat().coerceAtLeast(2f)
                drawRoundRect(
                    color = if (v > 0.0) color else OutlineDim,
                    topLeft = Offset(i * (barW + gap), size.height - bh),
                    size = Size(barW, bh),
                    cornerRadius = CornerRadius(barW / 2)
                )
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            listOf("12am", "6am", "12pm", "6pm", "12am").forEach {
                Text(
                    it, style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ---------- Sleep hypnogram ----------

data class StageSpan(val startMs: Long, val endMs: Long, val row: Int)  // row 0=Awake 1=REM 2=Light 3=Deep

private val StageColors = listOf(StageAwake, StageRem, StageLight, StageDeep)
private val StageNames = listOf("Awake", "REM", "Light", "Deep")

@Composable
fun Hypnogram(
    spans: List<StageSpan>,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    if (spans.isEmpty()) {
        if (!compact) EmptyChartHint(modifier)
        return
    }
    val start = spans.minOf { it.startMs }
    val end = spans.maxOf { it.endMs }
    val total = (end - start).coerceAtLeast(1)

    Row(modifier.fillMaxWidth()) {
        if (!compact) {
            Column(
                Modifier.height(120.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                StageNames.forEach {
                    Text(
                        it, style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
        }
        Canvas(
            Modifier
                .weight(1f)
                .height(if (compact) 44.dp else 120.dp)
                .semantics { contentDescription = "Sleep stages chart" }
        ) {
            val rowH = size.height / 4
            spans.forEach { s ->
                val x0 = (s.startMs - start).toFloat() / total * size.width
                val x1 = (s.endMs - start).toFloat() / total * size.width
                val y = s.row * rowH
                drawRoundRect(
                    color = StageColors[s.row],
                    topLeft = Offset(x0, y + rowH * 0.15f),
                    size = Size((x1 - x0).coerceAtLeast(2f), rowH * 0.7f),
                    cornerRadius = CornerRadius(rowH * 0.35f)
                )
            }
        }
    }
}

// ---------- 24h heart rate band ----------

/** Per-hour (min, max) BPM, null when no samples that hour. */
@Composable
fun HrBandChart(
    hourly: List<Pair<Long, Long>?>,
    restingHr: Long?,
    modifier: Modifier = Modifier,
    color: Color = Accent
) {
    val present = hourly.filterNotNull()
    if (present.isEmpty()) {
        EmptyChartHint(modifier)
        return
    }
    val minV = minOf(present.minOf { it.first }, restingHr ?: Long.MAX_VALUE) - 5
    val maxV = present.maxOf { it.second } + 5
    val range = (maxV - minV).coerceAtLeast(1)

    Column(modifier.fillMaxWidth()) {
        Canvas(Modifier.fillMaxWidth().height(140.dp)) {
            hairlineGrid()
            val n = hourly.size
            val gap = 4.dp.toPx()
            val barW = (size.width - gap * (n - 1)) / n
            fun yFor(v: Long) = size.height * (1f - (v - minV).toFloat() / range)
            restingHr?.let {
                drawLine(
                    Color.White.copy(alpha = 0.35f),
                    Offset(0f, yFor(it)), Offset(size.width, yFor(it)),
                    strokeWidth = 2f
                )
            }
            hourly.forEachIndexed { i, mm ->
                if (mm != null) {
                    val top = yFor(mm.second)
                    val bottom = yFor(mm.first)
                    drawRoundRect(
                        color = color.copy(alpha = 0.85f),
                        topLeft = Offset(i * (barW + gap), top),
                        size = Size(barW, (bottom - top).coerceAtLeast(barW)),
                        cornerRadius = CornerRadius(barW / 2)
                    )
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            listOf("12am", "6am", "12pm", "6pm", "12am").forEach {
                Text(
                    it, style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/** Week / Month / 6M segmented pill. */
@Composable
fun Segmented(
    options: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = 0.06f))
            .padding(3.dp)
    ) {
        options.forEachIndexed { i, label ->
            val active = i == selected
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = if (active) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .clip(RoundedCornerShape(17.dp))
                    .background(if (active) Color.White.copy(alpha = 0.14f) else Color.Transparent)
                    .clickable { onSelect(i) }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            )
        }
    }
}
