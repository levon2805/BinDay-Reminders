package com.example.binminder.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ColorLens
import androidx.compose.material.icons.rounded.GridOn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.binminder.data.model.BinColor

/**
 * Data representation of a curated color swatch item.
 */
data class ColorSwatchItem(
    val name: String,
    val hex: String,
    val presetColor: BinColor? = null
)

/**
 * Pre-curated color swatch categories covering Reds, Pinks, Purples, Indigos, Blues, Cyans,
 * Teals, Greens, Limes, Yellows, Ambers, Oranges, Browns, Greys, and Blacks.
 */
val CuratedColorCategories: List<Pair<String, List<ColorSwatchItem>>> = listOf(
    "Reds & Pinks" to listOf(
        ColorSwatchItem("Red", "#D32F2F", BinColor.RED),
        ColorSwatchItem("Dark Red", "#B71C1C"),
        ColorSwatchItem("Pink", "#E91E63"),
        ColorSwatchItem("Magenta", "#C2185B", BinColor.MAGENTA)
    ),
    "Purples & Indigos" to listOf(
        ColorSwatchItem("Purple", "#7B1FA2", BinColor.PURPLE),
        ColorSwatchItem("Deep Purple", "#4A148C"),
        ColorSwatchItem("Indigo", "#3F51B5")
    ),
    "Blues & Cyans" to listOf(
        ColorSwatchItem("Blue", "#1E88E5", BinColor.BLUE),
        ColorSwatchItem("Dark Blue", "#0D47A1"),
        ColorSwatchItem("Cyan", "#00ACC1")
    ),
    "Teals & Greens" to listOf(
        ColorSwatchItem("Teal", "#009688", BinColor.CUSTOM),
        ColorSwatchItem("Green", "#388E3C", BinColor.GREEN),
        ColorSwatchItem("Dark Green", "#1B5E20"),
        ColorSwatchItem("Lime", "#7CB342")
    ),
    "Yellows, Ambers & Oranges" to listOf(
        ColorSwatchItem("Yellow", "#FBC02D", BinColor.YELLOW),
        ColorSwatchItem("Amber", "#FFA000"),
        ColorSwatchItem("Orange", "#F57C00", BinColor.ORANGE)
    ),
    "Browns & Burgundy" to listOf(
        ColorSwatchItem("Brown", "#6D4C41", BinColor.BROWN),
        ColorSwatchItem("Burgundy", "#800020", BinColor.BURGUNDY)
    ),
    "Greys & Blacks" to listOf(
        ColorSwatchItem("Light Grey", "#B0BEC5", BinColor.LIGHT_GREY),
        ColorSwatchItem("Grey", "#757575", BinColor.GREY),
        ColorSwatchItem("Dark Grey", "#424242", BinColor.DARK_GREY),
        ColorSwatchItem("Black", "#212121", BinColor.BLACK)
    )
)

