package com.yugentech.quill.ui.sources.standard.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.HistoryEdu
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

data class CategorySuggestion(
    val name: String,
    val icon: ImageVector
)

@Composable
fun SearchSuggestions(
    onSuggestionClick: (String) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val categories = listOf(
            CategorySuggestion("Science Fiction", Icons.Default.RocketLaunch),
            CategorySuggestion("Mystery", Icons.Default.Search),
            CategorySuggestion("Philosophy", Icons.Default.Lightbulb),
            CategorySuggestion("Fantasy", Icons.Default.AutoAwesome),
            CategorySuggestion("Romance", Icons.Default.Favorite),
            CategorySuggestion("Horror", Icons.Default.Nightlight),
            CategorySuggestion("Adventure", Icons.Default.Explore),
            CategorySuggestion("Poetry", Icons.Default.HistoryEdu)
        )

        items(categories) { category ->
            CategorySuggestionItem(
                category = category,
                onClick = { onSuggestionClick(category.name) }
            )
        }
    }
}

@Composable
fun CategorySuggestionItem(
    category: CategorySuggestion,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = category.icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.width(16.dp))

        Text(
            text = category.name,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}