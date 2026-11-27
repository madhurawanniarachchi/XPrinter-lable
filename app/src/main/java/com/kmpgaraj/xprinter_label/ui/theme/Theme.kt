package com.kmpgaraj.xprinter_label.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * The app's own palette. Material 3 has no slot for most of these (field
 * border, preview stage, soft accent...), so screens read them from here via
 * [MaterialTheme.palette]; [XPrinterLabelTheme] also maps the core ones onto the
 * Material colour scheme so dialogs and menus match.
 */
@Immutable
data class AppColors(
    val background: Color,
    val card: Color,
    /** Hairline borders around cards and dividers. */
    val line: Color,
    /** Borders of input fields. */
    val border: Color,
    val ink: Color,
    val label: Color,
    val mute: Color,
    val placeholder: Color,
    val accent: Color,
    /** Accent used for text on neutral backgrounds (lighter than [accent] in dark mode). */
    val accentText: Color,
    val onAccent: Color,
    val accentSoft: Color,
    val field: Color,
    /** Backdrop behind the label preview. */
    val stage: Color,
    val tabTrack: Color,
    val tabSelected: Color,
    val disabled: Color,
    val error: Color,
    val ok: Color,
    val off: Color
)

private val LightAppColors = AppColors(
    background = Color(0xFFF3F5F9),
    card = Color(0xFFFFFFFF),
    line = Color(0xFFE2E8F0),
    border = Color(0xFFCBD5E1),
    ink = Color(0xFF0F172A),
    label = Color(0xFF334155),
    mute = Color(0xFF475569),
    placeholder = Color(0xFF94A3B8),
    accent = Color(0xFF4F46E5),
    accentText = Color(0xFF4F46E5),
    onAccent = Color(0xFFFFFFFF),
    accentSoft = Color(0xFFEEF2FF),
    field = Color(0xFFFFFFFF),
    stage = Color(0xFFE9EDF5),
    tabTrack = Color(0xFFE6EAF2),
    tabSelected = Color(0xFFFFFFFF),
    disabled = Color(0xFFCBD5E1),
    error = Color(0xFFB91C1C),
    ok = Color(0xFF16A34A),
    off = Color(0xFF94A3B8)
)

private val DarkAppColors = AppColors(
    background = Color(0xFF0B0E14),
    card = Color(0xFF151922),
    line = Color(0xFF262B38),
    border = Color(0xFF3A4152),
    ink = Color(0xFFF1F5F9),
    label = Color(0xFFCBD5E1),
    mute = Color(0xFF94A3B8),
    placeholder = Color(0xFF64748B),
    accent = Color(0xFF6366F1),
    accentText = Color(0xFFA5B4FC),
    onAccent = Color(0xFFFFFFFF),
    accentSoft = Color(0xFF1E2145),
    field = Color(0xFF0F131B),
    stage = Color(0xFF0F131B),
    tabTrack = Color(0xFF0F131B),
    tabSelected = Color(0xFF2A3042),
    disabled = Color(0xFF2A3042),
    error = Color(0xFFF87171),
    ok = Color(0xFF4ADE80),
    off = Color(0xFF64748B)
)

private val LocalAppColors = staticCompositionLocalOf { LightAppColors }

val MaterialTheme.palette: AppColors
    @Composable
    @ReadOnlyComposable
    get() = LocalAppColors.current

private fun ColorScheme.withPalette(p: AppColors) = copy(
    primary = p.accent,
    onPrimary = p.onAccent,
    background = p.background,
    onBackground = p.ink,
    surface = p.card,
    onSurface = p.ink,
    onSurfaceVariant = p.mute,
    outline = p.border,
    outlineVariant = p.line,
    error = p.error,
    surfaceContainer = p.card,
    surfaceContainerHigh = p.card
)

/** A fixed brand palette (no dynamic colour), so the app looks the same on every phone. */
@Composable
fun XPrinterLabelTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val palette = if (darkTheme) DarkAppColors else LightAppColors
    val base = if (darkTheme) darkColorScheme() else lightColorScheme()
    CompositionLocalProvider(LocalAppColors provides palette) {
        MaterialTheme(colorScheme = base.withPalette(palette), content = content)
    }
}
