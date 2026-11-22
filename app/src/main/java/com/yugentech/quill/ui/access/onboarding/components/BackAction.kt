package com.yugentech.quill.ui.access.onboarding.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.yugentech.quill.R

/** The round back button left of the main button, tinted with the theme's primary. */
@Composable
fun BackAction(onClick: () -> Unit) {
    val primary = MaterialTheme.colorScheme.primary
    Box(
        modifier = Modifier
            .size(58.dp)
            .clip(CircleShape)
            .background(primary.copy(alpha = 0.12f))
            .border(1.dp, primary.copy(alpha = 0.3f), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = stringResource(R.string.back),
            modifier = Modifier.size(22.dp),
            tint = primary
        )
    }
}
