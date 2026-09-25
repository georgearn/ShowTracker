package com.georgearn.showtracker.util

import com.georgearn.showtracker.data.model.ReleaseStatus
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoUnit
import java.util.Locale

object DateUtils {
    private val ISO: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    private val MONTH_YEAR: DateTimeFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH)

    fun today(): LocalDate = LocalDate.now()

    fun todayIso(): String = today().format(ISO)

    fun isoDaysAgo(days: Long): String = today().minusDays(days).format(ISO)

    fun isoDaysAhead(days: Long): String = today().plusDays(days).format(ISO)

    fun parseOrNull(iso: String?): LocalDate? {
        if (iso.isNullOrBlank()) return null
        return try { LocalDate.parse(iso, ISO) } catch (e: DateTimeParseException) { null }
    }

    fun releaseStatus(iso: String?): ReleaseStatus {
        val date = parseOrNull(iso) ?: return ReleaseStatus.UNKNOWN
        return if (date.isAfter(today())) ReleaseStatus.UPCOMING else ReleaseStatus.RELEASED
    }

    fun daysUntil(iso: String?): Long? {
        val date = parseOrNull(iso) ?: return null
        val diff = ChronoUnit.DAYS.between(today(), date)
        return if (diff >= 0) diff else null
    }

    /** Days since release, or null if not yet released / unknown. */
    fun daysSince(iso: String?): Long? {
        val date = parseOrNull(iso) ?: return null
        val diff = ChronoUnit.DAYS.between(date, today())
        return if (diff >= 0) diff else null
    }

    fun formatForDisplay(iso: String?): String {
        val date = parseOrNull(iso) ?: return "TBA"
        return date.format(DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH))
    }

    /** Short "in 3 days" / "today" / "tomorrow" caption for an upcoming release. */
    fun countdownLabel(iso: String?): String {
        val days = daysUntil(iso) ?: return formatForDisplay(iso)
        return when (days) {
            0L -> "Today"
            1L -> "Tomorrow"
            else -> "in ${days}d"
        }
    }

    /** Short "2 days ago" / "today" / "yesterday" caption for a released title. */
    fun agoLabel(iso: String?): String {
        val days = daysSince(iso) ?: return formatForDisplay(iso)
        return when (days) {
            0L -> "Today"
            1L -> "Yesterday"
            in 2..13 -> "${days}d ago"
            else -> formatForDisplay(iso)
        }
    }

    /**
     * Groups upcoming items into human buckets. Assumes the caller passes items
     * already sorted ascending by release date, so bucket encounter order stays
     * chronological.
     */
    fun upcomingBucket(iso: String?): String {
        val days = daysUntil(iso) ?: return "Later"
        val date = parseOrNull(iso)
        return when {
            days == 0L -> "Today"
            days == 1L -> "Tomorrow"
            days in 2..7 -> "This Week"
            days in 8..14 -> "Next Week"
            days in 15..31 -> "Later This Month"
            date != null -> date.format(MONTH_YEAR)
            else -> "Later"
        }
    }
}
