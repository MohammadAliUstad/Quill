package com.yugentech.quill.ui.info.insights

import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalLayoutDirection
import com.yugentech.quill.insghts.state.InsightsUiState
import com.yugentech.quill.ui.info.insights.components.AiraEngagementCard
import com.yugentech.quill.ui.info.insights.components.EmptyDistributionPlaceholder
import com.yugentech.quill.ui.info.insights.components.GlanceStatCard
import com.yugentech.quill.ui.info.insights.components.InsightSectionHeader
import com.yugentech.quill.ui.info.insights.components.PeakHourCard
import com.yugentech.quill.ui.info.insights.components.ProgressBracketsCard
import com.yugentech.quill.ui.info.insights.components.TopAuthorsCard
import com.yugentech.theme.tokens.corners
import com.yugentech.theme.tokens.icons
import com.yugentech.theme.tokens.spacing
import com.yugentech.quill.R
import androidx.compose.ui.res.stringResource

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun InsightsScreen(
    uiState: InsightsUiState,
    onBack: () -> Unit,
    // Off for a guest: Aira needs an account, so there's nothing to show
    showAira: Boolean = true
) {
    val haptic = LocalHapticFeedback.current
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    val totalTimeFormatted = run {
        val totalMinutes = (uiState.totalReadingTimeMillis / (1000 * 60)).toInt()
        val hours = totalMinutes / 60
        val mins = totalMinutes % 60
        if (hours > 0) stringResource(R.string.duration_hours_minutes, hours, mins) else stringResource(R.string.duration_minutes, mins)
    }

    val layoutDirection = LocalLayoutDirection.current
    val sectionShape = RoundedCornerShape(MaterialTheme.corners.large)

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            LargeTopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.profile_insights))
                        Text(
                            stringResource(R.string.insights_subtitle),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.GestureEnd)
                        onBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = paddingValues.calculateTopPadding()),
            contentAlignment = Alignment.Center
        ) {
            if (uiState.isLoading) {
                CircularWavyProgressIndicator()
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        top = MaterialTheme.spacing.s,
                        bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 8.dp,
                        start = MaterialTheme.spacing.m + paddingValues.calculateStartPadding(layoutDirection),
                        end = MaterialTheme.spacing.m + paddingValues.calculateEndPadding(layoutDirection)
                    ),
                    verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.s)
                ) {

                    // The three stats read as one grouped block (primary / secondary / tertiary
                    // containers, tight gap, large corners only on the outer edges) -- the
                    // Sessions insights treatment, laid out horizontally.
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            GlanceStatCard(
                                icon = {
                                    Icon(
                                        Icons.Outlined.Timer,
                                        null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(MaterialTheme.icons.medium)
                                    )
                                },
                                value = totalTimeFormatted,
                                label = "Time read",
                                modifier = Modifier.weight(1f),
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                shape = rowItemShape(index = 0, count = 3)
                            )
                            GlanceStatCard(
                                icon = {
                                    Icon(
                                        Icons.Outlined.AutoStories,
                                        null,
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(MaterialTheme.icons.medium)
                                    )
                                },
                                value = "${uiState.streakCount}",
                                label = "Day streak",
                                modifier = Modifier.weight(1f),
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                shape = rowItemShape(index = 1, count = 3)
                            )
                            GlanceStatCard(
                                icon = {
                                    Icon(
                                        Icons.Outlined.CheckCircle,
                                        null,
                                        tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                        modifier = Modifier.size(MaterialTheme.icons.medium)
                                    )
                                },
                                value = "${uiState.finishedBooksCount}",
                                label = "Finished",
                                modifier = Modifier.weight(1f),
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                                shape = rowItemShape(index = 2, count = 3)
                            )
                        }
                    }

                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = sectionShape,
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainer
                            )
                        ) {
                            PeakHourCard(
                                peakHour = uiState.peakHour,
                                modifier = Modifier.padding(MaterialTheme.spacing.m)
                            )
                        }
                    }


                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = sectionShape,
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainer
                            )
                        ) {
                            Column(Modifier.padding(MaterialTheme.spacing.m)) {
                                InsightSectionHeader(
                                    title = stringResource(R.string.insights_library_title),
                                    subtitle = stringResource(R.string.insights_library_subtitle)
                                )
                                Spacer(Modifier.height(MaterialTheme.spacing.m))
                                val totalBooks = uiState.progressBrackets.notStarted +
                                        uiState.progressBrackets.inProgress +
                                        uiState.progressBrackets.finished
                                if (totalBooks == 0) {
                                    EmptyDistributionPlaceholder(stringResource(R.string.insights_library_empty))
                                } else {
                                    ProgressBracketsCard(progressBrackets = uiState.progressBrackets)
                                }
                            }
                        }
                    }

                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = sectionShape,
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainer
                            )
                        ) {
                            Column(Modifier.padding(MaterialTheme.spacing.m)) {
                                InsightSectionHeader(
                                    title = stringResource(R.string.insights_authors_title),
                                    subtitle = stringResource(R.string.insights_authors_subtitle)
                                )
                                Spacer(Modifier.height(MaterialTheme.spacing.m))
                                if (uiState.topAuthors.isEmpty()) {
                                    EmptyDistributionPlaceholder(stringResource(R.string.insights_authors_empty))
                                } else {
                                    TopAuthorsCard(topAuthors = uiState.topAuthors)
                                }
                            }
                        }
                    }

                    if (showAira) item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = sectionShape,
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainer
                            )
                        ) {
                            Column(Modifier.padding(MaterialTheme.spacing.m)) {
                                InsightSectionHeader(
                                    title = stringResource(R.string.insights_aira_title),
                                    subtitle = stringResource(R.string.insights_aira_subtitle)
                                )
                                Spacer(Modifier.height(MaterialTheme.spacing.m))
                                AiraEngagementCard(
                                    totalQuestionsAsked = uiState.totalQuestionsAsked,
                                    mostExploredBookId = uiState.mostExploredBookName
                                )
                            }
                        }
                    }

                }
            }
        }
    }
}
// Horizontal counterpart of itemShape: a row of cards reads as one grouped block, with large
// corners only on the outer edges and small corners where neighbours meet.
@Composable
private fun rowItemShape(index: Int, count: Int): Shape {
    val largeCorner = MaterialTheme.corners.large
    val smallCorner = MaterialTheme.corners.small

    return when {
        count == 1 -> RoundedCornerShape(largeCorner)
        index == 0 -> RoundedCornerShape(
            topStart = largeCorner,
            bottomStart = largeCorner,
            topEnd = smallCorner,
            bottomEnd = smallCorner
        )

        index == count - 1 -> RoundedCornerShape(
            topStart = smallCorner,
            bottomStart = smallCorner,
            topEnd = largeCorner,
            bottomEnd = largeCorner
        )

        else -> RoundedCornerShape(smallCorner)
    }
}
