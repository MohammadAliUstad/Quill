package com.yugentech.quill.insights.state

import com.yugentech.quill.insights.engine.InsightsReport

data class InsightsUiState(
    val report: InsightsReport = InsightsReport(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)
