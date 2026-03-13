package com.jchat.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.jchat.presentation.settings.ThemeOption

private val JChatLightColors = lightColorScheme(
    primary = Color(0xFF0F6A5E),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFDCF8C6),
    onPrimaryContainer = Color(0xFF0E2D1E),
    secondary = Color(0xFF128C7E),
    onSecondary = Color(0xFFFFFFFF),
    tertiary = Color(0xFF25D366),
    onTertiary = Color(0xFF07331C),
    background = Color(0xFFF0F4F6),
    onBackground = Color(0xFF101C20),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF101C20),
    surfaceVariant = Color(0xFFE3EAED),
    onSurfaceVariant = Color(0xFF4D6068),
    outline = Color(0xFF7A8E97),
    error = Color(0xFFD32F2F),
)

private val JChatDarkColors = darkColorScheme(
    primary = Color(0xFF4BC2A7),
    onPrimary = Color(0xFF002019),
    primaryContainer = Color(0xFF0E3B32),
    onPrimaryContainer = Color(0xFFB9F3E5),
    secondary = Color(0xFF80D2C7),
    onSecondary = Color(0xFF003730),
    tertiary = Color(0xFF7CE3A0),
    onTertiary = Color(0xFF00391C),
    background = Color(0xFF101A1D),
    onBackground = Color(0xFFDCE5E8),
    surface = Color(0xFF182427),
    onSurface = Color(0xFFDCE5E8),
    surfaceVariant = Color(0xFF2A3A3F),
    onSurfaceVariant = Color(0xFFB2C6CD),
    outline = Color(0xFF8EA3AA),
    error = Color(0xFFFFB4AB),
)

private val JChatTypography = Typography(
    headlineLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 34.sp,
        lineHeight = 40.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 22.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
    ),
)

@Composable
fun JChatTheme(
    themeOption: ThemeOption = ThemeOption.System,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (themeOption) {
        ThemeOption.System -> isSystemInDarkTheme()
        ThemeOption.Dark -> true
        ThemeOption.Light -> false
    }
    MaterialTheme(
        colorScheme = if (darkTheme) JChatDarkColors else JChatLightColors,
        typography = JChatTypography,
        content = content,
    )
}
