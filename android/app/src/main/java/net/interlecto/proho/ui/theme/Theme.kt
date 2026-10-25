package net.interlecto.proho.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Brand ink and gold from brand/proho-wordmark.svg and brand/proho-monogram.svg
// (sampled from the night-skyline hero photo). The light scheme uses ink as
// primary with a darker gold secondary so both meet contrast requirements on
// white; the dark scheme lightens both for contrast on dark surfaces.
val ProHoInk = Color(0xFF0E1B2E)
val ProHoGold = Color(0xFF9C6418)

private val LightColors = lightColorScheme(
    primary = Color(0xFF0E1B2E),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD7DEE8),
    onPrimaryContainer = Color(0xFF0E1B2E),
    secondary = Color(0xFF9C6418),
    onSecondary = Color.White,
    background = Color(0xFFF5F5F5),
    surface = Color.White,
    surfaceVariant = Color(0xFFEEEEEE),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFAFC0D6),
    onPrimary = Color(0xFF0E1B2E),
    primaryContainer = Color(0xFF28415E),
    onPrimaryContainer = Color(0xFFD7DEE8),
    secondary = Color(0xFFF0B36B),
    onSecondary = Color(0xFF0E1B2E),
)

@Composable
fun ProHoTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
