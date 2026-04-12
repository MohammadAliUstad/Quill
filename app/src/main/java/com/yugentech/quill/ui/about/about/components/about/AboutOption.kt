package com.yugentech.quill.ui.about.about.components.about

import androidx.compose.ui.graphics.vector.ImageVector

data class AboutOption(
    val title: String,
    val subtitle: String?,
    val icon: ImageVector,
    val onClick: () -> Unit
)