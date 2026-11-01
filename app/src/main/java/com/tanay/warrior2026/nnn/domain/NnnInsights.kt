package com.tanay.warrior.nnn.domain

data class WeekInsight(val weekNumber: Int, val cleanDays: Int, val elapsedDays: Int)

/**
 * Factual, locally calculated numbers for the Insights screen. No causation, no "risk hours" without
 * enough samples, and nothing here mixes into ordinary habit statistics.
 */
data class NnnInsights(
    /** Calendar progress: elapsed days / total days. */
    val progressPercent: Int,
    /** Of the days elapsed so far, how many are clean-status (self-reported or monitored). */
    val cleanDayPercent: Int,
    val selfReportedDays: Int,
    val monitoredCleanDays: Int,
    val failedDays: Int,
    val confirmedIncidents: Int,
    val possibleExposureIncidents: Int,
    val weeks: List<WeekInsight>,
    /** Percentage-point change in clean-day rate, latest week vs the one before; null if either has < [MIN_WEEK_DAYS] days. */
    val weekOverWeekChange: Int?,
    /** Counts per local hour (size 24) of counted events; null when fewer than [MIN_TIME_SAMPLES] events exist. */
    val hourlyCounts: List<Int>?,
    val peakHour: Int?,
    val eventSampleSize: Int
) {
    companion object {
        const val MIN_TIME_SAMPLES = 5
        const val MIN_WEEK_DAYS = 3

        fun from(snapshot: NnnSnapshot, events: List<NnnEvent>): NnnInsights {
            val c = snapshot.challenge
            val elapsed = snapshot.days.count { it.status != DayStatus.FUTURE }
            fun clean(d: DayRecord) = d.status == DayStatus.SELF_REPORTED_CLEAN || d.status == DayStatus.MONITORED_CLEAN
            val cleanDays = snapshot.days.count(::clean)

            val weeks = snapshot.days.chunked(7).mapIndexedNotNull { i, week ->
                val e = week.count { it.status != DayStatus.FUTURE }
                if (e == 0) null else WeekInsight(i + 1, week.count(::clean), e)
            }
            val change = if (weeks.size >= 2) {
                val prev = weeks[weeks.size - 2]; val last = weeks.last()
                if (prev.elapsedDays >= MIN_WEEK_DAYS && last.elapsedDays >= MIN_WEEK_DAYS)
                    (last.cleanDays * 100) / last.elapsedDays - (prev.cleanDays * 100) / prev.elapsedDays
                else null
            } else null

            val counted = events.filter {
                it.challengeId == c.id && !it.challengeDate.isBefore(c.startDate) && !it.challengeDate.isAfter(c.endDate) &&
                    (it.countsAsConfirmedFailure || it.isPendingReview)
            }
            val hours = if (counted.size >= MIN_TIME_SAMPLES) {
                val h = IntArray(24)
                counted.forEach { h[it.at.atZone(c.zoneId).hour]++ }
                h.toList()
            } else null

            return NnnInsights(
                progressPercent = if (c.totalDays == 0) 0 else snapshot.dayNumber * 100 / c.totalDays,
                cleanDayPercent = if (elapsed == 0) 0 else cleanDays * 100 / elapsed,
                selfReportedDays = snapshot.selfReportedDays, monitoredCleanDays = snapshot.monitoredCleanDays,
                failedDays = snapshot.failedDays, confirmedIncidents = snapshot.confirmedIncidents,
                possibleExposureIncidents = snapshot.possibleExposureIncidents,
                weeks = weeks, weekOverWeekChange = change,
                hourlyCounts = hours, peakHour = hours?.let { hh -> hh.indices.maxByOrNull { hh[it] } },
                eventSampleSize = counted.size
            )
        }
    }
}
