package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val PartlyLightColorScheme = lightColorScheme(
    primary = ElectricBlue,
    onPrimary = Color.White,
    primaryContainer = ElectricBlueSoft,
    onPrimaryContainer = ElectricBlue,
    secondary = DeepCharcoalText,
    onSecondary = Color.White,
    secondaryContainer = WarmStone,
    onSecondaryContainer = DeepCharcoalText,
    tertiary = VerifiedGreen,
    onTertiary = Color.White,
    background = WarmOffWhite,
    onBackground = DeepCharcoalText,
    surface = PureSurface,
    onSurface = DeepCharcoalText,
    surfaceVariant = WarmStone,
    onSurfaceVariant = MutedGreyText,
    outline = WarmStoneBorder,
    outlineVariant = WarmStoneBorder,
    error = IncompatibleRed,
    onError = Color.White,
    errorContainer = IncompatibleRedBg,
    onErrorContainer = IncompatibleRed
)

private val PartlyDarkColorScheme = darkColorScheme(
    primary = ElectricBlueDark,
    onPrimary = Color.White,
    primaryContainer = ElectricBlueDarkContainer,
    onPrimaryContainer = Color(0xFFDCE4FF),
    secondary = DarkPrimaryText,
    onSecondary = DarkCharcoalBg,
    secondaryContainer = DarkSurfaceElevated,
    onSecondaryContainer = DarkPrimaryText,
    tertiary = VerifiedGreen,
    onTertiary = Color.White,
    background = DarkCharcoalBg,
    onBackground = DarkPrimaryText,
    surface = DarkSurface,
    onSurface = DarkPrimaryText,
    surfaceVariant = DarkSurfaceElevated,
    onSurfaceVariant = DarkSecondaryText,
    outline = DarkBorder,
    outlineVariant = DarkBorder,
    error = Color(0xFFF87171),
    onError = Color(0xFF450A0A),
    errorContainer = Color(0xFF3F1518),
    onErrorContainer = Color(0xFFFECACA)
)

// Precision geometric shapes (avoiding overly bubbly corners)
val PartlyShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(20.dp)
)

@Composable
fun PartlyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) PartlyDarkColorScheme else PartlyLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = PartlyTypography,
        shapes = PartlyShapes,
        content = content
    )
}
