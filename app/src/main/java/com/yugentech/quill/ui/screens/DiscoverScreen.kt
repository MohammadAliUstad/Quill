package com.yugentech.quill.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun DiscoverScreen(
    onBookClick: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 80.dp), // Space for Bottom Nav
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // 1. Hero Banner (Featured Book)
        item {
            FeaturedHeroSection(onBookClick = onBookClick)
        }

        // 2. Categories (Netflix-style Rows)
        item {
            CategoryRow(
                title = "Trending Now",
                onBookClick = onBookClick
            )
        }

        item {
            CategoryRow(
                title = "New Releases from Standard Ebooks",
                onBookClick = onBookClick
            )
        }

        item {
            CategoryRow(
                title = "Philosophy & Thought",
                onBookClick = onBookClick
            )
        }
        
        item {
            CategoryRow(
                title = "Sci-Fi Classics",
                onBookClick = onBookClick
            )
        }
    }
}

@Composable
fun FeaturedHeroSection(
    onBookClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp) // Tall, immersive header
            .clickable(onClick = onBookClick)
    ) {
        // Background Gradient/Image Placeholder
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
        )

        // Content
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            // Featured Cover
            Card(
                modifier = Modifier
                    .width(100.dp)
                    .aspectRatio(0.66f),
                shape = RoundedCornerShape(8.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E88E5))
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text("Featured", color = Color.White.copy(alpha = 0.8f))
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Featured Text Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "FEATURED",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = "The Picture of Dorian Gray",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = "Oscar Wilde",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = onBookClick,
                    contentPadding = PaddingValues(horizontal = 24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Icon(
                        Icons.Default.PlayArrow, 
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Read Now")
                }
            }
        }
    }
}

@Composable
fun CategoryRow(
    title: String,
    onBookClick: () -> Unit
) {
    Column {
        // Section Title
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        // Horizontal Scroll List
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(10) { index ->
                DiscoverBookItem(index = index, onClick = onBookClick)
            }
        }
    }
}

@Composable
fun DiscoverBookItem(
    index: Int,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(120.dp)
            .clickable(onClick = onClick)
    ) {
        // Cover
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.66f),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(
                containerColor = getCategoryDummyColor(index)
            )
        ) {
            Box(Modifier.fillMaxSize()) // Placeholder image area
        }
        
        Spacer(modifier = Modifier.height(6.dp))
        
        // Title (No author to keep it clean like Netflix)
        Text(
            text = "Book Title $index",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

fun getCategoryDummyColor(index: Int): Color {
    // A slightly different palette for the store
    val colors = listOf(
        Color(0xFF00695C), Color(0xFFAD1457), Color(0xFF283593),
        Color(0xFFF9A825), Color(0xFF4E342E), Color(0xFF37474F)
    )
    return colors[index % colors.size]
}