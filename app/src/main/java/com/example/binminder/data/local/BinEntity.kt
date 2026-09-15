package com.example.binminder.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.binminder.data.model.Bin
import com.example.binminder.data.model.BinColor
import com.example.binminder.data.model.RecurrenceType
import java.time.LocalDate

/**
 * Room database entity representing a household bin in local persistence.
 * 
 * Stores bin properties such as name, hex colour code, lid colour, recurrence pattern, and start date.
 */
@Entity(tableName = "bins")
data class BinEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val colorHex: String,
    val presetColor: String,
    val lidColorHex: String? = null,
    val lidPresetColor: String? = null,
    val recurrence: String,
    val repeatIntervalWeeks: Int,
    val startDateEpochDay: Long,
    val customNote: String,
    val isEnabled: Boolean,
    val adjustForBankHolidays: Boolean
) {
    /**
     * Converts this database entity into a domain model bin object.
     */
    fun toDomain(): Bin {
        return Bin(
            id = id,
            name = name,
            colorHex = colorHex,
            presetColor = BinColor.fromName(presetColor),
            lidColorHex = lidColorHex,
            lidPresetColor = lidPresetColor?.let { BinColor.fromName(it) },
            recurrence = RecurrenceType.fromName(recurrence),
            repeatIntervalWeeks = repeatIntervalWeeks,
            startDate = LocalDate.ofEpochDay(startDateEpochDay),
            customNote = customNote,
            isEnabled = isEnabled,
            adjustForBankHolidays = adjustForBankHolidays
        )
    }

    companion object {
        /**
         * Creates a database entity from a domain model bin object.
         */
        fun fromDomain(bin: Bin): BinEntity {
            return BinEntity(
                id = bin.id,
                name = bin.name,
                colorHex = bin.colorHex,
                presetColor = bin.presetColor.name,
                lidColorHex = bin.lidColorHex,
                lidPresetColor = bin.lidPresetColor?.name,
                recurrence = bin.recurrence.name,
                repeatIntervalWeeks = bin.repeatIntervalWeeks,
                startDateEpochDay = bin.startDate.toEpochDay(),
                customNote = bin.customNote,
                isEnabled = bin.isEnabled,
                adjustForBankHolidays = bin.adjustForBankHolidays
            )
        }
    }
}
