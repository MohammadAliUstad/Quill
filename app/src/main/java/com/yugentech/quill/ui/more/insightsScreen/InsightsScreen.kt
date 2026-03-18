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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.text.font.FontWeight
import com.yugentech.quill.ui.more.insightsScreen.components.ConsistencyCard
import com.yugentech.quill.ui.more.insightsScreen.components.EmptyDistributionPlaceholder
import com.yugentech.quill.ui.more.insightsScreen.components.InsightSectionHeader
import com.yugentech.quill.ui.more.insightsScreen.components.MetricCard
import com.yugentech.quill.ui.more.insightsScreen.components.PeakHourCard
import com.yugentech.quill.ui.more.insightsScreen.components.TaskDistributionList
import com.yugentech.quill.ui.more.insightsScreen.components.WeeklyRhythmChart
import com.yugentech.quill.ui.more.insightsScreen.insights.InsightsUiState
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

    // Format Milliseconds into readable "Xh Ym" format
    val totalTimeFormatted = remember(uiState.totalReadingTimeMillis) {
        val totalMinutes = (uiState.totalReadingTimeMillis / (1000 * 60)).toInt()
        val hours = totalMinutes / 60
        val mins = totalMinutes % 60
        if (hours > 0) "${hours}h ${mins}m" else "${mins}m"
    }

    // Convert ms Longs to minute Ints so your existing WeeklyRhythmChart doesn't break
    val dailyVolumeInMinutes = remember(uiState.dailyVolume) {
        uiState.dailyVolume.mapValues { (it.value / (1000 * 60)).toInt() }
    }

    val topCategory = uiState.categoryDistribution.maxByOrNull { it.value }?.key ?: "No data yet"

    val layoutDirection = LocalLayoutDirection.current
    val navBarPadding = WindowInsets.navigationBars.asPaddingValues()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Column {
                        Text("Reading Insights")
                        Text(
                            "Your reading patterns & habits",
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
                        bottom = navBarPadding.calculateBottomPadding(),
                        start = MaterialTheme.spacing.m + paddingValues.calculateStartPadding(
                            layoutDirection
                        ),
                        end = MaterialTheme.spacing.m + paddingValues.calculateEndPadding(
                            layoutDirection
                        )
                    ),
                    verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.m)
                ) {
                    item {
                        MetricCard(
                            title = "Total Reading Time",
                            value = totalTimeFormatted,
                            subtitle = "Cumulative time spent reading",
                            icon = Icons.Default.Timer
                        )
                    }

                    if (uiState.categoryDistribution.isNotEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(MaterialTheme.corners.large),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(MaterialTheme.spacing.m),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.MenuBook, // Swapped to a book icon
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(MaterialTheme.icons.medium)
                                    )

                                    Spacer(Modifier.width(MaterialTheme.spacing.m))

                                    Column {
                                        Text(
                                            "Top Genre",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(
                                                alpha = 0.7f
                                            )
                                        )

                                        Text(
                                            topCategory,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    item {
                        PeakHourCard(peakHour = uiState.peakHour)
                    }

                    item {
                        Heatmap(data = uiState.heatmapHistory)
                    }

                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(MaterialTheme.corners.large),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainer
                            )
                        ) {
                            Column(
                                Modifier.padding(MaterialTheme.spacing.m)
                            ) {
                                InsightSectionHeader(
                                    title = "Momentum",
                                    subtitle = "Your daily reading streak"
                                )

                                Spacer(Modifier.height(MaterialTheme.spacing.s))

                                ConsistencyCard(
                                    streakCount = uiState.streakCount
                                )
                            }
                        }
                    }

                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(MaterialTheme.corners.large),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainer
                            )
                        ) {
                            Column(
                                Modifier.padding(MaterialTheme.spacing.m)
                            ) {
                                InsightSectionHeader(
                                    title = "Weekly Rhythm",
                                    subtitle = "Reading volume by day of week"
                                )

                                Spacer(Modifier.height(MaterialTheme.spacing.m))

                                if (uiState.categoryDistribution.isEmpty()) {
                                    EmptyDistributionPlaceholder(
                                        "Read a book to see your volume"
                                    )
                                } else {
                                    WeeklyRhythmChart(
                                        dailyVolume = dailyVolumeInMinutes
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(MaterialTheme.corners.large),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainer
                            )
                        ) {
                            Column(
                                Modifier.padding(MaterialTheme.spacing.m)
                            ) {
                                InsightSectionHeader(
                                    title = "Library Distribution",
                                    subtitle = "Genres and categories you read"
                                )

                                Spacer(Modifier.height(MaterialTheme.spacing.s))

                                if (uiState.categoryDistribution.isEmpty()) {
                                    EmptyDistributionPlaceholder(
                                        "Add books to see your library distribution"
                                    )
                                } else {
                                    TaskDistributionList(
                                        taskDistribution = uiState.categoryDistribution
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}