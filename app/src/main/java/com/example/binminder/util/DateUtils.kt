package com.example.binminder.util

import android.content.Context
import android.text.format.DateFormat
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Utility functions for date and time formatting aligned with system user preferences.
 */
object DateUtils {

    private val FORMATTER_12H = DateTimeFormatter.ofPattern("h:mm a", Locale.UK)
    private val FORMATTER_24H = DateTimeFormatter.ofPattern("HH:mm", Locale.UK)

    /**
     * Formats a [LocalTime] according to the system 12-hour or 24-hour clock setting.
     * 
     * @param time The [LocalTime] to format.
     * @param context The Android [Context] used to check [DateFormat.is24HourFormat].
     * @return Formatted string e.g. "7:00 PM" (12h) or "19:00" (24h).
     */
    fun formatTimeForUser(time: LocalTime, context: Context): String {
        val is24Hour = DateFormat.is24HourFormat(context)
        return formatTime(time, is24Hour)
    }

    /**
     * Formats a [LocalTime] according to an explicit [is24Hour] flag.
     * 
     * @param time The [LocalTime] to format.
     * @param is24Hour True for "19:00", false for "7:00 PM".
     * @return Formatted string.
     */
    fun formatTime(time: LocalTime, is24Hour: Boolean): String {
        val formatter = if (is24Hour) FORMATTER_24H else FORMATTER_12H
        val formatted = time.format(formatter)
        return if (is24Hour) formatted else formatted.uppercase(Locale.UK)
    }
}

/**
 * Top-level convenience wrapper for [DateUtils.formatTimeForUser].
 */
fun formatTimeForUser(time: LocalTime, context: Context): String {
    return DateUtils.formatTimeForUser(time, context)
}
