package com.agguy.moni.app.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = PrimaryLight,
    onPrimary = OnPrimaryLight,
    primaryContainer = PrimaryContainerLight,
    onPrimaryContainer = OnPrimaryContainerLight,
    secondary = SecondaryLight,
    onSecondary = OnSecondaryLight,
    secondaryContainer = SecondaryContainerLight,
    onSecondaryContainer = OnSecondaryContainerLight,
    tertiary = TertiaryLight,
    onTertiary = OnTertiaryLight,
    tertiaryContainer = TertiaryContainerLight,
    onTertiaryContainer = OnTertiaryContainerLight,
    error = ErrorLight,
    onError = OnErrorLight,
    errorContainer = ErrorContainerLight,
    onErrorContainer = OnErrorContainerLight,
    background = BackgroundLight,
    onBackground = OnBackgroundLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    outline = OutlineLight
)

private val DarkColors = darkColorScheme(
    primary = PrimaryContainerLight,
    onPrimary = OnPrimaryContainerLight,
    primaryContainer = PrimaryLight,
    onPrimaryContainer = OnPrimaryLight,
    secondary = SecondaryContainerLight,
    onSecondary = OnSecondaryContainerLight,
    secondaryContainer = SecondaryLight,
    onSecondaryContainer = OnSecondaryLight,
    tertiary = TertiaryContainerLight,
    onTertiary = OnTertiaryContainerLight,
    tertiaryContainer = TertiaryLight,
    onTertiaryContainer = OnTertiaryLight,
    error = ErrorContainerLight,
    onError = OnErrorContainerLight,
    errorContainer = ErrorLight,
    onErrorContainer = OnErrorLight,
    background = OnBackgroundLight,
    onBackground = BackgroundLight,
    surface = OnSurfaceLight,
    onSurface = SurfaceLight,
    surfaceVariant = OnSurfaceVariantLight,
    onSurfaceVariant = SurfaceVariantLight,
    outline = OutlineLight
)

@Composable
fun MoniTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
