package com.example.flowday.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

// Theme Controller to manage global state
object ThemeController {
    private val _themeState = MutableStateFlow(AppTheme.DEFAULT)
    val themeState = _themeState.asStateFlow()

    fun switchTheme(theme: AppTheme) {
        _themeState.value = theme
    }
}

enum class AppTheme {
    DEFAULT,
    GREEN,
    DARK
}

private val DefaultColorScheme = lightColorScheme(
    primary = BluePrimary,
    background = BlueBackground,
    surface = BlueSurface,
    onPrimary = BlueOnPrimary,
    onBackground = BlueOnBackground
)

private val GreenColorScheme = lightColorScheme(
    primary = GreenPrimary,
    background = GreenBackground,
    surface = GreenSurface,
    onPrimary = GreenOnPrimary,
    onBackground = GreenOnBackground
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    background = DarkBackground,
    surface = DarkSurface,
    onPrimary = DarkOnPrimary,
    onBackground = DarkOnBackground
)

@Composable
fun FlowdayTheme(
    content: @Composable () -> Unit
) {
    val theme by ThemeController.themeState.collectAsState()
    
    val colorScheme = when (theme) {
        AppTheme.DEFAULT -> DefaultColorScheme
        AppTheme.GREEN -> GreenColorScheme
        AppTheme.DARK -> DarkColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = theme != AppTheme.DARK
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
