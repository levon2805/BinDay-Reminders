package com.example.binminder.ui.settings

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.EventRepeat
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
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.Image
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.binminder.R
import com.example.binminder.data.model.AppThemeMode
import com.example.binminder.data.model.NotificationSettings
import com.example.binminder.engine.BankHolidayCalculator
import com.example.binminder.engine.BankHolidayShiftPreview
import com.example.binminder.ui.dialogs.CalendarExportDialog
import com.example.binminder.ui.dialogs.MultipleRemindersDialog
import com.example.binminder.ui.theme.BinMinderTheme
import com.example.binminder.ui.theme.neoShadow
import com.example.binminder.ui.theme.BrandError
import androidx.compose.ui.graphics.Color
import com.example.binminder.util.formatTimeForUser
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
            viewModel.onToastShown()
            snackbarHostState.showSnackbar(message)
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
                    viewModel.toggleReminders(true)
                }
            } else {
                viewModel.toggleReminders(false)
            }
        },
        onUpdateEveningTime = { time -> viewModel.updateEveningReminderTime(time) },
        onUpdateMorningTime = { time -> viewModel.updateMorningReminderTime(time) },
        onAddExtraTime = { time -> viewModel.addExtraReminderTime(time) },
        onDeleteExtraTime = { time -> viewModel.removeExtraReminderTime(time) },
        onAddEveningTime = { time -> viewModel.addEveningReminderTime(time) },
        onEditEveningTime = { oldTime, newTime -> viewModel.editEveningReminderTime(oldTime, newTime) },
        onDeleteEveningTime = { time -> viewModel.removeEveningReminderTime(time) },
        onAddMorningTime = { time -> viewModel.addMorningReminderTime(time) },
        onEditMorningTime = { oldTime, newTime -> viewModel.editMorningReminderTime(oldTime, newTime) },
        onDeleteMorningTime = { time -> viewModel.removeMorningReminderTime(time) },
        onUpdateSchedule = { time, eveningBefore ->
            viewModel.updateReminderSchedule(time, eveningBefore)
        },
        onSendTestNotification = {
            runWithNotificationPermission {
                viewModel.sendTestNotification(context)
            }
        },
        onResetAndStartSetup = {
            viewModel.resetTimetableAndAddress(context) {
                onReRunSetupWizard()
            }
        },
        modifier = modifier
    )
}

