package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = GoldPrimary,
    onPrimary = Color.White,
    primaryContainer = AmberAccent,
    onPrimaryContainer = Color(0xFF042100),
    secondary = GoldSecondary,
    onSecondary = Color.White,
    background = Color(0xFFF7F9F2),
    onBackground = Color(0xFF1A1C18),
    surface = Color(0xFFE1E4D5),
    onSurface = Color(0xFF1A1C18),
    surfaceVariant = Color(0xFFF0F3E8),
    onSurfaceVariant = Color(0xFF43493E),
    outline = Color(0xFFD6D8C0),
    error = CrimsonError,
    onError = Color.White
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF9BDD76),
    onPrimary = Color(0xFF133800),
    primaryContainer = Color(0xFF235008),
    onPrimaryContainer = Color(0xFFB8F397),
    secondary = Color(0xFFBCCBB0),
    onSecondary = Color(0xFF273421),
    background = Color(0xFF121410),
    onBackground = Color(0xFFE2E3DC),
    surface = Color(0xFF1C1F18),
    onSurface = Color(0xFFE2E3DC),
    surfaceVariant = Color(0xFF242820),
    onSurfaceVariant = Color(0xFFC3C8BC),
    outline = Color(0xFF3C4237),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005)
)

val SlateDarkBackground: Color
    @Composable get() = MaterialTheme.colorScheme.background

val SlateDarkSurface: Color
    @Composable get() = MaterialTheme.colorScheme.surface

val SlateDarkCard: Color
    @Composable get() = MaterialTheme.colorScheme.surfaceVariant

val GeoBorderColor: Color
    @Composable get() = MaterialTheme.colorScheme.outline

val GeoTextPrimary: Color
    @Composable get() = MaterialTheme.colorScheme.onBackground

val GeoTextSecondary: Color
    @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant

@Composable
fun ChessAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}


