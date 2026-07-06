package com.arie.recomp.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.arie.recomp.data.Exercise
import com.arie.recomp.ui.theme.Accent
import com.arie.recomp.ui.theme.OutlineDim

fun fmtLbs(v: Double): String =
    if (v == v.toLong().toDouble()) v.toLong().toString() else v.toString()

/** Opens the curated demo video; falls back to a YouTube search if that fails. */
fun openVideo(context: Context, exercise: Exercise) {
    val tryOpen: (String) -> Boolean = { url ->
        runCatching {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }.isSuccess
    }
    if (!tryOpen(exercise.videoUrl)) tryOpen(exercise.searchUrl)
}

fun openUrl(context: Context, url: String) {
    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
}

@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    var m = modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(20.dp))
        .background(MaterialTheme.colorScheme.surface)
    if (onClick != null) m = m.clickable(onClick = onClick)
    Column(m.padding(16.dp), content = content)
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
    )
}

/** Circular progress ring with content in the middle (water ring, etc.). */
@Composable
fun ProgressRing(
    progress: Float,
    modifier: Modifier = Modifier,
    ringSize: Dp = 110.dp,
    strokeWidth: Dp = 10.dp,
    color: Color = Accent,
    content: @Composable () -> Unit
) {
    Box(modifier.size(ringSize), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val sw = strokeWidth.toPx()
            val arcSize = Size(size.width - sw, size.height - sw)
            val topLeft = Offset(sw / 2, sw / 2)
            drawArc(
                color = OutlineDim, startAngle = -90f, sweepAngle = 360f, useCenter = false,
                topLeft = topLeft, size = arcSize, style = Stroke(sw, cap = StrokeCap.Round)
            )
            drawArc(
                color = color, startAngle = -90f,
                sweepAngle = 360f * progress.coerceIn(0f, 1f), useCenter = false,
                topLeft = topLeft, size = arcSize, style = Stroke(sw, cap = StrokeCap.Round)
            )
        }
        content()
    }
}

/** Huge +/- stepper built for one-handed mid-workout use. */
@Composable
fun BigStepper(
    value: String,
    label: String,
    onDec: () -> Unit,
    onInc: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        SectionLabel(label)
        Row(verticalAlignment = Alignment.CenterVertically) {
            FilledTonalIconButton(
                onClick = onDec,
                modifier = Modifier.size(56.dp),
                shape = CircleShape,
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) { Icon(Icons.Filled.Remove, contentDescription = "decrease $label") }
            Text(
                value,
                style = MaterialTheme.typography.displayMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp).widthAtLeast(96.dp)
            )
            FilledTonalIconButton(
                onClick = onInc,
                modifier = Modifier.size(56.dp),
                shape = CircleShape,
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) { Icon(Icons.Filled.Add, contentDescription = "increase $label") }
        }
    }
}

private fun Modifier.widthAtLeast(min: Dp): Modifier =
    this.then(Modifier.defaultMinSize(minWidth = min))
