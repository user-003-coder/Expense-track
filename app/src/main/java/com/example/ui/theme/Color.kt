package com.example.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

// Core Light Palette — Deep Emerald Green + Warm Off-White
val DeepEmerald = Color(0xFF146C55)
val DeepEmeraldDark = Color(0xFF0E5240)
val MutedSage = Color(0xFFA8C8B7)
val SageSurfaceTint = Color(0xFFEAF3EE)
val WarmOffWhite = Color(0xFFF8FAF8)
val PureWhite = Color(0xFFFFFFFF)
val CharcoalText = Color(0xFF202824)
val MutedGreyText = Color(0xFF89938D)
val PositiveGreen = Color(0xFF23865B)
val PositiveGreenBg = Color(0xFFE9F5EE)
val NegativeRed = Color(0xFFD65C5C)
val NegativeRedBg = Color(0xFFFCEEEE)
val LightBorder = Color(0xFFE6ECE8)
val WarningAmber = Color(0xFFB87B24)
val WarningAmberBg = Color(0xFFFDF4E5)

// Coordinated Dark Emerald & Charcoal Palette
val DarkEmeraldPrimary = Color(0xFF47B894)
val DarkEmeraldContainer = Color(0xFF123B2F)
val DarkMutedSage = Color(0xFF7FA893)
val DarkSageSurfaceTint = Color(0xFF1A2923)
val DarkCharcoalBg = Color(0xFF0F1412)
val DarkCardSurface = Color(0xFF171F1C)
val DarkSurfaceElevated = Color(0xFF1F2A25)
val DarkPrimaryText = Color(0xFFECF2EE)
val DarkSecondaryText = Color(0xFF93A19A)
val DarkPositiveGreen = Color(0xFF3CC488)
val DarkPositiveGreenBg = Color(0xFF122C21)
val DarkNegativeRed = Color(0xFFEA7A7A)
val DarkNegativeRedBg = Color(0xFF331B1C)
val DarkBorder = Color(0xFF25322C)
val DarkWarningAmber = Color(0xFFE5A94C)
val DarkWarningAmberBg = Color(0xFF2E2414)

// Harmonized Chart Palette (derived from Emerald, Sage, Slate, Earth & Coral)
val ChartEmeraldPaletteLight = listOf(
    Color(0xFF146C55),
    Color(0xFF2E8B6E),
    Color(0xFF5CA48C),
    Color(0xFF8BBCA8),
    Color(0xFF476961),
    Color(0xFFD65C5C),
    Color(0xFFB88746),
    Color(0xFF6B857B),
    Color(0xFF3B7A7A),
    Color(0xFF9E7B6E)
)

val ChartEmeraldPaletteDark = listOf(
    Color(0xFF47B894),
    Color(0xFF34997A),
    Color(0xFF6EC9AB),
    Color(0xFF8AB5A0),
    Color(0xFF5B877B),
    Color(0xFFEA7A7A),
    Color(0xFFDDA95D),
    Color(0xFF7F9E92),
    Color(0xFF53A3A3),
    Color(0xFFC29B8C)
)

@Immutable
data class VerdantExtendedColors(
    val income: Color,
    val incomeContainer: Color,
    val expense: Color,
    val expenseContainer: Color,
    val border: Color,
    val mutedText: Color,
    val sageTint: Color,
    val warning: Color,
    val warningContainer: Color,
    val chartPalette: List<Color>
)

val LightVerdantColors = VerdantExtendedColors(
    income = PositiveGreen,
    incomeContainer = PositiveGreenBg,
    expense = NegativeRed,
    expenseContainer = NegativeRedBg,
    border = LightBorder,
    mutedText = MutedGreyText,
    sageTint = SageSurfaceTint,
    warning = WarningAmber,
    warningContainer = WarningAmberBg,
    chartPalette = ChartEmeraldPaletteLight
)

val DarkVerdantColors = VerdantExtendedColors(
    income = DarkPositiveGreen,
    incomeContainer = DarkPositiveGreenBg,
    expense = DarkNegativeRed,
    expenseContainer = DarkNegativeRedBg,
    border = DarkBorder,
    mutedText = DarkSecondaryText,
    sageTint = DarkSageSurfaceTint,
    warning = DarkWarningAmber,
    warningContainer = DarkWarningAmberBg,
    chartPalette = ChartEmeraldPaletteDark
)
