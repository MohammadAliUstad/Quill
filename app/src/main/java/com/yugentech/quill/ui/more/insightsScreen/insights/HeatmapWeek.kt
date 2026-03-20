package com.yugentech.quill.ui.more.insightsScreen.insights

data class HeatmapWeek(
    val weekIndex: Int,
    val days: List<HeatmapDay>,
    val firstDayOfMonth: String?
)