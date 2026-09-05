package com.example.binminder.data.model

import java.time.LocalDate

/**
 * Data model representing a specific bin collection occurrence on a calculated date.
 */
data class CollectionEvent(
    val binId: Long,
    val binName: String,
    val binColorHex: String,
    val presetColor: BinColor,
    val collectionDate: LocalDate,
    val originalDate: LocalDate,
    val isBankHolidayAdjusted: Boolean,
    val customNote: String = ""
)
