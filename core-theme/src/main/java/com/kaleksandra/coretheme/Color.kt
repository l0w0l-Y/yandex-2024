package com.kaleksandra.coretheme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

private val primary = Color(0xFFA8DB10)

internal val DarkColors = darkColorScheme(
    primary = primary,
    background = Color(0xFF000000),
    onSurface = Color(0x80FFFFFF),
    //TODO: Add dark colors theme
)

internal val LightColors = lightColorScheme(
    primary = primary,
    background = Color(0xFFFFFFFF),
    onSurface = Color(0x80000000),
    //TODO: Add light colors theme
)