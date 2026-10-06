package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val MesraDarkColorScheme = darkColorScheme(
    primary = MesraPinkPrimary,
    onPrimary = Color(0xFF280514),
    primaryContainer = Color(0xFF5D1637),
    onPrimaryContainer = Color(0xFFFFD9E3),
    secondary = MesraSoftPink,
    onSecondary = Color(0xFF331020),
    secondaryContainer = Color(0xFF472135),
    onSecondaryContainer = Color(0xFFFFD8E6),
    tertiary = MesraMagentaAccent,
    onTertiary = Color(0xFF2A0036),
    tertiaryContainer = Color(0xFF531463),
    onTertiaryContainer = Color(0xFFFAD7FF),
    background = MesraDarkBg,
    onBackground = TextPrimaryLight,
    surface = MesraDarkSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = MesraCardSurface,
    onSurfaceVariant = TextSecondarySoft,
    outline = MesraBorderSubtle,
    error = ErrorCoral,
    onError = Color(0xFF3B0604)
)

val MesraShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(26.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun MesraAITheme(
    content: @Composable () -> Unit
) {
    // Dark mode is forced as default per specification for an intimate, romantic AI companion feel
    MaterialTheme(
        colorScheme = MesraDarkColorScheme,
        typography = Typography,
        shapes = MesraShapes,
        content = content
    )
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MesraAITheme(content = content)
}
