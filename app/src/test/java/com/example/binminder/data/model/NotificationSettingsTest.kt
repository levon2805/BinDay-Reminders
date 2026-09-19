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
        assertEquals(setOf(LocalTime.of(19, 0)), defaultSettings.eveningReminderTimes)
        assertEquals(setOf(LocalTime.of(7, 0)), defaultSettings.morningReminderTimes)
        assertEquals(LocalTime.of(19, 0), defaultSettings.eveningReminderTime)
        assertEquals(LocalTime.of(7, 0), defaultSettings.morningReminderTime)
    }

    @Test
    fun testMultipleRemindersPerSlotConfiguration() {
        val multiSettings = NotificationSettings(
            reminderEnabled = true,
            eveningReminderTimes = setOf(LocalTime.of(19, 0), LocalTime.of(20, 0)),
            morningReminderTimes = setOf(LocalTime.of(6, 30), LocalTime.of(7, 30))
        )

        assertEquals(2, multiSettings.eveningReminderTimes.size)
        assertTrue(multiSettings.eveningReminderTimes.contains(LocalTime.of(19, 0)))
        assertTrue(multiSettings.eveningReminderTimes.contains(LocalTime.of(20, 0)))

        assertEquals(2, multiSettings.morningReminderTimes.size)
        assertTrue(multiSettings.morningReminderTimes.contains(LocalTime.of(6, 30)))
        assertTrue(multiSettings.morningReminderTimes.contains(LocalTime.of(7, 30)))
    }

    @Test
    fun testBackwardCompatibilityConstructor() {
        val dualSettings = NotificationSettings(
            reminderEnabled = true,
            eveningReminderTime = LocalTime.of(20, 0),
            morningReminderTime = LocalTime.of(6, 30)
        )

        assertEquals(setOf(LocalTime.of(20, 0)), dualSettings.eveningReminderTimes)
        assertEquals(setOf(LocalTime.of(6, 30)), dualSettings.morningReminderTimes)
        assertEquals(LocalTime.of(20, 0), dualSettings.eveningReminderTime)
        assertEquals(LocalTime.of(6, 30), dualSettings.morningReminderTime)
    }

    @Test
    fun testNoneOptionForReminderSlots() {
        val noneEveningSettings = NotificationSettings(
            reminderEnabled = true,
            eveningReminderTimes = emptySet(),
            morningReminderTimes = setOf(LocalTime.of(7, 0))
        )
        assertTrue(noneEveningSettings.eveningReminderTimes.isEmpty())
        assertNull(noneEveningSettings.eveningReminderTime)
        assertEquals(LocalTime.of(7, 0), noneEveningSettings.morningReminderTime)

        val noneMorningSettings = NotificationSettings(
            reminderEnabled = true,
            eveningReminderTimes = setOf(LocalTime.of(19, 0)),
            morningReminderTimes = emptySet()
        )
        assertEquals(LocalTime.of(19, 0), noneMorningSettings.eveningReminderTime)
        assertTrue(noneMorningSettings.morningReminderTimes.isEmpty())
        assertNull(noneMorningSettings.morningReminderTime)
    }

    @Test
    fun testReminderTargetDateCalculationForEveningBefore() {
        val settings = NotificationSettings(
            reminderEnabled = true,
            eveningReminderTimes = setOf(LocalTime.of(20, 0)),
            morningReminderTimes = emptySet()
        )

        val collectionDate = LocalDate.of(2025, 5, 6) // Tuesday collection
        val reminderTriggerDate = if (settings.eveningReminderTimes.isNotEmpty()) {
            collectionDate.minusDays(1)
        } else {
            collectionDate
        }

        assertEquals(LocalDate.of(2025, 5, 6).minusDays(1), reminderTriggerDate) // Mon May 5 evening
    }

    @Test
    fun testReminderTargetDateCalculationForMorningOf() {
        val settings = NotificationSettings(
            reminderEnabled = true,
            eveningReminderTimes = emptySet(),
            morningReminderTimes = setOf(LocalTime.of(7, 0))
        )

        val collectionDate = LocalDate.of(2025, 5, 6) // Tuesday collection
        val reminderTriggerDate = if (settings.morningReminderTimes.isNotEmpty()) {
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
            eveningReminderTimes = setOf(LocalTime.of(22, 15)),
            morningReminderTimes = setOf(LocalTime.of(7, 0))
        )

        val collectionDate = LocalDate.of(2025, 5, 6)
        val eveningTargetDate = collectionDate.minusDays(1)
        val eveningTargetDateTime = LocalDateTime.of(eveningTargetDate, settings.eveningReminderTimes.first())

        val now = LocalDateTime.of(2025, 5, 5, 22, 10)
        val delayMillis = Duration.between(now, eveningTargetDateTime).toMillis()

        assertEquals(300000L, delayMillis) // 5 minutes (300,000 ms)
    }

    @Test
    fun testPrimaryAndExtraReminderTimesSeparation() {
        val settings = NotificationSettings(
            reminderEnabled = true,
            primaryEveningTime = null,
            primaryMorningTime = LocalTime.of(7, 0),
            eveningReminderTimes = setOf(LocalTime.of(21, 0)),
            morningReminderTimes = setOf(LocalTime.of(7, 0), LocalTime.of(8, 0))
        )

        assertNull(settings.primaryEveningTime)
        assertNull(settings.eveningReminderTime)
        assertEquals(1, settings.eveningReminderTimes.size)
        assertTrue(settings.eveningReminderTimes.contains(LocalTime.of(21, 0)))

        assertEquals(LocalTime.of(7, 0), settings.primaryMorningTime)
        assertEquals(LocalTime.of(7, 0), settings.morningReminderTime)
        assertEquals(2, settings.morningReminderTimes.size)
    }
}
