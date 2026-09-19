package com.example.binminder.ui.bins

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.EventRepeat
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
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
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedIconButton
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.Image
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.binminder.R
import com.example.binminder.data.model.Bin
import com.example.binminder.data.model.BinColor
import com.example.binminder.data.model.RecurrenceType
import com.example.binminder.ui.theme.BinMinderTheme
import com.example.binminder.ui.theme.WheelieBinVisualSwatch
import com.example.binminder.ui.theme.formatBritishDate
import com.example.binminder.ui.theme.formatRecurrenceLabel
import com.example.binminder.ui.theme.neoShadow
import com.example.binminder.ui.theme.BrandError
import androidx.compose.ui.graphics.Color
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
            viewModel.dismissUserMessage()
            snackbarHostState.showSnackbar(message)
        }
    }

    BinListContent(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onToggleBinEnabled = { viewModel.toggleBinEnabled(it) },
        onRequestDeleteBin = { viewModel.requestDeleteBin(it) },
        onCancelDeleteBin = { viewModel.cancelDeleteBin() },
        onConfirmDeleteBin = { viewModel.confirmDeleteBin() },
        onRunSetupWizard = { viewModel.resetTimetableAndAddress(context) },
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
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurface,
            title = {
                Text(
                    text = "Run Setup Wizard?",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "This will completely reset your configuration. Are you absolutely sure?",
                    style = MaterialTheme.typography.bodyLarge
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showResetDialog = false
                        viewModel.resetTimetableAndAddress(context)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandError, contentColor = Color.White),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Run Setup Wizard", fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                Button(
                    onClick = { showResetDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Cancel", fontWeight = FontWeight.SemiBold)
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
    onRunSetupWizard: () -> Unit,
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_app_logo),
                            contentDescription = "BinDay Logo",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .size(52.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )
                        Column {
                            Text(
                                text = "My Bins",
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }
                    }
                },
                actions = {
                    Button(
                        onClick = onResetClick,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BrandError,
                            contentColor = Color.White
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier
                            .padding(end = 16.dp)
                            .neoShadow(offset = 4.dp)
                    ) {
                        Text(
                            text = "Run Setup Wizard",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            if (uiState.bins.size < 50) {
                ExtendedFloatingActionButton(
                    onClick = onNavigateToAddBin,
                    icon = { Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(24.dp)) },
                    text = { Text("New Bin", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                        .neoShadow(offset = 6.dp)
                )
            }
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
                    onRunSetupWizardClicked = onRunSetupWizard,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
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
                        Spacer(modifier = Modifier.height(120.dp)) // Clearance for FAB
                    }
                }
            }
        }
    }

    // Delete Confirmation Dialogue
    if (uiState.binToDelete != null) {
        AlertDialog(
            onDismissRequest = onCancelDeleteBin,
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Text(
                    text = "Delete Bin?",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Are you absolutely sure you want to delete '${uiState.binToDelete.name}'? There's no going back!",
                    style = MaterialTheme.typography.bodyLarge
                )
            },
            confirmButton = {
                Button(
                    onClick = onConfirmDeleteBin,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BrandError,
                        contentColor = Color.White
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Delete Bin", fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                Button(
                    onClick = onCancelDeleteBin,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Cancel", fontWeight = FontWeight.SemiBold)
                }
            }
        )
    }
}

/**
 * Tactile card component displaying individual bin details, dual-colour swatches, recurrence chips, and action controls.
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
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (bin.isEnabled) {
                MaterialTheme.colorScheme.surface
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEditClicked)
            .neoShadow(offset = if (bin.isEnabled) 6.dp else 2.dp)
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
                            .size(60.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        WheelieBinVisualSwatch(
                            presetColor = bin.presetColor,
                            colorHex = bin.colorHex,
                            lidPresetColor = bin.lidPresetColor,
                            lidColorHex = bin.lidColorHex,
                            size = 48.dp,
                            isEnabled = bin.isEnabled
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = bin.name,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (bin.isEnabled) {
                                    MaterialTheme.colorScheme.onSurface
                                } else {
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                }
                            )
                        }
                        
                        if (!bin.isEnabled) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                            ) {
                                Text(
                                    text = "Disabled",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Recurrence Chip
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.tertiary,
                            contentColor = MaterialTheme.colorScheme.onTertiary,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Text(
                                text = formatRecurrenceLabel(bin.recurrence, bin.startDate),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                // Enable/Disable Switch with bold track
                Switch(
                    checked = bin.isEnabled,
                    onCheckedChange = { onToggleEnabled() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.primary,
                        checkedTrackColor = MaterialTheme.colorScheme.onPrimary,
                        checkedBorderColor = MaterialTheme.colorScheme.outline,
                        uncheckedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

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
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Starts: ${formatBritishDate(bin.startDate, includeDayOfWeek = false)}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (bin.adjustForBankHolidays) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(36.dp)
                            .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape)
                            .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                            .clip(CircleShape)
                            .clickable(onClick = onShowBankHolidayInfo)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = "Bank Holiday Auto-Adjustment Info",
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            if (bin.customNote.isNotBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = bin.customNote,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Edit & Delete Action Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedIconButton(
                    onClick = onEditClicked,
                    colors = IconButtonDefaults.outlinedIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = "Edit Bin",
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                OutlinedIconButton(
                    onClick = onDeleteClicked,
                    colors = IconButtonDefaults.outlinedIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = BrandError
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = "Delete Bin",
                        modifier = Modifier.size(24.dp)
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
    onRunSetupWizardClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.primaryContainer)
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
                .neoShadow(offset = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Delete,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "No Bins Configured",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Run the setup wizard to get started.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(32.dp))

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(
                onClick = onRunSetupWizardClicked,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .neoShadow(offset = 6.dp)
            ) {
                Icon(imageVector = Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Run Setup Wizard", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
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
            onRunSetupWizard = {},
            onResetClick = {},
            onShowBankHolidayInfo = {},
            onNavigateToAddBin = {},
            onNavigateToEditBin = {}
        )
    }
}
