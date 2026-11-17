package com.swifts.app.ui.theme

import android.content.res.AssetManager
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.unit.sp

/** Inter loaded from assets/fonts (filled by tools/install_fonts.sh). */
fun interFamily(assets: AssetManager): FontFamily = FontFamily(
    Font(path = "fonts/inter_regular.ttf", assetManager = assets, weight = FontWeight.Normal),
    Font(path = "fonts/inter_medium.ttf", assetManager = assets, weight = FontWeight.Medium),
    Font(path = "fonts/inter_semibold.ttf", assetManager = assets, weight = FontWeight.SemiBold),
    Font(path = "fonts/inter_bold.ttf", assetManager = assets, weight = FontWeight.Bold),
)

@Immutable
data class SwiftsColors(
    val primaryText: Color,
    val mutedText: Color,
    val accent: Color,
    val accentSoft: Color,
    val neutralPill: Color,
)

private val LightColors = SwiftsColors(
    primaryText = Color(0xFF050505),
    mutedText = Color(0xFF65676B),
    accent = Color(0xFF6A4CFF),
    accentSoft = Color(0x1F6A4CFF),
    neutralPill = Color(0xFFE4E6EB),
)

private val DarkColors = SwiftsColors(
    primaryText = Color(0xFFE4E6EB),
    mutedText = Color(0xFFB0B3B8),
    accent = Color(0xFF8B73FF),
    accentSoft = Color(0x338B73FF),
    neutralPill = Color(0xFF3A3B3C),
)

val LocalSwiftsColors = staticCompositionLocalOf { LightColors }

/** Type scale from the Swifts spec, all in Inter. */
fun swiftsTypography(f: FontFamily): Typography {
    val base = Typography()
    return Typography(
        displayLarge = base.displayLarge.copy(fontFamily = f),
        displayMedium = base.displayMedium.copy(fontFamily = f),
        displaySmall = base.displaySmall.copy(fontFamily = f),
        // Page / tab headers: 22-24sp, Bold, -0.4 letter spacing
        headlineLarge = base.headlineLarge.copy(fontFamily = f),
        headlineMedium = base.headlineMedium.copy(fontFamily = f),
        headlineSmall = base.headlineSmall.copy(
            fontFamily = f, fontWeight = FontWeight.Bold,
            fontSize = 24.sp, lineHeight = 30.sp, letterSpacing = (-0.4).sp,
        ),
        titleLarge = base.titleLarge.copy(fontFamily = f),
        // Section labels: 16-17sp, Bold
        titleMedium = base.titleMedium.copy(
            fontFamily = f, fontWeight = FontWeight.Bold,
            fontSize = 16.sp, lineHeight = 22.sp,
        ),
        // Usernames and post authors: 15sp, SemiBold
        titleSmall = base.titleSmall.copy(
            fontFamily = f, fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp, lineHeight = 20.sp,
        ),
        // Post body text: 15sp, Regular
        bodyLarge = base.bodyLarge.copy(
            fontFamily = f, fontWeight = FontWeight.Normal,
            fontSize = 15.sp, lineHeight = 22.sp,
        ),
        bodyMedium = base.bodyMedium.copy(fontFamily = f),
        // Timestamps and subtitles: 12-13sp, Regular
        bodySmall = base.bodySmall.copy(
            fontFamily = f, fontWeight = FontWeight.Normal,
            fontSize = 12.sp, lineHeight = 16.sp,
        ),
        // Filter pills and tab labels: 14sp, SemiBold
        labelLarge = base.labelLarge.copy(
            fontFamily = f, fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp, lineHeight = 18.sp,
        ),
        // Action buttons: 13-14sp, SemiBold
        labelMedium = base.labelMedium.copy(
            fontFamily = f, fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp, lineHeight = 16.sp,
        ),
        // Bottom navigation labels: 10-11sp, Medium
        labelSmall = base.labelSmall.copy(
            fontFamily = f, fontWeight = FontWeight.Medium,
            fontSize = 11.sp, lineHeight = 13.sp, letterSpacing = 0.sp,
        ),
    )
}

@Composable
fun SwiftsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val family = remember(context) { interFamily(context.assets) }
    val typography = remember(family) { swiftsTypography(family) }
    val colors = if (darkTheme) DarkColors else LightColors
    val scheme = if (darkTheme) {
        darkColorScheme(
            primary = colors.accent,
            onPrimary = Color.White,
            background = Color(0xFF18191A),
            onBackground = colors.primaryText,
            surface = Color(0xFF242526),
            onSurface = colors.primaryText,
        )
    } else {
        lightColorScheme(
            primary = colors.accent,
            onPrimary = Color.White,
            background = Color.White,
            onBackground = colors.primaryText,
            surface = Color.White,
            onSurface = colors.primaryText,
        )
    }
    CompositionLocalProvider(LocalSwiftsColors provides colors) {
        MaterialTheme(colorScheme = scheme, typography = typography, content = content)
    }
}
