package com.example.livesplitlike.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkBackground = Color(0xFF121212)
private val DarkSurface = Color(0xFF1E1E1E)
private val DarkOnSurface = Color(0xFFF1F1F1)

private val LightBackground = Color(0xFFF5F5F5)
private val LightSurface = Color(0xFFFFFFFF)
private val LightOnSurface = Color(0xFF111111)

private val DarkColors = darkColorScheme(
    primary = Color.White,
    onPrimary = Color.Black,
    background = DarkBackground,
    onBackground = DarkOnSurface,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = Color(0xFF2A2A2A)
)

private val LightColors = lightColorScheme(
    primary = Color.Black,
    onPrimary = Color.White,
    background = LightBackground,
    onBackground = LightOnSurface,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = Color(0xFFEDEDED)
)

@Composable
fun AppTheme(darkTheme: Boolean = true, content: @Composable () -> Unit) {
    val colors = if (darkTheme) DarkColors else LightColors

    MaterialTheme(
        colorScheme = colors,
        typography = Typography(),
        content = content
    )
}
