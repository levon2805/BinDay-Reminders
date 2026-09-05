package com.example.binminder.data.model

/**
 * Data model for configuring a bin during the first-time setup wizard.
 */
data class OnboardingBinSetup(
    val binType: String,
    val displayName: String,
    val presetColor: BinColor,
    val recurrence: RecurrenceType,
    val isEnabled: Boolean = true,
    val startNextWeek: Boolean = false,
    val customNote: String = ""
)