/**
 * Visual Colour Picker Dialog providing both a curated swatch grid and an interactive
 * Hue & Brightness slider/wheel for custom color selection, with live wheelie bin icon preview.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ColorPickerDialog(
    title: String,
    initialColorHex: String,
    initialPresetColor: BinColor? = null,
    onColorSelected: (BinColor, String) -> Unit,
    onDismissRequest: () -> Unit
) {
    var selectedColorHex by remember { mutableStateOf(initialColorHex.ifBlank { "#212121" }) }
    var selectedPreset by remember { mutableStateOf(initialPresetColor ?: findMatchingBinColor(selectedColorHex)) }
    var selectedTabIndex by remember { mutableIntStateOf(0) }

    // HSV state for custom color sliders
    val initialColor = parseBinColor(selectedColorHex, selectedPreset)
    val initialHsv = remember(initialColorHex) {
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(
            android.graphics.Color.argb(
                255,
                (initialColor.red * 255).toInt(),
                (initialColor.green * 255).toInt(),
                (initialColor.blue * 255).toInt()
            ),
            hsv
        )
        hsv
    }

    var hue by remember { mutableFloatStateOf(initialHsv[0]) }
    var saturation by remember { mutableFloatStateOf(if (initialHsv[1] == 0f) 0.8f else initialHsv[1]) }
    var value by remember { mutableFloatStateOf(if (initialHsv[2] == 0f) 0.8f else initialHsv[2]) }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(
                onClick = {
                    onColorSelected(selectedPreset, selectedColorHex)
                    onDismissRequest()
                }
            ) {
                Text("Apply Colour", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Cancel")
            }
        },
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Live Wheelie Bin Icon Preview Card
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surface)
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            WheelieBinVisualSwatch(
                                presetColor = selectedPreset,
                                colorHex = selectedColorHex,
                                size = 36.dp
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = if (selectedPreset != BinColor.CUSTOM) {
                                    selectedPreset.displayName
                                } else {
                                    "Custom Colour"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = selectedColorHex.uppercase(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Tab Selector: Swatch Grid vs Custom Wheel / Sliders
                TabRow(selectedTabIndex = selectedTabIndex) {
                    Tab(
                        selected = selectedTabIndex == 0,
                        onClick = { selectedTabIndex = 0 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.GridOn,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Swatches")
                            }
                        }
                    )
                    Tab(
                        selected = selectedTabIndex == 1,
                        onClick = { selectedTabIndex = 1 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.ColorLens,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Wheel & Custom")
                            }
                        }
                    )
                }

                when (selectedTabIndex) {
                    0 -> {
                        // Swatches Grid
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            CuratedColorCategories.forEach { (categoryName, swatches) ->
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        text = categoryName,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )

                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        swatches.forEach { swatch ->
                                            val swatchColor = parseBinColor(swatch.hex, swatch.presetColor ?: BinColor.CUSTOM)
                                            val isSelected = selectedColorHex.equals(swatch.hex, ignoreCase = true)

                                            Box(
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .clip(CircleShape)
                                                    .background(swatchColor)
                                                    .border(
                                                        width = if (isSelected) 3.dp else 1.dp,
                                                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.5f),
                                                        shape = CircleShape
                                                    )
                                                    .clickable {
                                                        selectedColorHex = swatch.hex
                                                        selectedPreset = swatch.presetColor ?: findMatchingBinColor(swatch.hex)
                                                    },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (isSelected) {
                                                    Icon(
                                                        imageVector = Icons.Rounded.Check,
                                                        contentDescription = swatch.name,
                                                        tint = getContrastingTextColor(swatchColor),
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    1 -> {
                        // Custom Hue & Brightness Sliders
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            // Hue Slider
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "Hue (${hue.toInt()}°)",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(14.dp)
                                        .clip(RoundedCornerShape(7.dp))
                                        .background(
                                            Brush.horizontalGradient(
                                                listOf(
                                                    Color.Red, Color.Yellow, Color.Green,
                                                    Color.Cyan, Color.Blue, Color.Magenta, Color.Red
                                                )
                                            )
                                        )
                                )

                                Slider(
                                    value = hue,
                                    onValueChange = { newHue ->
                                        hue = newHue
                                        val newColor = Color.hsv(hue, saturation, value)
                                        selectedColorHex = colorToHex(newColor)
                                        selectedPreset = findMatchingBinColor(selectedColorHex)
                                    },
                                    valueRange = 0f..360f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = Color.hsv(hue, 1f, 1f)
                                    )
                                )
                            }

                            // Brightness / Value Slider
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "Brightness (${(value * 100).toInt()}%)",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(14.dp)
                                        .clip(RoundedCornerShape(7.dp))
                                        .background(
                                            Brush.horizontalGradient(
                                                listOf(Color.Black, Color.hsv(hue, saturation, 1f))
                                            )
                                        )
                                )

                                Slider(
                                    value = value,
                                    onValueChange = { newValue ->
                                        value = newValue
                                        val newColor = Color.hsv(hue, saturation, value)
                                        selectedColorHex = colorToHex(newColor)
                                        selectedPreset = findMatchingBinColor(selectedColorHex)
                                    },
                                    valueRange = 0.05f..1f
                                )
                            }

                            // Saturation Slider
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "Saturation (${(saturation * 100).toInt()}%)",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(14.dp)
                                        .clip(RoundedCornerShape(7.dp))
                                        .background(
                                            Brush.horizontalGradient(
                                                listOf(Color.White, Color.hsv(hue, 1f, value))
                                            )
                                        )
                                )

                                Slider(
                                    value = saturation,
                                    onValueChange = { newSat ->
                                        saturation = newSat
                                        val newColor = Color.hsv(hue, saturation, value)
                                        selectedColorHex = colorToHex(newColor)
                                        selectedPreset = findMatchingBinColor(selectedColorHex)
                                    },
                                    valueRange = 0f..1f
                                )
                            }

                            // Manual Hex Code Input
                            OutlinedTextField(
                                value = selectedColorHex,
                                onValueChange = { input ->
                                    selectedColorHex = input
                                    selectedPreset = findMatchingBinColor(input)
                                },
                                label = { Text("Hex Code") },
                                placeholder = { Text("#1E88E5") },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    )
}
