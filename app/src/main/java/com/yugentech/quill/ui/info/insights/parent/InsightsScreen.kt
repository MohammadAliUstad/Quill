package com.yugentech.quill.ui.info.insights.parent

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.yugentech.quill.R
import com.yugentech.quill.insights.state.InsightsUiState
import com.yugentech.quill.ui.info.insights.components.AiraCard
import com.yugentech.quill.ui.info.insights.components.AuthorsCard
import com.yugentech.quill.ui.info.insights.components.EmptyInsightPlaceholder
import com.yugentech.quill.ui.info.insights.components.GlanceRow
import com.yugentech.quill.ui.info.insights.components.HighlightsCard
import com.yugentech.quill.ui.info.insights.components.InsightItem
import com.yugentech.quill.ui.info.insights.components.InsightSectionHeader
import com.yugentech.quill.ui.info.insights.components.LibraryCard
import com.yugentech.quill.ui.info.insights.components.RecordsCard
import com.yugentech.quill.ui.info.insights.components.RhythmCard
import com.yugentech.quill.ui.info.insights.components.TopBooksCard
import com.yugentech.quill.ui.info.insights.components.WeekHeroCard
import com.yugentech.quill.ui.info.insights.components.heatmap.Heatmap
import com.yugentech.theme.shapes.itemShape
import com.yugentech.theme.tokens.corners
import com.yugentech.theme.tokens.spacing

/**
 * Insights, laid out as Hajime's (shown plainly in a LazyColumn, as Sessions does): the week at a glance, then insights generated from the
 * reader's own records (see InsightsEngine), then the detail behind them: the year in pixels,
 * when they read, their records, the books and authors they spend time with, their library,
 * highlights, and reading with Aira.
 */
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
    val layoutDirection = LocalLayoutDirection.current
    val report = uiState.report
    val hasReading = report.sessionCount > 0

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
                return@Box
            }

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
                item(key = "week") { WeekHeroCard(report) }
                item(key = "glance") { GlanceRow(report) }

                // The generated insights, as one grouped list
                item(key = "forYouHeader") {
                    InsightSectionHeader(
                        title = stringResource(R.string.insights_for_you_title),
                        subtitle = stringResource(R.string.insights_for_you_subtitle)
                    )
                }
                item(key = "insights") {
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        report.insights.forEachIndexed { index, insight ->
                            InsightItem(insight = insight, shape = itemShape(index, report.insights.size))
                        }
                    }
                }

                item(key = "heatmap") {
                    Heatmap(intensities = report.heatmap, readByDay = report.readByDay)
                }

                section(
                    key = "rhythm",
                    title = { stringResource(R.string.insights_rhythm_title) },
                    subtitle = { stringResource(R.string.insights_rhythm_subtitle) }
                ) {
                    if (hasReading) RhythmCard(report.partsOfDay)
                    else EmptyInsightPlaceholder(stringResource(R.string.insights_rhythm_empty))
                }

                if (hasReading) {
                    section(
                        key = "records",
                        title = { stringResource(R.string.insights_bests_title) },
                        subtitle = { stringResource(R.string.insights_bests_subtitle) }
                    ) { RecordsCard(report.records) }
                }

                section(
                    key = "topBooks",
                    title = { stringResource(R.string.insights_top_books_title) },
                    subtitle = { stringResource(R.string.insights_top_books_subtitle) }
                ) {
                    if (report.topBooks.isEmpty()) EmptyInsightPlaceholder(stringResource(R.string.insights_top_books_empty))
                    else TopBooksCard(report.topBooks)
                }

                section(
                    key = "library",
                    title = { stringResource(R.string.insights_library_title) },
                    subtitle = { stringResource(R.string.insights_library_subtitle) }
                ) {
                    if (report.library.total == 0) EmptyInsightPlaceholder(stringResource(R.string.insights_library_empty))
                    else LibraryCard(report.library)
                }

                section(
                    key = "authors",
                    title = { stringResource(R.string.insights_authors_title) },
                    subtitle = { stringResource(R.string.insights_authors_subtitle) }
                ) {
                    if (report.topAuthors.isEmpty()) EmptyInsightPlaceholder(stringResource(R.string.insights_authors_empty))
                    else AuthorsCard(report.topAuthors)
                }

                section(
                    key = "highlights",
                    title = { stringResource(R.string.insights_highlights_section_title) },
                    subtitle = { stringResource(R.string.insights_highlights_section_subtitle) }
                ) {
                    if (report.highlights.total == 0) EmptyInsightPlaceholder(stringResource(R.string.insights_highlights_empty))
                    else HighlightsCard(report.highlights)
                }

                if (showAira) {
                    section(
                        key = "aira",
                        title = { stringResource(R.string.insights_aira_title) },
                        subtitle = { stringResource(R.string.insights_aira_subtitle) }
                    ) { AiraCard(report.aira) }
                }
            }
        }
    }
}

/** A titled section on its own card. */
private fun LazyListScope.section(
    key: String,
    title: @Composable () -> String,
    subtitle: @Composable () -> String,
    content: @Composable () -> Unit
) {
    item(key = key) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(MaterialTheme.corners.large),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
        ) {
            Column(Modifier.padding(MaterialTheme.spacing.m)) {
                InsightSectionHeader(title = title(), subtitle = subtitle())
                Spacer(Modifier.height(MaterialTheme.spacing.s))
                content()
            }
        }
    }
}
