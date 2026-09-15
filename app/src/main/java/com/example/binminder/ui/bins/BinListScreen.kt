package com.example.binminder.ui.bins

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.EventRepeat
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.EventRepeat
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.binminder.data.model.Bin
import com.example.binminder.data.model.BinColor
import com.example.binminder.data.model.RecurrenceType
import com.example.binminder.ui.theme.BinMinderTheme
import com.example.binminder.ui.theme.WheelieBinVisualSwatch
import com.example.binminder.ui.theme.formatBritishDate
import com.example.binminder.ui.theme.formatRecurrenceLabel
import com.example.binminder.ui.theme.getContrastingTextColor
import com.example.binminder.ui.theme.parseBinColor
import java.time.LocalDate

/**
 * Screen composable for managing household wheelie bin configurations.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BinListScreen(
    viewModel: BinListViewModel,
    onNavigateToAddBin: () -> Unit,
    onNavigateToEditBin: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    var showResetDialog by remember { mutableStateOf(false) }
    var showBankHolidayInfoDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.userMessage) {
        uiState.userMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.dismissUserMessage()
        }
    }

    BinListContent(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onToggleBinEnabled = { viewModel.toggleBinEnabled(it) },
        onRequestDeleteBin = { viewModel.requestDeleteBin(it) },
        onCancelDeleteBin = { viewModel.cancelDeleteBin() },
        onConfirmDeleteBin = { viewModel.confirmDeleteBin() },
        onResetDefaultBins = { viewModel.resetDefaultBins() },
        onResetClick = { showResetDialog = true },
        onShowBankHolidayInfo = { showBankHolidayInfoDialog = true },
        onNavigateToAddBin = onNavigateToAddBin,
        onNavigateToEditBin = onNavigateToEditBin,
        modifier = modifier
    )

    // Reset Confirmation Dialogue
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = {
                Text(
                    text = "Reset Bins & Timetable?",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "This will reset your current wheelie bins and timetable, opening the guided Setup Wizard so you can enter a new postcode or update your schedule. Are you sure you want to proceed?",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showResetDialog = false
                        viewModel.resetTimetableAndAddress(context)
                    }
                ) {
                    Text("Reset & Start Setup")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Bank Holiday Auto-Adjustment Info Dialogue
    if (showBankHolidayInfoDialog) {
        AlertDialog(
            onDismissRequest = { showBankHolidayInfoDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Outlined.EventRepeat,
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
 * Layout structure for the wheelie bin list screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BinListContent(
    uiState: BinListUiState,
    snackbarHostState: SnackbarHostState,
    onToggleBinEnabled: (Bin) -> Unit,
    onRequestDeleteBin: (Bin) -> Unit,
    onCancelDeleteBin: () -> Unit,
    onConfirmDeleteBin: () -> Unit,
    onResetDefaultBins: () -> Unit,
    onResetClick: () -> Unit,
    onShowBankHolidayInfo: () -> Unit,
    onNavigateToAddBin: () -> Unit,
    onNavigateToEditBin: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Bins",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Manage Council Bin Profiles",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    Button(
                        onClick = onResetClick,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text(
                            text = "Reset",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNavigateToAddBin,
                icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                text = { Text("Add Bin") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            } else if (uiState.bins.isEmpty()) {
                EmptyBinsView(
                    onAddBinClicked = onNavigateToAddBin,
                    onResetDefaultsClicked = onResetDefaultBins,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    items(
                        items = uiState.bins,
                        key = { it.id }
                    ) { bin ->
                        WheelieBinCard(
                            bin = bin,
                            onToggleEnabled = { onToggleBinEnabled(bin) },
                            onEditClicked = { onNavigateToEditBin(bin.id) },
                            onDeleteClicked = { onRequestDeleteBin(bin) },
                            onShowBankHolidayInfo = onShowBankHolidayInfo
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(88.dp)) // Clearance for FAB
                    }
                }
            }
        }
    }

    // Delete Confirmation Dialogue
    if (uiState.binToDelete != null) {
        AlertDialog(
            onDismissRequest = onCancelDeleteBin,
            title = {
                Text(
                    text = "Delete Bin?",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to delete '${uiState.binToDelete.name}'? This action will remove all calculated collection dates for this bin.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = onConfirmDeleteBin,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = onCancelDeleteBin) {
                    Text("Cancel")
                }
            }
        )
    }
}

/**
 * Card component displaying individual bin details, dual-colour swatches, and action controls.
 */
