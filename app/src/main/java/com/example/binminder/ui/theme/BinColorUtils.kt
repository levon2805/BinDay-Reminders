package com.example.binminder.ui.theme

import androidx.compose.ui.graphics.Color
import com.example.binminder.data.model.BinColor
import com.example.binminder.data.model.RecurrenceType
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Parses a hex colour string or preset into a Jetpack Compose [Color].
 */
fun parseBinColor(colorHex: String, presetColor: BinColor): Color {
    return try {
        val cleanHex = colorHex.trim().removePrefix("#")
        val colorInt = when (cleanHex.length) {
            6 -> (0xFF000000 or cleanHex.toLong(16)).toInt()
            8 -> cleanHex.toLong(16).toInt()
            else -> presetColor.argbColor.toInt()
        }
        Color(colorInt)
    } catch (e: Exception) {
        Color(presetColor.argbColor)
    }
}

/**
 * Returns either dark or light text colour to ensure readable contrast on top of a background colour.
 */
fun getContrastingTextColor(backgroundColor: Color): Color {
    val luminance = 0.299f * backgroundColor.red + 0.587f * backgroundColor.green + 0.114f * backgroundColor.blue
    return if (luminance > 0.55f) Color(0xFF1C1B1F) else Color.White
}

/**
 * Formats recurrence frequency and start day into a friendly text string.
 */
fun formatRecurrenceLabel(recurrence: RecurrenceType, startDate: LocalDate): String {
    val dayOfWeekName = startDate.dayOfWeek.name.lowercase().replaceFirstChar { it.titlecase(Locale.UK) }
    return when (recurrence) {
        RecurrenceType.WEEKLY -> "Weekly on $dayOfWeekName"
        RecurrenceType.FORTNIGHTLY -> "Fortnightly on $dayOfWeekName"
        RecurrenceType.EVERY_3_WEEKS -> "Every 3 weeks on $dayOfWeekName"
        RecurrenceType.EVERY_4_WEEKS -> "Every 4 weeks on $dayOfWeekName"
    }
}

/**
 * Formats a date using standard British English date order.
 */
fun formatBritishDate(date: LocalDate, includeDayOfWeek: Boolean = true): String {
    val pattern = if (includeDayOfWeek) "EEEE, d MMMM yyyy" else "d MMMM yyyy"
    return date.format(DateTimeFormatter.ofPattern(pattern, Locale.UK))
}

/**
 * Returns a human readable relative day label such as TODAY, TOMORROW, or count of days.
 */
fun formatRelativeDays(days: Long): String {
    return when (days) {
        0L -> "TODAY"
        1L -> "TOMORROW"
        in 2..13 -> "In $days days"
        else -> "In ${days / 7} weeks"
    }
}
