package com.yugentech.quill.ui.about.moreApps.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.carousel.HorizontalMultiBrowseCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yugentech.quill.R
import com.yugentech.quill.ui.common.components.CapabilityItem

/** An app's "Key features" title over its carousel of feature cards. */
@Composable
fun FeaturesSection(items: List<CapabilityItem>, colorOffset: Int = 0) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = stringResource(R.string.key_features),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 20.dp)
        )
        CapabilitiesCarousel(items = items, colorOffset = colorOffset)
    }
}

/**
 * [colorOffset] shifts where the primary / secondary / tertiary cycle starts, so carousels on the
 * same screen don't repeat the same colour order.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CapabilitiesCarousel(items: List<CapabilityItem>, colorOffset: Int) {
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
        val colorIndex = (item.slot + colorOffset) % containerColors.size
        val bg = containerColors[colorIndex]
        val fg = contentColors[colorIndex]

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
