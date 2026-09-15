package com.example.binminder.data.model

/**
 * Defines collection schedule recurrence intervals for UK bin collections.
 * 
 * Specifies how frequently collections repeat, such as weekly or fortnightly.
 */
enum class RecurrenceType(
    val displayName: String,
    val intervalWeeks: Int
) {
    WEEKLY("Weekly", 1),
    FORTNIGHTLY("Fortnightly", 2),
    EVERY_3_WEEKS("Every 3 Weeks", 3),
    EVERY_4_WEEKS("Every 4 Weeks", 4),
    EVERY_5_WEEKS("Every 5 Weeks", 5),
    EVERY_6_WEEKS("Every 6 Weeks", 6),
    EVERY_7_WEEKS("Every 7 Weeks", 7),
    EVERY_8_WEEKS("Every 8 Weeks", 8);

    companion object {
        /**
         * Safely resolves a recurrence type from its string representation, defaulting to fortnightly.
         */
        fun fromName(name: String): RecurrenceType {
            return entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: FORTNIGHTLY
        }
    }
}
