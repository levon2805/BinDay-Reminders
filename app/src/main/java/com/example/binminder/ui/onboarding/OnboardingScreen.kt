package com.example.binminder.ui.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.binminder.data.model.BinColor
import com.example.binminder.data.model.CouncilScheduleResult
import com.example.binminder.data.model.OnboardingBinSetup
import com.example.binminder.data.model.RecurrenceType
import com.example.binminder.ui.theme.BinMinderTheme
import com.example.binminder.ui.theme.getContrastingTextColor
import com.example.binminder.ui.theme.parseBinColor
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.format.TextStyle
import java.util.Locale

/**
 * Screen composable for the first-time setup onboarding wizard.
 */
@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel,
    onOnboardingComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    OnboardingContent(
        uiState = uiState,
        onPostcodeOrCouncilChange = { viewModel.setPostcodeOrCouncil(it) },
        onSearchPostcodeClick = { viewModel.searchPostcodeTimetable() },
        onAcceptAndApplyDetected = { viewModel.acceptAndApplyDetectedTimetable(onOnboardingComplete) },
        onCustomiseDetected = { viewModel.customiseDetectedTimetable() },
        onContinueGuided = { viewModel.continueToGuidedSetup() },
        onPrimaryDaySelect = { viewModel.setPrimaryCollectionDay(it) },
        onBinToggle = { viewModel.toggleBinEnabled(it) },
        onBinRecurrenceSelect = { binType, recurrence -> viewModel.setBinRecurrence(binType, recurrence) },
        onBinStartNextWeekToggle = { binType, startNextWeek -> viewModel.setBinStartNextWeek(binType, startNextWeek) },
        onBinColorSelect = { binType, color -> viewModel.setBinColor(binType, color) },
        onFortnightlyThisWeekSelect = { binType -> viewModel.setFortnightlyThisWeekBin(binType) },
        onReminderSettingsChange = { time, eveningBefore, enabled -> viewModel.setReminderSettings(time, eveningBefore, enabled) },
        onNextStep = { viewModel.nextStep() },
        onPreviousStep = { viewModel.previousStep() },
        onGoToStep = { viewModel.goToStep(it) },
        onCompleteSetup = { viewModel.completeSetup(onOnboardingComplete) },
        modifier = modifier
    )
}

