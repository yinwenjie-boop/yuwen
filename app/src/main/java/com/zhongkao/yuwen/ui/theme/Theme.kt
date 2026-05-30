package com.zhongkao.yuwen.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Green = Color(0xFF1B5E20)
private val GreenLight = Color(0xFF4C8C4A)

private val LightColors = lightColorScheme(
    primary = Green,
    secondary = GreenLight
)

private val DarkColors = darkColorScheme(
    primary = GreenLight,
    secondary = Green
)

@Composable
fun ZhongkaoYuwenTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content
    )
}
