package com.yugentech.quill.ui.more.subscriptions.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.yugentech.quill.R

@Composable
fun AiraHero() {
    // Randomize all 8 avatars and their horizontal orientation
    val airaCluster = remember {
        listOf(
            R.drawable.aira1, R.drawable.aira2, R.drawable.aira3, R.drawable.aira4,
            R.drawable.aira9, R.drawable.aira6, R.drawable.aira7, R.drawable.aira8
        )
            .shuffled()
            .map { id -> id to (Math.random() > 0.5) }
    }

    val topRow = airaCluster.take(4)
    val bottomRow = airaCluster.drop(4)

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // The Overlapping Avatar Grid (2 Rows of 4)
        Column(
            verticalArrangement = Arrangement.spacedBy((-40).dp), // Negative vertical overlap
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // TOP ROW
            Row(
                horizontalArrangement = Arrangement.spacedBy((-56).dp), // Aggressive horizontal overlap
                verticalAlignment = Alignment.CenterVertically
            ) {
                topRow.forEachIndexed { index, (drawableId, isFlipped) ->
                    Image(
                        painter = painterResource(id = drawableId),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(120.dp)
                            .zIndex(index.toFloat())
                            .clip(CircleShape)
                            .scale(scaleX = if (isFlipped) -1f else 1f, scaleY = 1f)
                    )
                }
            }

            // BOTTOM ROW
            Row(
                horizontalArrangement = Arrangement.spacedBy((-56).dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                bottomRow.forEachIndexed { index, (drawableId, isFlipped) ->
                    Image(
                        painter = painterResource(id = drawableId),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(120.dp)
                            .zIndex((index + 4).toFloat()) // Ensures bottom row stacks on top
                            .clip(CircleShape)
                            .scale(scaleX = if (isFlipped) -1f else 1f, scaleY = 1f)
                    )
                }
            }
        }

        Text(
            text = "Your Personal AI Assistant",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}