/**
 * Structural layout for the application settings screen with grouped tactile cards.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsContent(
    uiState: SettingsUiState,
    snackbarHostState: SnackbarHostState,
    onSetThemeMode: (AppThemeMode) -> Unit,
    onToggleReminders: (Boolean) -> Unit,
    onUpdateEveningTime: (LocalTime?) -> Unit,
    onUpdateMorningTime: (LocalTime?) -> Unit,
    onAddExtraTime: (LocalTime) -> Unit = {},
    onDeleteExtraTime: (LocalTime) -> Unit = {},
    onAddEveningTime: (LocalTime) -> Unit = {},
    onEditEveningTime: (LocalTime, LocalTime) -> Unit = { _, _ -> },
    onDeleteEveningTime: (LocalTime) -> Unit = {},
    onAddMorningTime: (LocalTime) -> Unit = {},
    onEditMorningTime: (LocalTime, LocalTime) -> Unit = { _, _ -> },
    onDeleteMorningTime: (LocalTime) -> Unit = {},
    onUpdateSchedule: (LocalTime, Boolean) -> Unit = { _, _ -> },
    onSendTestNotification: () -> Unit,
    onResetAndStartSetup: () -> Unit,
    modifier: Modifier = Modifier
) {
    val settings = uiState.notificationSettings
    val context = LocalContext.current
    var showTimePickerDialog by remember { mutableStateOf(false) }
    var customTimeEveningBefore by remember { mutableStateOf(settings.reminderEveningBefore) }
    var showResetDialog by remember { mutableStateOf(false) }
    var isBankHolidaysExpanded by remember { mutableStateOf(false) }
    var showSubstituteHolidayInfoDialog by remember { mutableStateOf(false) }
    var showCalendarExportDialog by remember { mutableStateOf(false) }
    var showMultipleRemindersDialog by remember { mutableStateOf(false) }

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
                                text = "Settings",
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }
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
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Section 1: Notifications & Reminder Schedule
            Card(
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier
                    .fillMaxWidth()
                    .neoShadow(offset = 6.dp)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Notifications,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = "Notifications",
                            style = MaterialTheme.typography.titleLarge,
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
                                text = "Push Alerts",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Remind me before collection day",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Switch(
                            checked = settings.reminderEnabled,
                            onCheckedChange = onToggleReminders,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.primary,
                                checkedTrackColor = MaterialTheme.colorScheme.onPrimary,
                                checkedBorderColor = MaterialTheme.colorScheme.outline,
                                uncheckedBorderColor = MaterialTheme.colorScheme.outline
                            )
                        )
                    }

                    if (settings.reminderEnabled) {
                        Spacer(modifier = Modifier.height(24.dp))

                        Text(
                            text = "Timing",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        val primaryEvening = settings.eveningReminderTime
                        val primaryMorning = settings.morningReminderTime
                        val extraEveningTimes = if (primaryEvening != null) settings.eveningReminderTimes - primaryEvening else settings.eveningReminderTimes
                        val extraMorningTimes = if (primaryMorning != null) settings.morningReminderTimes - primaryMorning else settings.morningReminderTimes
                        val extraTimes = (extraEveningTimes + extraMorningTimes).toList().sorted()

                        Spacer(modifier = Modifier.height(16.dp))

                        // Preset Schedule Options: Day Before
                        Text(
                            text = "Day Before",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val isEveningNone = primaryEvening == null
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isEveningNone) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                contentColor = if (isEveningNone) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                modifier = Modifier
                                    .clickable { onUpdateEveningTime(null) }
                                    .neoShadow(offset = if (isEveningNone) 0.dp else 4.dp)
                            ) {
                                Text(
                                    text = "None",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                                )
                            }

                            listOf(
                                LocalTime.of(19, 0),
                                LocalTime.of(20, 0),
                                LocalTime.of(21, 0)
                            ).forEach { time ->
                                val label = formatTimeForUser(time, context)
                                val isSelected = primaryEvening == time
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                    contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                    modifier = Modifier
                                        .clickable { onUpdateEveningTime(time) }
                                        .neoShadow(offset = if (isSelected) 0.dp else 4.dp)
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                                    )
                                }
                            }

                            val isEveningCustom = primaryEvening != null && primaryEvening !in listOf(
                                LocalTime.of(19, 0),
                                LocalTime.of(20, 0),
                                LocalTime.of(21, 0)
                            )
                            val customEveningLabel = if (isEveningCustom && primaryEvening != null) {
                                "Custom (${formatTimeForUser(primaryEvening, context)})"
                            } else {
                                "Custom..."
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isEveningCustom) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                contentColor = if (isEveningCustom) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                modifier = Modifier
                                    .clickable {
                                        customTimeEveningBefore = true
                                        showTimePickerDialog = true
                                    }
                                    .neoShadow(offset = if (isEveningCustom) 0.dp else 4.dp)
                            ) {
                                Text(
                                    text = customEveningLabel,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Preset Schedule Options: Day Of
                        Text(
                            text = "Day Of",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val isMorningNone = primaryMorning == null
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isMorningNone) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                contentColor = if (isMorningNone) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                modifier = Modifier
                                    .clickable { onUpdateMorningTime(null) }
                                    .neoShadow(offset = if (isMorningNone) 0.dp else 4.dp)
                            ) {
                                Text(
                                    text = "None",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                                )
                            }

                            listOf(
                                LocalTime.of(6, 0),
                                LocalTime.of(7, 0),
                                LocalTime.of(8, 0)
                            ).forEach { time ->
                                val label = formatTimeForUser(time, context)
                                val isSelected = primaryMorning == time
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                    contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                    modifier = Modifier
                                        .clickable { onUpdateMorningTime(time) }
                                        .neoShadow(offset = if (isSelected) 0.dp else 4.dp)
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                                    )
                                }
                            }

                            val isMorningCustom = primaryMorning != null && primaryMorning !in listOf(
                                LocalTime.of(6, 0),
                                LocalTime.of(7, 0),
                                LocalTime.of(8, 0)
                            )
                            val customMorningLabel = if (isMorningCustom && primaryMorning != null) {
                                "Custom (${formatTimeForUser(primaryMorning, context)})"
                            } else {
                                "Custom..."
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isMorningCustom) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                contentColor = if (isMorningCustom) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                modifier = Modifier
                                    .clickable {
                                        customTimeEveningBefore = false
                                        showTimePickerDialog = true
                                    }
                                    .neoShadow(offset = if (isMorningCustom) 0.dp else 4.dp)
                            ) {
                                Text(
                                    text = customMorningLabel,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        val totalRemindersCount = (settings.eveningReminderTimes + settings.morningReminderTimes).size
                        val extraButtonText = if (totalRemindersCount > 0) "Advanced Notifications ($totalRemindersCount)" else "Advanced Notifications"

                        Button(
                            onClick = { showMultipleRemindersDialog = true },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .neoShadow(offset = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.NotificationsActive,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = extraButtonText,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        if (showMultipleRemindersDialog) {
                            MultipleRemindersDialog(
                                eveningReminderTimes = settings.eveningReminderTimes,
                                morningReminderTimes = settings.morningReminderTimes,
                                onAddEveningTime = onAddEveningTime,
                                onEditEveningTime = onEditEveningTime,
                                onDeleteEveningTime = onDeleteEveningTime,
                                onAddMorningTime = onAddMorningTime,
                                onEditMorningTime = onEditMorningTime,
                                onDeleteMorningTime = onDeleteMorningTime,
                                extraTimes = extraTimes,
                                onAddExtraTime = onAddExtraTime,
                                onDeleteExtraTime = onDeleteExtraTime,
                                onDismissRequest = { showMultipleRemindersDialog = false }
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Test Notification Button
                        Button(
                            onClick = onSendTestNotification,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary,
                                contentColor = MaterialTheme.colorScheme.onSecondary
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .neoShadow(color = MaterialTheme.colorScheme.outline, offset = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.NotificationsActive,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Test Notification", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            // Section 2: UK Bank Holiday Shift Rules (Expandable)
            Card(
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize()
                    .neoShadow(offset = 4.dp)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
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
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = "Bank Holidays",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        IconButton(
                            onClick = { isBankHolidaysExpanded = !isBankHolidaysExpanded },
                            modifier = Modifier.border(1.dp, MaterialTheme.colorScheme.outline, CircleShape).background(MaterialTheme.colorScheme.surface, CircleShape)
                        ) {
                            Icon(
                                imageVector = if (isBankHolidaysExpanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                                contentDescription = if (isBankHolidaysExpanded) "Collapse" else "Expand"
                            )
                        }
                    }

                    if (isBankHolidaysExpanded) {
                        Spacer(modifier = Modifier.height(24.dp))

                        if (uiState.bankHolidayPreviews.isEmpty()) {
                            Text(
                                text = "No upcoming bank holiday shifts found.",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            val grouped = uiState.bankHolidayPreviews.groupBy { it.holidayName to it.holidayDate }
                            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                grouped.forEach { (holidayKey, shifts) ->
                                    val (holidayName, holidayDate) = holidayKey
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                        modifier = Modifier.fillMaxWidth().neoShadow(offset = 4.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(16.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = MaterialTheme.colorScheme.primaryContainer,
                                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                                            ) {
                                                Column(
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                                ) {
                                                    Text(
                                                        text = holidayDate.format(DateTimeFormatter.ofPattern("dd MMM")),
                                                        style = MaterialTheme.typography.labelLarge,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    Text(
                                                        text = holidayDate.format(DateTimeFormatter.ofPattern("yyyy")),
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.Medium
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.width(16.dp))

                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = holidayName,
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                shifts.forEach { shift ->
                                                    Text(
                                                        text = "${shift.binName}: ${shift.originalDayName} ➔ ${shift.shiftedDayName} (+1d)",
                                                        style = MaterialTheme.typography.labelMedium,
                                                        fontWeight = FontWeight.Medium,
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
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier
                    .fillMaxWidth()
                    .neoShadow(offset = 6.dp)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = "Timetable",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Button(
                        onClick = {
                            val activeBins = uiState.allBins.filter { it.isEnabled }
                            if (activeBins.isNotEmpty()) {
                                showCalendarExportDialog = true
                            } else {
                                Toast.makeText(context, "No active bins found to export.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier.fillMaxWidth().height(56.dp).neoShadow(offset = 4.dp)
                    ) {
                        Icon(imageVector = Icons.Outlined.CalendarToday, contentDescription = null, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Add to Calendar", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = { showResetDialog = true },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BrandError,
                            contentColor = Color.White
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier.fillMaxWidth().height(56.dp).neoShadow(offset = 4.dp)
                    ) {
                        Icon(imageVector = Icons.Rounded.RestartAlt, contentDescription = null, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Factory Reset",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Section 4: Theme Preference
            Card(
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier
                    .fillMaxWidth()
                    .neoShadow(offset = 6.dp)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Palette,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = "Theme",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        AppThemeMode.entries.forEach { mode ->
                            val isSelected = uiState.themeMode == mode
                            FilterChip(
                                selected = isSelected,
                                onClick = { onSetThemeMode(mode) },
                                label = {
                                    Text(
                                        text = mode.label,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.SemiBold,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                },
                                leadingIcon = if (isSelected) {
                                    {
                                        Icon(
                                            imageVector = Icons.Rounded.Check,
                                            contentDescription = null,
                                            modifier = Modifier.size(FilterChipDefaults.IconSize)
                                        )
                                    }
                                } else null,
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    labelColor = MaterialTheme.colorScheme.onSurface,
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                    selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = isSelected,
                                    borderColor = MaterialTheme.colorScheme.outline,
                                    selectedBorderColor = MaterialTheme.colorScheme.outline,
                                    borderWidth = 1.dp,
                                    selectedBorderWidth = 1.dp
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }



    // Reset Timetable Confirmation Dialogue
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Text(
                    text = "Factory Reset?",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "This will wipe all bins, settings, and start the setup wizard over. Are you sure?",
                    style = MaterialTheme.typography.bodyLarge
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showResetDialog = false
                        onResetAndStartSetup()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandError, contentColor = Color.White),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Reset All", fontWeight = FontWeight.SemiBold)
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

    // Custom Time Picker Dialogue
    if (showTimePickerDialog) {
        val timePickerState = rememberTimePickerState(
            initialHour = settings.reminderTime.hour,
            initialMinute = settings.reminderTime.minute,
            is24Hour = true
        )

        AlertDialog(
            onDismissRequest = { showTimePickerDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            confirmButton = {
                Button(
                    onClick = {
                        val selectedTime = LocalTime.of(timePickerState.hour, timePickerState.minute)
                        if (customTimeEveningBefore) {
                            onUpdateEveningTime(selectedTime)
                        } else {
                            onUpdateMorningTime(selectedTime)
                        }
                        showTimePickerDialog = false
                    },
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Set Time", fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                Button(
                    onClick = { showTimePickerDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Cancel", fontWeight = FontWeight.SemiBold)
                }
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (customTimeEveningBefore) "Evening Time" else "Morning Time",
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
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp)
                )
            },
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Text(
                    text = "Substitute Holidays",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = BankHolidayCalculator.SUBSTITUTE_BANK_HOLIDAY_EXPLANATION,
                    style = MaterialTheme.typography.bodyLarge
                )
            },
            confirmButton = {
                Button(
                    onClick = { showSubstituteHolidayInfoDialog = false },
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Got It", fontWeight = FontWeight.SemiBold)
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
                    eveningReminderTime = LocalTime.of(19, 0)
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
            onUpdateEveningTime = {},
            onUpdateMorningTime = {},
            onUpdateSchedule = { _, _ -> },
            onSendTestNotification = {},
            onResetAndStartSetup = {}
        )
    }
}
