package com.yugentech.quill.ui.more.aboutAira.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.carousel.HorizontalMultiBrowseCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CapabilitiesCarousel() {
    val items = listOf(
        CapabilityItem(
            icon = Icons.Outlined.ChatBubbleOutline,
            title = "Ask Anything",
            description = "Chat naturally about the plot, world-building, or confusing events directly from the book.",
            slot = 0
        ),
        CapabilityItem(
            icon = Icons.Outlined.AutoStories,
            title = "Chapter Summaries",
            description = "Get instant, spoiler-free recaps of specific chapters to refresh your memory.",
            slot = 1
        ),
        CapabilityItem(
            icon = Icons.Outlined.PeopleOutline,
            title = "Character Tracking",
            description = "Quickly identify who is speaking, who a character is, or recall their backstory.",
            slot = 2
        ),
        CapabilityItem(
            icon = Icons.Outlined.Translate,
            title = "Reading Aid",
            description = "Simplify complex passages and define archaic or difficult words right in context.",
            slot = 0
        ),
        CapabilityItem(
            icon = Icons.Outlined.Lightbulb,
            title = "Uncover Themes",
            description = "Explore the deeper significance behind symbols, motives, and author intent.",
            slot = 1
        ),
        CapabilityItem(
            icon = Icons.Outlined.Lock,
            title = "Spoiler Protection",
            description = "Aira locks her knowledge to your exact reading progress to protect the story.",
            slot = 2
        )
    )

    val containerColors = listOf(
        MaterialTheme.colorScheme.primaryContainer,
        MaterialTheme.colorScheme.secondaryContainer,
        MaterialTheme.colorScheme.tertiaryContainer
    )
    val contentColors = listOf(
        MaterialTheme.colorScheme.onPrimaryContainer,
        MaterialTheme.colorScheme.onSecondaryContainer,
        MaterialTheme.colorScheme.onTertiaryContainer
    )

    HorizontalMultiBrowseCarousel(
        state = rememberCarouselState { items.size },
        preferredItemWidth = 256.dp,
        itemSpacing = 12.dp,
        contentPadding = PaddingValues(horizontal = 20.dp),
        modifier = Modifier.fillMaxWidth()
    ) { index ->
        val item = items[index]
        val bg = containerColors[item.slot]
        val fg = contentColors[item.slot]

        Card(
            modifier = Modifier
                .height(196.dp)
                .maskClip(MaterialTheme.shapes.extraLarge),
            colors = CardDefaults.cardColors(containerColor = bg)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = null,
                    tint = fg,
                    modifier = Modifier.size(30.dp)
                )
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = fg
                    )
                    Text(
                        text = item.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = fg.copy(alpha = 0.8f),
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}

data class CapabilityItem(
    val icon: ImageVector,
    val title: String,
    val description: String,
    val slot: Int
)