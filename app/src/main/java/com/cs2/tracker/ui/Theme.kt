package com.cs2.tracker.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Cs2Orange = Color(0xFFF7A600)
val Cs2OrangeDim = Color(0xFF3A3120)
val Cs2Green = Color(0xFF37D67A)
val Cs2Red = Color(0xFFFF5B5B)
val BgColor = Color(0xFF14161A)
val CardColor = Color(0xFF1E222A)
val CardColor2 = Color(0xFF1B1E24)
val LineColor = Color(0xFF2C313B)
val TextMain = Color(0xFFE8EAED)
val TextDim = Color(0xFF9AA1AD)
val TextFaint = Color(0xFF6B7280)
val FieldColor = Color(0xFF22262E)

// 深色主题完整配色：确保所有 on* 均为浅色，避免暗色下出现黑字
private val DarkScheme = darkColorScheme(
    primary = Cs2Orange,
    onPrimary = Color(0xFF1A1A1A),
    primaryContainer = Cs2OrangeDim,
    onPrimaryContainer = TextMain,
    secondary = Cs2Orange,
    onSecondary = Color(0xFF1A1A1A),
    secondaryContainer = Cs2OrangeDim,
    onSecondaryContainer = TextMain,
    tertiary = Cs2Green,
    onTertiary = Color(0xFF1A1A1A),
    background = BgColor,
    onBackground = TextMain,
    surface = CardColor,
    onSurface = TextMain,
    surfaceVariant = FieldColor,
    onSurfaceVariant = TextDim,
    surfaceContainerLowest = CardColor2,
    surfaceContainerLow = Color(0xFF22262E),
    surfaceContainer = Color(0xFF22262E),
    surfaceContainerHigh = Color(0xFF2A2F3A),
    surfaceContainerHighest = Color(0xFF303643),
    onError = Color.White,
    errorContainer = Color(0xFF4A2525),
    onErrorContainer = Color(0xFFFFDAD6),
    outline = LineColor,
    outlineVariant = Color(0xFF3A4049),
    inverseSurface = TextMain,
    inverseOnSurface = Color(0xFF1A1A1A),
    inversePrimary = Cs2Orange,
    scrim = Color.Black
)

@Composable
fun Cs2Theme(content: @Composable () -> Unit) {
    // 固定深色（CS2 风格），不随系统切换
    MaterialTheme(colorScheme = DarkScheme, content = content)
}
