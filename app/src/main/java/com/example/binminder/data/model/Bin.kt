package com.example.binminder.data.model

import java.time.LocalDate

/**
 * Data model representing a UK household wheelie bin or collection container.
 * 
 * Holds information about the bin name, chosen colour, collection recurrence pattern,
 * start date, and whether collections automatically shift for bank holidays.
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
