package com.yugentech.theme

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalHapticFeedback
import com.yugentech.theme.builder.getColorScheme
import com.yugentech.theme.builder.getFontFamily
import com.yugentech.theme.builder.getTypography
import com.yugentech.theme.models.ThemeConfiguration
import com.yugentech.theme.tokens.LocalDesignTokens
import com.yugentech.theme.tokens.TokensCompact

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun QuillTheme(
    themeConfiguration: ThemeConfiguration,
    hapticsEnabled: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = getColorScheme(themeConfiguration = themeConfiguration)

    val currentTypography = remember(themeConfiguration.appFont) {
        getTypography(getFontFamily(themeConfiguration.appFont))
    }

    val tokens = TokensCompact

    // Provide the tokens, haptics and theme data to the entire UI tree
    CompositionLocalProvider(
        LocalDesignTokens provides tokens,
        LocalHapticFeedback provides rememberGatedHaptics(hapticsEnabled)
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = currentTypography,
            content = content,
            motionScheme = MotionScheme.expressive()
        )
    }
}
