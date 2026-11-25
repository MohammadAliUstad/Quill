package com.yugentech.quill.insights.engine

import com.yugentech.quill.R
import com.yugentech.quill.database.dao.BookQuestionCount
import com.yugentech.quill.database.entity.BookEntity
import com.yugentech.quill.database.entity.HighlightEntity
import com.yugentech.quill.database.entity.ReadingSessionEntity
import com.yugentech.quill.localization.UiText
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

// Every number and every generated insight here is a fact read from the reader's own records.
// The thresholds that decide when an insight is worth showing are the constants at the bottom,
// so a single short session never turns into advice.

object InsightsEngine {

    fun analyze(
        now: Long,
        zone: ZoneId,
        sessions: List<ReadingSessionEntity>,
        books: List<BookEntity>,
        highlights: List<HighlightEntity>,
        streak: Int,
        visuals: Int,
        totalQuestions: Int,
        questionsPerBook: List<BookQuestionCount>
    ): InsightsReport {
        val read = sessions.filter { it.durationMillis > 0 }
        val today = now.toDate(zone)
        val booksById = books.associateBy { it.id }

        val readByDay = read.groupBy { it.startTime.toDate(zone) }
            .mapValues { (_, day) -> day.sumOf { it.durationMillis } }

        val week = (6 downTo 0).map { back ->
            val date = today.minusDays(back.toLong())
            DayActivity(date, readByDay[date] ?: 0L, isToday = back == 0)
        }
        val millisLastWeek = (7..13).sumOf { readByDay[today.minusDays(it.toLong())] ?: 0L }

        val millisByBook = read.groupBy { it.bookId }.mapValues { (_, s) -> s.sumOf { it.durationMillis } }
        val library = LibraryStats(
            notStarted = books.count { it.progressPercent <= 0f },
            inProgress = books.count { it.progressPercent > 0f && it.progressPercent < FINISHED },
            finished = books.count { it.progressPercent >= FINISHED }
        )

        val report = InsightsReport(
            totalReadMillis = read.sumOf { it.durationMillis },
            streak = streak,
            finishedBooks = library.finished,
            week = week,
            millisThisWeek = week.sumOf { it.millis },
            millisLastWeek = millisLastWeek,
            daysThisWeek = week.count { it.millis > 0 },
            partsOfDay = partsOfDay(read, zone),
            heatmap = readByDay.mapValues { (_, millis) -> intensity(millis) },
            readByDay = readByDay,
            records = records(read, readByDay),
            topBooks = millisByBook.entries
                .sortedByDescending { it.value }
                .mapNotNull { (id, millis) ->
                    booksById[id]?.let { BookTime(id, it.title, it.author, it.coverUrl, millis, it.progressPercent) }
                }
                .take(TOP_BOOKS),
            topAuthors = topAuthors(books, millisByBook),
            library = library,
            highlights = highlightStats(highlights, booksById, now),
            aira = AiraStats(
                questions = totalQuestions,
                mostExploredTitle = questionsPerBook.filter { it.count > 0 }.maxByOrNull { it.count }
                    ?.let { booksById[it.bookId]?.title },
                visuals = visuals
            ),
            sessionCount = read.size
        )
        return report.copy(insights = insights(report, read, books, millisByBook, highlights, now, today, zone))
    }

    // Generated insights
    private fun insights(
        report: InsightsReport,
        read: List<ReadingSessionEntity>,
        books: List<BookEntity>,
        millisByBook: Map<String, Long>,
        highlights: List<HighlightEntity>,
        now: Long,
        today: LocalDate,
        zone: ZoneId
    ): List<Insight> {
        if (read.isEmpty()) {
            return listOf(
                Insight(
                    InsightIcon.SEEDLING, InsightTone.NEUTRAL,
                    UiText.Res(R.string.insights_seed_title),
                    UiText.Res(R.string.insights_seed_body),
                    0
                )
            )
        }
        return listOfNotNull(
            week(report, read, today, zone),
            streak(report),
            bestTime(report),
            sessionStyle(report),
            finishForecast(books, millisByBook),
            waitingBook(books, today, zone),
            hoursMilestone(report.totalReadMillis),
            highlightHabit(report.highlights)
        )
            .sortedByDescending { it.priority }
            .take(MAX_INSIGHTS)
    }

