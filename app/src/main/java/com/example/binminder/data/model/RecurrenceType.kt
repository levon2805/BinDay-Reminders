package com.example.binminder.data.model

/**
 * Defines collection schedule recurrence intervals for UK bin collections.
 */
enum class RecurrenceType(
    val displayName: String,
    val intervalWeeks: Int
) {
    WEEKLY("Weekly", 1),
    FORTNIGHTLY("Fortnightly", 2),
    EVERY_3_WEEKS("Every 3 Weeks", 3),
    EVERY_4_WEEKS("Every 4 Weeks", 4);

    companion object {
        fun fromName(name: String): RecurrenceType {
            return entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: FORTNIGHTLY
        }
    }
}
