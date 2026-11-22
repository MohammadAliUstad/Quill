package com.yugentech.quill.ui.book.bookDetails.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import com.yugentech.quill.R
import androidx.compose.ui.res.stringResource
import com.yugentech.quill.ui.book.bookDetails.util.cleanDescription

@Composable
fun BookDescriptionSection(
    modifier: Modifier = Modifier,
    description: String?,
    subjects: List<String> = emptyList(),
    isExpanded: Boolean,
    // The page's side margin: the title and text sit inside it, while the subjects row runs out
    // to the screen's edges and only starts at it
    horizontalPadding: Dp = 0.dp,
    onExpandedChange: (Boolean) -> Unit
) {
    val cleanedSubjects = remember(subjects) { cleanSubjects(subjects) }

    val processedDescription = remember(description) {
        description?.replace(
            oldValue = "(This is an automatically generated summary.)",
            newValue = "",
            ignoreCase = true
        )?.replace(
            regex = Regex("(<br\\s*/?>|\\s|<p>\\s*</p>)+$"),
            replacement = ""
        )?.trim()
    }

    var fullLineCount by remember { mutableIntStateOf(0) }
    var isUserInitiated by remember { mutableStateOf(false) }
    var allowAnimations by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(100)
        allowAnimations = true
    }

    val showExpandToggle = fullLineCount > 3 || (processedDescription?.length ?: 0) > 150

    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        Text(
            text = stringResource(R.string.description),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(horizontal = horizontalPadding, vertical = 8.dp)
        )

        Spacer(modifier = Modifier.height(4.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = horizontalPadding)
                .animateContentSize(
                    animationSpec = if (allowAnimations || isUserInitiated) {
                        spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    } else {
                        snap()
                    }
                ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                HtmlText(
                    html = cleanDescription(processedDescription),
                    maxLines = if (isExpanded) Int.MAX_VALUE else 3,
                    textColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            if (showExpandToggle) {
                                isUserInitiated = true
                                onExpandedChange(!isExpanded)
                            }
                        },
                    onLineCountChanged = { lineCount ->
                        if (lineCount > fullLineCount) {
                            fullLineCount = lineCount
                        }
                    }
                )

                // The arrow sits exactly midway between the text and the subjects, expanded or
                // not, for every book: the same gap above it as below, and nothing nudged
                if (showExpandToggle) {
                    Spacer(modifier = Modifier.height(ArrowGap))
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable {
                                isUserInitiated = true
                                onExpandedChange(!isExpanded)
                            }
                            .size(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = if (isExpanded) stringResource(R.string.collapse) else stringResource(R.string.expand),
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    if (cleanedSubjects.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(ArrowGap))
                    }
                }
            }
        }

        if (cleanedSubjects.isNotEmpty()) {
            // With the arrow, its own gap below it is the space; without, the text's
            if (!showExpandToggle) {
                Spacer(modifier = Modifier.height(TextToSubjectsGap))
            }
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = horizontalPadding)
            ) {
                items(cleanedSubjects) { subject ->
                    Box(
                        modifier = Modifier
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = subject,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        } else if (!showExpandToggle) {
            // No subjects row, so the chapters title adds no space of its own above it (see
            // chaptersListSection's topSpacing): this is the gap below the text. Below the
            // arrow, none is added; its tap target and the glyph's own blank space are the gap.
            Spacer(modifier = Modifier.height(TextToChaptersGap))
        }
    }
}

/**
 * The subjects as the row shows them: "A -- B" split apart, trimmed, without repeats, shortest
 * first. The book screen uses it too, to know whether the row will show.
 */
internal fun cleanSubjects(subjects: List<String>): List<String> =
    subjects.asSequence().flatMap { it.split("--") }
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .distinct()
        .sortedBy { it.length }.toList()

// Above and below the expand arrow alike; its 40dp tap target already pads the 32dp icon
private val ArrowGap = 4.dp

// Between the text and the subjects when there's no arrow between them
private val TextToSubjectsGap = 12.dp

// Between the text and the chapters title when there's neither subjects nor an arrow: the
// chapters section's usual top spacing, which it leaves out without the subjects row
private val TextToChaptersGap = 16.dp
