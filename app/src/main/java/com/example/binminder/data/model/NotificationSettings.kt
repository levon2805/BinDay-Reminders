package com.example.binminder.data.model

import java.time.LocalTime

/**
 * User preferences for bin collection reminders and notifications.
 * 
 * Controls whether reminders are active, what time they fire, whether to notify
 * the evening before collection, and the preferred application theme mode.
 */
data class NotificationSettings(
    val reminderEnabled: Boolean = true,
    val reminderTime: LocalTime = LocalTime.of(19, 0), // Default 19:00 (7:00 PM)
    val reminderEveningBefore: Boolean = true, // Evening before collection day
    val themeMode: AppThemeMode = AppThemeMode.SYSTEM
)
