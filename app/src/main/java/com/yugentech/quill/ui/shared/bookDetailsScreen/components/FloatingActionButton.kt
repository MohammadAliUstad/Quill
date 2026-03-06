package com.yugentech.quill.ui.shared.bookDetailsScreen.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yugentech.quill.R
import com.yugentech.theme.getters.AppFont
import kotlinx.coroutines.delay

@Composable
fun FloatingActionButton(
    currentTabHasFab: Boolean,
    isScrollingDown: Boolean,
    onClick: () -> Unit
) {
    var isVisible by remember { mutableStateOf(currentTabHasFab) }
    var isExpanded by remember { mutableStateOf(true) }

    // --- SEQUENCER LOGIC ---
    LaunchedEffect(currentTabHasFab) {
        if (currentTabHasFab) {
            isExpanded = false
            isVisible = true
            delay(200)
            isExpanded = true
        } else {
            isExpanded = false
            delay(200)
            isVisible = false
        }
    }

    // --- SCROLL OVERRIDE ---
    LaunchedEffect(isScrollingDown) {
        if (isVisible) {
            isExpanded = !isScrollingDown
        }
    }

    val windSongFont = remember { AppFont.WindSong.toFontFamily() }

    AnimatedVisibility(
        visible = isVisible,
        enter = scaleIn(animationSpec = tween(200)),
        exit = scaleOut(animationSpec = tween(200))
    ) {
        Surface(
            shape = FloatingActionButtonDefaults.extendedFabShape,
            color = Color.Transparent // Surface handles the border, FAB handles the fill
        ) {
            FloatingActionButton(
                onClick = onClick,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                elevation = FloatingActionButtonDefaults.elevation(0.dp),
                shape = FloatingActionButtonDefaults.extendedFabShape,
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    // ICON
                    Box(
                        modifier = Modifier
                            .padding(12.dp)
                            .size(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.aira),
                            contentDescription = "AI Reading Assistant",
                            modifier = Modifier.requiredSize(52.dp),
                            tint = Color.Unspecified
                        )
                    }

                    // TEXT ANIMATION
                    AnimatedVisibility(
                        visible = isExpanded,
                        enter = fadeIn() + expandHorizontally(),
                        exit = fadeOut() + shrinkHorizontally()
                    ) {
                        Box(
                            modifier = Modifier.padding(start = 6.dp, end = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Aira",
                                fontFamily = windSongFont,
                                fontSize = 30.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}