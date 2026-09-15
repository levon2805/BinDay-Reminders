package com.example.binminder.ui.addedit

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Label
import androidx.compose.material.icons.automirrored.rounded.Notes
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.EventRepeat
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import com.example.binminder.ui.theme.ColorPickerDialog
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.binminder.data.model.BinColor
import com.example.binminder.data.model.RecurrenceType
import com.example.binminder.ui.theme.BinMinderTheme
import com.example.binminder.ui.theme.WheelieBinVisualSwatch
import com.example.binminder.ui.theme.formatBritishDate
import com.example.binminder.ui.theme.getContrastingTextColor
import com.example.binminder.ui.theme.parseBinColor
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Screen composable for creating or editing a wheelie bin profile.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditBinScreen(
    viewModel: AddEditBinViewModel,
    binId: Long?,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(binId) {
        viewModel.loadBin(binId)
    }

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            onNavigateBack()
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearErrorMessage()
        }
    }

    AddEditBinContent(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onNameChange = { viewModel.onNameChange(it) },
        onPresetColorSelected = { viewModel.onPresetColorSelected(it) },
        onCustomHexChange = { viewModel.onCustomHexChange(it) },
        onLidPresetColorSelected = { viewModel.onLidPresetColorSelected(it) },
        onCustomLidHexChange = { viewModel.onCustomLidHexChange(it) },
        onRecurrenceSelected = { viewModel.onRecurrenceSelected(it) },
        onStartDateSelected = { viewModel.onStartDateSelected(it) },
        onAdjustForBankHolidaysChange = { viewModel.onAdjustForBankHolidaysChange(it) },
        onCustomNoteChange = { viewModel.onCustomNoteChange(it) },
        onSaveBin = { viewModel.saveBin() },
        onNavigateBack = onNavigateBack,
        modifier = modifier
    )
}

