package com.yugentech.quill.ui.book.bookDetails.components

import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.yugentech.quill.ui.common.components.rememberSmoothScrimBrush
import com.yugentech.quill.R
import androidx.compose.ui.res.stringResource

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun BookDetailsTopBar(
    bookTitle: String,
    bookAuthor: String,
    isVisible: Boolean,
    isFavorite: Boolean,
    isDownloaded: Boolean = true,
    hasProgress: Boolean = false,
    onBackClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onHighlightsClick: () -> Unit,
    // Null hides Visuals (Aira's images), for a guest
    onVisualsClick: (() -> Unit)?,
    onResetProgressClick: () -> Unit,
    onDeleteClick: () -> Unit,
    scrollBehavior: TopAppBarScrollBehavior
) {
    val haptic = LocalHapticFeedback.current
    var menuExpanded by remember { mutableStateOf(false) }
    val surfaceColor = MaterialTheme.colorScheme.surface

    Box(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer {
                    alpha = (-scrollBehavior.state.contentOffset / 100f).coerceIn(0f, 1f)
                }
                .background(rememberSmoothScrimBrush(surfaceColor))
        )

        TopAppBar(
            title = {
                AnimatedVisibility(
                    visible = isVisible,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Column {
                        Text(
                            text = bookTitle,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = bookAuthor,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            },
            navigationIcon = {
                IconButton(onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.GestureEnd)
                    onBackClick()
                }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back)
                    )
                }
            },
            actions = {
                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(
                            if (isFavorite) HapticFeedbackType.ToggleOff else HapticFeedbackType.ToggleOn
                        )
                        onFavoriteClick()
                    },
                    modifier = Modifier.size(44.dp)
                ) {
                    AnimatedHeartIcon(
                        isLiked = isFavorite,
                        modifier = Modifier.size(52.dp)
                    )
                }

                AnimatedVisibility(
                    visible = isDownloaded,
                    enter = fadeIn() + expandHorizontally(expandFrom = Alignment.Start) + slideInHorizontally(initialOffsetX = { it }),
                    exit = fadeOut() + shrinkHorizontally(shrinkTowards = Alignment.Start) + slideOutHorizontally(targetOffsetX = { it })
                ) {
                    Box {
                        IconButton(onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.GestureEnd)
                            menuExpanded = true
                        }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = stringResource(R.string.more_options),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false },
                            shape = RoundedCornerShape(24.dp),
                            containerColor = MaterialTheme.colorScheme.surfaceContainer,
                            tonalElevation = 8.dp,
                            shadowElevation = 8.dp
                        ) {

                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = stringResource(R.string.highlights),
                                        fontWeight = FontWeight.Medium
                                    )
                                },
                                leadingIcon = {
                                    Icon(Icons.AutoMirrored.Filled.Notes, contentDescription = stringResource(R.string.highlights))
                                },
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.GestureEnd)
                                    menuExpanded = false
                                    onHighlightsClick()
                                },
                                modifier = Modifier
                                    .padding(horizontal = 8.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                            )

                            if (onVisualsClick != null) {
                                Spacer(modifier = Modifier.height(4.dp))

                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = stringResource(R.string.visuals),
                                            fontWeight = FontWeight.Medium
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Default.Image, contentDescription = stringResource(R.string.visuals))
                                    },
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.GestureEnd)
                                        menuExpanded = false
                                        onVisualsClick()
                                    },
                                    modifier = Modifier
                                        .padding(horizontal = 8.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                                )
                            }

                            if (hasProgress) {
                                Spacer(modifier = Modifier.height(4.dp))

                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = stringResource(R.string.reset_progress),
                                            fontWeight = FontWeight.Medium
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Default.Restore, contentDescription = stringResource(R.string.reset_progress))
                                    },
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.GestureEnd)
                                        menuExpanded = false
                                        onResetProgressClick()
                                    },
                                    modifier = Modifier
                                        .padding(horizontal = 8.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = stringResource(R.string.delete_book),
                                        color = MaterialTheme.colorScheme.error,
                                        fontWeight = FontWeight.Medium
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = stringResource(R.string.delete_book),
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                },
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.GestureEnd)
                                    menuExpanded = false
                                    onDeleteClick()
                                },
                                modifier = Modifier
                                    .padding(horizontal = 8.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f))
                            )
                        }
                    }
                }
            },
            scrollBehavior = scrollBehavior,
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
                scrolledContainerColor = Color.Transparent
            )
        )
    }
}