    /** How this week compares: quiet, steady, or more or less than last week. */
    private fun week(report: InsightsReport, read: List<ReadingSessionEntity>, today: LocalDate, zone: ZoneId): Insight? {
        val days = report.daysThisWeek
        val thisWeek = report.millisThisWeek
        val lastWeek = report.millisLastWeek
        return when {
            days == 0 -> {
                val lastRead = read.maxOf { it.startTime }.toDate(zone)
                val daysAgo = ChronoUnit.DAYS.between(lastRead, today).toInt()
                Insight(
                    InsightIcon.CALENDAR, InsightTone.NUDGE,
                    UiText.Res(R.string.insights_quiet_title),
                    UiText.Res(R.string.insights_quiet_body, UiText.Plural(R.plurals.days_ago, daysAgo, daysAgo)),
                    90
                )
            }

            days >= STEADY_DAYS -> Insight(
                InsightIcon.STREAK, InsightTone.GOOD,
                UiText.Res(R.string.insights_steady_title, days),
                UiText.Res(R.string.insights_steady_body),
                70
            )

            lastWeek > 0 && thisWeek > lastWeek && days >= 2 -> Insight(
                InsightIcon.TREND_UP, InsightTone.GOOD,
                UiText.Res(R.string.insights_up_title),
                UiText.Res(R.string.insights_up_body, duration(thisWeek), duration(lastWeek)),
                55
            )

            lastWeek > 0 && thisWeek * 2 < lastWeek -> Insight(
                InsightIcon.TREND_DOWN, InsightTone.NUDGE,
                UiText.Res(R.string.insights_down_title),
                UiText.Res(R.string.insights_down_body, duration(thisWeek), duration(lastWeek)),
                50
            )

            else -> null
        }
    }

    /** The streak: at risk today, a new record, or close to one. */
    private fun streak(report: InsightsReport): Insight? {
        val streak = report.streak
        val best = report.records.bestStreak
        val readToday = report.week.lastOrNull()?.let { it.millis > 0 } == true
        return when {
            streak >= MIN_STREAK && !readToday -> Insight(
                InsightIcon.STREAK, InsightTone.NUDGE,
                UiText.Res(R.string.insights_streak_risk_title),
                UiText.Plural(R.plurals.insights_streak_risk_body, streak, streak),
                85
            )

            streak >= MIN_STREAK && streak >= best -> Insight(
                InsightIcon.MILESTONE, InsightTone.GOOD,
                UiText.Res(R.string.insights_record_title),
                UiText.Plural(R.plurals.insights_record_body, streak, streak, streak + 1),
                80
            )

            streak > 0 && best >= MIN_STREAK && best - streak in 1..NEAR_RECORD -> Insight(
                InsightIcon.STREAK, InsightTone.NEUTRAL,
                UiText.Plural(R.plurals.insights_near_record_title, best - streak, best - streak),
                UiText.Plural(R.plurals.insights_near_record_body, best, best),
                65
            )

            else -> null
        }
    }

    /** When most of the reading happens, once there's enough of it to say. */
    private fun bestTime(report: InsightsReport): Insight? {
        if (report.sessionCount < MIN_SESSIONS) return null
        val total = report.partsOfDay.sumOf { it.millis }.takeIf { it > 0 } ?: return null
        val top = report.partsOfDay.maxBy { it.millis }
        val percent = (top.millis * 100 / total).toInt()
        if (percent < MIN_PART_SHARE) return null
        val (title, body) = when (top.part) {
            PartOfDay.MORNING -> R.string.peak_morning to R.string.insights_time_morning
            PartOfDay.AFTERNOON -> R.string.peak_afternoon to R.string.insights_time_afternoon
            PartOfDay.EVENING -> R.string.peak_evening to R.string.insights_time_evening
            PartOfDay.NIGHT -> R.string.peak_night to R.string.insights_time_night
        }
        return Insight(InsightIcon.CLOCK, InsightTone.NEUTRAL, UiText.Res(title), UiText.Res(body, percent), 40)
    }

