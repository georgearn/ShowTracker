package com.arno.showtracker.util

import com.arno.showtracker.data.model.ReleaseStatus
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

object DateUtils {
    private val ISO: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    fun today(): LocalDate = LocalDate.now()

    fun todayIso(): String = today().format(ISO)

    fun isoDaysAgo(days: Long): String = today().minusDays(days).format(ISO)

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
        val diff = java.time.temporal.ChronoUnit.DAYS.between(today(), date)
        return if (diff >= 0) diff else null
    }

    fun formatForDisplay(iso: String?): String {
        val date = parseOrNull(iso) ?: return "TBA"
        return date.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))
    }
}
