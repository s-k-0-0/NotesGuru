package com.example.optimusnotes.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// Vibrant Color Palette
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF00B4D8),  // Vibrant Cyan
    secondary = Color(0xFF90CAF9),  // Light Blue
    background = Color(0xFF121212),  // Dark Background
    surface = Color(0xFF1E1E1E),  // Dark Surface
    onPrimary = Color.White,  // Text on Cyan
    onSecondary = Color.Black,  // Text on Light Blue
    onBackground = Color.White,  // Text on Dark Background
    onSurface = Color.White  // Text on Dark Surface
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF0077B6),  // Deep Blue
    secondary = Color(0xFF00B4D8),  // Bright Cyan
    background = Color(0xFFE3F2FD),  // Soft Light Blue
    surface = Color.White,  // White Surface
    onPrimary = Color.White,  // Text on Primary
    onSecondary = Color.Black,  // Text on Cyan
    onBackground = Color.Black,  // Text on Light Background
    onSurface = Color.Black  // Text on White Surface
)

@Composable
fun OptimusNotesTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true, // Enable dynamic color support
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
