package com.arie.recomp.ui

import android.provider.Settings
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.arie.recomp.ui.theme.Bg
import com.arie.recomp.ui.theme.Blob1
import com.arie.recomp.ui.theme.Blob2
import com.arie.recomp.ui.theme.Blob3
import com.arie.recomp.ui.theme.Surface1
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource

/** Shared blur graph: the mesh background is the haze source; glass surfaces sample it. */
val LocalHazeState = compositionLocalOf<HazeState?> { null }

fun glassStyle(tintAlpha: Float = 0.08f): HazeStyle = HazeStyle(
    backgroundColor = Bg,
    tint = HazeTint(Color.White.copy(alpha = tintAlpha)),
    blurRadius = 28.dp,
    noiseFactor = 0.02f,
    fallbackTint = HazeTint(Surface1.copy(alpha = 0.94f))   // < Android 12
)

/** Specular edge: brighter 1px top edge, fading darker toward the bottom. */
fun glassBorder(): Brush = Brush.verticalGradient(
    listOf(Color.White.copy(alpha = 0.26f), Color.White.copy(alpha = 0.05f))
)

@Composable
fun ReducedMotion(): Boolean {
    val context = LocalContext.current
    return remember {
        Settings.Global.getFloat(
            context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f
        ) == 0f
    }
}

/**
 * Soft animated mesh gradient — 3 blurred color blobs drifting almost
 * imperceptibly so the glass has something to refract. Frozen under
 * reduced-motion settings.
 */
@Composable
fun MeshBackground(modifier: Modifier = Modifier) {
    val reduced = ReducedMotion()
    val transition = rememberInfiniteTransition(label = "mesh")
    val drift by transition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(45_000, easing = LinearEasing), RepeatMode.Reverse),
        label = "drift"
    )
    val t = if (reduced) 0.5f else drift

    Canvas(modifier.background(Bg)) {
        val w = size.width
        val h = size.height
        fun blob(color: Color, cx: Float, cy: Float, r: Float, alpha: Float) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(color.copy(alpha = alpha), Color.Transparent),
                    center = Offset(cx, cy),
                    radius = r
                ),
                radius = r,
                center = Offset(cx, cy)
            )
        }
        blob(Blob1, w * (0.15f + 0.25f * t), h * 0.10f, w * 0.85f, 0.30f)
        blob(Blob2, w * (0.95f - 0.20f * t), h * (0.42f + 0.10f * t), w * 0.75f, 0.24f)
        blob(Blob3, w * 0.25f, h * (0.95f - 0.12f * t), w * 0.80f, 0.20f)
    }
}

/** Springy 0.97 scale-on-press. */
@Composable
fun Modifier.pressScale(interaction: MutableInteractionSource): Modifier {
    val pressed by interaction.collectIsPressedAsState()
    val reduced = ReducedMotion()
    val scale by animateFloatAsState(
        targetValue = if (pressed && !reduced) 0.97f else 1f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 600f),
        label = "pressScale"
    )
    return graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

/**
 * Frosted glass card — the app's standard surface. 28dp radius; nested
 * content should use 16–20dp radii to stay concentric.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    radius: Dp = 28.dp,
    tintAlpha: Float = 0.08f,
    contentPadding: Dp = 18.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val haze = LocalHazeState.current
    val shape = RoundedCornerShape(radius)
    val interaction = remember { MutableInteractionSource() }

    var m = modifier.fillMaxWidth()
    if (onClick != null) m = m.pressScale(interaction)
    m = m.clip(shape)
    m = if (haze != null) {
        m.hazeEffect(state = haze, style = glassStyle(tintAlpha))
    } else {
        m.background(Surface1.copy(alpha = 0.94f))
    }
    m = m.border(1.dp, glassBorder(), shape)
    if (onClick != null) {
        m = m.clickable(interactionSource = interaction, indication = null, onClick = onClick)
    }
    Column(m.padding(contentPadding), content = content)
}
