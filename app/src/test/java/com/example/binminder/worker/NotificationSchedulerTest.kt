package com.example.binminder.worker

import com.example.binminder.data.model.Bin
import com.example.binminder.data.model.BinColor
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
}
