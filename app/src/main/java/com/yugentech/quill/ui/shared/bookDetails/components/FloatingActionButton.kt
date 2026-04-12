package com.yugentech.quill.ui.shared.bookDetails.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yugentech.quill.R
import com.yugentech.theme.WindSongFont
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun FloatingActionButton(
    currentTabHasFab: Boolean,
    isScrollingDown: Boolean,
    onClick: () -> Unit
) {
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(currentTabHasFab, isScrollingDown) {
        if (currentTabHasFab && !isScrollingDown) {
            delay(150)
            isVisible = true
        } else {
            isVisible = false
        }
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(
            initialOffsetY = { it },
            animationSpec = tween(250)
        ) + fadeIn(animationSpec = tween(250)),
        exit = slideOutVertically(
            targetOffsetY = { it },
            animationSpec = tween(200)
        ) + fadeOut(animationSpec = tween(200))
    ) {
        FloatingActionButton(
            onClick = onClick,
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            elevation = FloatingActionButtonDefaults.elevation(0.dp),
            shape = FloatingActionButtonDefaults.extendedFabShape,
            modifier = Modifier.height(76.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                // Avatar Box
                Box(
                    modifier = Modifier
                        .width(84.dp)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    // --- THE SINGLE WATERMARK SHAPE ---
                    // Drawn first so it stays behind the image
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center) // Centers the shape beautifully in the available space
                            .size(72.dp) // Slightly smaller than the FAB height to give it breathing room
                            .background(
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f),
                                shape = MaterialShapes.SoftBurst.toShape() // Try .Bun or .Clover4Leaf here too!
                            )
                    )

                    // The Avatar Image
                    Image(
                        painter = painterResource(id = R.drawable.psmile_calm),
                        contentDescription = "Aira",
                        modifier = Modifier
                            .requiredSize(120.dp)
                            .offset(y = 12.dp)
                    )
                }

                Text(
                    text = "Aira",
                    fontFamily = WindSongFont,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Normal,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(start = 0.dp, end = 24.dp)
                )
            }
        }
    }
}