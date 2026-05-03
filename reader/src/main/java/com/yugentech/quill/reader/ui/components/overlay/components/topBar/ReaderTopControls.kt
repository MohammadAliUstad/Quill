package com.yugentech.quill.reader.ui.components.overlay.components.topBar

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow

import com.yugentech.theme.service.HapticService
import org.koin.compose.koinInject

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
    val haptic = koinInject<HapticService>()

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
                            haptic.performHaptic()
                            onBackClick()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            haptic.performHaptic()
                            onSoundClick()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = "Ambient Sounds"
                        )
                    }

                    IconButton(
                        onClick = {
                            haptic.performHaptic()
                            onTocClick()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "TOC"
                        )
                    }

                    IconButton(
                        onClick = {
                            haptic.performHaptic()
                            onSettingsClick()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.FormatSize,
                            contentDescription = "Settings"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    }
}