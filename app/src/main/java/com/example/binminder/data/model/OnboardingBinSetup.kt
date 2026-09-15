package com.example.binminder.data.model

/**
 * Model for configuring individual bins during the initial setup wizard.
 * 
 * Allows users to review and customise preset bin configurations before saving them.
 */
data class OnboardingBinSetup(
    val binType: String,
    val displayName: String,
    val presetColor: BinColor,
    val lidPresetColor: BinColor? = null,
    val recurrence: RecurrenceType = RecurrenceType.FORTNIGHTLY,
    val isEnabled: Boolean = true,
    val collectionDay: java.time.DayOfWeek? = null,
    val startNextWeek: Boolean = false,
    val customNote: String = ""
)
