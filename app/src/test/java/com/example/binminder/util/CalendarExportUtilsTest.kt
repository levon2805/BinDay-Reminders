package com.example.binminder.util

import android.provider.CalendarContract
import com.example.binminder.data.model.Bin
import com.example.binminder.data.model.BinColor
import com.example.binminder.data.model.CollectionEvent
import com.example.binminder.data.model.RecurrenceType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

class CalendarExportUtilsTest {

    @Test
    fun buildRRule_weekly_returnsCorrectRRuleString() {
        val rruleWithPrefix = CalendarExportUtils.buildRRule(1, includePrefix = true)
        val rruleNoPrefix = CalendarExportUtils.buildRRule(1, includePrefix = false)
        val rruleEnum = CalendarExportUtils.buildRRule(RecurrenceType.WEEKLY)

        assertEquals("RRULE:FREQ=WEEKLY;INTERVAL=1", rruleWithPrefix)
        assertEquals("FREQ=WEEKLY;INTERVAL=1", rruleNoPrefix)
        assertEquals("FREQ=WEEKLY;INTERVAL=1", rruleEnum)
    }

    @Test
    fun buildRRule_fortnightly_returnsCorrectRRuleString() {
        val rruleWithPrefix = CalendarExportUtils.buildRRule(2, includePrefix = true)
        val rruleNoPrefix = CalendarExportUtils.buildRRule(2, includePrefix = false)
        val rruleEnum = CalendarExportUtils.buildRRule(RecurrenceType.FORTNIGHTLY)

        assertEquals("RRULE:FREQ=WEEKLY;INTERVAL=2", rruleWithPrefix)
        assertEquals("FREQ=WEEKLY;INTERVAL=2", rruleNoPrefix)
        assertEquals("FREQ=WEEKLY;INTERVAL=2", rruleEnum)
    }

    @Test
    fun buildRRule_threeWeekly_returnsCorrectRRuleString() {
        val rruleWithPrefix = CalendarExportUtils.buildRRule(3, includePrefix = true)
        val rruleNoPrefix = CalendarExportUtils.buildRRule(3, includePrefix = false)
        val rruleEnum = CalendarExportUtils.buildRRule(RecurrenceType.EVERY_3_WEEKS)

        assertEquals("RRULE:FREQ=WEEKLY;INTERVAL=3", rruleWithPrefix)
        assertEquals("FREQ=WEEKLY;INTERVAL=3", rruleNoPrefix)
        assertEquals("FREQ=WEEKLY;INTERVAL=3", rruleEnum)
    }

    @Test
    fun buildRRule_fourWeekly_returnsCorrectRRuleString() {
        val rruleWithPrefix = CalendarExportUtils.buildRRule(4, includePrefix = true)
        val rruleNoPrefix = CalendarExportUtils.buildRRule(4, includePrefix = false)
        val rruleEnum = CalendarExportUtils.buildRRule(RecurrenceType.EVERY_4_WEEKS)

        assertEquals("RRULE:FREQ=WEEKLY;INTERVAL=4", rruleWithPrefix)
        assertEquals("FREQ=WEEKLY;INTERVAL=4", rruleNoPrefix)
        assertEquals("FREQ=WEEKLY;INTERVAL=4", rruleEnum)
    }

    @Test
    fun createAddCollectionToCalendarIntent_returnsNonNullIntentWithHasAlarmAndDuration() {
        val date = LocalDate.of(2025, 6, 12)
        val reminderTime = LocalTime.of(19, 0)
        val event = CollectionEvent(
            binId = 1,
            binName = "General Waste",
            binColorHex = BinColor.BLACK.defaultHex,
            presetColor = BinColor.BLACK,
            collectionDate = date,
            originalDate = date,
            isBankHolidayAdjusted = false,
            customNote = "Put lid down",
            repeatIntervalWeeks = 2
        )

        val intent = CalendarExportUtils.createAddCollectionToCalendarIntent(
            events = listOf(event),
            collectionDate = date,
            reminderTime = reminderTime
        )

        assertNotNull(intent)
    }

    @Test
    fun createAddBinToCalendarIntent_weeklyBin_returnsNonNullIntentWithWeeklyRRuleAndHasAlarm() {
        val bin = Bin(
            id = 1,
            name = "General Waste",
            colorHex = BinColor.BLACK.defaultHex,
            presetColor = BinColor.BLACK,
            recurrence = RecurrenceType.WEEKLY,
            repeatIntervalWeeks = 1,
            startDate = LocalDate.of(2025, 5, 1),
            customNote = "Put bin out by 7am"
        )
        val nextCollectionDate = LocalDate.of(2025, 5, 8)
        val reminderTime = LocalTime.of(19, 0)

        val intent = CalendarExportUtils.createAddBinToCalendarIntent(
            bin = bin,
            nextCollectionDate = nextCollectionDate,
            reminderTime = reminderTime
        )

        assertNotNull(intent)
        val expectedRRule = CalendarExportUtils.buildRRule(bin.recurrence.intervalWeeks, includePrefix = false)
        assertEquals("FREQ=WEEKLY;INTERVAL=1", expectedRRule)
    }

