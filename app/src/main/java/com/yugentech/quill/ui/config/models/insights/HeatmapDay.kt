package com.yugentech.quill.ui.config.models.insights

import java.time.LocalDate

data class HeatmapDay(
    val date: LocalDate,
    val intensity: Int
)