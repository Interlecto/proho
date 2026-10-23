package net.interlecto.proho.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Brand blue from the original mockups (#428BCA). The light scheme uses a darker
// shade as primary so white text on it meets contrast requirements.
val ProHoBlue = Color(0xFF428BCA)

private val LightColors = lightColorScheme(
    primary = Color(0xFF2A6497),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD3E4F5),
    onPrimaryContainer = Color(0xFF0B2A47),
    secondary = Color(0xFF52606D),
    background = Color(0xFFF5F5F5),
    surface = Color.White,
    surfaceVariant = Color(0xFFEEEEEE),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF9CC3EA),
    onPrimary = Color(0xFF0B2A47),
    primaryContainer = Color(0xFF1F4C75),
    onPrimaryContainer = Color(0xFFD3E4F5),
    secondary = Color(0xFFB8C4CF),
)

@Composable
fun ProHoTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
