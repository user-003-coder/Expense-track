package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.dp

private val VerdantLightColorScheme = lightColorScheme(
    primary = DeepEmerald,
    onPrimary = PureWhite,
    primaryContainer = SageSurfaceTint,
    onPrimaryContainer = DeepEmeraldDark,
    secondary = MutedSage,
    onSecondary = CharcoalText,
    secondaryContainer = SageSurfaceTint,
    onSecondaryContainer = DeepEmeraldDark,
    tertiary = PositiveGreen,
    onTertiary = PureWhite,
    background = WarmOffWhite,
    onBackground = CharcoalText,
    surface = PureWhite,
    onSurface = CharcoalText,
    surfaceVariant = SageSurfaceTint,
    onSurfaceVariant = MutedGreyText,
    outline = LightBorder,
    outlineVariant = LightBorder,
    error = NegativeRed,
    onError = PureWhite,
    errorContainer = NegativeRedBg,
    onErrorContainer = NegativeRed
)

private val VerdantDarkColorScheme = darkColorScheme(
    primary = DarkEmeraldPrimary,
    onPrimary = DarkCharcoalBg,
    primaryContainer = DarkEmeraldContainer,
    onPrimaryContainer = DarkPrimaryText,
    secondary = DarkMutedSage,
    onSecondary = DarkCharcoalBg,
    secondaryContainer = DarkSageSurfaceTint,
    onSecondaryContainer = DarkPrimaryText,
    tertiary = DarkPositiveGreen,
    onTertiary = DarkCharcoalBg,
    background = DarkCharcoalBg,
    onBackground = DarkPrimaryText,
    surface = DarkCardSurface,
    onSurface = DarkPrimaryText,
    surfaceVariant = DarkSurfaceElevated,
    onSurfaceVariant = DarkSecondaryText,
    outline = DarkBorder,
    outlineVariant = DarkBorder,
    error = DarkNegativeRed,
    onError = DarkCharcoalBg,
    errorContainer = DarkNegativeRedBg,
    onErrorContainer = DarkNegativeRed
)

val VerdantShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

val LocalVerdantColors = staticCompositionLocalOf { LightVerdantColors }

object VerdantThemeTokens {
    val colors: VerdantExtendedColors
        @Composable
        @ReadOnlyComposable
        get() = LocalVerdantColors.current
}

@Composable
fun VerdantTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) VerdantDarkColorScheme else VerdantLightColorScheme
    val extendedColors = if (darkTheme) DarkVerdantColors else LightVerdantColors

    CompositionLocalProvider(LocalVerdantColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = VerdantTypography,
            shapes = VerdantShapes,
            content = content
        )
    }
}