    @Test
    fun createAddBinToCalendarIntent_fortnightlyBin_returnsNonNullIntentWithFortnightlyRRuleAndHasAlarm() {
        val bin = Bin(
            id = 2,
            name = "Recycling",
            colorHex = BinColor.BLUE.defaultHex,
            presetColor = BinColor.BLUE,
            recurrence = RecurrenceType.FORTNIGHTLY,
            repeatIntervalWeeks = 2,
            startDate = LocalDate.of(2025, 5, 1),
            customNote = "Rinse tins"
        )
        val nextCollectionDate = LocalDate.of(2025, 5, 15)
        val reminderTime = LocalTime.of(7, 0)

        val intent = CalendarExportUtils.createAddBinToCalendarIntent(
            bin = bin,
            nextCollectionDate = nextCollectionDate,
            reminderTime = reminderTime
        )

        assertNotNull(intent)
        val expectedRRule = CalendarExportUtils.buildRRule(bin.recurrence.intervalWeeks, includePrefix = false)
        assertEquals("FREQ=WEEKLY;INTERVAL=2", expectedRRule)
    }

    @Test
    fun calculateNextCollectionDate_calculatesCorrectFirstDateForBin() {
        val bin = Bin(
            id = 1,
            name = "General Waste",
            colorHex = BinColor.BLACK.defaultHex,
            presetColor = BinColor.BLACK,
            recurrence = RecurrenceType.FORTNIGHTLY,
            repeatIntervalWeeks = 2,
            startDate = LocalDate.of(2025, 1, 2),
            adjustForBankHolidays = false
        )

        val fromDate = LocalDate.of(2025, 5, 1)
        val calculatedDate = CalendarExportUtils.calculateNextCollectionDate(bin, fromDate)

        assertEquals(LocalDate.of(2025, 5, 8), calculatedDate)
    }

    @Test
    fun calculateNextCollectionDate_withBankHolidayAdjustment_returnsShiftedDate() {
        val bin = Bin(
            id = 1,
            name = "General Waste",
            colorHex = BinColor.BLACK.defaultHex,
            presetColor = BinColor.BLACK,
            recurrence = RecurrenceType.FORTNIGHTLY,
            repeatIntervalWeeks = 2,
            startDate = LocalDate.of(2025, 1, 2),
            adjustForBankHolidays = true
        )

        val fromDate = LocalDate.of(2025, 5, 1)
        val calculatedDate = CalendarExportUtils.calculateNextCollectionDate(bin, fromDate)

        assertEquals(LocalDate.of(2025, 5, 9), calculatedDate)
    }

    @Test
    fun generateIcsContent_multipleActiveBins_generatesValidIcsWithMultipleVeventsAndValarm() {
        val bin1 = Bin(
            id = 1,
            name = "General Waste",
            colorHex = BinColor.BLACK.defaultHex,
            presetColor = BinColor.BLACK,
            recurrence = RecurrenceType.WEEKLY,
            repeatIntervalWeeks = 1,
            startDate = LocalDate.of(2025, 5, 1)
        )
        val bin2 = Bin(
            id = 2,
            name = "Dry Mixed Recycling",
            colorHex = BinColor.BLUE.defaultHex,
            presetColor = BinColor.BLUE,
            recurrence = RecurrenceType.FORTNIGHTLY,
            repeatIntervalWeeks = 2,
            startDate = LocalDate.of(2025, 5, 1)
        )

        val icsContent = CalendarExportUtils.generateIcsContent(listOf(bin1, bin2), LocalTime.of(19, 0))

        assertTrue(icsContent.startsWith("BEGIN:VCALENDAR"))
        assertTrue(icsContent.trimEnd().endsWith("END:VCALENDAR"))
        assertTrue(icsContent.contains("VERSION:2.0"))
        assertTrue(icsContent.contains("PRODID:-//BinMinder//Bin Collection Schedule//EN"))

        val veventCount = icsContent.split("BEGIN:VEVENT").size - 1
        assertEquals(2, veventCount)

        assertTrue(icsContent.contains("SUMMARY:Bin Collection: General Waste"))
        assertTrue(icsContent.contains("SUMMARY:Bin Collection: Dry Mixed Recycling"))
        assertTrue(icsContent.contains("RRULE:FREQ=WEEKLY;INTERVAL=1"))
        assertTrue(icsContent.contains("RRULE:FREQ=WEEKLY;INTERVAL=2"))
        assertTrue(icsContent.contains("BEGIN:VALARM"))
        assertTrue(icsContent.contains("TRIGGER:-PT0M"))
        assertTrue(icsContent.contains("ACTION:DISPLAY"))
        assertTrue(icsContent.contains("DESCRIPTION:Bin Collection Reminder: General Waste"))
        assertTrue(icsContent.contains("DESCRIPTION:Bin Collection Reminder: Dry Mixed Recycling"))
        assertTrue(icsContent.contains("END:VALARM"))
    }

    @Test
    fun generateIcsContent_filtersDisabledBins() {
        val activeBin = Bin(
            id = 1,
            name = "General Waste",
            colorHex = BinColor.BLACK.defaultHex,
            presetColor = BinColor.BLACK,
            recurrence = RecurrenceType.WEEKLY,
            repeatIntervalWeeks = 1,
            startDate = LocalDate.of(2025, 5, 1),
            isEnabled = true
        )
        val disabledBin = Bin(
            id = 2,
            name = "Garden Waste",
            colorHex = BinColor.GREEN.defaultHex,
            presetColor = BinColor.GREEN,
            recurrence = RecurrenceType.EVERY_3_WEEKS,
            repeatIntervalWeeks = 3,
            startDate = LocalDate.of(2025, 5, 1),
            isEnabled = false
        )

        val icsContent = CalendarExportUtils.generateIcsContent(listOf(activeBin, disabledBin))

        val veventCount = icsContent.split("BEGIN:VEVENT").size - 1
        assertEquals(1, veventCount)

        assertTrue(icsContent.contains("SUMMARY:Bin Collection: General Waste"))
        assertFalse(icsContent.contains("SUMMARY:Bin Collection: Garden Waste"))
    }
}
