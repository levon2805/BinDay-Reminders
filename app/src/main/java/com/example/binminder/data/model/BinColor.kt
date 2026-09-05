package com.example.binminder.data.model

/**
 * Common preset colours for UK wheelie bins and recycling containers.
 */
enum class BinColor(
    val displayName: String,
    val defaultHex: String,
    val argbColor: Long
) {
    BLACK("Black / Dark Grey", "#212121", 0xFF212121),
    BLUE("Blue", "#1E88E5", 0xFF1E88E5),
    GREEN("Green", "#388E3C", 0xFF388E3C),
    BROWN("Brown", "#6D4C41", 0xFF6D4C41),
    PURPLE("Purple", "#7B1FA2", 0xFF7B1FA2),
    RED("Red", "#D32F2F", 0xFFD32F2F),
    GREY("Grey", "#757575", 0xFF757575),
    YELLOW("Yellow", "#FBC02D", 0xFFFBC02D),
    CUSTOM("Custom", "#009688", 0xFF009688);

    companion object {
        fun fromName(name: String): BinColor {
            return entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: BLACK
        }
    }
}
