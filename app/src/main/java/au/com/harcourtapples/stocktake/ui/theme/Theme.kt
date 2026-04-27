package au.com.harcourtapples.stocktake.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable

private val DarkColors = darkColorScheme(
    primary   = Green80,
    secondary = Teal80,
    tertiary  = Green80
)

private val LightColors = lightColorScheme(
    primary   = Green40,
    secondary = Teal40,
    tertiary  = Green40
)

@Composable
fun StocktakeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography  = Typography,
        content     = content
    )
}
