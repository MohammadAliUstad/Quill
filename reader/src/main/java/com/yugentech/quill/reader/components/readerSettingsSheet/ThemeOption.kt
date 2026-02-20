package com.yugentech.quill.reader.reader.components.readerSettingsSheet

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun ThemeOption(
    modifier: Modifier = Modifier,
    color: Color,
    label: String,
    isSelected: Boolean,
    useLightBorder: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            // Removed .aspectRatio(1f) to prevent text clipping on narrow screens
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            // Use vertical padding to give it a nice balanced shape instead
            .padding(vertical = 12.dp, horizontal = 2.dp)
    ) {
        val borderColor = if (isSelected) {
            MaterialTheme.colorScheme.primary
        } else if (useLightBorder) {
            Color.White.copy(alpha = 0.3f)
        } else {
            Color.Black.copy(alpha = 0.15f)
        }

        Box(
            modifier = Modifier
                // Scaled down slightly from 52.dp to safely fit 4 items horizontally
                .size(52.dp)
                .clip(CircleShape)
                .background(color)
                .border(
                    width = if (isSelected) 3.dp else 1.dp,
                    color = borderColor,
                    shape = CircleShape
                )
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            textAlign = TextAlign.Center
        )
    }
}