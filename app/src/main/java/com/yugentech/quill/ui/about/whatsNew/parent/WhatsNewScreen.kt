package com.yugentech.quill.ui.about.whatsNew.parent

import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.NoAccounts
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Highlight
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.toShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yugentech.quill.ui.main.components.itemShape
import com.yugentech.theme.tokens.corners
import com.yugentech.theme.tokens.dimensions.AppAnimations
import com.yugentech.theme.tokens.spacing
import com.yugentech.quill.R
import androidx.compose.ui.res.stringResource
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.WavingHand
import androidx.compose.material.icons.filled.HourglassTop

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhatsNewScreen(
    onNavigateBack: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val layoutDirection = LocalLayoutDirection.current
    val versionName = stringResource(R.string.current_version, com.yugentech.quill.BuildConfig.VERSION_NAME)

    // 5.0.0: the release this build is, shown first without a header (the top bar names it)
    val v5Updates = listOf(
        UpdateItem(
            stringResource(R.string.wn_5_guest_title),
            stringResource(R.string.wn_5_guest_body),
            Icons.Default.NoAccounts
        ),
        UpdateItem(
            stringResource(R.string.wn_5_backup_title),
            stringResource(R.string.wn_5_backup_body),
            Icons.Default.Backup
        ),
        UpdateItem(
            stringResource(R.string.wn_5_languages_title),
            stringResource(R.string.wn_5_languages_body),
            Icons.Default.Translate
        ),
        UpdateItem(
            stringResource(R.string.wn_5_welcome_title),
            stringResource(R.string.wn_5_welcome_body),
            Icons.Default.WavingHand
        ),
        UpdateItem(
            stringResource(R.string.wn_5_appearance_title),
            stringResource(R.string.wn_5_appearance_body),
            Icons.Default.Brush
        ),
        UpdateItem(
            stringResource(R.string.wn_5_loading_title),
            stringResource(R.string.wn_5_loading_body),
            Icons.Default.HourglassTop
        )
    )

    // 4.5.0, the previous release
    val v45Updates = listOf(
        UpdateItem(
            stringResource(R.string.wn_41_sources_title),
            stringResource(R.string.wn_41_sources_body),
            Icons.Default.Explore
        ),
        UpdateItem(
            stringResource(R.string.wn_41_profile_title),
            stringResource(R.string.wn_41_profile_body),
            Icons.Default.AccountCircle
        ),
        UpdateItem(
            stringResource(R.string.wn_41_replies_title),
            stringResource(R.string.wn_41_replies_body),
            Icons.Default.AutoAwesome
        ),
        UpdateItem(
            stringResource(R.string.wn_41_covers_title),
            stringResource(R.string.wn_41_covers_body),
            Icons.Default.Image
        ),
        UpdateItem(
            stringResource(R.string.wn_41_reminders_title),
            stringResource(R.string.wn_41_reminders_body),
            Icons.Default.NotificationsActive
        ),
        UpdateItem(
            stringResource(R.string.wn_41_polish_title),
            stringResource(R.string.wn_41_polish_body),
            Icons.Default.Palette
        )
    )

    val v4Updates = listOf(
        UpdateItem(
            stringResource(R.string.wn_4_aira_title),
            stringResource(R.string.wn_4_aira_body),
            Icons.Default.AutoAwesome
        ),
        UpdateItem(
            stringResource(R.string.wn_4_page_title),
            stringResource(R.string.wn_4_page_body),
            Icons.Default.TouchApp
        ),
        UpdateItem(
            stringResource(R.string.wn_4_character_title),
            stringResource(R.string.wn_4_character_body),
            Icons.Default.Face
        ),
        UpdateItem(
            stringResource(R.string.feature_visualize_title),
            stringResource(R.string.wn_4_visualize_body),
            Icons.Default.Brush
        ),
        UpdateItem(
            stringResource(R.string.highlights),
            stringResource(R.string.wn_4_highlights_body),
            Icons.Default.Highlight
        ),
        UpdateItem(
            stringResource(R.string.wn_4_reader_title),
            stringResource(R.string.wn_4_reader_body),
            Icons.Default.AutoStories
        ),
        UpdateItem(
            stringResource(R.string.wn_4_sounds_title),
            stringResource(R.string.wn_4_sounds_body),
            Icons.Default.MusicNote
        ),
        UpdateItem(
            stringResource(R.string.wn_4_reminders_title),
            stringResource(R.string.wn_4_reminders_body),
            Icons.Default.NotificationsActive
        ),
        UpdateItem(
            stringResource(R.string.wn_4_discover_title),
            stringResource(R.string.wn_4_discover_body),
            Icons.Default.Explore
        ),
        UpdateItem(
            stringResource(R.string.wn_4_insights_title),
            stringResource(R.string.wn_4_insights_body),
            Icons.Default.Insights
        ),
        UpdateItem(
            stringResource(R.string.wn_4_look_title),
            stringResource(R.string.wn_4_look_body),
            Icons.Default.Palette
        )
    )

    var v45Expanded by rememberSaveable { mutableStateOf(false) }
    var v4Expanded by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.more_whats_new))
                        Text(
                            text = versionName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.GestureEnd)
                        onNavigateBack()
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { scaffoldPadding ->
        val navBarPadding = WindowInsets.navigationBars.asPaddingValues()

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = scaffoldPadding.calculateTopPadding()),
            contentPadding = PaddingValues(
                bottom = navBarPadding.calculateBottomPadding(),
                start = MaterialTheme.spacing.m + scaffoldPadding.calculateStartPadding(layoutDirection),
                end = MaterialTheme.spacing.m + scaffoldPadding.calculateEndPadding(layoutDirection)
            ),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xxs)
        ) {
            itemsIndexed(v5Updates) { index, item ->
                UpdateCard(item, index, v5Updates.size)
            }

            // 4.5.0 collapsible section
            item {
                VersionSectionHeader(
                    version = "4.5.0",
                    isLatest = false,
                    expanded = v45Expanded,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.GestureEnd)
                        v45Expanded = !v45Expanded
                    }
                )
            }

            item(key = "v45_collapsible_content") {
                AnimatedVisibility(
                    visible = v45Expanded,
                    enter = fadeIn(tween(AppAnimations.Durations.Standard)) + expandVertically(tween(AppAnimations.Durations.Standard)),
                    exit = fadeOut(tween(AppAnimations.Durations.Standard)) + shrinkVertically(tween(AppAnimations.Durations.Standard))
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xxs)
                    ) {
                        v45Updates.forEachIndexed { index, item ->
                            UpdateCard(item, index, v45Updates.size)
                        }
                    }
                }
            }

            // 4.0.0 collapsible section
            item {
                VersionSectionHeader(
                    version = "4.0.0",
                    isLatest = false,
                    expanded = v4Expanded,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.GestureEnd)
                        v4Expanded = !v4Expanded
                    }
                )
            }

            item(key = "v4_collapsible_content") {
                AnimatedVisibility(
                    visible = v4Expanded,
                    enter = fadeIn(tween(AppAnimations.Durations.Standard)) + expandVertically(tween(AppAnimations.Durations.Standard)),
                    exit = fadeOut(tween(AppAnimations.Durations.Standard)) + shrinkVertically(tween(AppAnimations.Durations.Standard))
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xxs)
                    ) {
                        v4Updates.forEachIndexed { index, item ->
                            UpdateCard(item, index, v4Updates.size)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VersionSectionHeader(
    version: String,
    isLatest: Boolean,
    expanded: Boolean = true,
    onClick: (() -> Unit)? = null
) {
    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        label = "chevron"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(MaterialTheme.corners.medium))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(
                horizontal = if (onClick != null) MaterialTheme.spacing.s else 0.dp,
                vertical = MaterialTheme.spacing.m
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = stringResource(R.string.more_version, version),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (isLatest) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (onClick != null) {
            Icon(
                imageVector = Icons.Default.ExpandMore,
                contentDescription = if (expanded) stringResource(R.string.collapse) else stringResource(R.string.expand),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.graphicsLayer { rotationZ = chevronRotation }
            )
        }
    }
}

// A few expressive shapes, cycled through so the list doesn't look like a column of dots.
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private val updateIconShapes = listOf(
    MaterialShapes.Cookie9Sided,
    MaterialShapes.Clover4Leaf,
    MaterialShapes.Sunny,
    MaterialShapes.Pill
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun UpdateCard(item: UpdateItem, index: Int, totalCount: Int) {
    val shape = itemShape(index, totalCount)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Row(
            modifier = Modifier
                .padding(MaterialTheme.spacing.m)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.m)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(44.dp)
                    .clip(updateIconShapes[index % updateIconShapes.size].toShape())
                    .background(MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(22.dp)
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = item.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private data class UpdateItem(
    val title: String,
    val description: String,
    val icon: ImageVector
)
