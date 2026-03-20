package com.yugentech.quill.reader.ui.components.aira.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.yugentech.quill.reader.R

@Composable
fun AiraPeekAvatar(
    modifier: Modifier = Modifier,
    isLoading: Boolean,
    isStreaming: Boolean,
    isVisuallyTyping: Boolean = false
) {
    val avatarRes = when {
        isStreaming || isVisuallyTyping -> R.drawable.pexplaining
        isLoading -> R.drawable.psmile_calm
        else -> R.drawable.pneutral
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(48.dp)
            .clip(CircleShape)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
        ) {}

        Image(
            painter = painterResource(id = avatarRes),
            contentDescription = "Aira",
            modifier = Modifier
                .requiredSize(120.dp)
                .offset(
                    x = (-3).dp,
                    y = 26.dp
                )
        )
    }
}