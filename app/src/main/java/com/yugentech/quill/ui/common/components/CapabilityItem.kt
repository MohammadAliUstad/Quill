package com.yugentech.quill.ui.common.components

import androidx.compose.ui.graphics.vector.ImageVector

/** One feature card on the About Aira and More apps screens. [slot] picks its colour (primary, secondary, tertiary). */
data class CapabilityItem(
    val icon: ImageVector,
    val title: String,
    val description: String,
    val slot: Int
)
