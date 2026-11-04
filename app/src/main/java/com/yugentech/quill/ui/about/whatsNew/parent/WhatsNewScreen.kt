package com.yugentech.quill.ui.about.whatsNew.parent

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhatsNewScreen(
    onNavigateBack: () -> Unit
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val layoutDirection = LocalLayoutDirection.current
    val versionName = "Current Version ${com.yugentech.quill.BuildConfig.VERSION_NAME}"

    val v41Updates = listOf(
        UpdateItem(
            "A Redesigned Sources Tab",
            "Sources has a fresh new look, with a friendlier way to import books from your device, cover previews from each catalogue, and a count of how many of their books are already in your library.",
            Icons.Default.Explore
        ),
        UpdateItem(
            "A New Profile Card",
            "Your profile card in More is more compact, with a bigger avatar. Tap the avatar to pick a new one from a sheet, or the pencil to change your name, without leaving the page.",
            Icons.Default.AccountCircle
        ),
        UpdateItem(
            "Smoother Aira Replies",
            "Aira's answers now flow in a few words at a time, and the conversation glides up gently as they appear, both in the chat and in the quick panel while you read.",
            Icons.Default.AutoAwesome
        ),
        UpdateItem(
            "Faster Covers",
            "Standard Ebooks covers now load as lighter thumbnails, so Discover, your Library, and book pages fill in much quicker.",
            Icons.Default.Image
        ),
        UpdateItem(
            "Gentler Reminders",
            "Your daily reading reminder now shows when it rings, like \"Every evening at 7:00 PM\". Playful reminders are quieter and never more than one a day, with a few kind words if you've been away for a while.",
            Icons.Default.NotificationsActive
        ),
        UpdateItem(
            "Polish & Fixes",
            "Smoother, consistent fades behind the top bars across the app, Aira's quick panel now resizes back to its full height, and the sign-in spinner shows only on the button you tapped.",
            Icons.Default.Palette
        )
    )

    val v4Updates = listOf(
        UpdateItem(
            "Meet the New Aira",
            "Aira has been rebuilt from the ground up. She knows whether you're asking about your book or anything else, answers straight from the pages you've read, never spoils what's ahead, and can read her answers aloud.",
            Icons.Default.AutoAwesome
        ),
        UpdateItem(
            "Ask Aira From the Page",
            "Select any word or passage while reading and Aira is right there. Define words, explain tricky lines, or find out why a moment matters, without leaving the book.",
            Icons.Default.TouchApp
        ),
        UpdateItem(
            "Character Companion",
            "Select a character's name to recall who they are, see what they're up to lately, or trace their whole journey so far. Always spoiler-free.",
            Icons.Default.Face
        ),
        UpdateItem(
            "Visualize Scenes",
            "Turn any passage into a painted illustration. Your images are kept in a Visuals gallery, and tapping one takes you right back to the passage it came from.",
            Icons.Default.Brush
        ),
        UpdateItem(
            "Highlights",
            "A smoother way to select text, plus highlights you can revisit from one place and jump straight back to in the book.",
            Icons.Default.Highlight
        ),
        UpdateItem(
            "A Reimagined Reader",
            "A refreshed reading screen with new reading settings, a cleaner table of contents, a warm night light, and page turning with your volume keys.",
            Icons.Default.AutoStories
        ),
        UpdateItem(
            "Ambient Sounds",
            "Set the mood with six soundscapes, from gentle rain to a crackling fireplace. Preview them before you play, or have them start automatically when you open a book.",
            Icons.Default.MusicNote
        ),
        UpdateItem(
            "Reading Reminders",
            "Pick a time for a daily reading reminder, or let playful nudges help you keep your streak going.",
            Icons.Default.NotificationsActive
        ),
        UpdateItem(
            "Discover & Organize",
            "A redesigned Discover tab, better Project Gutenberg and Standard Ebooks browsing, a quick way to add your own books, and categories you can reorder by dragging.",
            Icons.Default.Explore
        ),
        UpdateItem(
            "Reading Insights",
            "See your reading time, streak, peak reading hours, favorite authors, and how you read with Aira, all on a refreshed Insights screen.",
            Icons.Default.Insights
        ),
        UpdateItem(
            "A Fresh Look",
            "A brand new app icon and splash screen, the new Harbor color theme, and a polished feel across the whole app.",
            Icons.Default.Palette
        )
    )

    var v4Expanded by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Column {
                        Text("What's New")
                        Text(
                            text = versionName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
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
            itemsIndexed(v41Updates) { index, item ->
                UpdateCard(item, index, v41Updates.size)
            }

            // 4.0.0 collapsible section
            item {
                VersionSectionHeader(
                    version = "4.0.0",
                    isLatest = false,
                    expanded = v4Expanded,
                    onClick = { v4Expanded = !v4Expanded }
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
                text = "Version $version",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (isLatest) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (onClick != null) {
            Icon(
                imageVector = Icons.Default.ExpandMore,
                contentDescription = if (expanded) "Collapse" else "Expand",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.graphicsLayer { rotationZ = chevronRotation }
            )
        }
    }
}

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
            Icon(
                imageVector = item.icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
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
