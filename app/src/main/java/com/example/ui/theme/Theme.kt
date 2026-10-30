package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val ShareDabbaDarkColorScheme = darkColorScheme(
    primary = ShareDabbaCyan,
    onPrimary = ShareDabbaBlack,
    primaryContainer = ShareDabbaSurfaceVariant,
    onPrimaryContainer = ShareDabbaWhite,
    secondary = ShareDabbaPurple,
    onSecondary = ShareDabbaWhite,
    secondaryContainer = ShareDabbaCardElevated,
    onSecondaryContainer = ShareDabbaWhite,
    tertiary = ShareDabbaMagenta,
    onTertiary = ShareDabbaBlack,
    background = ShareDabbaBlack,
    onBackground = ShareDabbaWhite,
    surface = ShareDabbaSurface,
    onSurface = ShareDabbaWhite,
    surfaceVariant = ShareDabbaSurfaceVariant,
    onSurfaceVariant = ShareDabbaTextSecondary,
    outline = ShareDabbaOutline,
    error = ShareDabbaError,
    onError = ShareDabbaWhite
)

@Composable
fun ShareDabbaTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = ShareDabbaDarkColorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    ShareDabbaTheme(content = content)
}
