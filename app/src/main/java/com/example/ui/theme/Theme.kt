package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private val SalatiDarkColorScheme = darkColorScheme(
    primary = EmeraldMist,
    onPrimary = SanctuaryEmeraldDark,
    primaryContainer = NightJadeElevated,
    onPrimaryContainer = EmeraldMist,
    secondary = MedinaGoldLight,
    onSecondary = MedinaOnCream,
    secondaryContainer = Color(0xFF3E3010),
    onSecondaryContainer = MedinaCreamContainer,
    tertiary = Color(0xFF8DBAE0),
    onTertiary = Color(0xFF0A2236),
    background = NightTealBackground,
    onBackground = IvoryText,
    surface = NightJadeSurface,
    onSurface = IvoryText,
    surfaceVariant = NightJadeElevated,
    onSurfaceVariant = MutedSageText,
    outline = NightOutline,
    error = Color(0xFFF2B8B5)
)

private val SalatiLightColorScheme = lightColorScheme(
    primary = SanctuaryEmerald,
    onPrimary = Color.White,
    primaryContainer = SanctuaryMintContainer,
    onPrimaryContainer = SanctuaryOnMint,
    secondary = MedinaGold,
    onSecondary = Color.White,
    secondaryContainer = MedinaCreamContainer,
    onSecondaryContainer = MedinaOnCream,
    tertiary = TwilightIndigo,
    onTertiary = Color.White,
    tertiaryContainer = TwilightContainer,
    background = ParchmentBackground,
    onBackground = CharcoalInk,
    surface = AlabasterSurface,
    onSurface = CharcoalInk,
    surfaceVariant = WarmSandSurface,
    onSurfaceVariant = MutedSlateInk,
    outline = OutlineStone,
    error = AlertCrimson
)

val SalatiShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun SalatiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> SalatiDarkColorScheme
        else -> SalatiLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = SalatiTypography,
        shapes = SalatiShapes,
        content = content
    )
}
