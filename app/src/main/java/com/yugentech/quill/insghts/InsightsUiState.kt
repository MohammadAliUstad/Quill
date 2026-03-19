package com.yugentech.quill.insghts

import java.time.LocalDate

data class InsightsUiState(

    // --- At a Glance ---
    val totalReadingTimeMillis: Long = 0L,
    val streakCount: Int = 0,
    val finishedBooksCount: Int = 0,

    // --- Reading Patterns ---
    val peakHour: Int? = null,
    val dailyVolume: Map<Int, Long> = emptyMap(),   // Calendar.DAY_OF_WEEK index -> millis
    val heatmapHistory: Map<LocalDate, Int> = emptyMap(),

    // --- Library ---
    val genreDistribution: Map<String, Int> = emptyMap(),   // subject -> book count
    val topAuthors: Map<String, Int> = emptyMap(),          // author -> book count
    val progressBrackets: ProgressBrackets = ProgressBrackets(),

    // --- Aira ---
    val totalQuestionsAsked: Int = 0,
    val mostExploredBookName: String? = null,   // bookId with most USER messages

    // --- State ---
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

data class ProgressBrackets(
    val notStarted: Int = 0,    // progressPercent == 0
    val inProgress: Int = 0,    // 0 < progressPercent < 1
    val finished: Int = 0       // progressPercent >= 1
)