package com.example.binminder.worker

import com.example.binminder.data.model.Bin
import com.example.binminder.data.model.BinColor
import com.example.binminder.data.model.CollectionEvent
import com.example.binminder.data.model.NotificationSettings
import com.example.binminder.data.model.RecurrenceType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class NotificationSchedulerTest {

    private fun createTestBin(
        name: String = "General Waste",
        startDate: LocalDate = LocalDate.of(2025, 3, 10), // Monday
        recurrence: RecurrenceType = RecurrenceType.WEEKLY
    ): Bin {
        return Bin(
            id = 1L,
            name = name,
            colorHex = BinColor.BLACK.defaultHex,
            presetColor = BinColor.BLACK,
            recurrence = recurrence,
            startDate = startDate,
            isEnabled = true,
            adjustForBankHolidays = false
        )
    }

    @Test
    fun testExactAlarmTargetCalculationForEveningBefore() {
        val bin = createTestBin(startDate = LocalDate.of(2025, 3, 10))
        val settings = NotificationSettings(
            reminderEnabled = true,
            eveningReminderTime = LocalTime.of(20, 0),
            morningReminderTime = null
        )

        // Sunday evening before Monday collection
        val now = LocalDateTime.of(2025, 3, 9, 18, 0)
        val targets = NotificationScheduler.calculateNextReminderTargets(listOf(bin), settings, now)

        assertEquals(1, targets.size)
        val target = targets.first()
        assertEquals(ReminderSlot.EVENING, target.slot)
        assertEquals(LocalDateTime.of(2025, 3, 9, 20, 0), target.targetDateTime)
        assertEquals(LocalDate.of(2025, 3, 10), target.collectionDate)
        assertEquals(2 * 3600 * 1000L, target.delayMillis) // 2 hours in ms
    }

    @Test
    fun testExactAlarmTargetCalculationForMorningOf() {
        val bin = createTestBin(startDate = LocalDate.of(2025, 3, 10))
        val settings = NotificationSettings(
            reminderEnabled = true,
            eveningReminderTime = null,
            morningReminderTime = LocalTime.of(7, 0)
        )

        val now = LocalDateTime.of(2025, 3, 9, 22, 0)
        val targets = NotificationScheduler.calculateNextReminderTargets(listOf(bin), settings, now)

        assertEquals(1, targets.size)
        val target = targets.first()
        assertEquals(ReminderSlot.MORNING, target.slot)
        assertEquals(LocalDateTime.of(2025, 3, 10, 7, 0), target.targetDateTime)
        assertEquals(LocalDate.of(2025, 3, 10), target.collectionDate)
        assertEquals(9 * 3600 * 1000L, target.delayMillis) // 9 hours in ms
    }

    @Test
    fun testSameDayFutureCustomTimeDelay() {
        val bin = createTestBin(startDate = LocalDate.of(2025, 3, 9))
        val settings = NotificationSettings(
            reminderEnabled = true,
            eveningReminderTime = null,
            morningReminderTime = LocalTime.of(22, 15) // Custom time setting at 22:15
        )

        // Same day at 22:10 (5 minutes before custom target time)
        val now = LocalDateTime.of(2025, 3, 9, 22, 10)
        val targets = NotificationScheduler.calculateNextReminderTargets(listOf(bin), settings, now)

        assertEquals(1, targets.size)
        val target = targets.first()
        assertEquals(ReminderSlot.MORNING, target.slot)
        assertEquals(LocalDateTime.of(2025, 3, 9, 22, 15), target.targetDateTime)
        assertEquals(LocalDate.of(2025, 3, 9), target.collectionDate)
        assertEquals(5 * 60 * 1000L, target.delayMillis) // 5 minutes (300,000 ms)
    }

    @Test
    fun testPastCustomTimeTargetIgnoredAndNextTargetSelected() {
        val bin = createTestBin(startDate = LocalDate.of(2025, 3, 9), recurrence = RecurrenceType.WEEKLY)
        val settings = NotificationSettings(
            reminderEnabled = true,
            eveningReminderTime = null,
            morningReminderTime = LocalTime.of(22, 15)
        )

        // Same day at 22:20 (5 minutes AFTER custom target time 22:15)
        val now = LocalDateTime.of(2025, 3, 9, 22, 20)
        val targets = NotificationScheduler.calculateNextReminderTargets(listOf(bin), settings, now)

        assertEquals(1, targets.size)
        val target = targets.first()
        assertEquals(ReminderSlot.MORNING, target.slot)
        assertEquals(LocalDateTime.of(2025, 3, 16, 22, 15), target.targetDateTime)
        assertEquals(LocalDate.of(2025, 3, 16), target.collectionDate)
    }

    @Test
    fun testDisabledRemindersReturnEmptyList() {
        val bin = createTestBin()
        val settings = NotificationSettings(
            reminderEnabled = false,
            eveningReminderTime = LocalTime.of(19, 0),
            morningReminderTime = LocalTime.of(7, 0)
        )

        val now = LocalDateTime.of(2025, 3, 9, 12, 0)
        val targets = NotificationScheduler.calculateNextReminderTargets(listOf(bin), settings, now)

        assertTrue(targets.isEmpty())
    }

    @Test
    fun testDualRemindersReturnBothEveningAndMorningTargets() {
        val bin = createTestBin(startDate = LocalDate.of(2025, 3, 10))
        val settings = NotificationSettings(
            reminderEnabled = true,
            eveningReminderTime = LocalTime.of(19, 0),
            morningReminderTime = LocalTime.of(7, 0)
        )

        val now = LocalDateTime.of(2025, 3, 9, 12, 0)
        val targets = NotificationScheduler.calculateNextReminderTargets(listOf(bin), settings, now)

        assertEquals(2, targets.size)

        val eveningTarget = targets.first { it.slot == ReminderSlot.EVENING }
        assertEquals(LocalDateTime.of(2025, 3, 9, 19, 0), eveningTarget.targetDateTime)
        assertEquals(7 * 3600 * 1000L, eveningTarget.delayMillis)

        val morningTarget = targets.first { it.slot == ReminderSlot.MORNING }
        assertEquals(LocalDateTime.of(2025, 3, 10, 7, 0), morningTarget.targetDateTime)
        assertEquals(19 * 3600 * 1000L, morningTarget.delayMillis)
    }

    @Test
    fun testMultipleRemindersPerSlotTargetCalculation() {
        val bin = createTestBin(startDate = LocalDate.of(2025, 3, 10))
        val settings = NotificationSettings(
            reminderEnabled = true,
            eveningReminderTimes = setOf(LocalTime.of(19, 0), LocalTime.of(20, 0)),
            morningReminderTimes = setOf(LocalTime.of(6, 0), LocalTime.of(7, 0))
        )

        val now = LocalDateTime.of(2025, 3, 9, 12, 0)
        val targets = NotificationScheduler.calculateNextReminderTargets(listOf(bin), settings, now)

        assertEquals(4, targets.size)

        val evening19 = targets.find { it.slot == ReminderSlot.EVENING && it.reminderTime == LocalTime.of(19, 0) }
        val evening20 = targets.find { it.slot == ReminderSlot.EVENING && it.reminderTime == LocalTime.of(20, 0) }
        val morning6 = targets.find { it.slot == ReminderSlot.MORNING && it.reminderTime == LocalTime.of(6, 0) }
        val morning7 = targets.find { it.slot == ReminderSlot.MORNING && it.reminderTime == LocalTime.of(7, 0) }

        assertTrue(evening19 != null)
        assertTrue(evening20 != null)
        assertTrue(morning6 != null)
        assertTrue(morning7 != null)
    }

    @Test
    fun testPrimaryNoneWithExtraReminderTimesTargetCalculation() {
        val bin = createTestBin(startDate = LocalDate.of(2025, 3, 10))
        val settings = NotificationSettings(
            reminderEnabled = true,
            primaryEveningTime = null,
            primaryMorningTime = LocalTime.of(7, 0),
            eveningReminderTimes = setOf(LocalTime.of(21, 0)),
            morningReminderTimes = setOf(LocalTime.of(7, 0))
        )

        val now = LocalDateTime.of(2025, 3, 9, 12, 0)
        val targets = NotificationScheduler.calculateNextReminderTargets(listOf(bin), settings, now)

        assertEquals(2, targets.size)

        val eveningTarget = targets.find { it.slot == ReminderSlot.EVENING }
        assertTrue(eveningTarget != null)
        assertEquals(LocalTime.of(21, 0), eveningTarget?.reminderTime)
        assertEquals(LocalDateTime.of(2025, 3, 9, 21, 0), eveningTarget?.targetDateTime)

        val morningTarget = targets.find { it.slot == ReminderSlot.MORNING }
        assertTrue(morningTarget != null)
        assertEquals(LocalTime.of(7, 0), morningTarget?.reminderTime)
    }

    @Test
    fun testGenerateAlarmRequestCodeUniquenessForCloseTimes() {
        val date = LocalDate.of(2025, 3, 10)
        val time1 = LocalTime.of(22, 10)
        val time2 = LocalTime.of(22, 11)
        val time3 = LocalTime.of(22, 12)

        val code1 = NotificationScheduler.generateAlarmRequestCode(date, time1, isMorning = false)
        val code2 = NotificationScheduler.generateAlarmRequestCode(date, time2, isMorning = false)
        val code3 = NotificationScheduler.generateAlarmRequestCode(date, time3, isMorning = false)

        val codeMorning1 = NotificationScheduler.generateAlarmRequestCode(date, time1, isMorning = true)

        // All request codes must be strictly distinct
        val codes = setOf(code1, code2, code3, codeMorning1)
        assertEquals(4, codes.size)
    }

    @Test
    fun testMultipleTimesCloseTogetherGenerateDistinctRequestCodesAndTargets() {
        val bin = createTestBin(startDate = LocalDate.of(2025, 3, 10))
        val times = setOf(LocalTime.of(22, 10), LocalTime.of(22, 11), LocalTime.of(22, 12))
        val settings = NotificationSettings(
            reminderEnabled = true,
            eveningReminderTimes = times,
            morningReminderTimes = emptySet()
        )

        val now = LocalDateTime.of(2025, 3, 9, 20, 0)
        val targets = NotificationScheduler.calculateNextReminderTargets(listOf(bin), settings, now)

        assertEquals(3, targets.size)

        val requestCodes = targets.map { target ->
            val time = target.reminderTime ?: target.targetDateTime.toLocalTime()
            NotificationScheduler.generateAlarmRequestCode(target.collectionDate, time, target.slot == ReminderSlot.MORNING)
        }.toSet()

        assertEquals(3, requestCodes.size)
    }

    @Test
    fun testGenerateAlarmRequestCodeDifferenceAcrossDatesAndSlots() {
        val date1 = LocalDate.of(2025, 3, 10)
        val date2 = LocalDate.of(2025, 3, 11)
        val time = LocalTime.of(8, 0)

        val codeDate1Evening = NotificationScheduler.generateAlarmRequestCode(date1, time, isMorning = false)
        val codeDate1Morning = NotificationScheduler.generateAlarmRequestCode(date1, time, isMorning = true)
        val codeDate2Evening = NotificationScheduler.generateAlarmRequestCode(date2, time, isMorning = false)
        val codeDate2Morning = NotificationScheduler.generateAlarmRequestCode(date2, time, isMorning = true)

        val codes = setOf(codeDate1Evening, codeDate1Morning, codeDate2Evening, codeDate2Morning)
        assertEquals(4, codes.size)
    }

    @Test
    fun testTargetInPastWithinSixtySecondsIsScheduledWithMinDelay() {
        val bin = createTestBin(startDate = LocalDate.of(2025, 3, 10))
        val settings = NotificationSettings(
            reminderEnabled = true,
            eveningReminderTime = null,
            morningReminderTime = LocalTime.of(7, 0)
        )

        // Target time is 07:00:00 on March 10.
        // Current time is 07:00:30 (30 seconds in the past, within 60s window).
        val now = LocalDateTime.of(2025, 3, 10, 7, 0, 30)
        val targets = NotificationScheduler.calculateNextReminderTargets(listOf(bin), settings, now)

        assertEquals(1, targets.size)
        val target = targets.first()
        assertEquals(LocalDate.of(2025, 3, 10), target.collectionDate)
        assertEquals(1000L, target.delayMillis) // Coerced to maxOf(1000L, delayMillis)
    }

    @Test
    fun testTargetInPastMoreThanOneMinuteIsSkippedForToday() {
        val bin = createTestBin(startDate = LocalDate.of(2025, 3, 10), recurrence = RecurrenceType.WEEKLY)
        val settings = NotificationSettings(
            reminderEnabled = true,
            eveningReminderTime = null,
            morningReminderTime = LocalTime.of(7, 0)
        )

        // Target time is 07:00:00 on March 10.
        // Current time is 07:01:30 (1 min 30 sec in the past, > 60 seconds).
        val now = LocalDateTime.of(2025, 3, 10, 7, 1, 30)
        val targets = NotificationScheduler.calculateNextReminderTargets(listOf(bin), settings, now)

        assertEquals(1, targets.size)
        val target = targets.first()
        // Skipped March 10 collection, selected next collection on March 17
        assertEquals(LocalDate.of(2025, 3, 17), target.collectionDate)
    }

    @Test
    fun testFutureTargetDelayCalculationMaxOfThousand() {
        val bin = createTestBin(startDate = LocalDate.of(2025, 3, 10))
        val settings = NotificationSettings(
            reminderEnabled = true,
            eveningReminderTime = null,
            morningReminderTime = LocalTime.of(7, 0)
        )

        // 5 minutes in the future (300,000 ms)
        val now = LocalDateTime.of(2025, 3, 10, 6, 55, 0)
        val targets = NotificationScheduler.calculateNextReminderTargets(listOf(bin), settings, now)

        assertEquals(1, targets.size)
        assertEquals(300_000L, targets.first().delayMillis)
    }

    @Test
    fun testFormatNotificationContentFiltersUnPutOutBins() {
        val collectionDate = LocalDate.of(2025, 3, 10)
        val generalWasteEvent = CollectionEvent(
            binId = 1L,
            binName = "General Waste",
            binColorHex = "#000000",
            presetColor = BinColor.BLACK,
            collectionDate = collectionDate,
            originalDate = collectionDate,
            isBankHolidayAdjusted = false
        )
        val foodWasteEvent = CollectionEvent(
            binId = 2L,
            binName = "Food Waste Caddy",
            binColorHex = "#008000",
            presetColor = BinColor.GREEN,
            collectionDate = collectionDate,
            originalDate = collectionDate,
            isBankHolidayAdjusted = false
        )

        // Case A: Both bins not put out
        val bothEvents = listOf(generalWasteEvent, foodWasteEvent)
        val contentBoth = NotificationHelper.formatNotificationContent(bothEvents, isEvening = false)
        assertTrue(contentBoth != null)
        assertEquals("Today's Bin Collection", contentBoth?.title)
        assertEquals("General Waste and Food Waste Caddy", contentBoth?.binNames)
        assertEquals("Please remember to put out your General Waste and Food Waste Caddy bins.", contentBoth?.message)
        assertEquals(listOf(1L, 2L), contentBoth?.unPutOutBinIds)

        // Case B: General Waste is put out, only Food Waste Caddy remains un-put-out
        val singleEvent = listOf(foodWasteEvent)
        val contentSingle = NotificationHelper.formatNotificationContent(singleEvent, isEvening = false)
        assertTrue(contentSingle != null)
        assertEquals("Today's Bin Collection", contentSingle?.title)
        assertEquals("Food Waste Caddy", contentSingle?.binNames)
        assertEquals("Please remember to put out your Food Waste Caddy bin.", contentSingle?.message)
        assertEquals(listOf(2L), contentSingle?.unPutOutBinIds)

        // Case C: All bins put out (unPutOutEvents empty)
        val contentEmpty = NotificationHelper.formatNotificationContent(emptyList(), isEvening = false)
        assertEquals(null, contentEmpty)
    }

    @Test
    fun testFormatNotificationContentEveningTitle() {
        val collectionDate = LocalDate.of(2025, 3, 10)
        val event = CollectionEvent(
            binId = 1L,
            binName = "General Waste",
            binColorHex = "#000000",
            presetColor = BinColor.BLACK,
            collectionDate = collectionDate,
            originalDate = collectionDate,
            isBankHolidayAdjusted = false
        )

        val content = NotificationHelper.formatNotificationContent(listOf(event), isEvening = true)
        assertTrue(content != null)
        assertEquals("Tomorrow's Bin Collection", content?.title)
        assertEquals("Please remember to put out your General Waste bin.", content?.message)
    }
}