/**
 * Form layout structure for bin details editing.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddEditBinContent(
    uiState: AddEditBinUiState,
    snackbarHostState: SnackbarHostState,
    onNameChange: (String) -> Unit,
    onPresetColorSelected: (BinColor) -> Unit,
    onCustomHexChange: (String) -> Unit,
    onLidPresetColorSelected: (BinColor?) -> Unit,
    onCustomLidHexChange: (String) -> Unit,
    onRecurrenceSelected: (RecurrenceType) -> Unit,
    onStartDateSelected: (LocalDate) -> Unit,
    onAdjustForBankHolidaysChange: (Boolean) -> Unit,
    onCustomNoteChange: (String) -> Unit,
    onSaveBin: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showDatePickerDialog by remember { mutableStateOf(false) }
    var showBodyColorPickerDialog by remember { mutableStateOf(false) }
    var showLidColorPickerDialog by remember { mutableStateOf(false) }
    var showBankHolidayInfoDialog by remember { mutableStateOf(false) }

    val isEditing = uiState.binId != null && uiState.binId != 0L
    val screenTitle = if (isEditing) "Edit Bin" else "Add Bin"

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = screenTitle,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Live Bin Preview Card Header with Dual-Colour Swatch
            BinPreviewHeader(
                name = uiState.name.ifBlank { "Bin Name" },
                presetColor = uiState.presetColor,
                colorHex = uiState.colorHex,
                lidPresetColor = uiState.lidPresetColor,
                lidColorHex = uiState.lidColorHex,
                recurrence = uiState.recurrence,
                startDate = uiState.startDate
            )

            // 1. Bin Name Input
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Bin Name",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                OutlinedTextField(
                    value = uiState.name,
                    onValueChange = onNameChange,
                    leadingIcon = {
                        Icon(imageVector = Icons.AutoMirrored.Rounded.Label, contentDescription = null)
                    },
                    placeholder = { Text("e.g., General Waste, Dry Mixed Recycling") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // 2. Bin Body Colour Section
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Palette,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Bin Body Colour",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    BinColor.entries.forEach { preset ->
                        ColorSwatchChip(
                            preset = preset,
                            isSelected = uiState.presetColor == preset,
                            onSelected = {
                                if (preset == BinColor.CUSTOM) {
                                    showBodyColorPickerDialog = true
                                } else {
                                    onPresetColorSelected(preset)
                                }
                            }
                        )
                    }
                }

                if (uiState.isCustomColor) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Custom Colour: ${uiState.colorHex.uppercase()}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            OutlinedButton(
                                onClick = { showBodyColorPickerDialog = true },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Rounded.Palette, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Colour Picker")
                            }
                        }
                    }
                }
            }

            // 3. Bin Lid Colour Section (Dual-Colour Support)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Palette,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Bin Lid Colour (Dual-Colour)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Select distinct lid colour if your council bin has a different lid",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // "Same as Body" option
                    FilterChip(
                        selected = uiState.lidPresetColor == null,
                        onClick = { onLidPresetColorSelected(null) },
                        label = { Text("Same as Body (Solid)") },
                        shape = RoundedCornerShape(12.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    )

                    BinColor.entries.forEach { preset ->
                        ColorSwatchChip(
                            preset = preset,
                            isSelected = uiState.lidPresetColor == preset,
                            onSelected = {
                                if (preset == BinColor.CUSTOM) {
                                    showLidColorPickerDialog = true
                                } else {
                                    onLidPresetColorSelected(preset)
                                }
                            }
                        )
                    }
                }

                if (uiState.isCustomLidColor) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Custom Lid Colour: ${(uiState.lidColorHex ?: "").uppercase()}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            OutlinedButton(
                                onClick = { showLidColorPickerDialog = true },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Rounded.Palette, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Colour Picker")
                            }
                        }
                    }
                }
            }

            // 4. Collection Schedule / Recurrence
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Collection Recurrence",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    RecurrenceType.entries.forEach { recurrence ->
                        val isSelected = uiState.recurrence == recurrence
                        FilterChip(
                            selected = isSelected,
                            onClick = { onRecurrenceSelected(recurrence) },
                            label = { Text(recurrence.displayName) },
                            leadingIcon = if (isSelected) {
                                { Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null,
                            shape = RoundedCornerShape(12.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }
            }

            // 5. First Collection Date Picker
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "First Collection Date",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDatePickerDialog = true }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CalendarToday,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = formatBritishDate(uiState.startDate),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        OutlinedButton(
                            onClick = { showDatePickerDialog = true },
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Edit,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Change",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            // 6. Bank Holiday Shift Toggle
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.EventRepeat,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Adjust for UK Bank Holidays",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                IconButton(
                                    onClick = { showBankHolidayInfoDialog = true },
                                    modifier = Modifier.size(24.dp).padding(start = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Info,
                                        contentDescription = "Bank Holiday Auto-Adjustment Info",
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            Text(
                                text = "Automatically shifts collection by +1 day when on a Bank Holiday",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                            )
                        }
                    }

                    Switch(
                        checked = uiState.adjustForBankHolidays,
                        onCheckedChange = onAdjustForBankHolidaysChange
                    )
                }
            }

            // 7. Custom Instructions / Notes
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Kerbside Instructions / Notes",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                OutlinedTextField(
                    value = uiState.customNote,
                    onValueChange = onCustomNoteChange,
                    leadingIcon = {
                        Icon(imageVector = Icons.AutoMirrored.Rounded.Notes, contentDescription = null)
                    },
                    placeholder = { Text("e.g., Put lid down completely, place next to driveway") },
                    minLines = 2,
                    maxLines = 4,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 8. Save / Cancel Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onNavigateBack,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel")
                }

                Button(
                    onClick = onSaveBin,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(imageVector = Icons.Rounded.Save, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Save Bin")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Material 3 DatePickerDialog
    if (showDatePickerDialog) {
        val initialSelectedMillis = uiState.startDate
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = initialSelectedMillis
        )

        DatePickerDialog(
            onDismissRequest = { showDatePickerDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val selectedMillis = datePickerState.selectedDateMillis
                        if (selectedMillis != null) {
                            val selectedDate = Instant.ofEpochMilli(selectedMillis)
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate()
                            onStartDateSelected(selectedDate)
                        }
                        showDatePickerDialog = false
                    }
                ) {
                    Text("Select")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePickerDialog = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showBodyColorPickerDialog) {
        ColorPickerDialog(
            title = "Select Bin Body Colour",
            initialColorHex = uiState.colorHex,
            initialPresetColor = uiState.presetColor,
            onColorSelected = { preset, hex ->
                if (preset == BinColor.CUSTOM) {
                    onCustomHexChange(hex)
                } else {
                    onPresetColorSelected(preset)
                }
            },
            onDismissRequest = { showBodyColorPickerDialog = false }
        )
    }

    if (showLidColorPickerDialog) {
        ColorPickerDialog(
            title = "Select Bin Lid Colour",
            initialColorHex = uiState.lidColorHex ?: uiState.colorHex,
            initialPresetColor = uiState.lidPresetColor,
            onColorSelected = { preset, hex ->
                if (preset == BinColor.CUSTOM) {
                    onCustomLidHexChange(hex)
                } else {
                    onLidPresetColorSelected(preset)
                }
            },
            onDismissRequest = { showLidColorPickerDialog = false }
        )
    }

    if (showBankHolidayInfoDialog) {
        AlertDialog(
            onDismissRequest = { showBankHolidayInfoDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Rounded.EventRepeat,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = {
                Text(
                    text = "Bank Holiday Auto-Adjustment",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "When a UK bank holiday falls in a collection week, BinDay automatically shifts this bin's collection date by +1 day (e.g. Friday ➔ Saturday).",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(onClick = { showBankHolidayInfoDialog = false }) {
                    Text("Got it")
                }
            }
        )
    }
}

/**
 * Live card preview showing the current bin name and selected dual-colour swatch.
 */
