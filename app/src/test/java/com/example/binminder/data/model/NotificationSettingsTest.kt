package com.example.binminder.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class NotificationSettingsTest {

    @Test
    fun testDefaultNotificationSettings() {
        val defaultSettings = NotificationSettings()

        assertTrue(defaultSettings.reminderEnabled)
        assertEquals(LocalTime.of(19, 0), defaultSettings.reminderTime)
        assertTrue(defaultSettings.reminderEveningBefore)
    }

    @Test
    fun testReminderTargetDateCalculationForEveningBefore() {
        val settings = NotificationSettings(
            reminderEnabled = true,
            reminderTime = LocalTime.of(20, 0),
            reminderEveningBefore = true
        )

        val collectionDate = LocalDate.of(2025, 5, 6) // Tuesday collection
        val reminderTriggerDate = if (settings.reminderEveningBefore) {
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
            reminderTime = LocalTime.of(7, 0),
            reminderEveningBefore = false
        )

        val collectionDate = LocalDate.of(2025, 5, 6) // Tuesday collection
        val reminderTriggerDate = if (settings.reminderEveningBefore) {
            collectionDate.minusDays(1)
        } else {
            collectionDate
        }

        assertEquals(LocalDate.of(2025, 5, 6), reminderTriggerDate) // Tue May 6 morning
    }
}
