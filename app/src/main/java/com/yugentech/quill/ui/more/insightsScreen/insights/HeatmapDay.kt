package com.yugentech.quill.ui.more.insightsScreen.insights

import java.time.LocalDate

data class HeatmapDay(
    val date: LocalDate,
    val intensity: Int
)