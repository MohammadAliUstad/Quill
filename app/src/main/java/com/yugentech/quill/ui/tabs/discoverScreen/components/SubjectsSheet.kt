package com.yugentech.quill.ui.tabs.discoverScreen.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.yugentech.quill.R
import com.yugentech.theme.SheetTheme
import com.yugentech.quill.ui.common.util.getTitleForCategory
import kotlinx.coroutines.launch

/** The shapes marking each subject, taken in turn. */
private val SubjectShapes = listOf(
    MaterialShapes.Cookie9Sided,
    MaterialShapes.Clover4Leaf,
    MaterialShapes.Sunny,
    MaterialShapes.Flower,
    MaterialShapes.SoftBurst,
    MaterialShapes.Cookie6Sided,
    MaterialShapes.Gem
)

/**
 * Every subject in the catalogue, opened from Discover's Subjects button: for whoever wants
 * something the curated shelves didn't show. Two to a row, each a small coloured Material shape
 * beside the subject's name; tapping one closes the sheet and opens that subject's whole shelf.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubjectsSheet(
    subjects: List<String>,
    onSubjectClick: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()

    SheetTheme {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = sheetState,
            dragHandle = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {}
                        )
                        .padding(vertical = 22.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 32.dp, height = 6.dp)
                            .background(
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                shape = CircleShape
                            )
                    )
                }
            }
        ) {
            Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                Text(
                    text = stringResource(R.string.browse_by_subject),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(R.string.browse_by_subject_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(16.dp))
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                // The sheet already keeps clear of the navigation bar
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp)
            ) {
                itemsIndexed(subjects, key = { _, subject -> subject }) { index, subject ->
                    SubjectItem(
                        subject = subject,
                        index = index,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.GestureEnd)
                            scope.launch { sheetState.hide() }.invokeOnCompletion {
                                onDismiss()
                                onSubjectClick(subject)
                            }
                        }
                    )
                }
            }
        }
    }
}

/**
 * One subject: a small Material shape in one of the theme's accent colours (taken in turn, so
 * neighbours differ) beside its name, on a rounded row.
 */
@Composable
private fun SubjectItem(
    subject: String,
    index: Int,
    onClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val accent = when (index % 3) {
        0 -> colors.primary
        1 -> colors.secondary
        else -> colors.tertiary
    }
    val marker = SubjectShapes[index % SubjectShapes.size].toShape()

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = colors.surfaceContainerHigh,
        contentColor = colors.onSurface,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(marker)
                    .background(accent)
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = getTitleForCategory(subject),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
