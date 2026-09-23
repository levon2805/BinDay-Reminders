package com.example.binminder.data.model

import java.time.LocalTime

/**
 * User preferences for bin collection reminders and notifications.
 * 
 * Controls whether reminders are active, sets of reminder times for evening before and morning of collection
 * (or empty set if NONE), and the preferred application theme mode.
 */
data class NotificationSettings(
    val reminderEnabled: Boolean = true,
    val primaryEveningTime: LocalTime? = LocalTime.of(19, 0),
    val primaryMorningTime: LocalTime? = LocalTime.of(7, 0),
    val eveningReminderTimes: Set<LocalTime> = setOfNotNull(primaryEveningTime),
    val morningReminderTimes: Set<LocalTime> = setOfNotNull(primaryMorningTime),
    val themeMode: AppThemeMode = AppThemeMode.SYSTEM
) {
    /**
     * Secondary constructor for backward compatibility supporting single nullable times.
     */
    constructor(
        reminderEnabled: Boolean = true,
        eveningReminderTime: LocalTime?,
        morningReminderTime: LocalTime? = LocalTime.of(7, 0),
        themeMode: AppThemeMode = AppThemeMode.SYSTEM
    ) : this(
        reminderEnabled = reminderEnabled,
        primaryEveningTime = eveningReminderTime,
        primaryMorningTime = morningReminderTime,
        eveningReminderTimes = if (eveningReminderTime != null) setOf(eveningReminderTime) else emptySet(),
        morningReminderTimes = if (morningReminderTime != null) setOf(morningReminderTime) else emptySet(),
        themeMode = themeMode
    )

    /**
     * Helper returning primary evening reminder time if active/configured, or null.
     */
    val eveningReminderTime: LocalTime?
        get() = if (primaryEveningTime != null && primaryEveningTime in eveningReminderTimes) {
            primaryEveningTime
        } else if (primaryEveningTime == null) {
            null
        } else {
            eveningReminderTimes.firstOrNull()
        }

    /**
     * Helper returning primary morning reminder time if active/configured, or null.
     */
    val morningReminderTime: LocalTime?
        get() = if (primaryMorningTime != null && primaryMorningTime in morningReminderTimes) {
            primaryMorningTime
        } else if (primaryMorningTime == null) {
            null
        } else {
            morningReminderTimes.firstOrNull()
        }

    /**
     * Backward compatibility helper returning true if evening reminder is active.
     */
    val reminderEveningBefore: Boolean
        get() = eveningReminderTimes.isNotEmpty()

    /**
     * Backward compatibility helper returning active reminder time or default.
     */
    val reminderTime: LocalTime
        get() = eveningReminderTime ?: morningReminderTime ?: LocalTime.of(19, 0)
}
