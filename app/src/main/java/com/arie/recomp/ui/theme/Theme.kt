package com.arie.recomp.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// ---- Liquid Glass tokens (dark-first, per fitness-app-ui-addendum.md) ----

val Bg = Color(0xFF0B0E14)                    // bg-base, near-black blue
val Blob1 = Color(0xFF2E5CFF)                 // electric blue (mesh only)
val Blob2 = Color(0xFF7C3AED)                 // violet (mesh only)
val Blob3 = Color(0xFF0EA5A4)                 // teal (mesh only)

val TextPrimary = Color(0xFFF5F7FA)
val TextDim = Color(0x9EF5F7FA)               // 62% opacity secondary
val OutlineDim = Color(0x14FFFFFF)            // hairline grids, white 8%

// Metric accents
val AccentActivity = Color(0xFF34D399)
val AccentSleep = Color(0xFF818CF8)
val AccentHeart = Color(0xFFFB7185)
val AccentWeight = Color(0xFFFBBF24)

// General action accent (buttons, CTAs, links)
val Accent = Blob1

// Hypnogram stage colors
val StageAwake = Color(0xFFFDA4AF)
val StageRem = Color(0xFF93C5FD)
val StageLight = Color(0xFFA5B4FC)
val StageDeep = Color(0xFF6366F1)

// Legacy aliases still used by a few screens (solid fallbacks behind glass)
val Surface1 = Color(0xFF141926)
val Surface2 = Color(0xFF1B2130)

private val Scheme = darkColorScheme(
    primary = Accent,
    onPrimary = Color.White,
    secondary = AccentActivity,
    onSecondary = Color.Black,
    background = Bg,
    onBackground = TextPrimary,
    surface = Surface1,
    onSurface = TextPrimary,
    surfaceVariant = Surface2,
    onSurfaceVariant = TextDim,
    outline = OutlineDim,
    error = Color(0xFFFB7185)
)

// Big-numeral + small-unit pattern; tabular figures so numbers don't jitter.
private const val TNUM = "tnum"

private val AppType = Typography(
    displayLarge = TextStyle(
        fontSize = 64.sp, fontWeight = FontWeight.Bold,
        letterSpacing = (-1).sp, fontFeatureSettings = TNUM
    ),
    displayMedium = TextStyle(
        fontSize = 44.sp, fontWeight = FontWeight.Bold, fontFeatureSettings = TNUM
    ),
    headlineLarge = TextStyle(fontSize = 30.sp, fontWeight = FontWeight.Bold),
    headlineMedium = TextStyle(
        fontSize = 24.sp, fontWeight = FontWeight.SemiBold, fontFeatureSettings = TNUM
    ),
    titleLarge = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 16.sp),
    bodyMedium = TextStyle(fontSize = 14.sp),
    labelLarge = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.4.sp),
    // ALL-CAPS eyebrow labels
    labelSmall = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.8.sp)
)

@Composable
fun RecompTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Scheme, typography = AppType, content = content)
}
