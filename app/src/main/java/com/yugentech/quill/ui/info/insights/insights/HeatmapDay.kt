package com.yugentech.quill.ui.info.insights.insights

import java.time.LocalDate

data class HeatmapDay(
    val date: LocalDate,
    val intensity: Int
)