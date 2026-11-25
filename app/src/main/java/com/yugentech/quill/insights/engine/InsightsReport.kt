package com.yugentech.quill.insights.engine

import com.yugentech.quill.localization.UiText
import java.time.LocalDate

// Everything the Insights screen draws, worked out by InsightsEngine from what Quill stores:
// reading sessions, the library, highlights, and Aira's chats and visuals.

/** How an insight reads: something going well, something to know, or something to work on. */
enum class InsightTone { GOOD, NEUTRAL, NUDGE }

/** The picture an insight carries; the screen turns it into an icon on an expressive shape. */
enum class InsightIcon { STREAK, TREND_UP, TREND_DOWN, CLOCK, CALENDAR, SPEED, DEEP, FINISH, BOOKMARK, MILESTONE, HIGHLIGHT, SEEDLING }

/** One generated observation: a short headline and a sentence on what it means or what to try. */
data class Insight(
    val icon: InsightIcon,
    val tone: InsightTone,
    val title: UiText,
    val body: UiText,
    /** Higher shows first; only the most useful few are shown. */
    val priority: Int
)

/** One of the last seven days: how long was read. */
data class DayActivity(val date: LocalDate, val millis: Long, val isToday: Boolean)

enum class PartOfDay { MORNING, AFTERNOON, EVENING, NIGHT }

/** How much reading happens in one part of the day. */
data class PartOfDayStat(val part: PartOfDay, val millis: Long, val sessions: Int)

/** The reading records so far. */
data class ReadingRecords(
    val longestSessionMillis: Long = 0,
    val averageSessionMillis: Long = 0,
    val bestDayMillis: Long = 0,
    val bestDay: LocalDate? = null,
    val bestStreak: Int = 0
)

/** A book and the time spent reading it. */
data class BookTime(
    val id: String,
    val title: String,
    val author: String,
    val coverUrl: String?,
    val millis: Long,
    val progress: Float
)

/** An author, the time spent with their books, and how many are in the library. */
data class AuthorStat(val name: String, val millis: Long, val books: Int)

/** Where the books in the library stand. */
data class LibraryStats(val notStarted: Int = 0, val inProgress: Int = 0, val finished: Int = 0) {
    val total: Int get() = notStarted + inProgress + finished
}

/** Highlights across the library. */
data class HighlightStats(
    val total: Int = 0,
    /** Highlights made in the last 30 days, for the "highlighting a lot lately" insight. */
    val thisMonth: Int = 0,
    val mostHighlightedTitle: String? = null,
    val mostHighlightedCount: Int = 0,
    /** Highlight colours (ARGB) and how often each is used, most used first. */
    val colors: List<Pair<Int, Int>> = emptyList()
)

/** Reading with Aira: questions asked, the book explored most, and visuals created. */
data class AiraStats(
    val questions: Int = 0,
    val mostExploredTitle: String? = null,
    val visuals: Int = 0
)

data class InsightsReport(
    val totalReadMillis: Long = 0,
    val streak: Int = 0,
    val finishedBooks: Int = 0,
    val week: List<DayActivity> = emptyList(),
    val millisThisWeek: Long = 0,
    val millisLastWeek: Long = 0,
    val daysThisWeek: Int = 0,
    val insights: List<Insight> = emptyList(),
    val partsOfDay: List<PartOfDayStat> = emptyList(),
    /** Heatmap intensity (1–5) for each day with reading. */
    val heatmap: Map<LocalDate, Int> = emptyMap(),
    /** Time read on each day with reading, for the heatmap's day chip. */
    val readByDay: Map<LocalDate, Long> = emptyMap(),
    val records: ReadingRecords = ReadingRecords(),
    val topBooks: List<BookTime> = emptyList(),
    val topAuthors: List<AuthorStat> = emptyList(),
    val library: LibraryStats = LibraryStats(),
    val highlights: HighlightStats = HighlightStats(),
    val aira: AiraStats = AiraStats(),
    val sessionCount: Int = 0
)
