package com.yugentech.quill.ui.about.aira.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yugentech.quill.R
import com.yugentech.theme.WindSongFont
import kotlinx.coroutines.delay

private val airaImages = listOf(
    R.drawable.psmile_calm,
    R.drawable.aira_fear,
    R.drawable.aira_awe,
    R.drawable.aira_eating,
    R.drawable.aira_blank,
    R.drawable.aira_smile_lol,
    R.drawable.aira_smile_big,
    R.drawable.aira_love_grin,
    R.drawable.aira_explaining,
    R.drawable.aira_suspicious,
    R.drawable.aira_cute
)

@Composable
fun HeroSection() {
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer
    val primary = MaterialTheme.colorScheme.primary

    var currentImage by remember { mutableStateOf(airaImages.first()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(2500L)
            currentImage = airaImages.filter { it != currentImage }.random()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(148.dp)
                    .border(
                        width = 1.5.dp,
                        color = primary.copy(alpha = 0.6f),
                        shape = CircleShape
                    )
                    .clip(CircleShape)
                    .background(primaryContainer.copy(alpha = 0.55f)),
                contentAlignment = Alignment.BottomCenter
            ) {
                Crossfade(
                    targetState = currentImage,
                    animationSpec = tween(durationMillis = 700),
                    label = "aira-avatar"
                ) { imageRes ->
                    Image(
                        painter = painterResource(imageRes),
                        contentDescription = "Aira",
                        modifier = Modifier
                            .requiredSize(176.dp)
                            .offset(x = (-1).dp, y = 18.dp)
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Aira",
                    fontFamily = WindSongFont,
                    fontSize = 64.sp,
                    fontWeight = FontWeight.Normal,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(start = 8.dp)
                )
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text(
                        text = "Your AI Reading Assistant",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                    )
                }
            }
        }
    }
}