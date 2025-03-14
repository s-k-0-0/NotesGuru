package com.example.optimusnotes.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// Modern Vibrant Color Palette
private val DarkColorSchemeVibrant = darkColorScheme(
    primary = Color(0xFF6200EE),      // Deep Purple - Vibrant primary action color
    secondary = Color(0xFF03DAC6),     // Teal - Accent color for secondary actions
    tertiary = Color(0xFFCF6679),      // Soft Pink - Decorative elements
    background = Color(0xFF121212),     // Rich Black - Deep background
    surface = Color(0xFF352C3B),       // Slightly lighter black - Card surfaces
    onPrimary = Color.White,           // White text on primary
    onSecondary = Color.Black,         // Black text on secondary
    onTertiary = Color.Black,          // Black text on tertiary
    onBackground = Color.White,         // White text on background
    onSurface = Color.White            // White text on surface
)

private val LightColorSchemeVibrant = lightColorScheme(
    primary = Color(0xFF6200EE),      // Deep Purple - Maintaining consistency with dark theme
    secondary = Color(0xFF018786),     // Darker Teal - Better contrast for light theme
    tertiary = Color(0xFFE0607E),      // Deeper Pink - Adjusted for light background
    background = Color(0xFFFAFAFA),    // Almost White - Clean background
    surface = Color.White,             // Pure White - Card surfaces
    onPrimary = Color.White,           // White text on primary
    onSecondary = Color.White,         // White text on secondary
    onTertiary = Color.White,          // White text on tertiary
    onBackground = Color(0xFF121212),  // Near-black text on background
    onSurface = Color(0xFF121212)      // Near-black text on surface
)

@Composable
fun OptimusNotesTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorSchemeVibrant
        else -> LightColorSchemeVibrant
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}