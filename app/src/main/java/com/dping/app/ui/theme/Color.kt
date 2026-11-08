package com.dping.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Dynamic theme palette for D Ping.
 * Dark Mode: Calming, practical obsidian-cyan cyber aesthetic matching the official mockups.
 * Light Mode: High-contrast, crystal-clear readability (dark charcoal text on light cards).
 */
data class DPingPalette(
    val isDark: Boolean,
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val border: Color,
    val borderGlow: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val accentCyan: Color,
    val accentTeal: Color,
    val accentGreen: Color,
    val accentRed: Color,
    val accentOrange: Color,
    val terminalBackground: Color,
    val terminalBorder: Color
)

// Practical Dark Palette (as per mockups in media_1792304879499_5bd04aa3.jpg)
val DarkDPingPalette = DPingPalette(
    isDark = true,
    background = Color(0xFF070D15),         // Deep calming midnight obsidian
    surface = Color(0xFF0E1622),            // Refined card surface
    surfaceVariant = Color(0xFF14202E),     // Card highlight / secondary container
    border = Color(0xFF1A293B),             // Subtle slate border (not blinding neon)
    borderGlow = Color(0xFF00C2FF),         // Cyan highlight
    textPrimary = Color(0xFFF1F5F9),        // High-contrast clean off-white
    textSecondary = Color(0xFF8B9EB5),      // Readable soft slate
    textMuted = Color(0xFF55687D),          // Subtle metadata slate
    accentCyan = Color(0xFF00C2FF),         // Electric Blue / Cyan
    accentTeal = Color(0xFF00E5FF),         // Vibrant Cyan
    accentGreen = Color(0xFF10B981),        // Emerald Green (connected / success)
    accentRed = Color(0xFFEF4444),          // Coral Red
    accentOrange = Color(0xFFF59E0B),       // Amber
    terminalBackground = Color(0xFF03070D), // Console jet black
    terminalBorder = Color(0xFF132030)
)

// Practical Light Palette (100% visible, high-contrast, professional)
val LightDPingPalette = DPingPalette(
    isDark = false,
    background = Color(0xFFF1F5F9),         // Clean soft slate gray
    surface = Color(0xFFFFFFFF),            // Pure white cards
    surfaceVariant = Color(0xFFE2E8F0),     // Subtle gray variant
    border = Color(0xFFCBD5E1),             // Defined slate border
    borderGlow = Color(0xFF0284C7),         // Deep cyan highlight
    textPrimary = Color(0xFF0F172A),        // Deep dark charcoal (14.8:1 contrast on white!)
    textSecondary = Color(0xFF475569),      // Medium dark slate
    textMuted = Color(0xFF64748B),          // Muted slate
    accentCyan = Color(0xFF0284C7),         // Deep vibrant cyan
    accentTeal = Color(0xFF0369A1),         // Deep teal/blue
    accentGreen = Color(0xFF059669),        // Deep emerald green
    accentRed = Color(0xFFDC2626),          // Deep red
    accentOrange = Color(0xFFD97706),       // Deep amber
    terminalBackground = Color(0xFF0A111A), // Stays dark console for authentic terminal contrast
    terminalBorder = Color(0xFFCBD5E1)
)

val LocalDPingPalette = staticCompositionLocalOf { DarkDPingPalette }

// Dynamic composable color accessors ensuring 100% visibility in both Light and Dark modes
val DarkBackground: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalDPingPalette.current.background

val DarkSurface: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalDPingPalette.current.surface

val DarkSurfaceVariant: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalDPingPalette.current.surfaceVariant

val DarkBorder: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalDPingPalette.current.border

val DarkBorderGlow: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalDPingPalette.current.borderGlow

val TextPrimaryDark: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalDPingPalette.current.textPrimary

val TextSecondaryDark: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalDPingPalette.current.textSecondary

val TextMutedDark: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalDPingPalette.current.textMuted

val TerminalBackground: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalDPingPalette.current.terminalBackground

val TerminalBorder: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalDPingPalette.current.terminalBorder

val NeonTeal: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalDPingPalette.current.accentCyan

val NeonCyan: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalDPingPalette.current.accentTeal

val NeonTealDark: Color
    @Composable
    @ReadOnlyComposable
    get() = if (LocalDPingPalette.current.isDark) Color(0xFF00A389) else Color(0xFF047857)

val DestructiveRed: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalDPingPalette.current.accentRed

val DestructiveRedDark: Color
    @Composable
    @ReadOnlyComposable
    get() = if (LocalDPingPalette.current.isDark) Color(0xFFD32F2F) else Color(0xFFB91C1C)

val WarningOrange: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalDPingPalette.current.accentOrange

val NeonGradient: Brush
    @Composable
    get() = if (LocalDPingPalette.current.isDark) {
        Brush.horizontalGradient(listOf(Color(0xFF00C2FF), Color(0xFF0096C7)))
    } else {
        Brush.horizontalGradient(listOf(Color(0xFF0284C7), Color(0xFF0369A1)))
    }

val CardGlowGradient: Brush
    @Composable
    get() = if (LocalDPingPalette.current.isDark) {
        Brush.linearGradient(listOf(Color(0x1400C2FF), Color(0x0400B4D8)))
    } else {
        Brush.linearGradient(listOf(Color(0x0A0284C7), Color(0x020369A1)))
    }

// Legacy static fallbacks
val LightBackground = Color(0xFFF1F5F9)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceVariant = Color(0xFFE2E8F0)
val LightBorder = Color(0xFFCBD5E1)
val TextPrimaryLight = Color(0xFF0F172A)
val TextSecondaryLight = Color(0xFF475569)
