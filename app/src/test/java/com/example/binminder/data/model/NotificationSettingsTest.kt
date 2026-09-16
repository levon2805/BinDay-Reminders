package com.example.binminder.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class NotificationSettingsTest {

    @Test
    fun testDefaultNotificationSettings() {
        val defaultSettings = NotificationSettings()

        assertTrue(defaultSettings.reminderEnabled)
        assertEquals(LocalTime.of(19, 0), defaultSettings.eveningReminderTime)
        assertEquals(LocalTime.of(7, 0), defaultSettings.morningReminderTime)
    }

    @Test
    fun testDualRemindersConfiguration() {
        val dualSettings = NotificationSettings(
            reminderEnabled = true,
            eveningReminderTime = LocalTime.of(20, 0),
            morningReminderTime = LocalTime.of(6, 30)
        )

        assertEquals(LocalTime.of(20, 0), dualSettings.eveningReminderTime)
        assertEquals(LocalTime.of(6, 30), dualSettings.morningReminderTime)
    }

    @Test
    fun testNoneOptionForReminderSlots() {
        val noneEveningSettings = NotificationSettings(
            reminderEnabled = true,
            eveningReminderTime = null,
            morningReminderTime = LocalTime.of(7, 0)
        )
        assertNull(noneEveningSettings.eveningReminderTime)
        assertEquals(LocalTime.of(7, 0), noneEveningSettings.morningReminderTime)

        val noneMorningSettings = NotificationSettings(
            reminderEnabled = true,
            eveningReminderTime = LocalTime.of(19, 0),
            morningReminderTime = null
        )
        assertEquals(LocalTime.of(19, 0), noneMorningSettings.eveningReminderTime)
        assertNull(noneMorningSettings.morningReminderTime)
    }

    @Test
    fun testReminderTargetDateCalculationForEveningBefore() {
        val settings = NotificationSettings(
            reminderEnabled = true,
            eveningReminderTime = LocalTime.of(20, 0),
            morningReminderTime = null
        )

        val collectionDate = LocalDate.of(2025, 5, 6) // Tuesday collection
        val reminderTriggerDate = if (settings.eveningReminderTime != null) {
            collectionDate.minusDays(1)
        } else {
            collectionDate
        }

        assertEquals(LocalDate.of(2025, 5, 5), reminderTriggerDate) // Mon May 5 evening
    }

    @Test
    fun testReminderTargetDateCalculationForMorningOf() {
        val settings = NotificationSettings(
            reminderEnabled = true,
            eveningReminderTime = null,
            morningReminderTime = LocalTime.of(7, 0)
        )

        val collectionDate = LocalDate.of(2025, 5, 6) // Tuesday collection
        val reminderTriggerDate = if (settings.morningReminderTime != null) {
            collectionDate
        } else {
            collectionDate.minusDays(1)
        }

        assertEquals(LocalDate.of(2025, 5, 6), reminderTriggerDate) // Tue May 6 morning
    }

    @Test
    fun testCustomTimeExactDelayCalculation() {
        val settings = NotificationSettings(
            reminderEnabled = true,
            eveningReminderTime = LocalTime.of(22, 15),
            morningReminderTime = LocalTime.of(7, 0)
        )

        val collectionDate = LocalDate.of(2025, 5, 6)
        val eveningTargetDate = collectionDate.minusDays(1)
        val eveningTargetDateTime = LocalDateTime.of(eveningTargetDate, settings.eveningReminderTime)

        val now = LocalDateTime.of(2025, 5, 5, 22, 10)
        val delayMillis = Duration.between(now, eveningTargetDateTime).toMillis()

        assertEquals(300000L, delayMillis) // 5 minutes (300,000 ms)
    }
}