@Composable
fun WheelieBinCard(
    bin: Bin,
    onToggleEnabled: () -> Unit,
    onEditClicked: () -> Unit,
    onDeleteClicked: () -> Unit,
    onShowBankHolidayInfo: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (bin.isEnabled) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
            }
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEditClicked)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Dual-Colour Visual Swatch & Bin Name
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)),
                        contentAlignment = Alignment.Center
                    ) {
                        WheelieBinVisualSwatch(
                            presetColor = bin.presetColor,
                            colorHex = bin.colorHex,
                            lidPresetColor = bin.lidPresetColor,
                            lidColorHex = bin.lidColorHex,
                            size = 36.dp,
                            isEnabled = bin.isEnabled
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = bin.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (bin.isEnabled) {
                                    MaterialTheme.colorScheme.onSurface
                                } else {
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                }
                            )
                            if (!bin.isEnabled) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant,
                                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ) {
                                    Text(
                                        text = "Disabled",
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        // Descriptive colour & recurrence label
                        Text(
                            text = bin.colorDisplayName,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Text(
                            text = formatRecurrenceLabel(bin.recurrence, bin.startDate),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Enable/Disable Switch
                Switch(
                    checked = bin.isEnabled,
                    onCheckedChange = { onToggleEnabled() }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Details section: First Collection Date & Bank Holiday adjustment badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Schedule,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "First: ${formatBritishDate(bin.startDate, includeDayOfWeek = false)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (bin.adjustForBankHolidays) {
                    IconButton(
                        onClick = onShowBankHolidayInfo,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = "Bank Holiday Auto-Adjustment Info",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            if (bin.customNote.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Note: ${bin.customNote}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Edit & Delete Action Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onEditClicked,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Edit,
                        contentDescription = "Edit Bin",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = onDeleteClicked,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Delete,
                        contentDescription = "Delete Bin",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

/**
 * Empty view displayed when no wheelie bins exist in the list.
 */
@Composable
fun EmptyBinsView(
    onAddBinClicked: () -> Unit,
    onResetDefaultsClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Delete,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "No Bins Added",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Create custom bin profiles or load standard UK council defaults.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = onAddBinClicked,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(imageVector = Icons.Rounded.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "Add Custom Bin")
            }

            OutlinedButton(
                onClick = onResetDefaultsClicked,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(imageVector = Icons.Rounded.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "Load Defaults")
            }
        }
    }
}

/**
 * Jetpack Compose preview function for the wheelie bin list screen.
 */
@Preview(showBackground = true)
@Composable
fun BinListScreenPreview() {
    val sampleBins = listOf(
        Bin(
            id = 1,
            name = "General Waste",
            colorHex = BinColor.BLACK.defaultHex,
            presetColor = BinColor.BLACK,
            lidPresetColor = BinColor.BLUE,
            recurrence = RecurrenceType.FORTNIGHTLY,
            startDate = LocalDate.now(),
            customNote = "Black bin with blue lid",
            isEnabled = true,
            adjustForBankHolidays = true
        ),
        Bin(
            id = 2,
            name = "Dry Mixed Recycling",
            colorHex = BinColor.BLUE.defaultHex,
            presetColor = BinColor.BLUE,
            recurrence = RecurrenceType.FORTNIGHTLY,
            startDate = LocalDate.now().plusWeeks(1),
            customNote = "Paper, card, tins, bottles",
            isEnabled = true,
            adjustForBankHolidays = true
        )
    )

    BinMinderTheme {
        BinListContent(
            uiState = BinListUiState(
                bins = sampleBins,
                isLoading = false
            ),
            snackbarHostState = remember { SnackbarHostState() },
            onToggleBinEnabled = {},
            onRequestDeleteBin = {},
            onCancelDeleteBin = {},
            onConfirmDeleteBin = {},
            onResetDefaultBins = {},
            onResetClick = {},
            onShowBankHolidayInfo = {},
            onNavigateToAddBin = {},
            onNavigateToEditBin = {}
        )
    }
}
