package com.example.binminder.ui.addedit

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Label
import androidx.compose.material.icons.automirrored.rounded.Notes
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.EventRepeat
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.binminder.data.model.BinColor
import com.example.binminder.data.model.RecurrenceType
import com.example.binminder.ui.theme.BinMinderTheme
import com.example.binminder.ui.theme.ColorPickerDialog
import com.example.binminder.ui.theme.WheelieBinVisualSwatch
import com.example.binminder.ui.theme.formatBritishDate
import com.example.binminder.ui.theme.getContrastingTextColor
import com.example.binminder.ui.theme.parseBinColor
import com.example.binminder.ui.theme.neoShadow
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
            viewModel.resetSaveState()
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
    val screenTitle = if (isEditing) "Edit Bin" else "New Bin"

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = screenTitle,
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .background(MaterialTheme.colorScheme.surface, CircleShape)
                            .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                            .size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
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
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(32.dp)
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
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Bin Name",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                OutlinedTextField(
                    value = uiState.name,
                    onValueChange = onNameChange,
                    leadingIcon = {
                        Icon(imageVector = Icons.AutoMirrored.Rounded.Label, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
                    },
                    placeholder = { Text("e.g. General Waste") },
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .neoShadow(color = MaterialTheme.colorScheme.outline, offset = 4.dp)
                )
            }

            // 2. Bin Body Colour Section
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Palette,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Body Colour",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
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
                    Card(
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier
                            .fillMaxWidth()
                            .neoShadow(color = MaterialTheme.colorScheme.outline, offset = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Hex: ${uiState.colorHex}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Button(
                                onClick = { showBodyColorPickerDialog = true },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface, contentColor = MaterialTheme.colorScheme.onSurface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                            ) {
                                Text("Change", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }

            // 3. Bin Lid Colour Section (Dual-Colour Support)
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Palette,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Lid Colour",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Does the lid look different?",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // "Same as Body" option
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (uiState.lidPresetColor == null) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surface,
                        contentColor = if (uiState.lidPresetColor == null) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier
                            .clickable { onLidPresetColorSelected(null) }
                            .neoShadow(color = MaterialTheme.colorScheme.outline, offset = 4.dp)
                    ) {
                        Text(
                            text = "Match Body",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                        )
                    }

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
                    Card(
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier
                            .fillMaxWidth()
                            .neoShadow(color = MaterialTheme.colorScheme.outline, offset = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Lid Hex: ${uiState.lidColorHex ?: ""}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Button(
                                onClick = { showLidColorPickerDialog = true },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface, contentColor = MaterialTheme.colorScheme.onSurface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                            ) {
                                Text("Change", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }

            // 4. Collection Schedule / Recurrence
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = "Collection Frequency",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    RecurrenceType.entries.forEach { recurrence ->
                        val isSelected = uiState.recurrence == recurrence
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                            contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            modifier = Modifier
                                .clickable { onRecurrenceSelected(recurrence) }
                                .neoShadow(color = MaterialTheme.colorScheme.outline, offset = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (isSelected) {
                                    Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                }
                                Text(
                                    text = recurrence.displayName,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }

            // 5. First Collection Date Picker
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Next Date",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Card(
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDatePickerDialog = true }
                        .neoShadow(color = MaterialTheme.colorScheme.outline, offset = 4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.CalendarToday,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = formatBritishDate(uiState.startDate),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // 6. Bank Holiday Shift Toggle
            Card(
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier
                    .fillMaxWidth()
                    .neoShadow(color = MaterialTheme.colorScheme.outline, offset = 4.dp)
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
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Bank Holiday Shift",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                IconButton(
                                    onClick = { showBankHolidayInfoDialog = true },
                                    modifier = Modifier.size(24.dp).padding(start = 4.dp)
                                ) {
                                    Icon(Icons.Outlined.Info, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(18.dp))
                                }
                            }
                            Text(
                                text = "Auto +1 day when active",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Switch(
                        checked = uiState.adjustForBankHolidays,
                        onCheckedChange = onAdjustForBankHolidaysChange,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.primary,
                            checkedTrackColor = MaterialTheme.colorScheme.onPrimary,
                            checkedBorderColor = MaterialTheme.colorScheme.outline,
                            uncheckedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )
                }
            }

            // 7. Custom Instructions / Notes
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Notes (Optional)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                OutlinedTextField(
                    value = uiState.customNote,
                    onValueChange = onCustomNoteChange,
                    leadingIcon = {
                        Icon(imageVector = Icons.AutoMirrored.Rounded.Notes, contentDescription = null)
                    },
                    placeholder = { Text("e.g. By the gate") },
                    textStyle = MaterialTheme.typography.bodySmall,
                    minLines = 2,
                    maxLines = 4,
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .neoShadow(color = MaterialTheme.colorScheme.outline, offset = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 8. Save / Cancel Action Buttons
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = onSaveBin,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .neoShadow(color = MaterialTheme.colorScheme.outline, offset = 6.dp)
                ) {
                    Icon(imageVector = Icons.Rounded.Save, contentDescription = null, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Save Bin", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
                
                Button(
                    onClick = onNavigateBack,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .neoShadow(color = MaterialTheme.colorScheme.outline, offset = 4.dp)
                ) {
                    Text("Cancel", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(48.dp))
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
            colors = DatePickerDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
            confirmButton = {
                Button(
                    onClick = {
                        val selectedMillis = datePickerState.selectedDateMillis
                        if (selectedMillis != null) {
                            val selectedDate = Instant.ofEpochMilli(selectedMillis)
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate()
                            onStartDateSelected(selectedDate)
                        }
                        showDatePickerDialog = false
                    },
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Text("Select", fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                Button(
                    onClick = { showDatePickerDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Text("Cancel", fontWeight = FontWeight.SemiBold)
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showBodyColorPickerDialog) {
        ColorPickerDialog(
            title = "Body Colour",
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
            title = "Lid Colour",
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
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp)
                )
            },
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Text(
                    text = "Bank Holiday Shift",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Text(
                    text = "When a UK bank holiday happens, we automatically bump this bin's collection date by +1 day (e.g. Friday ➔ Saturday).",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )
            },
            confirmButton = {
                Button(
                    onClick = { showBankHolidayInfoDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Got It", fontWeight = FontWeight.SemiBold)
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
        shape = RoundedCornerShape(8.dp),
        color = binBgColor,
        contentColor = binTextColor,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier
            .fillMaxWidth()
            .neoShadow(color = MaterialTheme.colorScheme.outline, offset = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.25f))
                    .border(3.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                WheelieBinVisualSwatch(
                    presetColor = presetColor,
                    colorHex = colorHex,
                    lidPresetColor = lidPresetColor,
                    lidColorHex = lidColorHex,
                    size = 56.dp
                )
            }

            Spacer(modifier = Modifier.width(24.dp))

            Column {
                Text(
                    text = name,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = binTextColor
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "${recurrence.displayName} • ${formatBritishDate(startDate, includeDayOfWeek = false)}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
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
        shape = RoundedCornerShape(8.dp),
        color = swatchColor,
        modifier = Modifier
            .clickable(onClick = onSelected)
            .border(
                width = 2.dp,
                color = MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(8.dp)
            )
            .neoShadow(color = MaterialTheme.colorScheme.outline, offset = if (isSelected) 4.dp else 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    tint = textColor,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = preset.displayName,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
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
