package com.example.binminder.ui.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Search
import com.example.binminder.ui.theme.ColorPickerDialog
import androidx.compose.material.icons.Icons.Default
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.res.painterResource
import com.example.binminder.R
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.binminder.data.model.BinColor
import com.example.binminder.data.model.CouncilScheduleResult
import com.example.binminder.data.model.OnboardingBinSetup
import com.example.binminder.data.model.RecurrenceType
import com.example.binminder.ui.theme.BinMinderTheme
import com.example.binminder.ui.theme.WheelieBinVisualSwatch
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
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.resetState()
    }

    OnboardingContent(
        uiState = uiState,
        onPostcodeOrCouncilChange = { viewModel.setPostcodeOrCouncil(it) },
        onSearchPostcodeClick = { viewModel.searchPostcodeTimetable() },
        onPrimaryDaySelect = { viewModel.setPrimaryCollectionDay(it) },
        onBinToggle = { viewModel.toggleBinEnabled(it) },
        onBinRecurrenceSelect = { binType, recurrence -> viewModel.setBinRecurrence(binType, recurrence) },
        onBinStartNextWeekToggle = { binType, startNextWeek -> viewModel.setBinStartNextWeek(binType, startNextWeek) },
        onBinColorSelect = { binType, color -> viewModel.setBinColor(binType, color) },
        onBinLidColorSelect = { binType, lidColor -> viewModel.setBinLidColor(binType, lidColor) },
        onBinDaySelect = { binType, day -> viewModel.setBinCollectionDay(binType, day) },
        onAddCustomBin = { viewModel.addCustomBin() },
        onRenameBin = { binType, newName -> viewModel.renameBin(binType, newName) },
        onReminderSettingsChange = { time, eveningBefore, enabled -> viewModel.setReminderSettings(time, eveningBefore, enabled) },
        onNextStep = { viewModel.nextStep() },
        onPreviousStep = { viewModel.previousStep() },
        onGoToStep = { viewModel.goToStep(it) },
        canAdvanceFromCurrentStep = viewModel.canAdvanceFromCurrentStep(),
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
    onPrimaryDaySelect: (DayOfWeek) -> Unit,
    onBinToggle: (String) -> Unit,
    onBinRecurrenceSelect: (String, RecurrenceType) -> Unit,
    onBinStartNextWeekToggle: (String, Boolean) -> Unit,
    onBinColorSelect: (String, BinColor) -> Unit,
    onBinLidColorSelect: (String, BinColor?) -> Unit,
    onBinDaySelect: (String, DayOfWeek) -> Unit,
    onAddCustomBin: () -> Unit,
    onRenameBin: (String, String) -> Unit,
    onReminderSettingsChange: (LocalTime, Boolean, Boolean) -> Unit,
    onNextStep: () -> Unit,
    onPreviousStep: () -> Unit,
    onGoToStep: (Int) -> Unit,
    canAdvanceFromCurrentStep: Boolean,
    onCompleteSetup: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_app_logo),
                            contentDescription = "BinDay Logo",
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                        Column {
                            Text(
                                text = "BinDay Setup Wizard",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Step ${uiState.currentStep} of 4: " + when (uiState.currentStep) {
                                    1 -> "Your Council"
                                    2 -> "Collection Day"
                                    3 -> "Bins & Lid Colours"
                                    4 -> "Reminder Alerts"
                                    else -> ""
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
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
                            onClick = onNextStep,
                            enabled = !uiState.isSearchingPostcode && canAdvanceFromCurrentStep,
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
                    1 -> Step1CouncilContent(
                        postcodeOrCouncil = uiState.postcodeOrCouncil,
                        isSearching = uiState.isSearchingPostcode,
                        detectedCouncil = uiState.detectedCouncil,
                        searchError = uiState.searchError,
                        onQueryChange = onPostcodeOrCouncilChange,
                        onSearchClick = onSearchPostcodeClick
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
                        onBinColorSelect = onBinColorSelect,
                        onBinLidColorSelect = onBinLidColorSelect,
                        onBinDaySelect = onBinDaySelect,
                        onAddCustomBin = onAddCustomBin,
                        onRenameBin = onRenameBin
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
 * Step 1 composable for identifying the user's local council via postcode
 * and providing a link to the council's website for schedule information.
 */
@Composable
fun Step1CouncilContent(
    postcodeOrCouncil: String,
    isSearching: Boolean,
    detectedCouncil: CouncilScheduleResult?,
    searchError: String?,
    onQueryChange: (String) -> Unit,
    onSearchClick: () -> Unit
) {
    val uriHandler = LocalUriHandler.current

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
                        text = "Find Your Council",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "Enter your UK postcode to identify your local council. You can then check their website for your collection schedule.",
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
                Text("Identifying Council...")
            } else {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Find My Council", fontWeight = FontWeight.Bold)
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
                        text = "Looking up council for ${postcodeOrCouncil.trim()}...",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Council identified successfully
        if (detectedCouncil != null && !isSearching) {
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
                                text = "Your Council",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                            )
                            Text(
                                text = detectedCouncil.councilName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }

                    // Council website link
                    Button(
                        onClick = {
                            try {
                                uriHandler.openUri(detectedCouncil.councilWebSearchUrl)
                            } catch (_: Exception) { }
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.OpenInNew,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Visit ${detectedCouncil.councilName} Website",
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Helpful guidance text
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Check your council's website to find out your collection day, which bins you have, and how often they're collected. You'll set these up in the next steps.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Error state
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
                }
            }
        }

        // Skip option — always visible
        if (detectedCouncil == null && !isSearching) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "You can skip this step and set up your bins manually. Tap 'Next' to continue.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * Step 2 composable for selecting primary collection day.
 * No day is pre-selected — the user must choose their actual collection day.
 */
@Composable
fun Step2CollectionDayContent(
    selectedDay: DayOfWeek?,
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

        // Show a hint if no day selected yet
        if (selectedDay == null) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Please select your collection day to continue.",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
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
 * Step 3 composable for selecting bins, editing names, and choosing collection frequencies.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun Step3BinsContent(
    binSetups: List<OnboardingBinSetup>,
    onBinToggle: (String) -> Unit,
    onRecurrenceSelect: (String, RecurrenceType) -> Unit,
    onStartNextWeekToggle: (String, Boolean) -> Unit,
    onBinColorSelect: (String, BinColor) -> Unit,
    onBinLidColorSelect: (String, BinColor?) -> Unit,
    onBinDaySelect: (String, DayOfWeek) -> Unit,
    onAddCustomBin: () -> Unit,
    onRenameBin: (String, String) -> Unit
) {
    var activeColorPickerBinType by remember { mutableStateOf<String?>(null) }
    var isLidPicker by remember { mutableStateOf(false) }

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
                        text = "Customise Bins & Lid Colours",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    Text(
                        text = "Rename bins, choose body and lid colours, and set collection frequencies.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.85f)
                    )
                }
            }
        }

        binSetups.forEach { setup ->
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
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                WheelieBinVisualSwatch(
                                    presetColor = setup.presetColor,
                                    lidPresetColor = setup.lidPresetColor,
                                    size = 32.dp,
                                    isEnabled = setup.isEnabled
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            OutlinedTextField(
                                value = setup.displayName,
                                onValueChange = { onRenameBin(setup.binType, it) },
                                modifier = Modifier.fillMaxWidth(),
                                textStyle = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                singleLine = true,
                                label = { Text("Bin Name") },
                                colors = OutlinedTextFieldDefaults.colors(
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedContainerColor = MaterialTheme.colorScheme.surface
                                )
                            )
                        }

                        Switch(
                            checked = setup.isEnabled,
                            onCheckedChange = { onBinToggle(setup.binType) }
                        )
                    }

                    if (setup.isEnabled) {
                        val availableColors = listOf(
                            BinColor.BLACK,
                            BinColor.DARK_GREY,
                            BinColor.LIGHT_GREY,
                            BinColor.BLUE,
                            BinColor.GREEN,
                            BinColor.BROWN,
                            BinColor.PURPLE,
                            BinColor.RED,
                            BinColor.YELLOW,
                            BinColor.ORANGE,
                            BinColor.BURGUNDY,
                            BinColor.MAGENTA
                        )

                        // Better visual separation for body vs lid
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surface,
                            tonalElevation = 1.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                // Body Colour Picker
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        text = "Bin Body Colour",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
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
                                                    .size(36.dp)
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
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                        }

                                        // Custom Colour Picker Wheel Button
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.primaryContainer)
                                                .border(
                                                    width = if (setup.presetColor == BinColor.CUSTOM) 3.dp else 1.dp,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    shape = CircleShape
                                                )
                                                .clickable {
                                                    activeColorPickerBinType = setup.binType
                                                    isLidPicker = false
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Palette,
                                                contentDescription = "Colour Wheel Picker",
                                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }

                                // Separator
                                Spacer(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(1.dp)
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                )

                                // Lid Colour Picker
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "Bin Lid Colour",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                        FilterChip(
                                            selected = setup.lidPresetColor == null,
                                            onClick = { onBinLidColorSelect(setup.binType, null) },
                                            label = { Text("Same as Body") },
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                    }

                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        availableColors.forEach { color ->
                                            val colorHex = parseBinColor(color.defaultHex, color)
                                            val isSelected = setup.lidPresetColor == color

                                            Box(
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .clip(CircleShape)
                                                    .background(colorHex)
                                                    .border(
                                                        width = if (isSelected) 3.dp else 1.dp,
                                                        color = if (isSelected) MaterialTheme.colorScheme.secondary else Color.White.copy(alpha = 0.5f),
                                                        shape = CircleShape
                                                    )
                                                    .clickable { onBinLidColorSelect(setup.binType, color) },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (isSelected) {
                                                    Icon(
                                                        imageVector = Icons.Rounded.Check,
                                                        contentDescription = color.displayName,
                                                        tint = getContrastingTextColor(colorHex),
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                        }

                                        // Custom Lid Colour Picker Wheel Button
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.secondaryContainer)
                                                .border(
                                                    width = if (setup.lidPresetColor == BinColor.CUSTOM) 3.dp else 1.dp,
                                                    color = MaterialTheme.colorScheme.secondary,
                                                    shape = CircleShape
                                                )
                                                .clickable {
                                                    activeColorPickerBinType = setup.binType
                                                    isLidPicker = true
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Palette,
                                                contentDescription = "Lid Colour Wheel Picker",
                                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Text(
                            text = "Collection Day",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 8.dp)
                        )

                        var expandedDay by remember { mutableStateOf(false) }
                        val currentDay = setup.collectionDay
                        
                        Box {
                            FilterChip(
                                selected = currentDay != null,
                                onClick = { expandedDay = true },
                                label = { 
                                    Text(
                                        currentDay?.name?.lowercase()?.replaceFirstChar { it.titlecase(java.util.Locale.UK) } ?: "Same as Primary"
                                    ) 
                                },
                                shape = RoundedCornerShape(12.dp),
                                trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) }
                            )
                            DropdownMenu(
                                expanded = expandedDay,
                                onDismissRequest = { expandedDay = false }
                            ) {
                                DayOfWeek.values().forEach { day ->
                                    DropdownMenuItem(
                                        text = { Text(day.name.lowercase().replaceFirstChar { it.titlecase(java.util.Locale.UK) }) },
                                        onClick = { 
                                            onBinDaySelect(setup.binType, day)
                                            expandedDay = false
                                        }
                                    )
                                }
                            }
                        }

                        Text(
                            text = "Collection Frequency",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 8.dp)
                        )

                        var expandedWeeks by remember { mutableStateOf(false) }
                        
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
                            
                            val isOtherSelected = setup.recurrence.intervalWeeks > 2
                            Box {
                                FilterChip(
                                    selected = isOtherSelected,
                                    onClick = { expandedWeeks = true },
                                    label = { 
                                        Text(if (isOtherSelected) "Every ${setup.recurrence.intervalWeeks} Weeks" else "Other")
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                )
                                DropdownMenu(
                                    expanded = expandedWeeks,
                                    onDismissRequest = { expandedWeeks = false }
                                ) {
                                    (3..8).forEach { weeks ->
                                        DropdownMenuItem(
                                            text = { Text("Every $weeks Weeks") },
                                            onClick = { 
                                                val rec = RecurrenceType.entries.firstOrNull { it.intervalWeeks == weeks } ?: RecurrenceType.FORTNIGHTLY
                                                onRecurrenceSelect(setup.binType, rec)
                                                expandedWeeks = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        if (setup.recurrence.intervalWeeks > 1) {
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
        OutlinedButton(
            onClick = onAddCustomBin,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Add,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Add Other Bin", fontWeight = FontWeight.Bold)
        }

        if (activeColorPickerBinType != null) {
            val targetBin = binSetups.find { it.binType == activeColorPickerBinType }
            if (targetBin != null) {
                val initialHex = if (isLidPicker) {
                    targetBin.lidPresetColor?.defaultHex ?: targetBin.presetColor.defaultHex
                } else {
                    targetBin.presetColor.defaultHex
                }
                val initialPreset = if (isLidPicker) targetBin.lidPresetColor else targetBin.presetColor

                ColorPickerDialog(
                    title = if (isLidPicker) "Select ${targetBin.displayName} Lid Colour" else "Select ${targetBin.displayName} Body Colour",
                    initialColorHex = initialHex,
                    initialPresetColor = initialPreset,
                    onColorSelected = { preset, _ ->
                        if (isLidPicker) {
                            onBinLidColorSelect(targetBin.binType, preset)
                        } else {
                            onBinColorSelect(targetBin.binType, preset)
                        }
                    },
                    onDismissRequest = { activeColorPickerBinType = null }
                )
            }
        }
    }
}

/**
 * Step 4 composable for configuring notification reminders.
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun Step4RemindersContent(
    reminderTime: LocalTime,
    reminderEveningBefore: Boolean,
    reminderEnabled: Boolean,
    onSettingsChange: (LocalTime, Boolean, Boolean) -> Unit
) {
    var showTimePickerDialog by remember { mutableStateOf(false) }
    var isEveningCustomTime by remember { mutableStateOf(true) }

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
                        
                        FilterChip(
                            selected = reminderEveningBefore && !listOf(LocalTime.of(19, 0), LocalTime.of(20, 0), LocalTime.of(21, 0)).contains(reminderTime),
                            onClick = {
                                isEveningCustomTime = true
                                showTimePickerDialog = true
                            },
                            label = { 
                                val isCustomEvening = reminderEveningBefore && !listOf(LocalTime.of(19, 0), LocalTime.of(20, 0), LocalTime.of(21, 0)).contains(reminderTime)
                                Text(if (isCustomEvening) "${reminderTime.hour.toString().padStart(2, '0')}:${reminderTime.minute.toString().padStart(2, '0')} (Custom)" else "Custom Time")
                            },
                            shape = RoundedCornerShape(12.dp)
                        )
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

                        FilterChip(
                            selected = !reminderEveningBefore && !listOf(LocalTime.of(6, 0), LocalTime.of(7, 0), LocalTime.of(8, 0)).contains(reminderTime),
                            onClick = {
                                isEveningCustomTime = false
                                showTimePickerDialog = true
                            },
                            label = { 
                                val isCustomMorning = !reminderEveningBefore && !listOf(LocalTime.of(6, 0), LocalTime.of(7, 0), LocalTime.of(8, 0)).contains(reminderTime)
                                Text(if (isCustomMorning) "${reminderTime.hour.toString().padStart(2, '0')}:${reminderTime.minute.toString().padStart(2, '0')} (Custom)" else "Custom Time")
                            },
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            }
        }
    }

    if (showTimePickerDialog) {
        val timePickerState = rememberTimePickerState(
            initialHour = reminderTime.hour,
            initialMinute = reminderTime.minute,
            is24Hour = true
        )

        AlertDialog(
            onDismissRequest = { showTimePickerDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val selectedTime = LocalTime.of(timePickerState.hour, timePickerState.minute)
                        onSettingsChange(selectedTime, isEveningCustomTime, reminderEnabled)
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
                        text = if (isEveningCustomTime) "Select Custom Evening Time" else "Select Custom Morning Time",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    TimePicker(state = timePickerState)
                }
            }
        )
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
            onPrimaryDaySelect = {},
            onBinToggle = {},
            onBinRecurrenceSelect = { _, _ -> },
            onBinStartNextWeekToggle = { _, _ -> },
            onBinColorSelect = { _, _ -> },
            onBinLidColorSelect = { _, _ -> },
            onBinDaySelect = { _, _ -> },
            onAddCustomBin = {},
            onRenameBin = { _, _ -> },
            onReminderSettingsChange = { _, _, _ -> },
            onNextStep = {},
            onPreviousStep = {},
            onGoToStep = {},
            canAdvanceFromCurrentStep = true,
            onCompleteSetup = {}
        )
    }
}
