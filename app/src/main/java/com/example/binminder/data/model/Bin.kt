package com.example.binminder.data.model

import java.time.LocalDate

/**
 * Data model representing a UK household wheelie bin or collection container.
 */
data class Bin(
    val id: Long = 0,
    val name: String,
    val colorHex: String,
    val presetColor: BinColor = BinColor.BLACK,
    val recurrence: RecurrenceType = RecurrenceType.FORTNIGHTLY,
    val repeatIntervalWeeks: Int = recurrence.intervalWeeks,
    val startDate: LocalDate = LocalDate.now(),
    val customNote: String = "",
    val isEnabled: Boolean = true,
    val adjustForBankHolidays: Boolean = true
)
