package com.yugentech.quill.insghts.state

import java.time.LocalDate

data class InsightsUiState(
    val totalReadingTimeMillis: Long = 0L,
    val streakCount: Int = 0,
    val finishedBooksCount: Int = 0,
    val peakHour: Int? = null,
    val dailyVolume: Map<Int, Long> = emptyMap(),
    val heatmapHistory: Map<LocalDate, Int> = emptyMap(),
    val genreDistribution: Map<String, Int> = emptyMap(),
    val topAuthors: Map<String, Int> = emptyMap(),
    val progressBrackets: ProgressBrackets = ProgressBrackets(),
    val totalQuestionsAsked: Int = 0,
    val mostExploredBookName: String? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

data class ProgressBrackets(
    val notStarted: Int = 0,
    val inProgress: Int = 0,
    val finished: Int = 0
)