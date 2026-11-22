package com.yugentech.quill.insights

import com.yugentech.quill.R
import com.yugentech.quill.database.dao.BookQuestionCount
import com.yugentech.quill.database.entity.BookEntity
import com.yugentech.quill.database.entity.HighlightEntity
import com.yugentech.quill.database.entity.ReadingSessionEntity
import com.yugentech.quill.database.model.BookSource
import com.yugentech.quill.insights.engine.InsightIcon
import com.yugentech.quill.insights.engine.InsightsEngine
import com.yugentech.quill.insights.engine.InsightsReport
import com.yugentech.quill.insights.engine.LibraryStats
import com.yugentech.quill.localization.UiText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

class InsightsEngineTest {

    private val zone = ZoneOffset.UTC
    private val today = LocalDate.of(2026, 11, 21)
    private val now = at(today, hour = 20)

    private fun at(date: LocalDate, hour: Int) = date.atTime(hour, 0).toInstant(zone).toEpochMilli()

    private fun minutes(n: Long) = n * 60_000L

    private fun session(daysAgo: Long, minutes: Long, hour: Int = 21, bookId: String = "a") = ReadingSessionEntity(
        bookId = bookId,
        startTime = at(today.minusDays(daysAgo), hour),
        endTime = at(today.minusDays(daysAgo), hour) + minutes(minutes),
        durationMillis = minutes(minutes)
    )

    private fun book(id: String, progress: Float = 0f, lastReadDaysAgo: Long? = null, author: String = "Austen") = BookEntity(
        id = id,
        title = "Book $id",
        author = author,
        coverUrl = null,
        progressPercent = progress,
        lastReadTime = lastReadDaysAgo?.let { at(today.minusDays(it), 21) } ?: 0L,
        downloadUrl = "",
        source = BookSource.USER_IMPORTED
    )

    private fun analyze(
        sessions: List<ReadingSessionEntity> = emptyList(),
        books: List<BookEntity> = listOf(book("a")),
        highlights: List<HighlightEntity> = emptyList(),
        streak: Int = 0,
        questionsPerBook: List<BookQuestionCount> = emptyList()
    ): InsightsReport = InsightsEngine.analyze(
        now = now,
        zone = zone,
        sessions = sessions,
        books = books,
        highlights = highlights,
        streak = streak,
        visuals = 0,
        totalQuestions = questionsPerBook.sumOf { it.count },
        questionsPerBook = questionsPerBook
    )

    private fun InsightsReport.insight(icon: InsightIcon) = insights.firstOrNull { it.icon == icon }

    private fun UiText.id() = when (this) {
        is UiText.Res -> id
        is UiText.Plural -> id
        is UiText.Raw -> 0
    }

    @Test
    fun `no reading shows only the seedling`() {
        val report = analyze()
        assertEquals(1, report.insights.size)
        assertEquals(InsightIcon.SEEDLING, report.insights.single().icon)
        assertEquals(0L, report.totalReadMillis)
        assertEquals(7, report.week.size)
        assertTrue(report.week.last().isToday)
    }

    @Test
    fun `this week and last week are summed separately`() {
        val report = analyze(
            sessions = listOf(
                session(0, 30), session(2, 20), session(6, 10), // this week: the last 7 days
                session(7, 40), session(13, 5), // last week
                session(14, 99) // older
            )
        )
        assertEquals(minutes(60), report.millisThisWeek)
        assertEquals(minutes(45), report.millisLastWeek)
        assertEquals(3, report.daysThisWeek)
        assertEquals(minutes(30), report.week.last().millis)
    }

    @Test
    fun `zero-length sessions are ignored`() {
        val report = analyze(sessions = listOf(session(0, 0), session(1, 10)))
        assertEquals(1, report.sessionCount)
        assertEquals(1, report.daysThisWeek)
    }

    @Test
    fun `a current run that is the longest is a record`() {
        val sessions = (0L..4L).map { session(it, 15) } + session(20, 15) + session(21, 15)
        val report = analyze(sessions = sessions, streak = 5)
        assertEquals(5, report.records.bestStreak)
        val record = report.insight(InsightIcon.MILESTONE)
        assertNotNull(record)
        assertEquals(R.string.insights_record_title, record!!.title.id())
    }

