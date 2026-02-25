package com.example.aitemplate.client.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ── Exact palette from frontend styles.css ───────────────────────────────────
val BrandPrimary   = Color(0xFF111111)   // primary Black
val AccentGreen    = Color(0xFF10A37F)   // success / write tool
val AccentGoldBg   = Color(0xFFFEF3C7)
val AccentGoldText = Color(0xFF92400E)

val BgLight        = Color(0xFFFFFFFF)
val SurfaceLight   = Color(0xFFF9F9FB)
val SurfaceHover   = Color(0xFFEAEAEB)
val BorderLight    = Color(0xFFE5E5E5)
val TextMain       = Color(0xFF111111)
val TextSecondary  = Color(0xFF666666)
val CodeBgLight    = Color(0xFFF5F5F5)

val Purple600      = Color(0xFF722ED1)   // skill/memory tool
val Amber500       = Color(0xFFFA8C16)   // shell/exec tool
val AmberDark      = Color(0xFFD48806)   // skill tag text
val Cyan500        = Color(0xFF13C2C2)   // default tool
val Red500         = Color(0xFFFF4D4F)   // error

// Semantic surfaces
val BgApp          = BgLight             // page background
val SkillRowBg     = AccentGoldBg        // skill-applied-row bg
val SkillRowBorder = Color(0xFFFFE58F)   // skill-applied-row border
val CodeBg         = Color(0xFF1E2A3A)   // dark pre/code block
val CodeText       = Color(0xFFE2E8F0)

// Dark Mode Colors for Clean Slate AI
val BgDark         = Color(0xFF111111)   // background-dark from HTML
val SurfaceDark    = Color(0xFF1E1E1E)
val SurfaceHoverDark = Color(0xFF2A2A2A)
val BorderDark     = Color(0xFF333333)
val TextMainDark   = Color(0xFFEEEEEE)
val TextMutedDark  = Color(0xFFAAAAAA)

private val LightColors = lightColorScheme(
    primary            = BrandPrimary,
    onPrimary          = Color.White,
    primaryContainer   = SurfaceHover,
    onPrimaryContainer = TextMain,
    secondary          = AccentGreen,
    onSecondary        = Color.White,
    error              = Red500,
    onError            = Color.White,
    surface            = SurfaceLight,
    onSurface          = TextMain,
    surfaceVariant     = SurfaceHover,
    onSurfaceVariant   = TextSecondary,
    outline            = BorderLight,
    outlineVariant     = BorderLight,
    background         = BgLight,
    onBackground       = TextMain,
)

private val DarkColors = darkColorScheme(
    primary            = Color.White,        // In dark mode, primary branding might invert or stay AccentGreen
    onPrimary          = Color.Black,
    primaryContainer   = SurfaceHoverDark,
    onPrimaryContainer = TextMainDark,
    secondary          = AccentGreen,
    onSecondary        = Color.White,
    error              = Red500,
    surface            = SurfaceDark,
    onSurface          = TextMainDark,
    surfaceVariant     = SurfaceHoverDark,
    onSurfaceVariant   = TextMutedDark,
    background         = BgDark,
    onBackground       = TextMainDark,
    outline            = BorderDark,
    outlineVariant     = BorderDark
)

@Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content
    )
}
