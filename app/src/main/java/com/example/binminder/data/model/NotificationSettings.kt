package com.example.binminder.data.model

import java.time.LocalTime

/**
 * Settings for collection reminder notifications.
 */
data class NotificationSettings(
    val reminderEnabled: Boolean = true,
    val reminderTime: LocalTime = LocalTime.of(19, 0), // Default 19:00 (7:00 PM)
    val reminderEveningBefore: Boolean = true, // Evening before collection day
    val themeMode: AppThemeMode = AppThemeMode.SYSTEM
)

