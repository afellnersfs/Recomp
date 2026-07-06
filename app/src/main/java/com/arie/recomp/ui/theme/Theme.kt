package com.arie.recomp.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Dark, bold, one electric-orange accent.
val Bg = Color(0xFF0C0D10)
val Surface1 = Color(0xFF16181D)
val Surface2 = Color(0xFF1E2127)
val Accent = Color(0xFFFF6B00)
val TextPrimary = Color(0xFFF2F3F5)
val TextDim = Color(0xFF9BA1AC)
val OutlineDim = Color(0xFF2A2E36)

private val Scheme = darkColorScheme(
    primary = Accent,
    onPrimary = Color.Black,
    secondary = Accent,
    onSecondary = Color.Black,
    background = Bg,
    onBackground = TextPrimary,
    surface = Surface1,
    onSurface = TextPrimary,
    surfaceVariant = Surface2,
    onSurfaceVariant = TextDim,
    outline = OutlineDim,
    error = Color(0xFFFF5252)
)

private val AppType = Typography(
    displayLarge = TextStyle(fontSize = 64.sp, fontWeight = FontWeight.Black, letterSpacing = (-1).sp),
    displayMedium = TextStyle(fontSize = 48.sp, fontWeight = FontWeight.Black),
    headlineLarge = TextStyle(fontSize = 32.sp, fontWeight = FontWeight.ExtraBold),
    headlineMedium = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold),
    titleLarge = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold),
    titleMedium = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 16.sp),
    bodyMedium = TextStyle(fontSize = 14.sp),
    labelLarge = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp),
    labelSmall = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.2.sp)
)

@Composable
fun RecompTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Scheme, typography = AppType, content = content)
}
