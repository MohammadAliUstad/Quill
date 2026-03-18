package com.yugentech.quill.ui.tabs.sourcesScreen.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yugentech.quill.database.model.BookSource

@Composable
fun LargeCatalogCard(
    catalog: CatalogInfo,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = catalog.containerColor()),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(32.dp))
            .clickable(onClick = onClick),
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            Surface(
                shape = catalog.shape,
                color = catalog.contentColor().copy(alpha = 0.05f),
                modifier = Modifier
                    .size(280.dp)
                    .align(Alignment.TopEnd)
                    .offset(x = 60.dp, y = (-40).dp),
            ) {}

            Icon(
                imageVector = catalog.icon,
                contentDescription = null,
                tint = catalog.contentColor().copy(alpha = 0.08f),
                modifier = Modifier
                    .size(160.dp)
                    .align(Alignment.TopEnd)
                    .offset(x = 20.dp, y = 20.dp),
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                // --- TOP GROUP (Title & Subtitle) ---
                Column {
                    Surface(
                        shape = catalog.shape,
                        color = catalog.contentColor().copy(alpha = 0.15f),
                        modifier = Modifier.size(64.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = catalog.icon,
                                contentDescription = null,
                                tint = catalog.contentColor(),
                                modifier = Modifier.size(36.dp),
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = catalog.title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black,
                        color = catalog.contentColor(),
                    )

                    Text(
                        text = catalog.subtitle,
                        style = MaterialTheme.typography.titleSmall,
                        color = catalog.contentColor().copy(alpha = 0.8f),
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                // --- BOTTOM GROUP (Description & Button) ---
                Column {
                    Text(
                        text = catalog.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = catalog.contentColor().copy(alpha = 0.9f),
                        // Controls the tight gap right above the button
                        modifier = Modifier.padding(bottom = 16.dp),
                    )

                    FilledTonalButton(
                        onClick = onClick,
                        shape = CircleShape,
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = catalog.buttonContainerColor(),
                            contentColor = catalog.buttonContentColor(),
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = catalog.buttonText,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}

data class CatalogInfo(
    val source: BookSource,
    val title: String,
    val subtitle: String,
    val description: String,
    val icon: ImageVector,
    val shape: Shape,
    val containerColor: @Composable () -> Color,
    val contentColor: @Composable () -> Color,
    val buttonContainerColor: @Composable () -> Color,
    val buttonContentColor: @Composable () -> Color,
    val buttonText: String,
)