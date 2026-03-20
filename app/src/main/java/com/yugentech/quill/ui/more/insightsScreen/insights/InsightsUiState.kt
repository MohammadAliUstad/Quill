package com.yugentech.quill.ui.more.insightsScreen.insights

import java.time.LocalDate

data class InsightsUiState(
    val totalReadingTimeMillis: Long = 0L,
    val streakCount: Int = 0,
    val dailyVolume: Map<Int, Long> = emptyMap(),
    val peakHour: Int? = null,
    val heatmapHistory: Map<LocalDate, Int> = emptyMap(),
    val categoryDistribution: Map<String, Int> = emptyMap(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)