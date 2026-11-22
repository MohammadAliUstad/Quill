package com.yugentech.quill.reader.ui.components.overlay.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.yugentech.quill.reader.R
import androidx.compose.ui.res.stringResource


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderTopBar(
    isVisible: Boolean,
    bookTitle: String,
    onBackClick: () -> Unit,
    onTocClick: () -> Unit,
    onSoundClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current

    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(
            initialOffsetY = { -it },
            animationSpec = tween(300, easing = FastOutSlowInEasing)
        ),
        exit = slideOutVertically(
            targetOffsetY = { -it },
            animationSpec = tween(250, easing = FastOutSlowInEasing)
        )
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerLow
        ) {
            TopAppBar(
                title = {
                    Text(
                        text = bookTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.GestureEnd)
                            onBackClick()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.reader_back)
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.GestureEnd)
                            onTocClick()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = stringResource(R.string.reader_toc)
                        )
                    }

                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.GestureEnd)
                            onSoundClick()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = stringResource(R.string.reader_ambient_sounds)
                        )
                    }

                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.GestureEnd)
                            onSettingsClick()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.FormatSize,
                            contentDescription = stringResource(R.string.reader_settings)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    }
}
