package com.yugentech.quill.ui.info.insights.heatmap

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

/** One day of the heatmap: its shade (0 for none, 1–5) and how long was read. */
data class HeatmapDay(
    val date: LocalDate,
    val intensity: Int,
    val readMillis: Long = 0L
)

/** A Monday-first column of the heatmap, labelled with the month that starts in it. */
data class HeatmapWeek(
    val days: List<HeatmapDay>,
    val firstDayOfMonth: String?
)

/** The last 52 weeks and this one, Monday first, ending today (as Sessions and Hajime lay it out). */
fun generateHeatmapData(
    intensities: Map<LocalDate, Int>,
    readByDay: Map<LocalDate, Long>,
    locale: Locale
): List<HeatmapWeek> {
    val endDate = LocalDate.now()
    val startDate = endDate.minusWeeks(52).minusDays(endDate.dayOfWeek.value.toLong() - 1)
    // "LLL" is the stand-alone month, so languages that inflect it read correctly on their own
    val monthFormat = DateTimeFormatter.ofPattern("LLL", locale)

    return (0..ChronoUnit.DAYS.between(startDate, endDate))
        .map { offset ->
            val date = startDate.plusDays(offset)
            HeatmapDay(date, intensities[date] ?: 0, readByDay[date] ?: 0L)
        }
        .chunked(7)
        .map { days ->
            HeatmapWeek(
                days = days,
                firstDayOfMonth = days.firstOrNull { it.date.dayOfMonth == 1 }?.date?.format(monthFormat)
            )
        }
}
