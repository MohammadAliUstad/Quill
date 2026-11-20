package com.yugentech.quill.library.viewmodel

import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.cancel
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Job
import kotlinx.coroutines.CoroutineScope
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yugentech.quill.category.repository.CategoryRepository
import com.yugentech.quill.database.entity.CategoryEntity
import com.yugentech.quill.database.view.LibraryBookView
import com.yugentech.quill.library.repository.LibraryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LibraryViewModel(
    private val libraryRepository: LibraryRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    // The sections start as soon as the Library opens and stay loaded while the app runs
    // (Eagerly, not WhileSubscribed): leaving the tab and coming back shows them as they were,
    // instead of starting from empty lists and filling in again.
    val userCategories: StateFlow<List<CategoryEntity>> = categoryRepository.getUserCategories()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // The main sections' queries, each run once and shared: the sections below show them, and
    // sectionsLoaded() waits for their first answers, without querying the database twice
    private val lastReadSource = libraryRepository.getLastReadBook()
        .shareIn(viewModelScope, SharingStarted.Eagerly, replay = 1)
    private val historySource = libraryRepository.getReadingHistory()
        .shareIn(viewModelScope, SharingStarted.Eagerly, replay = 1)
    private val favoritesSource = libraryRepository.getFavoriteBooks()
        .shareIn(viewModelScope, SharingStarted.Eagerly, replay = 1)
    private val shelfSource = libraryRepository.getBookShelf()
        .shareIn(viewModelScope, SharingStarted.Eagerly, replay = 1)

    val lastReadBook: StateFlow<LibraryBookView?> = lastReadSource
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val historyBooks: StateFlow<List<LibraryBookView>> = historySource
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val allHistoryBooks: StateFlow<List<LibraryBookView>> =
        libraryRepository.getCompleteReadingHistory()
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val favoriteBooks: StateFlow<List<LibraryBookView>> = favoritesSource
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val bookShelf: StateFlow<List<LibraryBookView>> = shelfSource
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /**
     * What the Library shows. [LibraryState.Loading] only until the database has answered
     * whether it has books (and, when it has, until the main sections' first rows are in, so
     * they don't pop in a moment after the layout). Then [LibraryState.Empty] or
     * [LibraryState.Content], following the books from there on.
     */
    val libraryState: StateFlow<LibraryState> = combine(
        libraryRepository.hasLibraryBooks(),
        sectionsLoaded()
    ) { hasBooks, loaded ->
        when {
            !hasBooks -> LibraryState.Empty
            loaded -> LibraryState.Content
            else -> LibraryState.Loading
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, LibraryState.Loading)

    /** Still deciding: the Library isn't yet known to be empty or not. */
    val isInitializing: StateFlow<Boolean> = libraryState
        .map { it == LibraryState.Loading }
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    /** False until each main section's query has returned once, then true. */
    private fun sectionsLoaded(): Flow<Boolean> = flow {
        emit(false)
        combine(lastReadSource, historySource, favoritesSource, shelfSource) { _, _, _, _ -> true }.first()
        emit(true)
    }

    /**
     * One flow per category, kept, so a category row doesn't start empty each time it's shown.
     * Each runs in its own scope, stopped when its category is renamed or deleted, so old names
     * don't keep querying the database.
     */
    private val categoryBooks = mutableMapOf<String, CategoryFlow>()

    private class CategoryFlow(val scope: CoroutineScope, val books: StateFlow<List<LibraryBookView>>)

    fun getBooksForCategory(category: String): StateFlow<List<LibraryBookView>> =
        categoryBooks.getOrPut(category) {
            val scope = CoroutineScope(viewModelScope.coroutineContext + SupervisorJob(viewModelScope.coroutineContext[Job]))
            CategoryFlow(
                scope,
                libraryRepository.getBooksByCategory(category).stateIn(scope, SharingStarted.Eagerly, emptyList())
            )
        }.books

    init {
        viewModelScope.launch {
            userCategories.collect { categories ->
                val current = categories.mapTo(HashSet()) { it.name }
                categoryBooks.keys.filter { it !in current }.forEach { gone ->
                    categoryBooks.remove(gone)?.scope?.cancel()
                }
            }
        }
    }

    fun initializeDefaultCategories() {
        viewModelScope.launch {
            if (categoryRepository.getCategoryCount() == 0) {
                categoryRepository.initializeDefaultCategories()
            }
        }
    }
}

/** What the Library screen shows. */
enum class LibraryState { Loading, Empty, Content }