/**
 * Layout structure hosting onboarding wizard progress and steps.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingContent(
    uiState: OnboardingUiState,
    onPostcodeOrCouncilChange: (String) -> Unit,
    onSearchPostcodeClick: () -> Unit,
    onAcceptAndApplyDetected: () -> Unit,
    onCustomiseDetected: () -> Unit,
    onContinueGuided: () -> Unit,
    onPrimaryDaySelect: (DayOfWeek) -> Unit,
    onBinToggle: (String) -> Unit,
    onBinRecurrenceSelect: (String, RecurrenceType) -> Unit,
    onBinStartNextWeekToggle: (String, Boolean) -> Unit,
    onBinColorSelect: (String, BinColor) -> Unit,
    onFortnightlyThisWeekSelect: (String) -> Unit,
    onReminderSettingsChange: (LocalTime, Boolean, Boolean) -> Unit,
    onNextStep: () -> Unit,
    onPreviousStep: () -> Unit,
    onGoToStep: (Int) -> Unit,
    onCompleteSetup: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "BinMinder Setup Wizard",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Step ${uiState.currentStep} of 4: " + when (uiState.currentStep) {
                                1 -> "Postcode Search"
                                2 -> "Collection Day"
                                3 -> "Bins"
                                4 -> "Reminder Alerts"
                                else -> ""
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    if (uiState.currentStep > 1) {
                        IconButton(onClick = onPreviousStep) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 3.dp,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (uiState.currentStep > 1) {
                        OutlinedButton(
                            onClick = onPreviousStep,
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Back")
                        }
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }

                    if (uiState.currentStep < 4) {
                        Button(
                            onClick = {
                                if (uiState.currentStep == 1 && uiState.detectedSchedule == null && uiState.postcodeOrCouncil.isNotBlank()) {
                                    onSearchPostcodeClick()
                                } else {
                                    onNextStep()
                                }
                            },
                            enabled = !uiState.isSearchingPostcode,
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text("Next")
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    } else {
                        Button(
                            onClick = onCompleteSetup,
                            enabled = !uiState.isCompleting,
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            if (uiState.isCompleting) {
                                CircularProgressIndicator(
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Setting Up...")
                            } else {
                                Icon(
                                    imageVector = Icons.Rounded.CheckCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Complete Setup")
                            }
                        }
                    }
                }
            }
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Step Progress Bar
            LinearProgressIndicator(
                progress = { uiState.currentStep / 4f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
            )

            // Step Content Animated Transition
            AnimatedContent(
                targetState = uiState.currentStep,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "WizardStepTransition",
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) { step ->
                when (step) {
                    1 -> Step1LocationContent(
                        postcodeOrCouncil = uiState.postcodeOrCouncil,
                        isSearching = uiState.isSearchingPostcode,
                        detectedSchedule = uiState.detectedSchedule,
                        binSetups = uiState.binSetups,
                        searchError = uiState.searchError,
                        selectedDay = uiState.primaryCollectionDay,
                        onQueryChange = onPostcodeOrCouncilChange,
                        onSearchClick = onSearchPostcodeClick,
                        onDaySelected = onPrimaryDaySelect,
                        onFortnightlyThisWeekSelect = onFortnightlyThisWeekSelect,
                        onBinColorSelect = onBinColorSelect,
                        onBinRecurrenceSelect = onBinRecurrenceSelect,
                        onBinStartNextWeekToggle = onBinStartNextWeekToggle,
                        onBinToggle = onBinToggle,
                        onAcceptAndApply = onAcceptAndApplyDetected,
                        onCustomiseSchedule = onCustomiseDetected,
                        onContinueGuided = onContinueGuided
                    )
                    2 -> Step2CollectionDayContent(
                        selectedDay = uiState.primaryCollectionDay,
                        onDaySelected = onPrimaryDaySelect
                    )
                    3 -> Step3BinsContent(
                        binSetups = uiState.binSetups,
                        onBinToggle = onBinToggle,
                        onRecurrenceSelect = onBinRecurrenceSelect,
                        onStartNextWeekToggle = onBinStartNextWeekToggle,
                        onBinColorSelect = onBinColorSelect
                    )
                    4 -> Step4RemindersContent(
                        reminderTime = uiState.reminderTime,
                        reminderEveningBefore = uiState.reminderEveningBefore,
                        reminderEnabled = uiState.reminderEnabled,
                        onSettingsChange = onReminderSettingsChange
                    )
                }
            }
        }
    }
}

/**
 * Step 1 composable for UK postcode search and auto-detected schedule options.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun Step1LocationContent(
    postcodeOrCouncil: String,
    isSearching: Boolean,
    detectedSchedule: CouncilScheduleResult?,
    binSetups: List<OnboardingBinSetup>,
    searchError: String?,
    selectedDay: DayOfWeek,
    onQueryChange: (String) -> Unit,
    onSearchClick: () -> Unit,
    onDaySelected: (DayOfWeek) -> Unit,
    onFortnightlyThisWeekSelect: (String) -> Unit,
    onBinColorSelect: (String, BinColor) -> Unit,
    onBinRecurrenceSelect: (String, RecurrenceType) -> Unit,
    onBinStartNextWeekToggle: (String, Boolean) -> Unit,
    onBinToggle: (String) -> Unit,
    onAcceptAndApply: () -> Unit,
    onCustomiseSchedule: () -> Unit,
    onContinueGuided: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "UK Postcode Timetable Search",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "Enter your UK postcode to auto-detect your council collection schedule and bins.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                    )
                }
            }
        }

        OutlinedTextField(
            value = postcodeOrCouncil,
            onValueChange = onQueryChange,
            leadingIcon = {
                Icon(imageVector = Icons.Rounded.Search, contentDescription = null)
            },
            trailingIcon = if (postcodeOrCouncil.isNotEmpty()) {
                {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(imageVector = Icons.Rounded.Clear, contentDescription = "Clear")
                    }
                }
            } else null,
            label = { Text("UK Postcode") },
            placeholder = { Text("Enter UK Postcode, e.g. SW1A 1AA or M1 1AE") },
            singleLine = true,
            enabled = !isSearching,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = onSearchClick,
            enabled = postcodeOrCouncil.isNotBlank() && !isSearching,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            if (isSearching) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text("Searching Timetable...")
            } else {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Search Bin Timetable", fontWeight = FontWeight.Bold)
            }
        }

        if (isSearching) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(28.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "Searching council timetable for ${postcodeOrCouncil.trim()}...",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (detectedSchedule != null && !isSearching) {
            val dayName = selectedDay.getDisplayName(TextStyle.FULL, Locale.UK)

            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Detected Council: ${detectedSchedule.councilName}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }

                    // Collection Day Selector
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Collection Day:",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )

                        val collectionDays = listOf(
                            DayOfWeek.MONDAY,
                            DayOfWeek.TUESDAY,
                            DayOfWeek.WEDNESDAY,
                            DayOfWeek.THURSDAY,
                            DayOfWeek.FRIDAY,
                            DayOfWeek.SATURDAY
                        )

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            collectionDays.forEach { day ->
                                val isSelected = day == selectedDay
                                val name = day.getDisplayName(TextStyle.FULL, Locale.UK)
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { onDaySelected(day) },
                                    label = {
                                        Text(
                                            text = name,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                        containerColor = MaterialTheme.colorScheme.surface,
                                        labelColor = MaterialTheme.colorScheme.onSurface
                                    )
                                )
                            }
                        }
                    }

                    // "Which Bin Goes Out THIS Coming Week?" Week-Cycle Toggle
                    val fortnightlyBins = binSetups.filter { it.isEnabled && it.recurrence == RecurrenceType.FORTNIGHTLY }
                    if (fortnightlyBins.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surface,
                            tonalElevation = 2.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "Which bin goes out THIS coming week?",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Tap to choose which fortnightly bin is collected on your next $dayName:",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    fortnightlyBins.forEach { bin ->
                                        val isThisWeek = !bin.startNextWeek
                                        val binBgColor = parseBinColor(bin.presetColor.defaultHex, bin.presetColor)

                                        FilterChip(
                                            selected = isThisWeek,
                                            onClick = { onFortnightlyThisWeekSelect(bin.binType) },
                                            leadingIcon = {
                                                Box(
                                                    modifier = Modifier
                                                        .size(14.dp)
                                                        .clip(CircleShape)
                                                        .background(binBgColor)
                                                )
                                            },
                                            trailingIcon = if (isThisWeek) {
                                                {
                                                    Icon(
                                                        imageVector = Icons.Rounded.Check,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            } else null,
                                            label = {
                                                Text(
                                                    text = "${bin.displayName} (${bin.presetColor.displayName})",
                                                    fontWeight = if (isThisWeek) FontWeight.Bold else FontWeight.Normal
                                                )
                                            },
                                            shape = RoundedCornerShape(12.dp),
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                                labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Live Schedule Preview
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "Live Schedule Preview",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            val thisWeekBins = binSetups.filter { it.isEnabled && (it.recurrence == RecurrenceType.WEEKLY || !it.startNextWeek) }
                            val nextWeekBins = binSetups.filter { it.isEnabled && (it.recurrence == RecurrenceType.WEEKLY || it.startNextWeek) }

                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "• This coming $dayName:",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                if (thisWeekBins.isEmpty()) {
                                    Text("  No collections scheduled", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                } else {
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        thisWeekBins.forEach { bin ->
                                            SchedulePreviewPill(bin)
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = "• Next $dayName:",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                                if (nextWeekBins.isEmpty()) {
                                    Text("  No collections scheduled", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                } else {
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        nextWeekBins.forEach { bin ->
                                            SchedulePreviewPill(bin)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // In-Place Bin Customisation List
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Detected Bins & Customisation:",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(
                                text = "Tap bin to customise",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        binSetups.forEach { bin ->
                            ResultCardBinItem(
                                bin = bin,
                                dayName = dayName,
                                onColorSelect = { color -> onBinColorSelect(bin.binType, color) },
                                onRecurrenceSelect = { rec -> onBinRecurrenceSelect(bin.binType, rec) },
                                onStartNextWeekToggle = { startNext -> onBinStartNextWeekToggle(bin.binType, startNext) },
                                onToggleEnabled = { onBinToggle(bin.binType) }
                            )
                        }
                    }

                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onAcceptAndApply,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Confirm & Create Timetable", fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = onCustomiseSchedule,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Customise Bins")
                        }
                    }
                }
            }
        }

        if (searchError != null && !isSearching) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = searchError,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }

                    Button(
                        onClick = onContinueGuided,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Continue to Guided Setup")
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Interactive bin item composable rendered inside the postcode lookup result card.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ResultCardBinItem(
    bin: OnboardingBinSetup,
    dayName: String,
    onColorSelect: (BinColor) -> Unit,
    onRecurrenceSelect: (RecurrenceType) -> Unit,
    onStartNextWeekToggle: (Boolean) -> Unit,
    onToggleEnabled: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }
    val binBgColor = parseBinColor(bin.presetColor.defaultHex, bin.presetColor)
    val binTextColor = getContrastingTextColor(binBgColor)

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(binBgColor)
                            .border(1.dp, Color.White.copy(alpha = 0.5f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Delete,
                            contentDescription = null,
                            tint = binTextColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = bin.displayName,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ) {
                                Text(
                                    text = bin.presetColor.displayName,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (!bin.isEnabled) "Disabled" else when (bin.recurrence) {
                                RecurrenceType.WEEKLY -> "Weekly on $dayName"
                                RecurrenceType.FORTNIGHTLY -> if (bin.startNextWeek) "Fortnightly on $dayName (Next Week)" else "Fortnightly on $dayName (This Week)"
                                else -> "${bin.recurrence.displayName} on $dayName"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { isExpanded = !isExpanded },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                            contentDescription = "Expand to tweak bin"
                        )
                    }
                    Switch(
                        checked = bin.isEnabled,
                        onCheckedChange = { onToggleEnabled() },
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Colour Palette Chooser
                    Text(
                        text = "Colour Theme:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    val availableColors = listOf(
                        BinColor.BLACK,
                        BinColor.GREY,
                        BinColor.BLUE,
                        BinColor.GREEN,
                        BinColor.BROWN,
                        BinColor.PURPLE,
                        BinColor.RED,
                        BinColor.YELLOW
                    )

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        availableColors.forEach { color ->
                            val colorHex = parseBinColor(color.defaultHex, color)
                            val isSelected = bin.presetColor == color

                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(colorHex)
                                    .border(
                                        width = if (isSelected) 3.dp else 1.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.5f),
                                        shape = CircleShape
                                    )
                                    .clickable { onColorSelect(color) },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Rounded.Check,
                                        contentDescription = color.displayName,
                                        tint = getContrastingTextColor(colorHex),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Recurrence Frequency
                    Text(
                        text = "Collection Frequency:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(RecurrenceType.WEEKLY, RecurrenceType.FORTNIGHTLY).forEach { rec ->
                            val isSelected = bin.recurrence == rec
                            FilterChip(
                                selected = isSelected,
                                onClick = { onRecurrenceSelect(rec) },
                                label = { Text(rec.displayName) },
                                shape = RoundedCornerShape(10.dp)
                            )
                        }
                    }

                    if (bin.recurrence == RecurrenceType.FORTNIGHTLY) {
                        Text(
                            text = "Start Cycle:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = !bin.startNextWeek,
                                onClick = { onStartNextWeekToggle(false) },
                                label = { Text("This Coming Week") },
                                shape = RoundedCornerShape(10.dp)
                            )
                            FilterChip(
                                selected = bin.startNextWeek,
                                onClick = { onStartNextWeekToggle(true) },
                                label = { Text("Next Week") },
                                shape = RoundedCornerShape(10.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Compact pill tag composable displaying a scheduled bin preview item.
 */
