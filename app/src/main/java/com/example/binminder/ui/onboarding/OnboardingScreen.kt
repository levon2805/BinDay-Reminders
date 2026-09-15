package com.example.binminder.ui.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.rounded.Add
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.binminder.R
import com.example.binminder.data.model.BinColor
import com.example.binminder.data.model.CouncilScheduleResult
import com.example.binminder.data.model.OnboardingBinSetup
import com.example.binminder.data.model.RecurrenceType
import com.example.binminder.ui.theme.BinMinderTheme
import com.example.binminder.ui.theme.ColorPickerDialog
import com.example.binminder.ui.theme.WheelieBinVisualSwatch
import com.example.binminder.ui.theme.getContrastingTextColor
import com.example.binminder.ui.theme.parseBinColor
import com.example.binminder.ui.theme.neoShadow
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.format.TextStyle
import java.time.format.DateTimeFormatter
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
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_app_logo),
                            contentDescription = "BinDay Logo",
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .border(2.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                        )
                        Column {
                            Text(
                                text = "SETUP WIZARD",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = "STEP ${uiState.currentStep}/4: " + when (uiState.currentStep) {
                                    1 -> "COUNCIL"
                                    2 -> "COLLECTION DAY"
                                    3 -> "MY BINS"
                                    4 -> "ALERTS"
                                    else -> ""
                                },
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    if (uiState.currentStep > 1) {
                        IconButton(
                            onClick = onPreviousStep,
                            modifier = Modifier
                                .padding(start = 12.dp)
                                .background(MaterialTheme.colorScheme.surface, CircleShape)
                                .border(2.dp, MaterialTheme.colorScheme.outline, CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier
                    .fillMaxWidth()
                    .neoShadow(color = MaterialTheme.colorScheme.outline, offset = 6.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (uiState.currentStep > 1) {
                        Button(
                            onClick = onPreviousStep,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            modifier = Modifier.height(56.dp).neoShadow(color = MaterialTheme.colorScheme.outline, offset = 4.dp)
                        ) {
                            Text("BACK", fontWeight = FontWeight.ExtraBold)
                        }
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }

                    if (uiState.currentStep < 4) {
                        Button(
                            onClick = onNextStep,
                            enabled = !uiState.isSearchingPostcode && canAdvanceFromCurrentStep,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            modifier = Modifier.height(56.dp).neoShadow(color = MaterialTheme.colorScheme.outline, offset = 4.dp)
                        ) {
                            Text("NEXT", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    } else {
                        Button(
                            onClick = onCompleteSetup,
                            enabled = !uiState.isCompleting,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            modifier = Modifier.height(56.dp).neoShadow(color = MaterialTheme.colorScheme.outline, offset = 4.dp)
                        ) {
                            if (uiState.isCompleting) {
                                CircularProgressIndicator(
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("SAVING...", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
                            } else {
                                Icon(
                                    imageVector = Icons.Rounded.CheckCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("LFG!", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
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
                    .height(8.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.outline
            )

            // Step Content Animated Transition
            AnimatedContent(
                targetState = uiState.currentStep,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "WizardStepTransition",
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp)
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
                        onBinStartNextWeekToggle = onBinStartNextWeekToggle,
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
 * with progress cards and council match success banners.
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
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Card(
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier.fillMaxWidth().neoShadow(color = MaterialTheme.colorScheme.outline, offset = 6.dp)
        ) {
            Row(
                modifier = Modifier.padding(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primary)
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(32.dp)
                    )
                }
                Spacer(modifier = Modifier.width(20.dp))
                Column {
                    Text(
                        text = "FIND COUNCIL",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Enter your postcode to check if we have your council's timetable on file.",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                    )
                }
            }
        }

        OutlinedTextField(
            value = postcodeOrCouncil.uppercase(),
            onValueChange = onQueryChange,
            leadingIcon = {
                Icon(imageVector = Icons.Rounded.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
            },
            trailingIcon = if (postcodeOrCouncil.isNotEmpty()) {
                {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(imageVector = Icons.Rounded.Clear, contentDescription = "Clear")
                    }
                }
            } else null,
            label = { Text("UK POSTCODE", fontWeight = FontWeight.ExtraBold) },
            placeholder = { Text("e.g. SW1A 1AA") },
            singleLine = true,
            enabled = !isSearching,
            shape = RoundedCornerShape(8.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface
            ),
            modifier = Modifier.fillMaxWidth().neoShadow(color = MaterialTheme.colorScheme.outline, offset = 4.dp)
        )

        Button(
            onClick = onSearchClick,
            enabled = postcodeOrCouncil.isNotBlank() && !isSearching,
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary, contentColor = MaterialTheme.colorScheme.onSecondary),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .neoShadow(color = MaterialTheme.colorScheme.outline, offset = 6.dp)
        ) {
            if (isSearching) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onSecondary,
                    strokeWidth = 3.dp
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text("SEARCHING...", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
            } else {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text("SEARCH", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
            }
        }

        // Council Match Success Banner
        if (detectedCouncil != null && !isSearching) {
            Card(
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth().neoShadow(color = MaterialTheme.colorScheme.outline, offset = 6.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primary)
                                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = "COUNCIL FOUND!",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = detectedCouncil.councilName.uppercase(),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
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
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface, contentColor = MaterialTheme.colorScheme.onSurface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier.fillMaxWidth().height(48.dp).neoShadow(color = MaterialTheme.colorScheme.outline, offset = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.OpenInNew,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "OPEN COUNCIL WEBSITE",
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // Error state
        if (searchError != null && !isSearching) {
            Card(
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth().neoShadow(color = MaterialTheme.colorScheme.error, offset = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = searchError.uppercase(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }

        // Skip option
        if (detectedCouncil == null && !isSearching) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth().neoShadow(color = MaterialTheme.colorScheme.outline, offset = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "Not found? Just tap NEXT to set up your bins manually.",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * Step 2 composable for selecting primary collection day with clear day cards.
 */
@Composable
fun Step2CollectionDayContent(
    selectedDay: DayOfWeek?,
    onDaySelected: (DayOfWeek) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Card(
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier.fillMaxWidth().neoShadow(color = MaterialTheme.colorScheme.outline, offset = 6.dp)
        ) {
            Row(
                modifier = Modifier.padding(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.secondary)
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CalendarMonth,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondary,
                        modifier = Modifier.size(32.dp)
                    )
                }
                Spacer(modifier = Modifier.width(20.dp))
                Column {
                    Text(
                        text = "BIN DAY",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "What day of the week are your bins collected?",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f)
                    )
                }
            }
        }

        DayOfWeek.entries.forEach { day ->
            val isSelected = day == selectedDay
            val dayName = day.getDisplayName(TextStyle.FULL, Locale.UK).uppercase()

            Card(
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onDaySelected(day) }
                    .neoShadow(color = MaterialTheme.colorScheme.outline, offset = if (isSelected) 0.dp else 4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = dayName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                    )

                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Step 3 composable for selecting bins, editing names, choosing body/lid colors,
 * and 1-tap week cycle selectors ("Which bin goes out THIS coming week?").
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun Step3BinsContent(
    binSetups: List<OnboardingBinSetup>,
    onBinToggle: (String) -> Unit,
    onRecurrenceSelect: (String, RecurrenceType) -> Unit,
    onBinStartNextWeekToggle: (String, Boolean) -> Unit,
    onBinColorSelect: (String, BinColor) -> Unit,
    onBinLidColorSelect: (String, BinColor?) -> Unit,
    onBinDaySelect: (String, DayOfWeek) -> Unit,
    onAddCustomBin: () -> Unit,
    onRenameBin: (String, String) -> Unit
) {
    var activeColorPickerBinType by remember { mutableStateOf<String?>(null) }
    var isLidPicker by remember { mutableStateOf(false) }

    Column(
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Card(
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier.fillMaxWidth().neoShadow(color = MaterialTheme.colorScheme.outline, offset = 6.dp)
        ) {
            Row(
                modifier = Modifier.padding(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.tertiary)
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onTertiary,
                        modifier = Modifier.size(32.dp)
                    )
                }
                Spacer(modifier = Modifier.width(20.dp))
                Column {
                    Text(
                        text = "CONFIGURE BINS",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Set the colours, names, and collection schedule for your bins.",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.85f)
                    )
                }
            }
        }

        binSetups.forEach { setup ->
            Card(
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (setup.isEnabled) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier
                    .fillMaxWidth()
                    .neoShadow(color = MaterialTheme.colorScheme.outline, offset = if (setup.isEnabled) 6.dp else 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f).padding(end = 12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                WheelieBinVisualSwatch(
                                    presetColor = setup.presetColor,
                                    lidPresetColor = setup.lidPresetColor,
                                    size = 48.dp,
                                    isEnabled = setup.isEnabled
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            OutlinedTextField(
                                value = setup.displayName.uppercase(),
                                onValueChange = { onRenameBin(setup.binType, it) },
                                modifier = Modifier.fillMaxWidth(),
                                textStyle = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                                singleLine = true,
                                label = { Text("BIN NAME", fontWeight = FontWeight.ExtraBold) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedContainerColor = MaterialTheme.colorScheme.surface
                                )
                            )
                        }

                        Switch(
                            checked = setup.isEnabled,
                            onCheckedChange = { onBinToggle(setup.binType) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.primary,
                                checkedTrackColor = MaterialTheme.colorScheme.onPrimary,
                                checkedBorderColor = MaterialTheme.colorScheme.outline,
                                uncheckedBorderColor = MaterialTheme.colorScheme.outline
                            )
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

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(20.dp)
                            ) {
                                // Body Colour Picker
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Text(
                                        text = "BODY COLOUR",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )

                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        availableColors.forEach { color ->
                                            val colorHex = parseBinColor(color.defaultHex, color)
                                            val isSelected = setup.presetColor == color

                                            Box(
                                                modifier = Modifier
                                                    .size(48.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(colorHex)
                                                    .border(
                                                        width = 2.dp,
                                                        color = MaterialTheme.colorScheme.outline,
                                                        shape = RoundedCornerShape(8.dp)
                                                    )
                                                    .clickable { onBinColorSelect(setup.binType, color) }
                                                    .neoShadow(color = MaterialTheme.colorScheme.outline, offset = if (isSelected) 0.dp else 2.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (isSelected) {
                                                    Icon(
                                                        imageVector = Icons.Rounded.Check,
                                                        contentDescription = color.displayName,
                                                        tint = getContrastingTextColor(colorHex),
                                                        modifier = Modifier.size(24.dp)
                                                    )
                                                }
                                            }
                                        }

                                        // Custom Colour Picker Button
                                        Box(
                                            modifier = Modifier
                                                .size(48.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(MaterialTheme.colorScheme.primaryContainer)
                                                .border(
                                                    width = 2.dp,
                                                    color = MaterialTheme.colorScheme.outline,
                                                    shape = RoundedCornerShape(8.dp)
                                                )
                                                .clickable {
                                                    activeColorPickerBinType = setup.binType
                                                    isLidPicker = false
                                                }
                                                .neoShadow(color = MaterialTheme.colorScheme.outline, offset = 2.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Palette,
                                                contentDescription = "Colour Wheel Picker",
                                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                                modifier = Modifier.size(28.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(2.dp)
                                        .background(MaterialTheme.colorScheme.outline)
                                )

                                // Lid Colour Picker
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "LID COLOUR",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (setup.lidPresetColor == null) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surface,
                                            contentColor = if (setup.lidPresetColor == null) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurface,
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                            modifier = Modifier
                                                .clickable { onBinLidColorSelect(setup.binType, null) }
                                                .neoShadow(color = MaterialTheme.colorScheme.outline, offset = if (setup.lidPresetColor == null) 0.dp else 2.dp)
                                        ) {
                                            Text(
                                                text = "MATCH",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.ExtraBold,
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                            )
                                        }
                                    }

                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        availableColors.forEach { color ->
                                            val colorHex = parseBinColor(color.defaultHex, color)
                                            val isSelected = setup.lidPresetColor == color

                                            Box(
                                                modifier = Modifier
                                                    .size(48.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(colorHex)
                                                    .border(
                                                        width = 2.dp,
                                                        color = MaterialTheme.colorScheme.outline,
                                                        shape = RoundedCornerShape(8.dp)
                                                    )
                                                    .clickable { onBinLidColorSelect(setup.binType, color) }
                                                    .neoShadow(color = MaterialTheme.colorScheme.outline, offset = if (isSelected) 0.dp else 2.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (isSelected) {
                                                    Icon(
                                                        imageVector = Icons.Rounded.Check,
                                                        contentDescription = color.displayName,
                                                        tint = getContrastingTextColor(colorHex),
                                                        modifier = Modifier.size(24.dp)
                                                    )
                                                }
                                            }
                                        }

                                        // Custom Lid Colour Picker Button
                                        Box(
                                            modifier = Modifier
                                                .size(48.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(MaterialTheme.colorScheme.secondaryContainer)
                                                .border(
                                                    width = 2.dp,
                                                    color = MaterialTheme.colorScheme.outline,
                                                    shape = RoundedCornerShape(8.dp)
                                                )
                                                .clickable {
                                                    activeColorPickerBinType = setup.binType
                                                    isLidPicker = true
                                                }
                                                .neoShadow(color = MaterialTheme.colorScheme.outline, offset = 2.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Palette,
                                                contentDescription = "Lid Colour Wheel Picker",
                                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                                modifier = Modifier.size(28.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Text(
                            text = "COLLECTION DAY",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 8.dp)
                        )

                        var expandedDay by remember { mutableStateOf(false) }
                        val currentDay = setup.collectionDay

                        Box {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                modifier = Modifier
                                    .clickable { expandedDay = true }
                                    .neoShadow(color = MaterialTheme.colorScheme.outline, offset = 4.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                                ) {
                                    Text(
                                        text = currentDay?.name?.uppercase() ?: "SAME AS MAIN DAY",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                }
                            }
                            
                            DropdownMenu(
                                expanded = expandedDay,
                                onDismissRequest = { expandedDay = false }
                            ) {
                                DayOfWeek.entries.forEach { day ->
                                    DropdownMenuItem(
                                        text = { Text(day.name.uppercase(), fontWeight = FontWeight.ExtraBold) },
                                        onClick = {
                                            onBinDaySelect(setup.binType, day)
                                            expandedDay = false
                                        }
                                    )
                                }
                            }
                        }

                        Text(
                            text = "HOW OFTEN?",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 8.dp)
                        )

                        var expandedWeeks by remember { mutableStateOf(false) }

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            listOf(RecurrenceType.WEEKLY, RecurrenceType.FORTNIGHTLY).forEach { rec ->
                                val isSelected = setup.recurrence == rec
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                    contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                    modifier = Modifier
                                        .clickable { onRecurrenceSelect(setup.binType, rec) }
                                        .neoShadow(color = MaterialTheme.colorScheme.outline, offset = if (isSelected) 0.dp else 4.dp)
                                ) {
                                    Text(
                                        text = rec.displayName.uppercase(),
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.ExtraBold,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                                    )
                                }
                            }

                            val isOtherSelected = setup.recurrence.intervalWeeks > 2
                            Box {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isOtherSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                    contentColor = if (isOtherSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                    modifier = Modifier
                                        .clickable { expandedWeeks = true }
                                        .neoShadow(color = MaterialTheme.colorScheme.outline, offset = if (isOtherSelected) 0.dp else 4.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                                    ) {
                                        Text(
                                            text = if (isOtherSelected) "EVERY ${setup.recurrence.intervalWeeks} WEEKS" else "OTHER",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                    }
                                }
                                
                                DropdownMenu(
                                    expanded = expandedWeeks,
                                    onDismissRequest = { expandedWeeks = false }
                                ) {
                                    (3..8).forEach { weeks ->
                                        DropdownMenuItem(
                                            text = { Text("EVERY $weeks WEEKS", fontWeight = FontWeight.ExtraBold) },
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

                        // 1-Tap Week Cycle Selectors ("Which bin goes out THIS coming week?")
                        if (setup.recurrence.intervalWeeks > 1) {
                            Text(
                                text = "STARTING WHEN?",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = 8.dp)
                            )

                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (!setup.startNextWeek) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                                    contentColor = if (!setup.startNextWeek) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                    modifier = Modifier
                                        .clickable { onBinStartNextWeekToggle(setup.binType, false) }
                                        .neoShadow(color = MaterialTheme.colorScheme.outline, offset = if (!setup.startNextWeek) 0.dp else 4.dp)
                                ) {
                                    Text(
                                        text = "THIS WEEK",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.ExtraBold,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (setup.startNextWeek) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                                    contentColor = if (setup.startNextWeek) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                    modifier = Modifier
                                        .clickable { onBinStartNextWeekToggle(setup.binType, true) }
                                        .neoShadow(color = MaterialTheme.colorScheme.outline, offset = if (setup.startNextWeek) 0.dp else 4.dp)
                                ) {
                                    Text(
                                        text = "NEXT WEEK",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.ExtraBold,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Button(
            onClick = onAddCustomBin,
            modifier = Modifier.fillMaxWidth().height(64.dp).padding(top = 8.dp).neoShadow(color = MaterialTheme.colorScheme.outline, offset = 6.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface, contentColor = MaterialTheme.colorScheme.onSurface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Icon(
                imageVector = Icons.Rounded.Add,
                contentDescription = null,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text("ADD ANOTHER BIN", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
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
                    title = if (isLidPicker) "SELECT LID COLOUR" else "SELECT BODY COLOUR",
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
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Card(
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier.fillMaxWidth().neoShadow(color = MaterialTheme.colorScheme.outline, offset = 6.dp)
        ) {
            Row(
                modifier = Modifier.padding(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primary)
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Notifications,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(32.dp)
                    )
                }
                Spacer(modifier = Modifier.width(20.dp))
                Column {
                    Text(
                        text = "GET ALERTED",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Never miss bin day again.",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Card(
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier.fillMaxWidth().neoShadow(color = MaterialTheme.colorScheme.outline, offset = 6.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ENABLE ALERTS",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold
                    )

                    Switch(
                        checked = reminderEnabled,
                        onCheckedChange = { onSettingsChange(reminderTime, reminderEveningBefore, it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.primary,
                            checkedTrackColor = MaterialTheme.colorScheme.onPrimary,
                            checkedBorderColor = MaterialTheme.colorScheme.outline,
                            uncheckedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )
                }

                if (reminderEnabled) {
                    Column {
                        Text(
                            text = "EVENING BEFORE",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            listOf(
                                LocalTime.of(19, 0) to "19:00",
                                LocalTime.of(20, 0) to "20:00",
                                LocalTime.of(21, 0) to "21:00"
                            ).forEach { (time, label) ->
                                val isSelected = reminderEveningBefore && reminderTime == time
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                    contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                    modifier = Modifier
                                        .clickable { onSettingsChange(time, true, true) }
                                        .neoShadow(color = MaterialTheme.colorScheme.outline, offset = if (isSelected) 0.dp else 4.dp)
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.ExtraBold,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                                    )
                                }
                            }

                            val isEveningCustom = reminderEveningBefore && !listOf(LocalTime.of(19, 0), LocalTime.of(20, 0), LocalTime.of(21, 0)).contains(reminderTime)
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isEveningCustom) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                contentColor = if (isEveningCustom) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                modifier = Modifier
                                    .clickable {
                                        isEveningCustomTime = true
                                        showTimePickerDialog = true
                                    }
                                    .neoShadow(color = MaterialTheme.colorScheme.outline, offset = if (isEveningCustom) 0.dp else 4.dp)
                            ) {
                                Text(
                                    text = if (isEveningCustom) "CUSTOM (${reminderTime.format(DateTimeFormatter.ofPattern("HH:mm"))})" else "CUSTOM",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                                )
                            }
                        }
                    }
                    
                    Column {
                        Text(
                            text = "MORNING OF",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            listOf(
                                LocalTime.of(6, 0) to "06:00",
                                LocalTime.of(7, 0) to "07:00",
                                LocalTime.of(8, 0) to "08:00"
                            ).forEach { (time, label) ->
                                val isSelected = !reminderEveningBefore && reminderTime == time
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                    contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                    modifier = Modifier
                                        .clickable { onSettingsChange(time, false, true) }
                                        .neoShadow(color = MaterialTheme.colorScheme.outline, offset = if (isSelected) 0.dp else 4.dp)
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.ExtraBold,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                                    )
                                }
                            }

                            val isMorningCustom = !reminderEveningBefore && !listOf(LocalTime.of(6, 0), LocalTime.of(7, 0), LocalTime.of(8, 0)).contains(reminderTime)
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isMorningCustom) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                contentColor = if (isMorningCustom) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                modifier = Modifier
                                    .clickable {
                                        isEveningCustomTime = false
                                        showTimePickerDialog = true
                                    }
                                    .neoShadow(color = MaterialTheme.colorScheme.outline, offset = if (isMorningCustom) 0.dp else 4.dp)
                            ) {
                                Text(
                                    text = if (isMorningCustom) "CUSTOM (${reminderTime.format(DateTimeFormatter.ofPattern("HH:mm"))})" else "CUSTOM",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                                )
                            }
                        }
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
            containerColor = MaterialTheme.colorScheme.surface,
            confirmButton = {
                Button(
                    onClick = {
                        val selectedTime = LocalTime.of(timePickerState.hour, timePickerState.minute)
                        onSettingsChange(selectedTime, isEveningCustomTime, reminderEnabled)
                        showTimePickerDialog = false
                    },
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("SET TIME", fontWeight = FontWeight.ExtraBold)
                }
            },
            dismissButton = {
                Button(
                    onClick = { showTimePickerDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("CANCEL", fontWeight = FontWeight.ExtraBold)
                }
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (isEveningCustomTime) "EVENING TIME" else "MORNING TIME",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
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
