package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = MintGreenDark,
    secondary = PaleOrangeDark,
    tertiary = MintSecondary,
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E),
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onBackground = Color(0xFFE0E0E0),
    onSurface = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF1B5E20),
    secondaryContainer = Color(0xFFE65100),
    onPrimaryContainer = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = GreenPrimary,
    secondary = WarmAmber,
    tertiary = GreenSecondary,
    background = Color(0xFFF1F8E9), // Light hint of green background
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color(0xFF2E3D30),
    onSurface = Color(0xFF1C281D),
    primaryContainer = Color(0xFFC8E6C9),
    secondaryContainer = Color(0xFFFFE0B2),
    onPrimaryContainer = Color(0xFF1B5E20)
)

@Composable
fun MyApplicationTheme(
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
