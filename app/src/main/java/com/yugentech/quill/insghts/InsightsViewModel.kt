package com.yugentech.quill.ui.more.insightsScreen.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yugentech.quill.database.entity.ReadingSessionEntity
import com.yugentech.quill.insghts.InsightsRepository
import com.yugentech.quill.insghts.InsightsUiState
import com.yugentech.quill.insghts.ProgressBrackets
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
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
    private val insightsRepository: InsightsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(InsightsUiState())
    val uiState: StateFlow<InsightsUiState> = _uiState.asStateFlow()

    init {
        loadInsights()
    }

    private fun loadInsights() {
        val sessionActivityFlow = combine(
            insightsRepository.getAllSessionsFlow(),
            insightsRepository.getStreakFlow(),
            insightsRepository.getFinishedBooksCountFlow()
        ) { sessions, streak, finishedCount ->
            Triple(sessions, streak, finishedCount)
        }

        // Group 2: Library and Aira Activity (3 flows)
        val libraryActivityFlow = combine(
            insightsRepository.getAllBooksFlow(),
            insightsRepository.getTotalUserQuestionsFlow(),
            insightsRepository.getQuestionsPerBookFlow()
        ) { books, totalQuestions, questionsPerBook ->
            Triple(books, totalQuestions, questionsPerBook)
        }

        // Final Combine: Merge the two groups
        combine(sessionActivityFlow, libraryActivityFlow) { sessionData, libraryData ->
            val (sessions, streak, finishedCount) = sessionData
            val (books, totalQuestions, questionsPerBook) = libraryData

            // --- Session derived stats ---
            val heatmapData = mutableMapOf<LocalDate, Int>()
            val hourlyCounts = IntArray(24)
            var totalTime = 0L

            sessions.forEach { session ->
                totalTime += session.durationMillis
                val cal = Calendar.getInstance().apply { timeInMillis = session.startTime }
                hourlyCounts[cal.get(Calendar.HOUR_OF_DAY)]++

                val date = Instant.ofEpochMilli(session.startTime)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
                heatmapData[date] = (heatmapData[date] ?: 0) + 1
            }

            val peakHourIndex = hourlyCounts.indices.maxByOrNull { hourlyCounts[it] }
            val validPeakHour = if (peakHourIndex != null && hourlyCounts[peakHourIndex] > 0) {
                peakHourIndex
            } else null

            // --- Aira / Library derived stats ---
            val mostExploredBookId = questionsPerBook.maxByOrNull { it.count }?.bookId
            val mostExploredBookName = books.find { it.id == mostExploredBookId }?.title ?: "None"

            InsightsUiState(
                totalReadingTimeMillis = totalTime,
                streakCount = streak,
                finishedBooksCount = finishedCount,
                peakHour = validPeakHour,
                heatmapHistory = heatmapData,
                topAuthors = books.filter { it.author.isNotBlank() }
                    .groupingBy { it.author }
                    .eachCount()
                    .entries
                    .sortedByDescending { it.value }
                    .take(5)
                    .associate { it.key to it.value },
                progressBrackets = ProgressBrackets(
                    notStarted = books.count { it.progressPercent == 0f },
                    inProgress = books.count { it.progressPercent > 0f && it.progressPercent < 1f },
                    finished = finishedCount
                ),
                totalQuestionsAsked = totalQuestions,
                mostExploredBookName = mostExploredBookName,
                isLoading = false
            )
        }
            .onEach { state -> _uiState.update { state } }
            .catch { e ->
                Timber.e(e, "Error loading insights")
                _uiState.update { it.copy(errorMessage = e.message, isLoading = false) }
            }
            .launchIn(viewModelScope)
    }
}