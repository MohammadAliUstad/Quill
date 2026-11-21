package com.yugentech.quill.insghts.state

import com.yugentech.quill.insghts.engine.InsightsReport

data class InsightsUiState(
    val report: InsightsReport = InsightsReport(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)
