package com.arie.recomp.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.arie.recomp.ui.theme.Accent
import com.arie.recomp.ui.theme.OutlineDim
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val DateFmt = DateTimeFormatter.ofPattern("MMM d")

fun millisToDate(t: Long): LocalDate =
    Instant.ofEpochMilli(t).atZone(ZoneId.systemDefault()).toLocalDate()

/** Simple line chart: x = evenly spaced points in time order, y = value. */
@Composable
fun LineChart(
    points: List<Pair<Long, Double>>,
    modifier: Modifier = Modifier,
    valueFormatter: (Double) -> String = { fmtLbs(it) }
) {
    if (points.size < 2) {
        Text(
            "Not enough data yet — keep logging.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier.padding(vertical = 24.dp)
        )
        return
    }
    val minV = points.minOf { it.second }
    val maxV = points.maxOf { it.second }
    val range = (maxV - minV).takeIf { it > 0.0 } ?: 1.0

    Column(modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                valueFormatter(maxV),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                valueFormatter(minV),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(160.dp)
                .padding(vertical = 8.dp)
        ) {
            val w = size.width
            val h = size.height
            // faint grid
            for (i in 0..2) {
                val y = h * i / 2f
                drawLine(OutlineDim, Offset(0f, y), Offset(w, y), strokeWidth = 1f)
            }
            val stepX = w / (points.size - 1)
            fun yFor(v: Double) = (h - ((v - minV) / range * h)).toFloat()

            val path = Path()
            points.forEachIndexed { i, (_, v) ->
                val x = stepX * i
                val y = yFor(v)
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(
                path, color = Accent,
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
            points.forEachIndexed { i, (_, v) ->
                drawCircle(Accent, radius = 4.dp.toPx(), center = Offset(stepX * i, yFor(v)))
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

/** Simple bar chart with labels under each bar. */
@Composable
fun BarChart(
    values: List<Pair<String, Double>>,
    modifier: Modifier = Modifier
) {
    if (values.isEmpty() || values.all { it.second == 0.0 }) {
        Text(
            "Not enough data yet — keep logging.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier.padding(vertical = 24.dp)
        )
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
                            .background(if (v > 0) Accent else OutlineDim)
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
