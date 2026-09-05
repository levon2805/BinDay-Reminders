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
    val recurrence: RecurrenceType,
    val isEnabled: Boolean = true,
    val startNextWeek: Boolean = false,
    val customNote: String = ""
)
