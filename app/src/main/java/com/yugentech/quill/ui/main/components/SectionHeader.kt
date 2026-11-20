package com.yugentech.quill.ui.main.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import com.yugentech.theme.tokens.dimensions.AppAnimations
import com.yugentech.theme.tokens.icons
import com.yugentech.theme.tokens.spacing

/**
 * A group's heading: an icon and a bold title in the primary colour. When the title changes (a
 * language switch, say) the new one slides up in place of the old, as in Sessions.
 *
 * [startPadding] defaults to Quill's inset; the language screen passes 0 to sit flush with its
 * rows, as Sessions' headers do.
 */
@Composable
fun SectionHeader(
    icon: ImageVector,
    title: String,
    startPadding: Dp = MaterialTheme.spacing.m
) {
    val fadeThrough = AppAnimations.Transitions.rememberExpressiveFadeThrough()

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = startPadding,
                bottom = MaterialTheme.spacing.s,
                top = MaterialTheme.spacing.m
            )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(MaterialTheme.icons.mediumSmall)
        )

        Spacer(modifier = Modifier.width(MaterialTheme.spacing.sm))

        AnimatedContent(
            targetState = title,
            transitionSpec = {
                if (targetState != initialState) {
                    (slideInVertically { height -> height } + fadeIn() togetherWith
                            slideOutVertically { height -> -height } + fadeOut())
                        .using(SizeTransform(clip = false))
                } else {
                    fadeThrough()
                }
            },
            label = "title_animation",
            modifier = Modifier.weight(1f)
        ) { targetTitle ->
            Text(
                text = targetTitle,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1
            )
        }
    }
}
