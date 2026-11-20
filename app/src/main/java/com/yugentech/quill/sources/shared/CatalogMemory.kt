package com.yugentech.quill.sources.shared

import com.yugentech.quill.database.model.Book
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onEach
import java.util.concurrent.ConcurrentHashMap

/**
 * The cached lists last read from the database, kept in memory for as long as the app runs, so
 * reopening a source screen shows its books in the very first frame.
 *
 * Reading the database takes a moment, and the screen's view model is new on each visit, so
 * without this every open started blank and the books faded in a beat later, even though they
 * were cached all along. A list remembered here is handed over straight away, before the
 * database answers; the database's own list follows and takes over if it differs.
 */
class CatalogMemory {

    private val lists = ConcurrentHashMap<String, List<Book>>()

    /**
     * [database]'s list for [key], led by the copy remembered from last time (if any). The copy
     * is emitted without suspending, so a view model collecting this on the main thread has its
     * books before its screen first draws.
     */
    fun follow(key: String, database: Flow<List<Book>>): Flow<List<Book>> = flow {
        lists[key]?.let { emit(it) }
        emitAll(database.onEach { remember(key, it) })
    }.distinctUntilChanged()

    /**
     * Records [books] as [key]'s list, as soon as they're written, so a read straight after a
     * refresh gets the new list rather than the one from before it. An empty list isn't kept:
     * it would only stand in for the database's answer.
     */
    fun remember(key: String, books: List<Book>) {
        if (books.isNotEmpty()) lists[key] = books
    }
}
