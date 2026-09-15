package com.example.binminder.engine

import com.example.binminder.data.model.Bin
import com.example.binminder.data.model.CollectionEvent
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Engine to calculate future bin collection schedules based on bin recurrence,
 * start dates, and UK bank holiday adjustments.
 */
object ScheduleEngine {

    /**
     * Generates a sorted list of upcoming collection events for all enabled bins
     * within the specified date range inclusive.
     */
    fun generateCollectionEvents(
        bins: List<Bin>,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<CollectionEvent> {
        val events = mutableListOf<CollectionEvent>()
        for (bin in bins) {
            if (!bin.isEnabled) continue
            events.addAll(generateEventsForBin(bin, startDate, endDate))
        }
        return events.sortedWith(compareBy({ it.collectionDate }, { it.binName }))
    }

    /**
     * Generates upcoming collection events for a single bin within the specified date range.
     */
    fun generateEventsForBin(
        bin: Bin,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<CollectionEvent> {
        if (startDate.isAfter(endDate)) return emptyList()

        val events = mutableListOf<CollectionEvent>()
        val rawWeeks = if (bin.repeatIntervalWeeks > 0) bin.repeatIntervalWeeks else bin.recurrence.intervalWeeks
        val intervalWeeks = maxOf(1, rawWeeks)
        val intervalDays = intervalWeeks * 7L

        var current = bin.startDate

        // Fast-forward current date to on or near startDate if bin.startDate is before startDate
        if (current.isBefore(startDate)) {
            val daysDiff = ChronoUnit.DAYS.between(current, startDate)
            val cyclesToSkip = daysDiff / intervalDays
            current = current.plusDays(cyclesToSkip * intervalDays)
            while (current.isBefore(startDate)) {
                current = current.plusDays(intervalDays)
            }
        }

        // Generate events up to endDate
        while (!current.isAfter(endDate)) {
            val (finalDate, isAdjusted) = if (bin.adjustForBankHolidays) {
                BankHolidayCalculator.adjustForBankHoliday(current)
            } else {
                Pair(current, false)
            }

            // Ensure the adjusted date falls within the target window (or at least <= endDate + 7 days)
            if (!finalDate.isBefore(startDate) && !finalDate.isAfter(endDate.plusDays(7))) {
                events.add(
                    CollectionEvent(
                        binId = bin.id,
                        binName = bin.name,
                        binColorHex = bin.colorHex,
                        presetColor = bin.presetColor,
                        lidColorHex = bin.lidColorHex,
                        lidPresetColor = bin.lidPresetColor,
                        collectionDate = finalDate,
                        originalDate = current,
                        isBankHolidayAdjusted = isAdjusted,
                        customNote = bin.customNote,
                        repeatIntervalWeeks = intervalWeeks
                    )
                )
            }

            current = current.plusDays(intervalDays)
        }

        return events
    }
}
