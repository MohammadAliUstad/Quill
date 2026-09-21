package com.yugentech.quill.ui.about.whatsNew.parent

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Highlight
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yugentech.quill.R
import com.yugentech.quill.ui.main.components.itemShape
import com.yugentech.theme.tokens.spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhatsNewScreen(
    onNavigateBack: () -> Unit
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val layoutDirection = LocalLayoutDirection.current
    val versionName = "Version ${com.yugentech.quill.BuildConfig.VERSION_NAME}"

    val updates = listOf(
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
            items(updates.size) { index ->
                UpdateCard(updates[index], index, updates.size)
            }
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