    /** Short bursts or long sittings, from the average session. */
    private fun sessionStyle(report: InsightsReport): Insight? {
        if (report.sessionCount < MIN_SESSIONS) return null
        val average = report.records.averageSessionMillis
        return when {
            average < SHORT_SESSION -> Insight(
                InsightIcon.SPEED, InsightTone.NEUTRAL,
                UiText.Res(R.string.insights_short_title),
                UiText.Res(R.string.insights_short_body, duration(average)),
                30
            )

            average >= LONG_SESSION -> Insight(
                InsightIcon.DEEP, InsightTone.GOOD,
                UiText.Res(R.string.insights_long_title),
                UiText.Res(R.string.insights_long_body, duration(average)),
                30
            )

            else -> null
        }
    }

    /**
     * How much reading is left in the book being read now, at the pace it's been read so far:
     * the progress made per minute read, carried on to the end.
     */
    private fun finishForecast(books: List<BookEntity>, millisByBook: Map<String, Long>): Insight? {
        val current = books
            .filter { it.progressPercent in MIN_FORECAST_PROGRESS..<FINISHED && it.lastReadTime > 0 }
            .maxByOrNull { it.lastReadTime } ?: return null
        val millis = millisByBook[current.id] ?: return null
        if (millis < MIN_FORECAST_READ) return null
        val remaining = ((1f - current.progressPercent) / current.progressPercent * millis).toLong()
        if (remaining < MINUTE) return null
        return Insight(
            InsightIcon.FINISH, InsightTone.GOOD,
            UiText.Res(R.string.insights_finish_title, duration(remaining), current.title),
            UiText.Res(R.string.insights_finish_body),
            60
        )
    }

    /** A book left part-read for a while, the furthest along first. */
    private fun waitingBook(books: List<BookEntity>, today: LocalDate, zone: ZoneId): Insight? {
        val waiting = books
            .filter { it.progressPercent > 0f && it.progressPercent < FINISHED && it.lastReadTime > 0 }
            .map { it to ChronoUnit.DAYS.between(it.lastReadTime.toDate(zone), today).toInt() }
            .filter { (_, daysAgo) -> daysAgo >= WAITING_DAYS }
            .maxByOrNull { (book, _) -> book.progressPercent } ?: return null
        val (book, daysAgo) = waiting
        return Insight(
            InsightIcon.BOOKMARK, InsightTone.NUDGE,
            UiText.Res(R.string.insights_waiting_title, book.title),
            UiText.Res(
                R.string.insights_waiting_body,
                (book.progressPercent * 100).toInt(),
                UiText.Plural(R.plurals.days_ago, daysAgo, daysAgo)
            ),
            45
        )
    }

    /** The last round number of hours passed. */
    private fun hoursMilestone(totalMillis: Long): Insight? {
        val hours = (totalMillis / HOUR).toInt()
        val milestone = HOUR_MILESTONES.lastOrNull { hours >= it } ?: return null
        return Insight(
            InsightIcon.MILESTONE, InsightTone.GOOD,
            UiText.Plural(R.plurals.insights_hours_title, milestone, milestone),
            UiText.Res(R.string.insights_hours_body),
            35
        )
    }

    /** Highlighting a lot lately, and the book highlighted most. */
    private fun highlightHabit(stats: HighlightStats): Insight? {
        if (stats.thisMonth < ACTIVE_HIGHLIGHTS || stats.mostHighlightedTitle == null) return null
        return Insight(
            InsightIcon.HIGHLIGHT, InsightTone.GOOD,
            UiText.Plural(R.plurals.insights_highlights_title, stats.thisMonth, stats.thisMonth),
            UiText.Res(R.string.insights_highlights_body, stats.mostHighlightedTitle),
            25
        )
    }

    // Detail
    private fun partsOfDay(read: List<ReadingSessionEntity>, zone: ZoneId): List<PartOfDayStat> {
        val byPart = read.groupBy { partOf(Instant.ofEpochMilli(it.startTime).atZone(zone).hour) }
        return PartOfDay.entries.map { part ->
            val sessions = byPart[part].orEmpty()
            PartOfDayStat(part, sessions.sumOf { it.durationMillis }, sessions.size)
        }
    }

    /** The same hours as the peak-reader lines: morning 5–11, afternoon 12–16, evening 17–20. */
    private fun partOf(hour: Int): PartOfDay = when (hour) {
        in 5..11 -> PartOfDay.MORNING
        in 12..16 -> PartOfDay.AFTERNOON
        in 17..20 -> PartOfDay.EVENING
        else -> PartOfDay.NIGHT
    }

