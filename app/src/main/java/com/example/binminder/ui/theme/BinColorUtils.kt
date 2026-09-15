package com.example.binminder.ui.theme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.binminder.data.model.BinColor
import com.example.binminder.data.model.RecurrenceType
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Parses a hex colour string or preset into a Jetpack Compose [Color].
 */
fun parseBinColor(colorHex: String?, presetColor: BinColor): Color {
    if (colorHex.isNullOrBlank()) {
        return Color(presetColor.argbColor)
    }
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
 * Converts a Jetpack Compose [Color] into a standard 6-character hex string (e.g. "#1E88E5").
 */
fun colorToHex(color: Color): String {
    val r = (color.red * 255).toInt().coerceIn(0, 255)
    val g = (color.green * 255).toInt().coerceIn(0, 255)
    val b = (color.blue * 255).toInt().coerceIn(0, 255)
    return String.format(Locale.ROOT, "#%02X%02X%02X", r, g, b)
}

/**
 * Finds a matching preset [BinColor] for a given hex code, or returns [BinColor.CUSTOM] if no preset matches.
 */
fun findMatchingBinColor(colorHex: String?): BinColor {
    if (colorHex.isNullOrBlank()) return BinColor.BLACK
    val cleanHex = colorHex.trim().removePrefix("#").uppercase(Locale.ROOT)
    val fullCleanHex = if (cleanHex.length == 6) "#$cleanHex" else colorHex.uppercase(Locale.ROOT)
    for (preset in BinColor.entries) {
        if (preset == BinColor.CUSTOM) continue
        if (preset.defaultHex.equals(fullCleanHex, ignoreCase = true) ||
            preset.defaultHex.removePrefix("#").equals(cleanHex, ignoreCase = true)) {
            return preset
        }
    }
    return BinColor.CUSTOM
}

/**
 * Generates an accurate, color-aware default note string for a bin based on its name and colours.
 */
fun getDefaultNotes(
    binName: String,
    bodyColor: BinColor,
    lidColor: BinColor? = null
): String {
    val cleanName = binName.ifBlank { "waste" }.trim().lowercase(Locale.UK)
    val bodyName = bodyColor.displayName.lowercase(Locale.UK)

    return if (lidColor != null && lidColor != bodyColor && lidColor != BinColor.CUSTOM) {
        val lidName = lidColor.displayName.lowercase(Locale.UK)
        "Standard $bodyName wheelie bin with $lidName lid for $cleanName"
    } else {
        "Standard $bodyName wheelie bin for $cleanName"
    }
}

/**
 * Checks whether a note string matches the default generated note template or legacy default strings.
 */
fun isDefaultNote(note: String, binName: String): Boolean {
    val trimmed = note.trim()
    if (trimmed.isBlank()) return true

    val legacyDefaults = setOf(
        "Black bin for non-recyclable household waste.",
        "Blue bin for paper, cardboard, plastic bottles, and cans.",
        "Green bin for grass cuttings and garden clippings.",
        "Brown caddy for kitchen food leftovers.",
        "Added custom bin."
    )
    if (trimmed in legacyDefaults) return true

    for (body in BinColor.entries) {
        if (trimmed.equals(getDefaultNotes(binName, body, null), ignoreCase = true)) return true
        for (lid in BinColor.entries) {
            if (trimmed.equals(getDefaultNotes(binName, body, lid), ignoreCase = true)) return true
        }
    }

    if (trimmed.startsWith("Standard ", ignoreCase = true) &&
        trimmed.contains(" wheelie bin", ignoreCase = true)) {
        return true
    }

    return false
}

/**
 * Formats recurrence frequency and start day into a friendly text string.
 */
fun formatRecurrenceLabel(recurrence: RecurrenceType, startDate: LocalDate): String {
    val dayOfWeekName = startDate.dayOfWeek.name.lowercase().replaceFirstChar { it.titlecase(Locale.UK) }
    return when (recurrence) {
        RecurrenceType.WEEKLY -> "Weekly on $dayOfWeekName"
        RecurrenceType.FORTNIGHTLY -> "Fortnightly on $dayOfWeekName"
        else -> "Every ${recurrence.intervalWeeks} weeks on $dayOfWeekName"
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

/**
 * Renders a visual wheelie bin icon / swatch with distinct body and lid colours, glossy rims, handles, and wheels.
 */
@Composable
fun WheelieBinVisualSwatch(
    presetColor: BinColor,
    modifier: Modifier = Modifier,
    colorHex: String = presetColor.defaultHex,
    lidPresetColor: BinColor? = null,
    lidColorHex: String? = null,
    size: Dp = 36.dp,
    isEnabled: Boolean = true
) {
        val bodyColor = parseBinColor(colorHex, presetColor)
        val lidColor = lidPresetColor?.let { parseBinColor(lidColorHex, it) } ?: bodyColor
        val alpha = if (isEnabled) 1.0f else 0.4f
        val strokeWidth = 1.4f
        val outlineColor = Color(0x33000000).copy(alpha = 0.2f * alpha)
    
        Canvas(
            modifier = modifier.size(size)
        ) {
            val w = size.toPx()
            val h = size.toPx()
    
            // Bin Body tapered coordinates
            val bodyTopLeftX = w * 0.15f
            val bodyTopRightX = w * 0.85f
            val bodyTopY = h * 0.28f
    
            val bodyBottomLeftX = w * 0.22f
            val bodyBottomRightX = w * 0.78f
            val bodyBottomY = h * 0.86f
    
            // Draw Wheels at bottom left and bottom right
            val wheelRadius = w * 0.12f
            // Outer tyre
            drawCircle(
                color = Color.Black.copy(alpha = 0.8f * alpha),
                radius = wheelRadius,
                center = Offset(bodyBottomLeftX, bodyBottomY)
            )
            drawCircle(
                color = Color.Black.copy(alpha = 0.8f * alpha),
                radius = wheelRadius,
                center = Offset(bodyBottomRightX, bodyBottomY)
            )
            // Hubcap rim
            drawCircle(
                color = Color.White.copy(alpha = alpha),
                radius = wheelRadius * 0.35f,
                center = Offset(bodyBottomLeftX, bodyBottomY)
            )
            drawCircle(
                color = Color.White.copy(alpha = alpha),
                radius = wheelRadius * 0.35f,
                center = Offset(bodyBottomRightX, bodyBottomY)
            )
    
            // Draw Bin Body Path
            val bodyPath = Path().apply {
                moveTo(bodyTopLeftX, bodyTopY)
                lineTo(bodyTopRightX, bodyTopY)
                lineTo(bodyBottomRightX, bodyBottomY)
                lineTo(bodyBottomLeftX, bodyBottomY)
                close()
            }
            drawPath(
                path = bodyPath,
                color = bodyColor.copy(alpha = alpha)
            )
    
            // Body outline stroke
            drawPath(
                path = bodyPath,
                color = outlineColor,
                style = Stroke(width = strokeWidth)
            )
            
            // Sleek bin, no faces.
    
            // Draw Lid Handle
            val handleWidth = w * 0.35f
            val handleHeight = h * 0.08f
            val handleLeft = (w - handleWidth) / 2f
            val handleTop = h * 0.04f
            drawRoundRect(
                color = lidColor.copy(alpha = alpha),
                topLeft = Offset(handleLeft, handleTop),
                size = Size(handleWidth, handleHeight),
                cornerRadius = CornerRadius(handleHeight / 2f, handleHeight / 2f)
            )
            // Handle outline
            drawRoundRect(
                color = outlineColor,
                topLeft = Offset(handleLeft, handleTop),
                size = Size(handleWidth, handleHeight),
                cornerRadius = CornerRadius(handleHeight / 2f, handleHeight / 2f),
                style = Stroke(width = strokeWidth)
            )
    
            // Draw Lid Rim (Glossy Top Lid)
            val lidWidth = w * 0.80f
            val lidHeight = h * 0.16f
            val lidLeft = (w - lidWidth) / 2f
            val lidTop = h * 0.11f
    
            drawRoundRect(
                color = lidColor.copy(alpha = alpha),
                topLeft = Offset(lidLeft, lidTop),
                size = Size(lidWidth, lidHeight),
                cornerRadius = CornerRadius(w * 0.04f, w * 0.04f)
            )
    
            // Lid outline stroke
            drawRoundRect(
                color = outlineColor,
                topLeft = Offset(lidLeft, lidTop),
                size = Size(lidWidth, lidHeight),
                cornerRadius = CornerRadius(w * 0.04f, w * 0.04f),
                style = Stroke(width = strokeWidth)
            )
        }
}
