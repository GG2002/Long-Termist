package com.cyc.yearlymemoir.ui.theme

import android.app.Activity
import android.os.Build
import android.view.WindowInsets
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// 使用 Color.kt 中定义的颜色来构建你的备用深色主题
private val DarkColorScheme = darkColorScheme(
    primary = GreenAccent,
    secondary = GreenAccent, // 通常 secondary 可以用一个不同的强调色
    background = DarkBackground,
    surface = DarkCard,
    onPrimary = Color.Black, // 在 GreenAccent 上显示黑色文字
    onSecondary = Color.Black,
    onBackground = Color.White,
    onSurface = Color.White.copy(alpha = 0.87f), // 对于主要文字，使用更高的 alpha
    onSurfaceVariant = Color.White.copy(alpha = 0.6f) // 对于次要文字
)

// 定义一个备用的浅色主题，即使你不常用，也最好有一个
private val LightColorScheme = lightColorScheme(
    primary = GreenAccent,
    secondary = PurpleGrey40,
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
)

@Composable
fun YearlyMemoirTheme(
    darkTheme: Boolean = !isSystemInDarkTheme(),
    // 默认开启动态颜色，这是最佳实践
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        // 核心逻辑保持不变，它本身就是正确的
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    SideEffect {
        val window = (view.context as Activity).window

        val color = Color.Transparent.toArgb()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            window.decorView.setOnApplyWindowInsetsListener { view, insets ->
                val statusBarInsets = insets.getInsets(WindowInsets.Type.statusBars())
                view.setBackgroundColor(color)
                insets
            }
        } else {
            // For Android 14 and below
            window.statusBarColor = color
        }

        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}