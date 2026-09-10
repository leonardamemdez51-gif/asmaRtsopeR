package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = RamaTealContainer,
    onPrimary = RamaTealOnContainer,
    primaryContainer = RamaTealPrimary,
    onPrimaryContainer = RamaTealOnPrimary,
    secondary = RamaGoldContainer,
    onSecondary = RamaGoldOnContainer,
    background = RamaOnSurface,
    surface = Color(0xFF1E293B),
    onBackground = RamaBackground,
    onSurface = RamaBackground
)

private val LightColorScheme = lightColorScheme(
    primary = RamaTealPrimary,
    onPrimary = RamaTealOnPrimary,
    primaryContainer = RamaTealContainer,
    onPrimaryContainer = RamaTealOnContainer,
    secondary = RamaGoldSecondary,
    onSecondary = RamaGoldOnSecondary,
    secondaryContainer = RamaGoldContainer,
    onSecondaryContainer = RamaGoldOnContainer,
    background = RamaBackground,
    surface = RamaSurface,
    surfaceVariant = RamaSurfaceVariant,
    onBackground = RamaOnSurface,
    onSurface = RamaOnSurface,
    onSurfaceVariant = RamaOnSurfaceVariant,
    outline = RamaOutline,
    error = RamaRedOverdue,
    errorContainer = RamaRedContainer
)

@Composable
fun RamaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
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

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) = RamaTheme(darkTheme = darkTheme, dynamicColor = dynamicColor, content = content)

