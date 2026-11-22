package com.yugentech.quill.ui.common.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.yugentech.quill.R
import com.yugentech.theme.tokens.dimensions.AppAnimations
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * A search that found nothing, the same in every search (Discover, Standard Ebooks) and laid out
 * like Ryori's: a soft search-off icon, a title, and a two-line hint at what to try, a little
 * above centre where the search prompt's icon sits.
 */
@Composable
fun NoSearchResults(modifier: Modifier = Modifier) {
    SearchStateLayout(
        icon = {
            Icon(
                imageVector = Icons.Rounded.SearchOff,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                modifier = Modifier.size(64.dp)
            )
        },
        title = stringResource(R.string.no_books_found),
        message = stringResource(R.string.search_no_results_message),
        modifier = modifier
    )
}

/**
 * The layout for a search's states: a 64dp icon, a title and a two-line message, a little above
 * centre. On appearing, the icon settles in from a little smaller and the text rises a few dp
 * just behind it.
 */
@Composable
fun SearchStateLayout(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit
) {
    val iconIn = remember { Animatable(0f) }
    val textIn = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        val spec = tween<Float>(
            durationMillis = AppAnimations.Durations.Emphasized,
            easing = AppAnimations.Easings.EmphasizedDecelerate
        )
        launch { iconIn.animateTo(1f, spec) }
        delay(AppAnimations.Durations.Rapid.toLong())
        textIn.animateTo(1f, spec)
    }
    val textModifier = Modifier.graphicsLayer {
        alpha = textIn.value
        translationY = (1f - textIn.value) * 8.dp.toPx()
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp)
    ) {
        Spacer(Modifier.weight(0.3f))
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(64.dp)
                .graphicsLayer {
                    val scale = 0.85f + 0.15f * iconIn.value
                    alpha = iconIn.value
                    scaleX = scale
                    scaleY = scale
                }
        ) {
            icon()
        }
        Spacer(Modifier.height(24.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = textModifier
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            minLines = 2,
            modifier = textModifier
        )
        Spacer(Modifier.weight(0.7f))
    }
}
