package com.yugentech.quill.ui.info.insights.insights

data class HeatmapWeek(
    val weekIndex: Int,
    val days: List<HeatmapDay>,
    val firstDayOfMonth: String?
)