    @Test
    fun `a streak not yet read today is a nudge, ahead of everything else`() {
        val sessions = (1L..4L).map { session(it, 15) }
        val report = analyze(sessions = sessions, streak = 4)
        assertEquals(R.string.insights_streak_risk_title, report.insights.first().title.id())
    }

    @Test
    fun `close to the record says how many days are left`() {
        // Best run of 6 earlier; now 4 days running including today
        val sessions = (30L..35L).map { session(it, 15) } + (0L..3L).map { session(it, 15) }
        val report = analyze(sessions = sessions, streak = 4)
        val near = report.insights.firstOrNull { it.title.id() == R.plurals.insights_near_record_title }
        assertNotNull(near)
        assertEquals(2, (near!!.title as UiText.Plural).count)
    }

    @Test
    fun `the finish forecast carries the pace on to the end`() {
        // A quarter read in an hour: three more hours to go
        val report = analyze(
            sessions = listOf(session(0, 60)),
            books = listOf(book("a", progress = 0.25f, lastReadDaysAgo = 0))
        )
        val finish = report.insight(InsightIcon.FINISH)
        assertNotNull(finish)
        val title = finish!!.title as UiText.Res
        assertEquals(UiText.Res(R.string.duration_hours_minutes, 3, 0), title.args[0])
        assertEquals("Book a", title.args[1])
    }

    @Test
    fun `no forecast without enough reading of the book`() {
        val report = analyze(
            sessions = listOf(session(0, 10)),
            books = listOf(book("a", progress = 0.25f, lastReadDaysAgo = 0))
        )
        assertNull(report.insight(InsightIcon.FINISH))
    }

    @Test
    fun `library brackets don't overlap`() {
        val report = analyze(
            sessions = listOf(session(0, 5)),
            books = listOf(book("a", 0f), book("b", 0.5f), book("c", 0.95f), book("d", 1f))
        )
        assertEquals(LibraryStats(notStarted = 1, inProgress = 1, finished = 2), report.library)
        assertEquals(2, report.finishedBooks)
    }

    @Test
    fun `a part-read book left for weeks is waiting`() {
        val report = analyze(
            sessions = listOf(session(0, 5)),
            books = listOf(book("a", 0.6f, lastReadDaysAgo = 30), book("b", 0.2f, lastReadDaysAgo = 25))
        )
        val waiting = report.insight(InsightIcon.BOOKMARK)
        assertNotNull(waiting)
        assertEquals(UiText.Res(R.string.insights_waiting_title, "Book a"), waiting!!.title)
    }

    @Test
    fun `heatmap shades by time read that day`() {
        val report = analyze(sessions = listOf(session(0, 5), session(1, 30), session(2, 50), session(2, 10)))
        assertEquals(1, report.heatmap[today])
        assertEquals(3, report.heatmap[today.minusDays(1)])
        assertEquals(5, report.heatmap[today.minusDays(2)])
        assertEquals(minutes(60), report.readByDay[today.minusDays(2)])
    }

    @Test
    fun `parts of day follow the session start hour`() {
        val report = analyze(
            sessions = listOf(session(0, 10, hour = 7), session(1, 20, hour = 13), session(2, 30, hour = 19), session(3, 40, hour = 2))
        )
        assertEquals(listOf(minutes(10), minutes(20), minutes(30), minutes(40)), report.partsOfDay.map { it.millis })
    }

    @Test
    fun `authors rank by time spent, then by books kept`() {
        val report = analyze(
            sessions = listOf(session(0, 30, bookId = "b")),
            books = listOf(book("a", author = "Austen"), book("c", author = "Austen"), book("b", author = "Brontë"))
        )
        assertEquals(listOf("Brontë", "Austen"), report.topAuthors.map { it.name })
        assertEquals(2, report.topAuthors[1].books)
    }

    @Test
    fun `at most five insights, highest priority first`() {
        val sessions = (1L..8L).map { session(it, 5, hour = 21) }
        val report = analyze(
            sessions = sessions,
            books = listOf(book("a", 0.3f, lastReadDaysAgo = 1)) + (1..6).map { book("n$it") } + book("w", 0.5f, lastReadDaysAgo = 40),
            streak = 8
        )
        assertTrue(report.insights.size <= 5)
        assertEquals(report.insights.sortedByDescending { it.priority }, report.insights)
    }
}
