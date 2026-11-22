package com.yugentech.quill.ui.tabs.settingsScreen.components

import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import com.yugentech.theme.tokens.spacing
import com.yugentech.theme.shapes.itemShape

@Composable
fun SettingsSwitchItem(
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    enabled: Boolean = true,
    checkedIcon: ImageVector? = null,
    index: Int,
    totalCount: Int,
    onCheckedChange: (Boolean) -> Unit,
    onClick: (() -> Unit)? = null
) {
    val haptic = LocalHapticFeedback.current
    val shape = itemShape(index, totalCount)
    // Disabled, the whole row fades back rather than the switch going grey
    val contentAlpha by animateFloatAsState(
        targetValue = if (enabled) 1f else 0.45f,
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
        label = "settingsSwitchContentAlpha"
    )

    ListItem(
        headlineContent = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(
                    start = MaterialTheme.spacing.s,
                    top = MaterialTheme.spacing.s
                )
            )
        },
        supportingContent = subtitle?.let {
            {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(
                        start = MaterialTheme.spacing.s,
                        bottom = MaterialTheme.spacing.s
                    )
                )
            }
        },
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                enabled = enabled,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                    uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                    checkedIconColor = MaterialTheme.colorScheme.primaryContainer,
                    uncheckedIconColor = MaterialTheme.colorScheme.surfaceVariant,
                    disabledCheckedThumbColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    disabledCheckedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                    disabledUncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    disabledUncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                    disabledCheckedIconColor = MaterialTheme.colorScheme.primaryContainer,
                    disabledUncheckedIconColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                thumbContent = {
                    Icon(
                        imageVector = if (checked) checkedIcon ?: Icons.Filled.Check else Icons.Filled.Close,
                        contentDescription = null,
                        modifier = Modifier.size(SwitchDefaults.IconSize)
                    )
                }
            )
        },
        modifier = Modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .graphicsLayer { alpha = contentAlpha }
            .then(if ((onClick != null) && enabled) Modifier.clickable {
                haptic.performHapticFeedback(HapticFeedbackType.GestureEnd)
                onClick()
            } else Modifier),
        colors = ListItemDefaults.colors(
            containerColor = Color.Transparent
        )
    )
}