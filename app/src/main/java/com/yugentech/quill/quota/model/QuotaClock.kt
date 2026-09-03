package com.yugentech.quill.quota.model

import java.time.LocalDate

// The single source of truth for "what day is it" across the whole quota system -- both the
// client (QuotaRepositoryImpl) and what gets written to Firestore (QuotaService) call this same
// function, so there's exactly one definition of "today" instead of two clocks that can drift.
fun todayDateString(): String = LocalDate.now().toString()
