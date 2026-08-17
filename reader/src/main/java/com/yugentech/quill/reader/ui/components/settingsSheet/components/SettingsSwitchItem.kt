package com.yugentech.quill.reader.ui.components.settingsSheet.components

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import com.yugentech.theme.service.HapticService
import com.yugentech.theme.tokens.spacing
import org.koin.compose.koinInject

@Composable
fun SettingsSwitchItem(
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    enabled: Boolean = true,
    index: Int,
    totalCount: Int,
    onCheckedChange: (Boolean) -> Unit,
    onClick: (() -> Unit)? = null
) {
    val haptic = koinInject<HapticService>()
    val view = LocalView.current
    val shape = itemShape(index, totalCount)

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
                onCheckedChange = {
                    haptic.performHaptic(view)
                    onCheckedChange(it)
                },
                enabled = enabled,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                    uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                    checkedIconColor = MaterialTheme.colorScheme.primaryContainer,
                    uncheckedIconColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                thumbContent = {
                    if (checked) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = null,
                            modifier = Modifier.size(SwitchDefaults.IconSize)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = null,
                            modifier = Modifier.size(SwitchDefaults.IconSize)
                        )
                    }
                }
            )
        },
        modifier = Modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .then(
                if (onClick != null) Modifier.clickable {
                    haptic.performHaptic(view)
                    onClick()
                } else Modifier
            ),
        colors = ListItemDefaults.colors(
            containerColor = Color.Transparent
        )
    )
}
