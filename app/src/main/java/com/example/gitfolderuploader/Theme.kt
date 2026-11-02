package com.example.gitfolderuploader

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// নীল + স্লেট: শান্ত, পেশাদার প্যালেট
private val LightColors = lightColorScheme(
    primary = Color(0xFF2563EB),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDBEAFE),
    onPrimaryContainer = Color(0xFF1E3A8A),
    secondary = Color(0xFF475569),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE8EEFB),
    onSecondaryContainer = Color(0xFF1E3A8A),
    background = Color(0xFFF8FAFC),
    onBackground = Color(0xFF0F172A),
    surface = Color.White,
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = Color(0xFF64748B),
    outline = Color(0xFFE2E8F0),
    outlineVariant = Color(0xFFCBD5E1),
    error = Color(0xFFDC2626),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF60A5FA),
    onPrimary = Color(0xFF0B1220),
    primaryContainer = Color(0xFF1E3A8A),
    onPrimaryContainer = Color(0xFFDBEAFE),
    secondary = Color(0xFF94A3B8),
    onSecondary = Color(0xFF0B1220),
    secondaryContainer = Color(0xFF1E2A44),
    onSecondaryContainer = Color(0xFFDBEAFE),
    background = Color(0xFF0B1120),
    onBackground = Color(0xFFE5E7EB),
    surface = Color(0xFF111827),
    onSurface = Color(0xFFE5E7EB),
    surfaceVariant = Color(0xFF1F2937),
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = Color(0xFF273244),
    outlineVariant = Color(0xFF334155),
    error = Color(0xFFF87171),
)

private val AppShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(18.dp),
)

// টার্মিনাল ব্লকের রং (দুই থিমেই গাঢ়)
internal val TermBg = Color(0xFF0B1220)
internal val TermFg = Color(0xFFE2E8F0)
internal val TermDim = Color(0xFF7C8AA0)
internal val TermOk = Color(0xFF4ADE80)
internal val TermWarn = Color(0xFFFBBF24)
internal val TermErr = Color(0xFFF87171)

@Composable
internal fun successColor(): Color =
    if (isDark()) Color(0xFF4ADE80) else Color(0xFF16A34A)

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isDark()) DarkColors else LightColors,
        shapes = AppShapes,
        content = content,
    )
}
