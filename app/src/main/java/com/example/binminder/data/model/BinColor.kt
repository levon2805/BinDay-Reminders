package com.example.binminder.data.model

/**
 * Preset colours commonly used for UK wheelie bins, lids, and recycling containers.
 * 
 * Provides display names and default hex values to make bin selection intuitive.
 */
enum class BinColor(
    val displayName: String,
    val defaultHex: String,
    val argbColor: Long
) {
    BLACK("Black", "#212121", 0xFF212121),
    DARK_GREY("Dark Grey", "#424242", 0xFF424242),
    LIGHT_GREY("Light Grey", "#B0BEC5", 0xFFB0BEC5),
    GREY("Grey", "#757575", 0xFF757575),
    BLUE("Blue", "#1E88E5", 0xFF1E88E5),
    GREEN("Green", "#388E3C", 0xFF388E3C),
    BROWN("Brown", "#6D4C41", 0xFF6D4C41),
    YELLOW("Yellow", "#FBC02D", 0xFFFBC02D),
    RED("Red", "#D32F2F", 0xFFD32F2F),
    PURPLE("Purple", "#7B1FA2", 0xFF7B1FA2),
    ORANGE("Orange", "#F57C00", 0xFFF57C00),
    BURGUNDY("Burgundy", "#800020", 0xFF800020),
    MAGENTA("Magenta", "#C2185B", 0xFFC2185B),
    CUSTOM("Custom", "#009688", 0xFF009688);

    companion object {
        /**
         * Safely finds a matching bin colour by its string name or display name, defaulting to BLACK if unmatched.
         */
        fun fromName(name: String?): BinColor {
            if (name.isNullOrBlank()) return BLACK
            val trimmed = name.trim()
            return entries.firstOrNull { it.name.equals(trimmed, ignoreCase = true) }
                ?: entries.firstOrNull { it.displayName.equals(trimmed, ignoreCase = true) }
                ?: when (trimmed.uppercase()) {
                    "BLACK / DARK GREY" -> BLACK
                    "DARK_GRAY" -> DARK_GREY
                    "LIGHT_GRAY" -> LIGHT_GREY
                    "GRAY" -> GREY
                    else -> BLACK
                }
        }
    }
}
