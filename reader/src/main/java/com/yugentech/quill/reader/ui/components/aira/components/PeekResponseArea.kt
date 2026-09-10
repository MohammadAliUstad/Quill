package com.yugentech.quill.reader.ui.components.aira.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.yugentech.quill.aira.chat.quickChat.prompt.QuickPrompt
import com.yugentech.quill.reader.viewmodel.quick.QuickUiState
import kotlinx.coroutines.delay
import java.io.File

private sealed class ResponseAreaState {
    data object Loading : ResponseAreaState()
    data object GeneratingImage : ResponseAreaState()
    data object LimitReached : ResponseAreaState()
    data class Error(val message: String) : ResponseAreaState()
    data class Response(val text: String) : ResponseAreaState()
    data class Image(val path: String) : ResponseAreaState()
    data object Chips : ResponseAreaState()
}

@Composable
fun PeekResponseArea(
    airaUiState: QuickUiState,
    showLimitReached: Boolean,
    selectedText: String? = null,
    activeChips: List<Pair<String, QuickPrompt>> = emptyList(),
    startRevealed: Boolean = false,
    onResponseRevealed: (String) -> Unit = {},
    onChipClick: (QuickPrompt) -> Unit = {},
    onGreetingSelected: (String) -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        Column {
            if (!selectedText.isNullOrBlank()) {
                SelectionSnippet(text = selectedText)
            }

            val contentState = when {
                airaUiState.isLoading && airaUiState.isGeneratingImage -> ResponseAreaState.GeneratingImage
                airaUiState.isLoading -> ResponseAreaState.Loading
                showLimitReached -> ResponseAreaState.LimitReached
                airaUiState.error != null -> ResponseAreaState.Error(airaUiState.error)
                airaUiState.imagePath != null -> ResponseAreaState.Image(airaUiState.imagePath)
                airaUiState.response != null -> ResponseAreaState.Response(airaUiState.response)
                else -> ResponseAreaState.Chips
            }

            AnimatedContent(
                targetState = contentState,
                transitionSpec = {
                    // Size interpolation is handled once, by animateContentSize() on the outer
                    // Surface in AiraPeekBar -- letting this AnimatedContent also animate its
                    // own bounds would double-animate the same resize with a different
                    // curve/duration, producing a jittery combined motion. snap() here just
                    // means this transition only crossfades content in place, instantly
                    // matching whatever size the outer animation is currently interpolating to.
                    (fadeIn(tween(260)) + slideInVertically(tween(260)) { it / 8 }) togetherWith
                        fadeOut(tween(160)) using
                        SizeTransform(clip = false, sizeAnimationSpec = { _, _ -> snap() })
                },
                label = "PeekResponseAreaTransition"
            ) { state ->
                when (state) {
                    is ResponseAreaState.Loading -> {
                        Column {
                            Spacer(Modifier.height(20.dp))
                            ThinkingIndicator(modifier = Modifier.padding(horizontal = 16.dp))
                            Spacer(Modifier.height(20.dp))
                        }
                    }

                    is ResponseAreaState.GeneratingImage -> {
                        Column {
                            Spacer(Modifier.height(12.dp))
                            ImageGeneratingCard(
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                            Spacer(Modifier.height(12.dp))
                        }
                    }

                    is ResponseAreaState.LimitReached -> {
                        Column {
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = "I'm out of energy for today! You've reached your daily AI query limit. Upgrade to Pro to keep chatting, or I'll see you tomorrow.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                            Spacer(Modifier.height(12.dp))
                        }
                    }

                    is ResponseAreaState.Error -> {
                        Column {
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = state.message,
                                style = MaterialTheme.typography.bodyMedium,
                                // Most of these are friendly "try something else" notes (name not
                                // found, passage too plain to visualize), not failures -- render
                                // them like a normal response instead of alarming red.
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                            Spacer(Modifier.height(12.dp))
                        }
                    }

                    is ResponseAreaState.Response -> {
                        Column {
                            Spacer(Modifier.height(12.dp))
                            TypewriterText(
                                text = state.text,
                                resetKey = state.text,
                                startRevealed = startRevealed,
                                onTypingStateChange = { isTyping ->
                                    if (!isTyping) onResponseRevealed(state.text)
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                            Spacer(Modifier.height(12.dp))
                        }
                    }

                    is ResponseAreaState.Image -> {
                        Column {
                            Spacer(Modifier.height(12.dp))
                            // No forced aspect ratio here -- the loading card above is the
                            // fixed-size placeholder; once the real file is decoded, the card
                            // adapts to whatever shape the generated image actually is instead
                            // of cropping it to fit a square.
                            AsyncImage(
                                model = File(state.path),
                                contentDescription = "Generated illustration",
                                contentScale = ContentScale.FillWidth,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                                    .clip(RoundedCornerShape(16.dp))
                            )
                            Spacer(Modifier.height(12.dp))
                        }
                    }

                    is ResponseAreaState.Chips -> {
                        QuickActionChips(
                            selectedText = selectedText,
                            activeChips = activeChips,
                            onChipClick = onChipClick,
                            onGreetingSelected = onGreetingSelected
                        )
                    }
                }
            }
        }
    }
}

private val IMAGE_GENERATION_CAPTIONS = listOf(
    "Rendering pixels into meaning…",
    "Translating words into light…",
    "Sketching the scene in silicon…",
    "Weaving imagination into form…",
    "Painting with borrowed light…",
    "Composing a visual echo…",
    "Summoning shapes from text…",
    "Turning ink into imagery…"
)

// A square placeholder matching the eventual image's rounded-corner treatment, so the
// transition into the real result reads as "this card fills in" rather than a layout swap.
// The motion is the standard skeleton-loader shimmer: a soft highlight band sweeps fully
// off-screen on both ends before restarting, so the loop has no visible snap. The icon stays
// completely still -- only the shimmer and the rotating caption move.
@Composable
private fun ImageGeneratingCard(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "imageGenCard")

    val shimmerProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerProgress"
    )

    var captionIndex by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(2800)
            captionIndex = (captionIndex + 1) % IMAGE_GENERATION_CAPTIONS.size
        }
    }

    // surfaceContainerHigh sits too close in tone to the sheet's own surfaceContainerLow
    // background to read as a distinct solid card -- surfaceContainerHighest gives it real
    // contrast instead of looking like a translucent patch over the sheet.
    val baseColor = MaterialTheme.colorScheme.surfaceContainerHighest
    val highlight = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(16.dp))
            .background(baseColor)
    ) {
        // The band spans the full box width already (horizontalGradient); translating it from
        // just past the left edge to just past the right edge sweeps the highlight across the
        // visible card while both loop endpoints sit outside the clipped bounds.
        Box(
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer { translationX = (shimmerProgress * 2.4f - 0.7f) * size.width }
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(Color.Transparent, highlight, Color.Transparent)
                    )
                )
        )

        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(Modifier.height(16.dp))
            AnimatedContent(
                targetState = captionIndex,
                transitionSpec = { fadeIn(tween(400)) togetherWith fadeOut(tween(300)) },
                label = "captionSwap"
            ) { index ->
                Text(
                    text = IMAGE_GENERATION_CAPTIONS[index],
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }
        }
    }
}

@Composable
private fun QuickActionChips(
    selectedText: String?,
    activeChips: List<Pair<String, QuickPrompt>>,
    onChipClick: (QuickPrompt) -> Unit,
    onGreetingSelected: (String) -> Unit
) {
    val greetings = listOf(
        "How can I help you today?",
        "What's on your mind regarding this book?",
        "Ask me anything about the story!",
        "Want to dive deeper into this chapter?"
    )
    val greeting = remember { greetings.random() }
    LaunchedEffect(Unit) { onGreetingSelected(greeting) }

    Column {
        Spacer(Modifier.height(12.dp))
        if (selectedText.isNullOrBlank()) {
            Text(
                text = greeting,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(Modifier.height(12.dp))
        }
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(activeChips) { (label, intent) ->
                Surface(
                    onClick = { onChipClick(intent) },
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f)
                ) {
                    Text(
                        text = label,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))
    }
}