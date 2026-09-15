package com.example.binminder.ui.settings

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.EventRepeat
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.Image
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import com.example.binminder.R
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.binminder.data.model.AppThemeMode
import com.example.binminder.data.model.NotificationSettings
import com.example.binminder.engine.BankHolidayCalculator
import com.example.binminder.engine.BankHolidayShiftPreview
import com.example.binminder.ui.dialogs.CalendarExportDialog
import com.example.binminder.ui.theme.BinMinderTheme
import com.example.binminder.ui.theme.formatBritishDate
import com.example.binminder.util.CalendarExportUtils
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * Screen composable for managing app theme options, notification schedules, and bank holiday previews.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier,
    onReRunSetupWizard: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    var pendingAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            pendingAction?.invoke()
            pendingAction = null
        } else {
            viewModel.onNotificationPermissionDenied()
            pendingAction = null
        }
    }

    fun runWithNotificationPermission(action: () -> Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val check = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            )
            if (check == PackageManager.PERMISSION_GRANTED) {
                action()
            } else {
                pendingAction = action
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            action()
        }
    }

    LaunchedEffect(uiState.userMessage) {
        uiState.userMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.dismissUserMessage()
        }
    }

    SettingsContent(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onSetThemeMode = { themeMode ->
            viewModel.setThemeMode(themeMode)
        },
        onToggleReminders = { enabled ->
            if (enabled) {
                runWithNotificationPermission {
                    viewModel.toggleReminders(true, context)
                }
            } else {
                viewModel.toggleReminders(false, context)
            }
        },
        onUpdateSchedule = { time, eveningBefore ->
            viewModel.updateReminderSchedule(time, eveningBefore)
        },
        onSendTestNotification = {
            runWithNotificationPermission {
                viewModel.sendTestNotification(context)
            }
        },
        onResetDefaultBins = { viewModel.resetDefaultBins() },
        onResetAndStartSetup = {
            viewModel.resetTimetableAndAddress(context) {
                onReRunSetupWizard()
            }
        },
        modifier = modifier
    )
}

