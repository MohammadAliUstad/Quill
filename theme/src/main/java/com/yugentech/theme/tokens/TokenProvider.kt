package com.yugentech.quill.theme.tokens

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import com.yugentech.quill.theme.tokens.dimensions.ComponentTokens
import com.yugentech.quill.theme.tokens.dimensions.CornerTokens
import com.yugentech.quill.theme.tokens.dimensions.ElevationTokens
import com.yugentech.quill.theme.tokens.dimensions.IconSizeTokens
import com.yugentech.quill.theme.tokens.dimensions.SpacingTokens
import com.yugentech.quill.theme.tokens.dimensions.StrokeTokens

// Creates a CompositionLocal to provide tokens down the UI tree, defaulting to Compact
val LocalDesignTokens = staticCompositionLocalOf { TokensCompact }

// Extension properties to allow easy access like `MaterialTheme.spacing.m`
val MaterialTheme.spacing: SpacingTokens
    @Composable
    @ReadOnlyComposable
    get() = LocalDesignTokens.current.spacing

val MaterialTheme.corners: CornerTokens
    @Composable
    @ReadOnlyComposable
    get() = LocalDesignTokens.current.corners

val MaterialTheme.icons: IconSizeTokens
    @Composable
    @ReadOnlyComposable
    get() = LocalDesignTokens.current.icons

val MaterialTheme.components: ComponentTokens
    @Composable
    @ReadOnlyComposable
    get() = LocalDesignTokens.current.components

val MaterialTheme.elevation: ElevationTokens
    @Composable
    @ReadOnlyComposable
    get() = LocalDesignTokens.current.elevation

val MaterialTheme.strokes: StrokeTokens
    @Composable
    @ReadOnlyComposable
    get() = LocalDesignTokens.current.strokeWidths