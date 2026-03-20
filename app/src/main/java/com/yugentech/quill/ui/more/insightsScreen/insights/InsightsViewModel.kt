package com.yugentech.quill.ui.more.insightsScreen.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yugentech.quill.database.entity.ReadingSessionEntity
import com.yugentech.quill.reader.repository.ReadingSessionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import timber.log.Timber
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Calendar
import java.util.concurrent.TimeUnit

class InsightsViewModel(
    private val sessionRepository: ReadingSessionRepository
    // You can inject BookRepository here later to add categoryDistribution!
) : ViewModel() {

    private val _uiState = MutableStateFlow(InsightsUiState())
    val uiState: StateFlow<InsightsUiState> = _uiState.asStateFlow()

    init {
        loadInsights()
    }

    private fun loadInsights() {
        sessionRepository.getAllSessionsFlow()
            .onEach { sessions ->

                val heatmapData = mutableMapOf<LocalDate, Int>()
                val hourlyCounts = IntArray(24)
                val dailyCounts = LongArray(8) // Days of the week (1-7)
                var totalTime = 0L

                sessions.forEach { session ->
                    totalTime += session.durationMillis

                    val cal = Calendar.getInstance().apply { timeInMillis = session.startTime }

                    // Track volume per day of the week
                    dailyCounts[cal.get(Calendar.DAY_OF_WEEK)] += session.durationMillis

                    // Track which hour of the day they read most
                    hourlyCounts[cal.get(Calendar.HOUR_OF_DAY)]++

                    // Track days read for the heatmap
                    val date = Instant.ofEpochMilli(session.startTime)
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate()
                    heatmapData[date] = (heatmapData[date] ?: 0) + 1
                }

                // Calculate the peak reading hour
                val mostActiveHourIndex = hourlyCounts.indices.maxByOrNull { hourlyCounts[it] }
                val validPeakHour =
                    if (mostActiveHourIndex != null && hourlyCounts[mostActiveHourIndex] > 0) {
                        mostActiveHourIndex
                    } else null

                // Push the unified state to the UI
                _uiState.update { state ->
                    state.copy(
                        totalReadingTimeMillis = totalTime,
                        streakCount = calculateStreak(sessions),
                        dailyVolume = dailyCounts.mapIndexed { index, time -> index to time }
                            .toMap(),
                        peakHour = validPeakHour,
                        heatmapHistory = heatmapData,
                        isLoading = false
                    )
                }
            }
            .catch { e ->
                Timber.e(e, "Error loading reading sessions")
                _uiState.update { it.copy(errorMessage = e.message, isLoading = false) }
            }
            .launchIn(viewModelScope)
    }

    private fun calculateStreak(sessions: List<ReadingSessionEntity>): Int {
        if (sessions.isEmpty()) return 0

        val sessionDates = sessions.map {
            val cal = Calendar.getInstance().apply { timeInMillis = it.startTime }
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            cal.timeInMillis
        }.distinct().sortedDescending()

        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val yesterday = today - TimeUnit.DAYS.toMillis(1)

        if (sessionDates.first() < yesterday) return 0

        var currentStreak = 0
        var lastDate =
            if (sessionDates.first() == today) today else yesterday + TimeUnit.DAYS.toMillis(1)

        for (date in sessionDates) {
            if (lastDate - date <= TimeUnit.DAYS.toMillis(1)) {
                currentStreak++
                lastDate = date
            } else {
                break
            }
        }
        return currentStreak
    }
}