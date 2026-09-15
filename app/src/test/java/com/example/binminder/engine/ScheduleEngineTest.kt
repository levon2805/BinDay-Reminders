package com.example.binminder.engine

import com.example.binminder.data.model.Bin
import com.example.binminder.data.model.BinColor
import com.example.binminder.data.model.RecurrenceType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class ScheduleEngineTest {

    @Test
    fun testWeeklyBinScheduleWithBankHolidayShifts() {
        val foodBin = Bin(
            id = 1,
            name = "Food Waste Caddy",
            colorHex = BinColor.BROWN.defaultHex,
            presetColor = BinColor.BROWN,
            recurrence = RecurrenceType.WEEKLY,
            startDate = LocalDate.of(2025, 5, 5), // Mon May 5 (Early May BH)
            isEnabled = true,
            adjustForBankHolidays = true
        )

        val startDate = LocalDate.of(2025, 5, 1)
        val endDate = LocalDate.of(2025, 5, 31)

        val events = ScheduleEngine.generateEventsForBin(foodBin, startDate, endDate)

        assertEquals(4, events.size)

        // Mon May 5 (Early May BH) -> shifted to Tue May 6
        assertEquals(LocalDate.of(2025, 5, 6), events[0].collectionDate)
        assertTrue(events[0].isBankHolidayAdjusted)

        // Mon May 12 -> normal
        assertEquals(LocalDate.of(2025, 5, 12), events[1].collectionDate)
        assertFalse(events[1].isBankHolidayAdjusted)

        // Mon May 19 -> normal
        assertEquals(LocalDate.of(2025, 5, 19), events[2].collectionDate)
        assertFalse(events[2].isBankHolidayAdjusted)

        // Mon May 26 (Spring BH) -> shifted to Tue May 27
        assertEquals(LocalDate.of(2025, 5, 27), events[3].collectionDate)
        assertTrue(events[3].isBankHolidayAdjusted)
    }

    @Test
    fun testFortnightlyAlternatingBins() {
        val generalWasteBin = Bin(
            id = 1,
            name = "General Waste",
            colorHex = BinColor.BLACK.defaultHex,
            presetColor = BinColor.BLACK,
            recurrence = RecurrenceType.FORTNIGHTLY,
            startDate = LocalDate.of(2025, 5, 5), // Mon May 5
            isEnabled = true,
            adjustForBankHolidays = true
        )

        val recyclingBin = Bin(
            id = 2,
            name = "Dry Mixed Recycling",
            colorHex = BinColor.BLUE.defaultHex,
            presetColor = BinColor.BLUE,
            recurrence = RecurrenceType.FORTNIGHTLY,
            startDate = LocalDate.of(2025, 5, 12), // Mon May 12 (Alternating week)
            isEnabled = true,
            adjustForBankHolidays = true
        )

        val startDate = LocalDate.of(2025, 5, 1)
        val endDate = LocalDate.of(2025, 5, 31)

        val allEvents = ScheduleEngine.generateCollectionEvents(
            listOf(generalWasteBin, recyclingBin),
            startDate,
            endDate
        )

        // 2 General Waste + 2 Recycling = 4 total events
        assertEquals(4, allEvents.size)

        // Verify dates in chronological order
        // May 6 (General Waste - shifted from May 5)
        assertEquals("General Waste", allEvents[0].binName)
        assertEquals(LocalDate.of(2025, 5, 6), allEvents[0].collectionDate)

        // May 12 (Recycling)
        assertEquals("Dry Mixed Recycling", allEvents[1].binName)
        assertEquals(LocalDate.of(2025, 5, 12), allEvents[1].collectionDate)

        // May 19 (General Waste)
        assertEquals("General Waste", allEvents[2].binName)
        assertEquals(LocalDate.of(2025, 5, 19), allEvents[2].collectionDate)

        // May 27 (Recycling - shifted from May 26)
        assertEquals("Dry Mixed Recycling", allEvents[3].binName)
        assertEquals(LocalDate.of(2025, 5, 27), allEvents[3].collectionDate)
    }

    @Test
    fun testDisabledBinExcluded() {
        val disabledBin = Bin(
            id = 1,
            name = "Garden Waste",
            colorHex = BinColor.GREEN.defaultHex,
            presetColor = BinColor.GREEN,
            recurrence = RecurrenceType.FORTNIGHTLY,
            startDate = LocalDate.of(2025, 5, 5),
            isEnabled = false
        )

        val events = ScheduleEngine.generateCollectionEvents(
            listOf(disabledBin),
            LocalDate.of(2025, 5, 1),
            LocalDate.of(2025, 5, 31)
        )

        assertEquals(0, events.size)
    }

    @Test
    fun testStartDateFarInPastFastForwardsCorrectly() {
        // Bin started 10 years ago (2015-01-05 Monday)
        val oldBin = Bin(
            id = 10,
            name = "General Waste",
            colorHex = BinColor.BLACK.defaultHex,
            presetColor = BinColor.BLACK,
            recurrence = RecurrenceType.FORTNIGHTLY,
            startDate = LocalDate.of(2015, 1, 5),
            isEnabled = true,
            adjustForBankHolidays = false
        )

        val startDate = LocalDate.of(2025, 5, 1)
        val endDate = LocalDate.of(2025, 5, 31)

        val events = ScheduleEngine.generateEventsForBin(oldBin, startDate, endDate)

        // 2015-01-05 + N * 14 days should align with Mon May 12, 2025 and Mon May 26, 2025
        assertTrue("Events should be generated", events.isNotEmpty())
        for (event in events) {
            val daysBetween = ChronoUnit.DAYS.between(LocalDate.of(2015, 1, 5), event.collectionDate)
            assertEquals("Event date must be exactly a multiple of 14 days from startDate", 0L, daysBetween % 14L)
        }
    }

    @Test
    fun testStartDateFarInFutureReturnsEmptyIfOutRange() {
        val futureBin = Bin(
            id = 11,
            name = "Future Bin",
            colorHex = BinColor.BLUE.defaultHex,
            presetColor = BinColor.BLUE,
            recurrence = RecurrenceType.WEEKLY,
            startDate = LocalDate.of(2030, 1, 1),
            isEnabled = true
        )

        val events = ScheduleEngine.generateEventsForBin(
            futureBin,
            LocalDate.of(2025, 1, 1),
            LocalDate.of(2025, 12, 31)
        )

        assertEquals(0, events.size)
    }

    @Test
    fun testStartDateAfterEndDateReturnsEmpty() {
        val bin = Bin(
            id = 12,
            name = "Test Bin",
            colorHex = BinColor.BLACK.defaultHex,
            presetColor = BinColor.BLACK,
            recurrence = RecurrenceType.WEEKLY,
            startDate = LocalDate.of(2025, 5, 1),
            isEnabled = true
        )

        val events = ScheduleEngine.generateEventsForBin(
            bin,
            LocalDate.of(2025, 5, 31),
            LocalDate.of(2025, 5, 1)
        )

        assertEquals(0, events.size)
    }

    @Test
    fun testLeapYearFeb29Schedule() {
        // Leap year 2028: Feb 29 is Tuesday
        val leapBin = Bin(
            id = 13,
            name = "Recycling",
            colorHex = BinColor.BLUE.defaultHex,
            presetColor = BinColor.BLUE,
            recurrence = RecurrenceType.FORTNIGHTLY,
            startDate = LocalDate.of(2028, 2, 15),
            isEnabled = true,
            adjustForBankHolidays = false
        )

        val events = ScheduleEngine.generateEventsForBin(
            leapBin,
            LocalDate.of(2028, 2, 1),
            LocalDate.of(2028, 3, 31)
        )

        val eventDates = events.map { it.collectionDate }
        assertTrue(eventDates.contains(LocalDate.of(2028, 2, 15)))
        assertTrue(eventDates.contains(LocalDate.of(2028, 2, 29))) // Feb 29 leap day!
        assertTrue(eventDates.contains(LocalDate.of(2028, 3, 14)))
    }

    @Test
    fun testYearBoundaryScheduleGeneration() {
        val bin = Bin(
            id = 14,
            name = "General Waste",
            colorHex = BinColor.BLACK.defaultHex,
            presetColor = BinColor.BLACK,
            recurrence = RecurrenceType.WEEKLY,
            startDate = LocalDate.of(2025, 12, 15),
            isEnabled = true,
            adjustForBankHolidays = true
        )

        val events = ScheduleEngine.generateEventsForBin(
            bin,
            LocalDate.of(2025, 12, 20),
            LocalDate.of(2026, 1, 10)
        )

        assertTrue(events.isNotEmpty())
        val dates = events.map { it.collectionDate }
        assertTrue(dates.any { it.year == 2025 })
        assertTrue(dates.any { it.year == 2026 })
    }
}
