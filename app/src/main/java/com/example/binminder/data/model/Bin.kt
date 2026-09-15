package com.example.binminder.data.model

import java.time.LocalDate

/**
 * Data model representing a UK household wheelie bin or collection container.
 * 
 * Holds information about the bin name, chosen body and lid colours, collection recurrence pattern,
 * start date, and whether collections automatically shift for bank holidays.
 */
data class Bin(
    val id: Long = 0,
    val name: String,
    val colorHex: String,
    val presetColor: BinColor = BinColor.BLACK,
    val lidColorHex: String? = null,
    val lidPresetColor: BinColor? = null,
    val recurrence: RecurrenceType = RecurrenceType.FORTNIGHTLY,
    val repeatIntervalWeeks: Int = recurrence.intervalWeeks,
    val startDate: LocalDate = LocalDate.now(),
    val customNote: String = "",
    val isEnabled: Boolean = true,
    val adjustForBankHolidays: Boolean = true
) {
    /**
     * Display helper returning descriptive text, e.g. "Black Bin with Blue Lid" or "Blue Bin".
     */
    val colorDisplayName: String
        get() {
            val bodyName = presetColor.displayName
            val lidName = lidPresetColor?.displayName
            return if ((lidPresetColor != null) && (lidPresetColor != BinColor.CUSTOM) && (lidPresetColor != presetColor)) {
                if (bodyName.endsWith("Bin", ignoreCase = true)) {
                    "$bodyName with $lidName Lid"
                } else {
                    "$bodyName Bin with $lidName Lid"
                }
            } else if (bodyName.endsWith("Bin", ignoreCase = true)) {
                bodyName
            } else {
                "$bodyName Bin"
            }
        }
}