    private fun records(read: List<ReadingSessionEntity>, readByDay: Map<LocalDate, Long>): ReadingRecords {
        if (read.isEmpty()) return ReadingRecords()
        val bestDay = readByDay.maxBy { it.value }
        return ReadingRecords(
            longestSessionMillis = read.maxOf { it.durationMillis },
            averageSessionMillis = read.sumOf { it.durationMillis } / read.size,
            bestDayMillis = bestDay.value,
            bestDay = bestDay.key,
            bestStreak = longestRun(readByDay.keys)
        )
    }

    /** The longest run of consecutive days with reading. */
    private fun longestRun(days: Set<LocalDate>): Int {
        var best = 0
        var run = 0
        var previous: LocalDate? = null
        for (day in days.sorted()) {
            run = if (previous != null && previous.plusDays(1) == day) run + 1 else 1
            best = maxOf(best, run)
            previous = day
        }
        return best
    }

    /** Authors by time spent with their books, then by how many of their books are kept. */
    private fun topAuthors(books: List<BookEntity>, millisByBook: Map<String, Long>): List<AuthorStat> =
        books.filter { it.author.isNotBlank() }
            .groupBy { it.author.trim() }
            .map { (author, theirs) -> AuthorStat(author, theirs.sumOf { millisByBook[it.id] ?: 0L }, theirs.size) }
            .sortedWith(compareByDescending<AuthorStat> { it.millis }.thenByDescending { it.books })
            .take(TOP_AUTHORS)

    private fun highlightStats(highlights: List<HighlightEntity>, booksById: Map<String, BookEntity>, now: Long): HighlightStats {
        if (highlights.isEmpty()) return HighlightStats()
        val mostHighlighted = highlights.groupingBy { it.bookId }.eachCount().maxBy { it.value }
        return HighlightStats(
            total = highlights.size,
            thisMonth = highlights.count { it.createdAt >= now - MONTH },
            mostHighlightedTitle = booksById[mostHighlighted.key]?.title,
            mostHighlightedCount = mostHighlighted.value,
            colors = highlights.groupingBy { it.colorInt }.eachCount().entries
                .sortedByDescending { it.value }
                .map { it.key to it.value }
                .take(MAX_COLORS)
        )
    }

    /** Heatmap shade for a day: a few minutes, a proper sitting, or a long read. */
    private fun intensity(millis: Long): Int = when {
        millis < 15 * MINUTE -> 1
        millis < 45 * MINUTE -> 3
        else -> 5
    }

    private fun Long.toDate(zone: ZoneId): LocalDate = Instant.ofEpochMilli(this).atZone(zone).toLocalDate()

    /** A length of time as "2h 15m" or "40m", in the app's language. */
    fun duration(millis: Long): UiText {
        val totalMinutes = (millis / MINUTE).toInt()
        val hours = totalMinutes / 60
        return if (hours > 0) UiText.Res(R.string.duration_hours_minutes, hours, totalMinutes % 60)
        else UiText.Res(R.string.duration_minutes, totalMinutes)
    }

    private const val MINUTE = 60_000L
    private const val HOUR = 60 * MINUTE
    private const val MONTH = 30 * 24 * HOUR

    /** The library counts a book as finished from 90% (as BookDao does). */
    private const val FINISHED = 0.9f

    private const val MAX_INSIGHTS = 5
    private const val TOP_BOOKS = 3
    private const val TOP_AUTHORS = 5
    private const val MAX_COLORS = 6

    private const val STEADY_DAYS = 6
    private const val MIN_STREAK = 3
    private const val NEAR_RECORD = 3
    private const val MIN_SESSIONS = 5
    private const val MIN_PART_SHARE = 50
    private const val SHORT_SESSION = 10 * MINUTE
    private const val LONG_SESSION = 45 * MINUTE
    private const val MIN_FORECAST_PROGRESS = 0.05f
    private const val MIN_FORECAST_READ = 20 * MINUTE
    private const val WAITING_DAYS = 21
    private val HOUR_MILESTONES = listOf(1, 5, 10, 25, 50, 100, 250, 500, 1000)
    private const val ACTIVE_HIGHLIGHTS = 5
}
