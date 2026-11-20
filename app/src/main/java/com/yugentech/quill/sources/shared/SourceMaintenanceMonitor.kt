package com.yugentech.quill.sources.shared

import kotlinx.coroutines.launch
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import com.yugentech.quill.sources.standard.repository.StandardRepository
import android.os.SystemClock
import com.yugentech.quill.database.model.BookSource
import com.yugentech.quill.sources.gutenberg.repository.GutenbergRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.concurrent.ConcurrentHashMap

/**
 * Which book sources are down for maintenance right now, for the whole app: the Sources card and
 * a book's download show it from here, so they never disagree.
 *
 * Each online source is asked directly, by a request to its own site ([check], [checkIfDue]):
 * marked down when it answers for maintenance, and up again by the next check that finds it back.
 * Asking the site, not waiting for a load to fail, matters for Gutenberg, whose catalogue comes
 * from Gutendex, which stays up while gutenberg.org is down.
 *
 * A check that gets no answer at all (no network yet as the app starts, a timeout) says nothing
 * either way: it changes nothing, doesn't count as a check, and [checkIfDue] tries again shortly.
 */
class SourceMaintenanceMonitor(
    private val gutenbergRepository: GutenbergRepository,
    private val standardRepository: StandardRepository
) {
    private val _down = MutableStateFlow<Map<BookSource, ServerMaintenanceException>>(emptyMap())
    val down: StateFlow<Map<BookSource, ServerMaintenanceException>> = _down.asStateFlow()

    private val lastChecked = ConcurrentHashMap<BookSource, Long>()

    // One check of a source at a time: screens opening together wait for it rather than repeat it
    private val locks = BookSource.entries.associateWith { Mutex() }

    /**
     * Asks [source] now whether it's down; returns its maintenance, or null if it's up. If it
     * couldn't be asked, nothing changes and what's already known is returned. A source with no
     * site to ask (a book the user imported) is never down.
     */
    suspend fun check(source: BookSource): ServerMaintenanceException? = ask(source).getOrElse { _down.value[source] }

    /**
     * [check], unless [source] was answered for in the last minute: screens opening one after
     * another share it. When the site can't be reached, it's asked again a few times, a little
     * longer apart each time, so a check made before the network was ready still gets its answer.
     */
    suspend fun checkIfDue(source: BookSource) {
        if (source !in ONLINE_SOURCES) return
        locks.getValue(source).withLock {
            for (wait in RETRY_AFTER_MILLIS) {
                val last = lastChecked[source]
                if (last != null && SystemClock.elapsedRealtime() - last <= RECHECK_AFTER_MILLIS) return
                if (ask(source).isSuccess) return
                delay(wait)
            }
            ask(source)
        }
    }

    /** [checkIfDue] for every online source at once. */
    suspend fun checkAllIfDue() = coroutineScope {
        ONLINE_SOURCES.forEach { launch { checkIfDue(it) } }
    }

    /** One request to [source]'s site; only an answer (down or up) is recorded. */
    private suspend fun ask(source: BookSource): Result<ServerMaintenanceException?> {
        val result = when (source) {
            BookSource.GUTENBERG -> gutenbergRepository.checkMaintenance()
            BookSource.STANDARD_EBOOKS -> standardRepository.checkMaintenance()
            BookSource.USER_IMPORTED -> return Result.success(null)
        }
        result.onSuccess { found ->
            lastChecked[source] = SystemClock.elapsedRealtime()
            _down.update { if (found != null) it + (source to found) else it - source }
        }
        return result
    }

    companion object {
        /** The sources with a site to be down: every one but the user's own imports. */
        val ONLINE_SOURCES = listOf(BookSource.STANDARD_EBOOKS, BookSource.GUTENBERG)

        private const val RECHECK_AFTER_MILLIS = 60_000L

        // Between tries when a site can't be reached: four more tries over about a minute
        private val RETRY_AFTER_MILLIS = listOf(3_000L, 8_000L, 15_000L, 30_000L)
    }
}
