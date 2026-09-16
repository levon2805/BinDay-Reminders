package com.example.binminder.data.model

import java.time.LocalTime

/**
 * User preferences for bin collection reminders and notifications.
 * 
 * Controls whether reminders are active, separate times for evening before and morning of collection
 * (or null for NONE), and the preferred application theme mode.
 */
data class NotificationSettings(
    val reminderEnabled: Boolean = true,
    val eveningReminderTime: LocalTime? = LocalTime.of(19, 0), // Default 19:00 (7:00 PM), null if NONE
    val morningReminderTime: LocalTime? = LocalTime.of(7, 0),  // Default 07:00 AM, null if NONE
    val themeMode: AppThemeMode = AppThemeMode.SYSTEM
) {
    /**
     * Backward compatibility helper returning true if evening reminder is active.
     */
    val reminderEveningBefore: Boolean
        get() = eveningReminderTime != null

    /**
     * Backward compatibility helper returning active reminder time or default.
     */
    val reminderTime: LocalTime
        get() = eveningReminderTime ?: morningReminderTime ?: LocalTime.of(19, 0)
}

