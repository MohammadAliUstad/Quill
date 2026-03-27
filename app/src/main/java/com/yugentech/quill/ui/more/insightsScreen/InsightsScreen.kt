package com.yugentech.quill.ui.more.insightsScreen

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
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import com.yugentech.quill.insghts.InsightsUiState
import com.yugentech.quill.ui.more.insightsScreen.components.AiraEngagementCard
import com.yugentech.quill.ui.more.insightsScreen.components.EmptyDistributionPlaceholder
import com.yugentech.quill.ui.more.insightsScreen.components.GlanceStatCard
import com.yugentech.quill.ui.more.insightsScreen.components.InsightSectionHeader
import com.yugentech.quill.ui.more.insightsScreen.components.PeakHourCard
import com.yugentech.quill.ui.more.insightsScreen.components.ProgressBracketsCard
import com.yugentech.quill.ui.more.insightsScreen.components.TopAuthorsCard
import com.yugentech.quill.ui.more.insightsScreen.components.heatMap.Heatmap
import com.yugentech.theme.tokens.corners
import com.yugentech.theme.tokens.icons
import com.yugentech.theme.tokens.spacing

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun InsightsScreen(
    uiState: InsightsUiState,
    onBack: () -> Unit
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    val totalTimeFormatted = remember(uiState.totalReadingTimeMillis) {
        val totalMinutes = (uiState.totalReadingTimeMillis / (1000 * 60)).toInt()
        val hours = totalMinutes / 60
        val mins = totalMinutes % 60
        if (hours > 0) "${hours}h ${mins}m" else "${mins}m"
    }

    val layoutDirection = LocalLayoutDirection.current
    val sectionShape = RoundedCornerShape(MaterialTheme.corners.large)

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        // Ensure the Scaffold doesn't force insets on the content itself
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            LargeTopAppBar(
                title = {
                    Column {
                        Text("Insights")
                        Text(
                            "Your reading habits",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
                        bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding(),
                        start = MaterialTheme.spacing.m + paddingValues.calculateStartPadding(layoutDirection),
                        end = MaterialTheme.spacing.m + paddingValues.calculateEndPadding(layoutDirection)
                    ),
                    verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.m)
                ) {

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.s)
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
                                modifier = Modifier.weight(1f)
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
                                modifier = Modifier.weight(1f)
                            )
                            GlanceStatCard(
                                icon = {
                                    Icon(
                                        Icons.Outlined.CheckCircle,
                                        null,
                                        tint = MaterialTheme.colorScheme.tertiary,
                                        modifier = Modifier.size(MaterialTheme.icons.medium)
                                    )
                                },
                                value = "${uiState.finishedBooksCount}",
                                label = "Finished",
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // ── PEAK READING TIME ────────────────────────────────────
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


                    // ── LIBRARY ──────────────────────────────────────────────
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
                                    title = "Your Library",
                                    subtitle = "Progress across your books"
                                )
                                Spacer(Modifier.height(MaterialTheme.spacing.m))
                                val totalBooks = uiState.progressBrackets.notStarted +
                                        uiState.progressBrackets.inProgress +
                                        uiState.progressBrackets.finished
                                if (totalBooks == 0) {
                                    EmptyDistributionPlaceholder("Add books to see your library breakdown")
                                } else {
                                    ProgressBracketsCard(progressBrackets = uiState.progressBrackets)
                                }
                            }
                        }
                    }

                    // ── TOP AUTHORS ──────────────────────────────────────────
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
                                    title = "Authors You Read",
                                    subtitle = "Writers you keep coming back to"
                                )
                                Spacer(Modifier.height(MaterialTheme.spacing.m))
                                if (uiState.topAuthors.isEmpty()) {
                                    EmptyDistributionPlaceholder("Add books to see your top authors")
                                } else {
                                    TopAuthorsCard(topAuthors = uiState.topAuthors)
                                }
                            }
                        }
                    }

                    // ── AIRA ─────────────────────────────────────────────────
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
                                    title = "With Aira",
                                    subtitle = "Your AI reading companion activity"
                                )
                                Spacer(Modifier.height(MaterialTheme.spacing.m))
                                AiraEngagementCard(
                                    totalQuestionsAsked = uiState.totalQuestionsAsked,
                                    mostExploredBookId = uiState.mostExploredBookName
                                )
                            }
                        }
                    }

                    // ── READING HEATMAP ──────────────────────────────────────
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = sectionShape,
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainer
                            )
                        ) {
                            Heatmap(data = uiState.heatmapHistory)
                        }
                    }
                }
            }
        }
    }
}