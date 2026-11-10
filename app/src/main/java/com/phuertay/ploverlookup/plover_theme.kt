package com.phuertay.ploverlookup

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColors = darkColorScheme(
    primary = Color(0xFFE6B35A),
    onPrimary = Color(0xFF2A1C00),
    background = Color(0xFF14120E),
    onBackground = Color(0xFFF3EDE2),
    surface = Color(0xFF1E1A14),
    onSurface = Color(0xFFF3EDE2),
    surfaceVariant = Color(0xFF2A241C),
    onSurfaceVariant = Color(0xFFD4CBBC),
    error = Color(0xFFFFB4AB),
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF8A5A00),
    onPrimary = Color(0xFFFFFBF5),
    background = Color(0xFFF7F1E6),
    onBackground = Color(0xFF1E1A14),
    surface = Color(0xFFFFFBF5),
    onSurface = Color(0xFF1E1A14),
    surfaceVariant = Color(0xFFE7DDCF),
    onSurfaceVariant = Color(0xFF564B3A),
)

@Composable
fun PloverLookupTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content,
    )
}
