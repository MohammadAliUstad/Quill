package com.yugentech.quill.ui.about.aira.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.PeopleOutline
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CapabilitiesCarousel() {
    val items = listOf(
        CapabilityItem(Icons.Outlined.ChatBubbleOutline, "Ask Anything", "Chat naturally about the plot, world-building, or confusing events directly from the book.", 0),
        CapabilityItem(Icons.Outlined.AutoStories, "Chapter Summaries", "Get instant, spoiler-free recaps of specific chapters to refresh your memory.", 1),
        CapabilityItem(Icons.Outlined.PeopleOutline, "Character Tracking", "Quickly identify who is speaking, who a character is, or recall their backstory.", 2),
        CapabilityItem(Icons.Outlined.Translate, "Reading Aid", "Simplify complex passages and define archaic or difficult words right in context.", 0),
        CapabilityItem(Icons.Outlined.Lightbulb, "Uncover Themes", "Explore the deeper significance behind symbols, motives, and author intent.", 1),
        CapabilityItem(Icons.Outlined.Lock, "Spoiler Protection", "Aira locks her knowledge to your exact reading progress to protect the story.", 2)
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
    val iconShapes = listOf(
        MaterialShapes.Arch.toShape(),
        MaterialShapes.Clover4Leaf.toShape(),
        MaterialShapes.Cookie9Sided.toShape(),
        MaterialShapes.Bun.toShape(),
        MaterialShapes.Clover8Leaf.toShape(),
        MaterialShapes.Slanted.toShape()
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        items.forEachIndexed { index, item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Large typographic number — decorative, very low alpha
                Text(
                    text = if (index < 9) "0${index + 1}" else "${index + 1}",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    lineHeight = 32.sp,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.28f),
                    modifier = Modifier.width(52.dp)
                )

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Icon + title on the same line
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = iconShapes[index],
                            color = containerColors[item.slot],
                            modifier = Modifier.size(34.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = null,
                                    tint = contentColors[item.slot],
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                        }

                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Text(
                        text = item.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )
                }
            }

            if (index < items.size - 1) {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                )
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
