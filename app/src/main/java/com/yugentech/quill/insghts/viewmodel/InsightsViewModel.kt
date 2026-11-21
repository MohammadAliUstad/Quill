package com.yugentech.quill.insghts.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yugentech.quill.database.dao.BookQuestionCount
import com.yugentech.quill.database.entity.BookEntity
import com.yugentech.quill.database.entity.HighlightEntity
import com.yugentech.quill.database.entity.ReadingSessionEntity
import com.yugentech.quill.insghts.engine.InsightsEngine
import com.yugentech.quill.insghts.repository.InsightsRepository
import com.yugentech.quill.insghts.state.InsightsUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import timber.log.Timber
import java.time.ZoneId

class InsightsViewModel(
    private val insightsRepository: InsightsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(InsightsUiState())
    val uiState: StateFlow<InsightsUiState> = _uiState.asStateFlow()

    init {
        loadInsights()
    }

    // Everything Quill keeps about the reader, worked into one report whenever any of it changes
    private fun loadInsights() {
        val readingFlow = combine(
            insightsRepository.getAllSessionsFlow(),
            insightsRepository.getStreakFlow(),
            insightsRepository.getAllBooksFlow(),
            insightsRepository.getAllHighlightsFlow()
        ) { sessions, streak, books, highlights -> ReadingData(sessions, streak, books, highlights) }

        val airaFlow = combine(
            insightsRepository.getVisualsCountFlow(),
            insightsRepository.getTotalUserQuestionsFlow(),
            insightsRepository.getQuestionsPerBookFlow()
        ) { visuals, questions, perBook -> AiraData(visuals, questions, perBook) }

        combine(readingFlow, airaFlow) { reading, aira ->
            InsightsEngine.analyze(
                now = System.currentTimeMillis(),
                zone = ZoneId.systemDefault(),
                sessions = reading.sessions,
                books = reading.books,
                highlights = reading.highlights,
                streak = reading.streak,
                visuals = aira.visuals,
                totalQuestions = aira.questions,
                questionsPerBook = aira.perBook
            )
        }
            .flowOn(Dispatchers.Default)
            .onEach { report -> _uiState.update { InsightsUiState(report = report, isLoading = false) } }
            .catch { e ->
                Timber.e(e, "Error loading insights")
                _uiState.update { it.copy(errorMessage = e.message, isLoading = false) }
            }
            .launchIn(viewModelScope)
    }

    private data class ReadingData(
        val sessions: List<ReadingSessionEntity>,
        val streak: Int,
        val books: List<BookEntity>,
        val highlights: List<HighlightEntity>
    )

    private data class AiraData(
        val visuals: Int,
        val questions: Int,
        val perBook: List<BookQuestionCount>
    )
}
