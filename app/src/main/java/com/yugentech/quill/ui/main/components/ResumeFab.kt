package com.yugentech.quill.ui.main.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun ResumeFab(
    visible: Boolean,
    isScrollingDown: Boolean,
    onClick: () -> Unit
) {
    var isVisible by remember { mutableStateOf(visible) }
    var isExpanded by remember { mutableStateOf(true) }

    LaunchedEffect(visible) {
        if (visible) {
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

    LaunchedEffect(isScrollingDown) {
        if (isVisible) {
            isExpanded = !isScrollingDown
        }
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = scaleIn(animationSpec = tween(200)),
        exit = scaleOut(animationSpec = tween(200))
    ) {
        FloatingActionButton(
            onClick = onClick,
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            elevation = FloatingActionButtonDefaults.elevation(0.dp),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .height(64.dp)
                .widthIn(min = 64.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.MenuBook,
                    contentDescription = "Continue Reading",
                    modifier = Modifier.size(26.dp)
                )
                AnimatedVisibility(
                    visible = isExpanded,
                    enter = fadeIn(animationSpec = tween(200)) + expandHorizontally(
                        animationSpec = tween(200),
                        expandFrom = Alignment.Start
                    ),
                    exit = fadeOut(animationSpec = tween(200)) + shrinkHorizontally(
                        animationSpec = tween(200),
                        shrinkTowards = Alignment.Start
                    )
                ) {
                    Text(
                        text = "Continue",
                        style = MaterialTheme.typography.labelLarge.copy(fontSize = 16.sp),
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(start = 12.dp)
                    )
                }
            }
        }
    }
}