@Composable
fun BinPreviewHeader(
    name: String,
    presetColor: BinColor,
    colorHex: String,
    lidPresetColor: BinColor?,
    lidColorHex: String?,
    recurrence: RecurrenceType,
    startDate: LocalDate
) {
    val binBgColor = parseBinColor(colorHex, presetColor)
    val binTextColor = getContrastingTextColor(binBgColor)

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = binBgColor,
        contentColor = binTextColor,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.25f)),
                contentAlignment = Alignment.Center
            ) {
                WheelieBinVisualSwatch(
                    presetColor = presetColor,
                    colorHex = colorHex,
                    lidPresetColor = lidPresetColor,
                    lidColorHex = lidColorHex,
                    size = 38.dp
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = binTextColor
                )
                val lidDesc = if (lidPresetColor != null && lidPresetColor != presetColor && lidPresetColor != BinColor.CUSTOM) {
                    " (${presetColor.displayName} Bin with ${lidPresetColor.displayName} Lid)"
                } else ""
                Text(
                    text = "${recurrence.displayName}$lidDesc • Starts ${formatBritishDate(startDate, includeDayOfWeek = false)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = binTextColor.copy(alpha = 0.85f)
                )
            }
        }
    }
}

/**
 * Interactive swatch chip representing a council bin colour preset.
 */
@Composable
fun ColorSwatchChip(
    preset: BinColor,
    isSelected: Boolean,
    onSelected: () -> Unit
) {
    val swatchColor = Color(preset.argbColor)
    val textColor = getContrastingTextColor(swatchColor)

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = swatchColor,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onSelected)
            .border(
                width = if (isSelected) 3.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.4f),
                shape = RoundedCornerShape(12.dp)
            )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    tint = textColor,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = preset.displayName,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = textColor
            )
        }
    }
}

/**
 * Jetpack Compose preview function for the add/edit wheelie bin screen.
 */
@Preview(showBackground = true)
@Composable
fun AddEditBinScreenPreview() {
    BinMinderTheme {
        AddEditBinContent(
            uiState = AddEditBinUiState(
                name = "General Waste",
                presetColor = BinColor.BLACK,
                lidPresetColor = BinColor.BLUE,
                recurrence = RecurrenceType.FORTNIGHTLY,
                startDate = LocalDate.now(),
                adjustForBankHolidays = true,
                customNote = "Black wheelie bin with blue lid"
            ),
            snackbarHostState = remember { SnackbarHostState() },
            onNameChange = {},
            onPresetColorSelected = {},
            onCustomHexChange = {},
            onLidPresetColorSelected = {},
            onCustomLidHexChange = {},
            onRecurrenceSelected = {},
            onStartDateSelected = {},
            onAdjustForBankHolidaysChange = {},
            onCustomNoteChange = {},
            onSaveBin = {},
            onNavigateBack = {}
        )
    }
}