@Composable
private fun SchedulePreviewPill(bin: OnboardingBinSetup) {
    val binBgColor = parseBinColor(bin.presetColor.defaultHex, bin.presetColor)
    val binTextColor = getContrastingTextColor(binBgColor)

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = binBgColor,
        contentColor = binTextColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.Delete,
                contentDescription = null,
                tint = binTextColor,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = bin.displayName,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * Step 2 composable for selecting primary collection day.
 */
@Composable
fun Step2CollectionDayContent(
    selectedDay: DayOfWeek,
    onDaySelected: (DayOfWeek) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CalendarMonth,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondary,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "Primary Collection Day",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        text = "Which day of the week are your household bins collected?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f)
                    )
                }
            }
        }

        DayOfWeek.entries.forEach { day ->
            val isSelected = day == selectedDay
            val dayName = day.getDisplayName(TextStyle.FULL, Locale.UK)

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    }
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onDaySelected(day) }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = dayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )

                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

/**
 * Step 3 composable for selecting bins and collection frequencies.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun Step3BinsContent(
    binSetups: List<OnboardingBinSetup>,
    onBinToggle: (String) -> Unit,
    onRecurrenceSelect: (String, RecurrenceType) -> Unit,
    onStartNextWeekToggle: (String, Boolean) -> Unit,
    onBinColorSelect: (String, BinColor) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.tertiary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onTertiary,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "Select Bins & Schedules",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    Text(
                        text = "Choose which bins you have and their collection frequencies.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.85f)
                    )
                }
            }
        }

        binSetups.forEach { setup ->
            val binBgColor = parseBinColor(setup.presetColor.defaultHex, setup.presetColor)
            val binTextColor = getContrastingTextColor(binBgColor)

            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(
                        alpha = if (setup.isEnabled) 0.8f else 0.3f
                    )
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(binBgColor)
                                    .border(1.dp, Color.White.copy(alpha = 0.5f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Delete,
                                    contentDescription = null,
                                    tint = binTextColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = setup.displayName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Switch(
                            checked = setup.isEnabled,
                            onCheckedChange = { onBinToggle(setup.binType) }
                        )
                    }

                    if (setup.isEnabled) {
                        // Colour Picker
                        Text(
                            text = "Bin Colour",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        val availableColors = listOf(
                            BinColor.BLACK,
                            BinColor.GREY,
                            BinColor.BLUE,
                            BinColor.GREEN,
                            BinColor.BROWN,
                            BinColor.PURPLE,
                            BinColor.RED,
                            BinColor.YELLOW
                        )

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            availableColors.forEach { color ->
                                val colorHex = parseBinColor(color.defaultHex, color)
                                val isSelected = setup.presetColor == color

                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(colorHex)
                                        .border(
                                            width = if (isSelected) 3.dp else 1.dp,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.5f),
                                            shape = CircleShape
                                        )
                                        .clickable { onBinColorSelect(setup.binType, color) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Rounded.Check,
                                            contentDescription = color.displayName,
                                            tint = getContrastingTextColor(colorHex),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Text(
                            text = "Collection Frequency",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            listOf(RecurrenceType.WEEKLY, RecurrenceType.FORTNIGHTLY).forEach { rec ->
                                val isSelected = setup.recurrence == rec
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { onRecurrenceSelect(setup.binType, rec) },
                                    label = { Text(rec.displayName) },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                )
                            }
                        }

                        if (setup.recurrence == RecurrenceType.FORTNIGHTLY) {
                            Text(
                                text = "Alternating Offset",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )

                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                FilterChip(
                                    selected = !setup.startNextWeek,
                                    onClick = { onStartNextWeekToggle(setup.binType, false) },
                                    label = { Text("Starts This Week") },
                                    shape = RoundedCornerShape(12.dp)
                                )
                                FilterChip(
                                    selected = setup.startNextWeek,
                                    onClick = { onStartNextWeekToggle(setup.binType, true) },
                                    label = { Text("Starts Next Week") },
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Step 4 composable for configuring notification reminders.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun Step4RemindersContent(
    reminderTime: LocalTime,
    reminderEveningBefore: Boolean,
    reminderEnabled: Boolean,
    onSettingsChange: (LocalTime, Boolean, Boolean) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Notifications,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "Reminder Preferences",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Choose when you want to receive push notifications before collection day.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f)
                    )
                }
            }
        }

        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Enable Collection Alerts",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Switch(
                        checked = reminderEnabled,
                        onCheckedChange = { onSettingsChange(reminderTime, reminderEveningBefore, it) }
                    )
                }

                if (reminderEnabled) {
                    Text(
                        text = "Evening Before Collection",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        listOf(
                            LocalTime.of(19, 0) to "19:00 (7 PM)",
                            LocalTime.of(20, 0) to "20:00 (8 PM)",
                            LocalTime.of(21, 0) to "21:00 (9 PM)"
                        ).forEach { (time, label) ->
                            val isSelected = reminderEveningBefore && reminderTime == time
                            FilterChip(
                                selected = isSelected,
                                onClick = { onSettingsChange(time, true, true) },
                                label = { Text(label) },
                                shape = RoundedCornerShape(12.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Morning Of Collection",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        listOf(
                            LocalTime.of(6, 0) to "06:00 (6 AM)",
                            LocalTime.of(7, 0) to "07:00 (7 AM)",
                            LocalTime.of(8, 0) to "08:00 (8 AM)"
                        ).forEach { (time, label) ->
                            val isSelected = !reminderEveningBefore && reminderTime == time
                            FilterChip(
                                selected = isSelected,
                                onClick = { onSettingsChange(time, false, true) },
                                label = { Text(label) },
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
        }
    }
}

/**
 * Jetpack Compose preview function for the onboarding setup screen.
 */
@Preview(showBackground = true)
@Composable
fun OnboardingScreenPreview() {
    BinMinderTheme {
        OnboardingContent(
            uiState = OnboardingUiState(currentStep = 1),
            onPostcodeOrCouncilChange = {},
            onSearchPostcodeClick = {},
            onAcceptAndApplyDetected = {},
            onCustomiseDetected = {},
            onContinueGuided = {},
            onPrimaryDaySelect = {},
            onBinToggle = {},
            onBinRecurrenceSelect = { _, _ -> },
            onBinStartNextWeekToggle = { _, _ -> },
            onBinColorSelect = { _, _ -> },
            onFortnightlyThisWeekSelect = {},
            onReminderSettingsChange = { _, _, _ -> },
            onNextStep = {},
            onPreviousStep = {},
            onGoToStep = {},
            onCompleteSetup = {}
        )
    }
}
