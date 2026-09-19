package com.example.binminder.ui.dialogs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.binminder.data.model.Bin
import com.example.binminder.data.model.BinColor
import com.example.binminder.data.model.RecurrenceType
import com.example.binminder.ui.theme.BinMinderTheme
import com.example.binminder.ui.theme.WheelieBinVisualSwatch
import com.example.binminder.ui.theme.neoShadow
import com.example.binminder.util.CalendarExportUtils
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Material 3 AlertDialog enabling users to export individual recurring bin schedules to their calendar app,
 * standard .ics file export, or a step-by-step guided queue export mode.
 */
@Composable
fun CalendarExportDialog(
    bins: List<Bin>,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val activeBins = remember(bins) { bins.filter { it.isEnabled } }

    var isGuidedQueueMode by remember { mutableStateOf(value = false) }
    var queueIndex by remember { mutableIntStateOf(0) }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            if (!isGuidedQueueMode) {
                Text(
                    text = "Sync to Calendar",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
            } else if (queueIndex < activeBins.size) {
                Text(
                    text = "Syncing (${queueIndex + 1}/${activeBins.size})",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
            } else {
                Text(
                    text = "All Done!",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
        },
        text = {
            Column(
                modifier = modifier.wrapContentHeight(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (activeBins.isEmpty()) {
                    Text(
                        text = "No active bins available to export.",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else if (!isGuidedQueueMode) {
                    // 1. Active Bins List at the top
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false),
                    ) {
                        items(activeBins, key = { it.id }) { bin ->
                            val nextDate = CalendarExportUtils.calculateNextCollectionDate(bin)
                            val formattedDate = nextDate.format(DateTimeFormatter.ofPattern("EEE d MMM", Locale.UK))
                            val recurrenceLabel = bin.recurrence.displayName
                            val subtitle = "Next: $formattedDate ($recurrenceLabel)"

                            Surface(
                                onClick = {
                                    CalendarExportUtils.exportBinToCalendar(context, bin, nextDate)
                                    onDismissRequest()
                                },
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .neoShadow(offset = 4.dp),
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    WheelieBinVisualSwatch(
                                        presetColor = bin.presetColor,
                                        colorHex = bin.colorHex,
                                        lidPresetColor = bin.lidPresetColor,
                                        lidColorHex = bin.lidColorHex,
                                        size = 40.dp,
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = bin.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                        )
                                        Text(
                                            text = subtitle,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    IconButton(
                                        onClick = {
                                            CalendarExportUtils.exportBinToCalendar(context, bin, nextDate)
                                            onDismissRequest()
                                        },
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.CalendarToday,
                                            contentDescription = "Export ${bin.name} to calendar",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 2. Clean Action Buttons Underneath (No Explanations)
                    Button(
                        onClick = {
                            CalendarExportUtils.exportAllBinsViaIcs(context, activeBins)
                            onDismissRequest()
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier
                            .fillMaxWidth()
                            .neoShadow(offset = 4.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(vertical = 4.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.FileDownload,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Import All Bins to Calendar (1-Click)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }

                    Button(
                        onClick = {
                            isGuidedQueueMode = true
                            queueIndex = 0
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary,
                            contentColor = MaterialTheme.colorScheme.onSecondary,
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier
                            .fillMaxWidth()
                            .neoShadow(offset = 4.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(vertical = 4.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.CalendarToday,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Add Bins One-by-One (Guided Setup)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                } else if (queueIndex < activeBins.size) {
                    // Guided Queue Mode
                    val currentBin = activeBins[queueIndex]
                    val nextDate = CalendarExportUtils.calculateNextCollectionDate(currentBin)
                    val formattedDate = nextDate.format(DateTimeFormatter.ofPattern("EEE d MMM", Locale.UK))
                    val recurrenceLabel = currentBin.recurrence.displayName

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier
                            .fillMaxWidth()
                            .neoShadow(offset = 4.dp),
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            WheelieBinVisualSwatch(
                                presetColor = currentBin.presetColor,
                                colorHex = currentBin.colorHex,
                                lidPresetColor = currentBin.lidPresetColor,
                                lidColorHex = currentBin.lidColorHex,
                                size = 48.dp,
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = currentBin.name,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    text = "Next: $formattedDate ($recurrenceLabel)",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Prominent Button
                    Button(
                        onClick = {
                            CalendarExportUtils.exportBinToCalendar(context, currentBin, nextDate)
                            queueIndex++
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .neoShadow(offset = 4.dp),
                    ) {
                        Text(
                            text = "Sync It ➔",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Button(
                            onClick = { queueIndex++ },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        ) {
                            Text("Skip", fontWeight = FontWeight.SemiBold)
                        }
                    }
                } else {
                    // All Bins Export Success State
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(64.dp),
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "✓ All Set!",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        },
        confirmButton = {
            if ((isGuidedQueueMode) && (queueIndex >= activeBins.size)) {
                Button(
                    onClick = onDismissRequest,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                ) {
                    Text("Done", fontWeight = FontWeight.SemiBold)
                }
            } else {
                OutlinedButton(
                    onClick = {
                        if (isGuidedQueueMode) {
                            isGuidedQueueMode = false
                            queueIndex = 0
                        } else {
                            onDismissRequest()
                        }
                    },
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                ) {
                    Text(
                        text = if (isGuidedQueueMode) "Back" else "Cancel",
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        },
    )
}

@Preview(showBackground = true)
@Composable
fun CalendarExportDialogPreview() {
    val sampleBins = listOf(
        Bin(
            id = 1,
            name = "General Waste",
            colorHex = BinColor.BLACK.defaultHex,
            presetColor = BinColor.BLACK,
            recurrence = RecurrenceType.FORTNIGHTLY,
            repeatIntervalWeeks = 2,
            startDate = LocalDate.now(),
        ),
        Bin(
            id = 2,
            name = "Dry Mixed Recycling",
            colorHex = BinColor.BLUE.defaultHex,
            presetColor = BinColor.BLUE,
            recurrence = RecurrenceType.FORTNIGHTLY,
            repeatIntervalWeeks = 2,
            startDate = LocalDate.now().plusWeeks(1),
        ),
        Bin(
            id = 3,
            name = "Garden Waste",
            colorHex = BinColor.GREEN.defaultHex,
            presetColor = BinColor.GREEN,
            recurrence = RecurrenceType.EVERY_3_WEEKS,
            repeatIntervalWeeks = 3,
            startDate = LocalDate.now().plusDays(3),
        ),
    )

    BinMinderTheme {
        CalendarExportDialog(
            bins = sampleBins,
            onDismissRequest = {},
        )
    }
}
