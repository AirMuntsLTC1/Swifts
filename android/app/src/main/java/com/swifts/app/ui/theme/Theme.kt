package com.swifts.app.ui.theme

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
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.swifts.app.R

/** Inter from res/font. */
fun interFamily(): FontFamily = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold, FontWeight.Bold),
)

// Palette: white, greys, and near-black text. No color accents.
private val INK = Color(0xFF1C1C1E)        // slightly black: text, buttons, icons
private val INK_SOFT = Color(0xFF3A3A3C)
private val MUTED = Color(0xFF65676B)      // secondary text
private val LINE = Color(0xFFE4E6EB)       // borders and dividers
private val OUTLINE = Color(0xFFC4C7CC)
private val WASH = Color(0xFFF0F2F5)       // pills, avatars, bars
private val WASH_STRONG = Color(0xFFEEEEF0)
private val WHITE = Color(0xFFFFFFFF)
private val SURFACE_LIGHT = Color(0xFFF7F7F8)   // web bg-surface

private val DARK_INK = Color(0xFFF2F2F4)
private val DARK_MUTED = Color(0xFFA8ABB0)
private val DARK_LINE = Color(0xFF2C2C2E)
private val DARK_BG = Color(0xFF121212)
private val DARK_SURFACE = Color(0xFF1C1C1E)
private val DARK_BRAND_HOVER = Color(0xFFD6D6D9) // web brand-hover (dark)

@Immutable
data class SwiftsColors(
    val primaryText: Color,
    val mutedText: Color,
    val accent: Color,
    val accentSoft: Color,
    val neutralPill: Color,
    val brandHover: Color,
)

private val LightColors = SwiftsColors(
    primaryText = INK,
    mutedText = MUTED,
    accent = INK,
    accentSoft = WASH_STRONG,
    neutralPill = WASH,
    brandHover = INK_SOFT,
)

private val DarkColors = SwiftsColors(
    primaryText = DARK_INK,
    mutedText = DARK_MUTED,
    accent = DARK_INK,
    accentSoft = DARK_LINE,
    neutralPill = DARK_LINE,
    brandHover = DARK_BRAND_HOVER,
)

val LocalSwiftsColors = staticCompositionLocalOf { LightColors }

private val LightScheme = lightColorScheme(
    primary = INK, onPrimary = WHITE,
    secondary = INK_SOFT, onSecondary = WHITE,
    tertiary = MUTED, onTertiary = WHITE,
    background = WHITE, onBackground = INK,
    surface = WHITE, onSurface = INK,
    surfaceVariant = WASH, onSurfaceVariant = MUTED,
    surfaceContainerLow = SURFACE_LIGHT,
    surfaceTint = WHITE,
    outline = OUTLINE, outlineVariant = LINE,
    primaryContainer = WASH_STRONG, onPrimaryContainer = INK,
    secondaryContainer = WASH, onSecondaryContainer = INK,
    error = INK, onError = WHITE,
    errorContainer = WASH, onErrorContainer = INK,
)

private val DarkScheme = darkColorScheme(
    primary = DARK_INK, onPrimary = DARK_BG,
    secondary = DARK_MUTED, onSecondary = DARK_BG,
    tertiary = DARK_MUTED, onTertiary = DARK_BG,
    background = DARK_BG, onBackground = DARK_INK,
    surface = DARK_SURFACE, onSurface = DARK_INK,
    surfaceVariant = DARK_LINE, onSurfaceVariant = DARK_MUTED,
    surfaceContainerLow = DARK_SURFACE,
    surfaceTint = DARK_BG,
    outline = DARK_MUTED, outlineVariant = DARK_LINE,
    primaryContainer = DARK_LINE, onPrimaryContainer = DARK_INK,
    secondaryContainer = DARK_LINE, onSecondaryContainer = DARK_INK,
    error = DARK_INK, onError = DARK_BG,
    errorContainer = DARK_LINE, onErrorContainer = DARK_INK,
)

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
    darkTheme: Boolean = false,
    content: @Composable () -> Unit,
) {
    val family = remember { interFamily() }
    val typography = remember(family) { swiftsTypography(family) }
    val colors = if (darkTheme) DarkColors else LightColors
    val scheme = if (darkTheme) DarkScheme else LightScheme
    CompositionLocalProvider(LocalSwiftsColors provides colors) {
        MaterialTheme(colorScheme = scheme, typography = typography, content = content)
    }
}