/**
 * Structural layout for the application settings screen.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsContent(
    uiState: SettingsUiState,
    snackbarHostState: SnackbarHostState,
    onSetThemeMode: (AppThemeMode) -> Unit,
    onToggleReminders: (Boolean) -> Unit,
    onUpdateSchedule: (LocalTime, Boolean) -> Unit,
    onSendTestNotification: () -> Unit,
    onResetDefaultBins: () -> Unit,
    onResetAndStartSetup: () -> Unit,
    modifier: Modifier = Modifier
) {
    val settings = uiState.notificationSettings
    val context = LocalContext.current
    var showTimePickerDialog by remember { mutableStateOf(false) }
    var customTimeEveningBefore by remember { mutableStateOf(settings.reminderEveningBefore) }
    var showResetDialog by remember { mutableStateOf(false) }
    var showRestoreDefaultsDialog by remember { mutableStateOf(false) }
    var isBankHolidaysExpanded by remember { mutableStateOf(false) }
    var showSubstituteHolidayInfoDialog by remember { mutableStateOf(false) }
    var showCalendarExportDialog by remember { mutableStateOf(false) }

    if (showCalendarExportDialog) {
        CalendarExportDialog(
            bins = uiState.allBins,
            onDismissRequest = { showCalendarExportDialog = false }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Settings & Preferences",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Notifications & Council Presets",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
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
            // Section 1: Notifications & Reminder Schedule
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Notifications,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Bin Collection Reminders",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Push Notifications",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Receive scheduled alerts before collection day",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Switch(
                            checked = settings.reminderEnabled,
                            onCheckedChange = onToggleReminders
                        )
                    }

                    if (settings.reminderEnabled) {
                        Spacer(modifier = Modifier.height(20.dp))

                        Text(
                            text = "Reminder Schedule & Timing",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Preset Schedule Options: Evening Before
                        Text(
                            text = "Evening Before Collection",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            listOf(
                                LocalTime.of(19, 0) to "19:00 (7:00 PM)",
                                LocalTime.of(20, 0) to "20:00 (8:00 PM)",
                                LocalTime.of(21, 0) to "21:00 (9:00 PM)"
                            ).forEach { (time, label) ->
                                val isSelected = settings.reminderEveningBefore && settings.reminderTime == time
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { onUpdateSchedule(time, true) },
                                    label = { Text(label) },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                )
                            }

                            val isEveningCustom = settings.reminderEveningBefore && settings.reminderTime !in listOf(
                                LocalTime.of(19, 0),
                                LocalTime.of(20, 0),
                                LocalTime.of(21, 0)
                            )
                            val customEveningLabel = if (isEveningCustom) {
                                "Custom (${settings.reminderTime.format(DateTimeFormatter.ofPattern("HH:mm"))})"
                            } else {
                                "Custom..."
                            }

                            FilterChip(
                                selected = isEveningCustom,
                                onClick = {
                                    customTimeEveningBefore = true
                                    showTimePickerDialog = true
                                },
                                label = { Text(customEveningLabel) },
                                shape = RoundedCornerShape(12.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Preset Schedule Options: Morning Of
                        Text(
                            text = "Morning Of Collection",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            listOf(
                                LocalTime.of(6, 0) to "06:00 (6:00 AM)",
                                LocalTime.of(7, 0) to "07:00 (7:00 AM)",
                                LocalTime.of(8, 0) to "08:00 (8:00 AM)"
                            ).forEach { (time, label) ->
                                val isSelected = !settings.reminderEveningBefore && settings.reminderTime == time
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { onUpdateSchedule(time, false) },
                                    label = { Text(label) },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                )
                            }

                            val isMorningCustom = !settings.reminderEveningBefore && settings.reminderTime !in listOf(
                                LocalTime.of(6, 0),
                                LocalTime.of(7, 0),
                                LocalTime.of(8, 0)
                            )
                            val customMorningLabel = if (isMorningCustom) {
                                "Custom (${settings.reminderTime.format(DateTimeFormatter.ofPattern("HH:mm"))})"
                            } else {
                                "Custom..."
                            }

                            FilterChip(
                                selected = isMorningCustom,
                                onClick = {
                                    customTimeEveningBefore = false
                                    showTimePickerDialog = true
                                },
                                label = { Text(customMorningLabel) },
                                shape = RoundedCornerShape(12.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Test Notification Button
                        Button(
                            onClick = onSendTestNotification,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary,
                                contentColor = MaterialTheme.colorScheme.onSecondary
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.NotificationsActive,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Send Test Notification")
                        }
                    }
                }
            }

            // Section 2: UK Bank Holiday Shift Rules (Expandable)
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isBankHolidaysExpanded = !isBankHolidaysExpanded }
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
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "UK Bank Holiday Shift Rules",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        IconButton(onClick = { isBankHolidaysExpanded = !isBankHolidaysExpanded }) {
                            Icon(
                                imageVector = if (isBankHolidaysExpanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                                contentDescription = if (isBankHolidaysExpanded) "Collapse" else "Expand"
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Info,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Collections delayed by +1 day after a bank holiday",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    if (isBankHolidaysExpanded) {
                        Spacer(modifier = Modifier.height(12.dp))

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Substitute Bank Holidays: In the UK, when Christmas, Boxing Day, or New Year's Day falls on a weekend, the UK government designates the following Monday/Tuesday as the official 'Substitute' Bank Holiday.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        if (uiState.bankHolidayPreviews.isEmpty()) {
                            Text(
                                text = "No upcoming bank holiday shifts found.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            val grouped = uiState.bankHolidayPreviews.groupBy { it.holidayName to it.holidayDate }
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                grouped.forEach { (holidayKey, shifts) ->
                                    val (holidayName, holidayDate) = holidayKey
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.surface,
                                        tonalElevation = 1.dp,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = MaterialTheme.colorScheme.primaryContainer,
                                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                            ) {
                                                Column(
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                                ) {
                                                    Text(
                                                        text = holidayDate.format(DateTimeFormatter.ofPattern("dd MMM")),
                                                        style = MaterialTheme.typography.labelLarge,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    Text(
                                                        text = holidayDate.format(DateTimeFormatter.ofPattern("yyyy")),
                                                        style = MaterialTheme.typography.labelSmall
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.width(12.dp))

                                            Column(modifier = Modifier.weight(1f)) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(
                                                        text = holidayName,
                                                        style = MaterialTheme.typography.titleSmall,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                    if (holidayName.contains("Substitute", ignoreCase = true) || holidayName.contains("Boxing Day", ignoreCase = true)) {
                                                        IconButton(
                                                            onClick = { showSubstituteHolidayInfoDialog = true },
                                                            modifier = Modifier.size(24.dp).padding(start = 4.dp)
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Outlined.Info,
                                                                contentDescription = "Substitute Bank Holiday Info",
                                                                tint = MaterialTheme.colorScheme.primary,
                                                                modifier = Modifier.size(16.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                                Spacer(modifier = Modifier.height(2.dp))
                                                shifts.forEach { shift ->
                                                    Text(
                                                        text = "${shift.binName}: ${shift.originalDayName} ➔ ${shift.shiftedDayName} (+1d)",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Section 3: Council & Timetable Management
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Council & Timetable Management",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            val activeBins = uiState.allBins.filter { it.isEnabled }
                            if (activeBins.isNotEmpty()) {
                                showCalendarExportDialog = true
                            } else {
                                Toast.makeText(context, "No active bins found to export.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Outlined.CalendarToday, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add Bins to Calendar", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedButton(
                        onClick = { showRestoreDefaultsDialog = true },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Rounded.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Restore Standard UK Bins")
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Resets your bins to default UK council profiles (General, Recycling, Garden, Food Caddy).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { showResetDialog = true },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Rounded.RestartAlt, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Reset Timetable & Address",
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Clears all bins and restarts the guided setup wizard to enter a new postcode or address.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            }

            // Section 3: Theme Preference
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Palette,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Theme Preference",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = "App Appearance",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Select your preferred app display theme",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        AppThemeMode.entries.forEach { mode ->
                            val isSelected = uiState.themeMode == mode
                            FilterChip(
                                selected = isSelected,
                                onClick = { onSetThemeMode(mode) },
                                label = { Text(mode.label) },
                                leadingIcon = if (isSelected) {
                                    {
                                        Icon(
                                            imageVector = Icons.Rounded.Check,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
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
            }

            // Section 4: About
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_app_logo),
                            contentDescription = "BinDay Logo",
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(6.dp))
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "About BinDay: Reminders",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "BinDay v1.0.0",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Designed for household bin timetable tracking across England, Wales, Scotland, and Northern Ireland.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Restore Standard Bins Confirmation Dialogue
    if (showRestoreDefaultsDialog) {
        AlertDialog(
            onDismissRequest = { showRestoreDefaultsDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Rounded.Refresh,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = {
                Text(
                    text = "Restore Standard UK Bins?",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "This will replace your current bins with the standard UK council profile (General Waste, Recycling, Garden, Food Caddy). Your postcode settings will be kept.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showRestoreDefaultsDialog = false
                        onResetDefaultBins()
                    }
                ) {
                    Text("Restore Bins")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreDefaultsDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Reset Timetable Confirmation Dialogue
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Rounded.RestartAlt,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = {
                Text(
                    text = "Reset Timetable & Address?",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "This will clear your current bins and timetable, and re-run the Setup Wizard so you can set up a new postcode or address.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showResetDialog = false
                        onResetAndStartSetup()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
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

    // Custom Time Picker Dialogue
    if (showTimePickerDialog) {
        val timePickerState = rememberTimePickerState(
            initialHour = settings.reminderTime.hour,
            initialMinute = settings.reminderTime.minute,
            is24Hour = true
        )

        AlertDialog(
            onDismissRequest = { showTimePickerDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val selectedTime = LocalTime.of(timePickerState.hour, timePickerState.minute)
                        onUpdateSchedule(selectedTime, customTimeEveningBefore)
                        showTimePickerDialog = false
                    }
                ) {
                    Text("Set Time")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePickerDialog = false }) {
                    Text("Cancel")
                }
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (customTimeEveningBefore) "Select Custom Evening Time" else "Select Custom Morning Time",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    TimePicker(state = timePickerState)
                }
            }
        )
    }

    // Substitute Bank Holiday Info Dialogue
    if (showSubstituteHolidayInfoDialog) {
        AlertDialog(
            onDismissRequest = { showSubstituteHolidayInfoDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = {
                Text(
                    text = "Substitute Bank Holidays",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = BankHolidayCalculator.SUBSTITUTE_BANK_HOLIDAY_EXPLANATION,
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(onClick = { showSubstituteHolidayInfoDialog = false }) {
                    Text("Got it")
                }
            }
        )
    }
}

/**
 * Jetpack Compose preview function for the settings screen.
 */
@Preview(showBackground = true)
@Composable
fun SettingsScreenPreview() {
    BinMinderTheme {
        SettingsContent(
            uiState = SettingsUiState(
                notificationSettings = NotificationSettings(
                    reminderEnabled = true,
                    reminderEveningBefore = true
                ),
                bankHolidayPreviews = listOf(
                    BankHolidayShiftPreview(
                        holidayName = "Early May Bank Holiday",
                        holidayDate = LocalDate.of(2025, 5, 5),
                        binName = "General Waste",
                        originalDayName = "Monday",
                        shiftedDayName = "Tuesday",
                        originalDate = LocalDate.of(2025, 5, 5),
                        shiftedDate = LocalDate.of(2025, 5, 6)
                    )
                )
            ),
            snackbarHostState = remember { SnackbarHostState() },
            onSetThemeMode = {},
            onToggleReminders = {},
            onUpdateSchedule = { _, _ -> },
            onSendTestNotification = {},
            onResetDefaultBins = {},
            onResetAndStartSetup = {}
        )
    }
}
