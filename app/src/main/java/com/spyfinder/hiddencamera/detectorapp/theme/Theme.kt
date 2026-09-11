package com.spyfinder.hiddencamera.detectorapp.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.core.view.WindowCompat

private val QuietLight = lightColorScheme(
    primary = Color(0xFF16684E), onPrimary = Color.White,
    primaryContainer = Color(0xFFE1EEDF), onPrimaryContainer = Color(0xFF172F28),
    background = Color(0xFFF5F6F2), onBackground = Color(0xFF172F28),
    surface = Color.White, onSurface = Color(0xFF172F28),
    surfaceVariant = Color(0xFFE1EEDF), onSurfaceVariant = Color(0xFF62736A),
    outline = Color(0xFFE0E7DF), outlineVariant = Color(0xFFE0E7DF),
    error = Color(0xFF8B501C), errorContainer = Color(0xFFFFF1DF), onErrorContainer = Color(0xFF8B501C)
)
private val QuietDark = darkColorScheme(
    primary = Color(0xFFA5E3B6), onPrimary = Color(0xFF173A28),
    primaryContainer = Color(0xFF263E2B), onPrimaryContainer = Color(0xFFEDF5ED),
    background = Color(0xFF131C19), onBackground = Color(0xFFEDF5ED),
    surface = Color(0xFF1D2924), onSurface = Color(0xFFEDF5ED),
    surfaceVariant = Color(0xFF263E2B), onSurfaceVariant = Color(0xFFA5B9AD),
    outline = Color(0xFF33483D), outlineVariant = Color(0xFF33483D),
    error = Color(0xFFF3C68E), errorContainer = Color(0xFF3E3021), onErrorContainer = Color(0xFFF3C68E)
)
var NO_PADDING_TEXT_STYLE = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))

@Composable
fun ComposeProjectTheme(darkTheme: Boolean = isSystemInDarkTheme(), dynamicColor: Boolean = false, fillScreen: Boolean = true, content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) SideEffect {
        (view.context as? Activity)?.window?.let { window ->
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }
    // Preserve the platform density and accessibility font scale.
    MaterialTheme(colorScheme = if (darkTheme) QuietDark else QuietLight, typography = Typography) {
        Surface(if (fillScreen) Modifier.fillMaxSize() else Modifier, color = MaterialTheme.colorScheme.background, content = content)
    }
}
