package com.yugentech.quill.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookDetailsScreen(
    onBackClick: () -> Unit,
    onReadClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {}, // Empty title for a cleaner look
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { /* Dummy Favorite */ }) {
                        Icon(Icons.Default.FavoriteBorder, contentDescription = "Favorite")
                    }
                    IconButton(onClick = { /* Dummy Share */ }) {
                        Icon(Icons.Default.Share, contentDescription = "Share")
                    }
                    IconButton(onClick = { /* Dummy Menu */ }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Options")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 32.dp)
        ) {
            // 1. Header Section (Cover + Basic Info)
            item {
                BookHeaderSection(onReadClick = onReadClick)
            }

            // 2. Stats Row
            item {
                BookStatsRow()
                HorizontalDivider(modifier = Modifier.padding(vertical = 24.dp))
            }

            // 3. Synopsis
            item {
                Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                    Text(
                        text = "Synopsis",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "The story follows the lives of the wealthy Jay Gatsby and his neighbor Nick Carraway, who recounts his encounter with Gatsby at the height of the Roaring Twenties. It explores themes of decadence, idealism, resistance to change, social upheaval, and excess.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = MaterialTheme.typography.bodyLarge.lineHeight
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            // 4. Chapters List Header
            item {
                Text(
                    text = "Table of Contents",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                )
            }

            // 5. Chapters Items
            items(12) { index ->
                ChapterItem(
                    index = index + 1,
                    title = getDummyChapterTitle(index),
                    onChapterClick = onReadClick
                )
            }
        }
    }
}

@Composable
fun BookHeaderSection(
    onReadClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Big Cover
        Card(
            modifier = Modifier
                .width(160.dp)
                .height(240.dp),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF37474F))
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Gatsby",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White.copy(alpha = 0.5f),
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Title & Author
        Text(
            text = "The Great Gatsby",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth(),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Text(
            text = "F. Scott Fitzgerald",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.fillMaxWidth(),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Button(
                onClick = onReadClick,
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Start Reading")
            }

            Spacer(modifier = Modifier.width(12.dp))

            FilledTonalButton(
                onClick = { /* Dummy Mark */ },
                modifier = Modifier.height(50.dp)
            ) {
                Icon(Icons.Default.BookmarkBorder, contentDescription = null)
            }
        }
    }
}

@Composable
fun BookStatsRow() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        BookStatItem(icon = Icons.Default.AccessTime, label = "3h 20m", subLabel = "Reading Time")
        BookStatItem(icon = Icons.AutoMirrored.Filled.MenuBook, label = "218", subLabel = "Pages")
        BookStatItem(icon = Icons.Default.Language, label = "English", subLabel = "Language")
    }
}

@Composable
fun BookStatItem(icon: ImageVector, label: String, subLabel: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = subLabel,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun ChapterItem(
    index: Int,
    title: String,
    onChapterClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onChapterClick)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Chapter Number Circle
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = index.toString(),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Title
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            // Optional: Subtitle for progress or page count
            Text(
                text = "${(index * 15) + 2} pages",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        // Progress (optional indicator if read)
        if (index < 3) {
             Icon(
                 imageVector = Icons.Default.AccessTime, // Just a placeholder for "In progress" or "Done"
                 contentDescription = null,
                 modifier = Modifier.size(16.dp),
                 tint = MaterialTheme.colorScheme.primary
             )
        }
    }
}

fun getDummyChapterTitle(index: Int): String {
    val titles = listOf(
        "Down the Rabbit-Hole",
        "The Pool of Tears",
        "A Caucus-Race and a Long Tale",
        "The Rabbit Sends in a Little Bill",
        "Advice from a Caterpillar",
        "Pig and Pepper",
        "A Mad Tea-Party",
        "The Queen's Croquet-Ground",
        "The Mock Turtle's Story",
        "The Lobster Quadrille",
        "Who Stole the Tarts?",
        "Alice's Evidence"
    )
    return titles.getOrElse(index) { "Chapter ${index + 1}" }
}