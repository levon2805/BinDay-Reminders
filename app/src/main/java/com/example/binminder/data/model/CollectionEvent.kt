package com.example.binminder.data.model

import java.time.LocalDate

/**
 * Data model representing a specific bin collection occurrence on a calculated date.
 * 
 * Contains details about which bin is due, its display colour, lid colour, the date it is due out,
 * and whether the collection date was moved due to a public bank holiday.
 */
data class CollectionEvent(
    val binId: Long,
    val binName: String,
    val binColorHex: String,
    val presetColor: BinColor,
    val lidColorHex: String? = null,
    val lidPresetColor: BinColor? = null,
    val collectionDate: LocalDate,
    val originalDate: LocalDate,
    val isBankHolidayAdjusted: Boolean,
    val customNote: String = "",
    val repeatIntervalWeeks: Int = 2
)
