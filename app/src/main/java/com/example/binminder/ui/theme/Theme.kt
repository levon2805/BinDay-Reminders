package com.example.binminder.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.binminder.data.model.AppThemeMode

import androidx.compose.ui.composed

private val DarkColorScheme = darkColorScheme(
    primary = BrandVibrantLeaf,
    onPrimary = BrandCharcoalDark,
    primaryContainer = BrandDeepEmerald,
    onPrimaryContainer = BrandOffWhite,
    secondary = BrandVibrantLeaf,
    onSecondary = BrandCharcoalDark,
    secondaryContainer = BrandDeepEmerald,
    onSecondaryContainer = BrandOffWhite,
    tertiary = BrandVibrantLeaf,
    onTertiary = BrandCharcoalDark,
    tertiaryContainer = BrandCharcoal,
    onTertiaryContainer = BrandOffWhite,
    background = BrandCharcoalDark,
    onBackground = BrandOffWhite,
    surface = BrandCharcoal,
    onSurface = BrandOffWhite,
    surfaceVariant = BrandCharcoal,
    onSurfaceVariant = BrandOffWhite,
    surfaceContainer = Color(0xFF181818),
    surfaceContainerLow = BrandCharcoalDark,
    surfaceContainerHigh = Color(0xFF2C2C2C),
    surfaceContainerHighest = Color(0xFF383838),
    outline = NeoBorderDark,
    outlineVariant = NeoBorderDark
)

private val LightColorScheme = lightColorScheme(
    primary = BrandVibrantLeaf,
    onPrimary = BrandWhite,
    primaryContainer = BrandDeepEmerald,
    onPrimaryContainer = BrandWhite,
    secondary = BrandVibrantLeaf,
    onSecondary = BrandWhite,
    secondaryContainer = BrandDeepEmerald,
    onSecondaryContainer = BrandWhite,
    tertiary = BrandVibrantLeaf,
    onTertiary = BrandWhite,
    tertiaryContainer = Color(0xFFE9F5EC),
    onTertiaryContainer = BrandDeepEmerald,
    background = BrandOffWhite,
    onBackground = BrandDeepEmerald,
    surface = BrandWhite,
    onSurface = BrandDeepEmerald,
    surfaceVariant = BrandWhite,
    onSurfaceVariant = BrandDeepEmerald,
    surfaceContainer = Color(0xFFF1F3F5),
    surfaceContainerLow = BrandOffWhite,
    surfaceContainerHigh = Color(0xFFE9ECEF),
    surfaceContainerHighest = Color(0xFFDEE2E6),
    outline = NeoBorderLight,
    outlineVariant = NeoBorderLight
)

val NeoShadowLight = Color(0x2B18261F) // Explicit dark slate/black shadow for Light Mode
val NeoShadowDark = Color(0x88000000)  // Crisp deep black drop shadow for Dark Mode

/**
 * Custom Modifier for Subtle Neo-Brutalist Shadows with high contrast in Light & Dark mode.
 * Light mode shadow color is explicitly dark slate/black and never resolves to green or container color.
 */
fun Modifier.neoShadow(
    color: Color = Color.Unspecified,
    offset: Dp = 4.dp
): Modifier = composed {
    val isDark = isSystemInDarkTheme()
    val defaultShadow = if (isDark) NeoShadowDark else NeoShadowLight
    val outlineColor = MaterialTheme.colorScheme.outline
    val shadowColor = if (color != Color.Unspecified && color != outlineColor) color else defaultShadow

    this.drawBehind {
        val offsetPx = offset.toPx()
        val cornerRadius = 8.dp.toPx()

        drawRoundRect(
            color = shadowColor,
            topLeft = Offset(offsetPx, offsetPx),
            size = size,
            cornerRadius = CornerRadius(cornerRadius, cornerRadius)
        )
    }
}

/**
 * Custom Material 3 Neobrutalist theme wrapper for BinMinder.
 */
@Composable
fun BinMinderTheme(
    themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    darkTheme: Boolean = when (themeMode) {
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK -> true
        AppThemeMode.SYSTEM -> isSystemInDarkTheme()
    },
    dynamicColor: Boolean = false, // Disabled by default for brutalist look
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
