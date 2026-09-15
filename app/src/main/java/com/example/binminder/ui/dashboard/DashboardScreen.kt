package com.example.binminder.ui.dashboard

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.EventAvailable
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.binminder.R
import com.example.binminder.data.model.BinColor
import com.example.binminder.data.model.CollectionEvent
import com.example.binminder.ui.dialogs.CalendarExportDialog
import com.example.binminder.ui.theme.BinMinderTheme
import com.example.binminder.ui.theme.WheelieBinVisualSwatch
import com.example.binminder.ui.theme.formatBritishDate
import com.example.binminder.ui.theme.formatRelativeDays
import com.example.binminder.ui.theme.neoShadow
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

/**
 * Main dashboard screen composable displaying upcoming UK bin collections and timetable cards.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onNavigateToAddBin: () -> Unit,
    onNavigateToBinDetail: (Long) -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(uiState.userMessage) {
        uiState.userMessage?.let { message ->
            viewModel.dismissUserMessage()
            snackbarHostState.showSnackbar(message)
        }
    }

    DashboardContent(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onMarkBinPutOut = { binId, name -> viewModel.markBinPutOut(binId, name, context) },
        onNavigateToAddBin = onNavigateToAddBin,
        onNavigateToBinDetail = onNavigateToBinDetail,
        onNavigateToSettings = onNavigateToSettings,
        modifier = modifier
    )
}

/**
 * Structural layout for the main dashboard screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardContent(
    uiState: DashboardUiState,
    snackbarHostState: SnackbarHostState,
    onMarkBinPutOut: (Long, String) -> Unit,
    onNavigateToAddBin: () -> Unit,
    onNavigateToBinDetail: (Long) -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_app_logo),
                            contentDescription = "BinDay Logo",
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .border(2.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                        )
                        Column {
                            Text(
                                text = "BINDAY",
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 2.sp
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = onNavigateToSettings,
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .background(MaterialTheme.colorScheme.secondary, CircleShape)
                            .border(2.dp, MaterialTheme.colorScheme.outline, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Notifications,
                            contentDescription = "Notification Settings",
                            tint = MaterialTheme.colorScheme.onSecondary
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            } else if (uiState.nextCollectionEvents.isEmpty()) {
                EmptyScheduleView(
                    onAddBinClicked = onNavigateToAddBin,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    item {
                        NextCollectionHeroCard(
                            nextDate = uiState.nextCollectionDate ?: LocalDate.now(),
                            daysRemaining = uiState.daysRemaining,
                            events = uiState.nextCollectionEvents,
                            putOutBins = uiState.putOutBins,
                            onMarkPutOut = onMarkBinPutOut,
                            onViewBinDetail = onNavigateToBinDetail
                        )
                    }

                    if (uiState.upcomingEventsGrouped.isNotEmpty()) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 16.dp, bottom = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "TIMETABLE",
                                    style = MaterialTheme.typography.displaySmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                if (uiState.allBins.any { it.isEnabled }) {
                                    Button(
                                        onClick = { showCalendarExportDialog = true },
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.tertiary,
                                            contentColor = MaterialTheme.colorScheme.onTertiary
                                        ),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                        modifier = Modifier.neoShadow(offset = 4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.CalendarToday,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "EXPORT",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    }
                                }
                            }
                        }

                        items(
                            items = uiState.upcomingEventsGrouped.toList(),
                            key = { it.first.toString() }
                        ) { (date, eventsOnDate) ->
                            UpcomingDateSection(
                                date = date,
                                events = eventsOnDate,
                                onViewBinDetail = onNavigateToBinDetail
                            )
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }
            }
        }
    }
}

/**
 * Featured hero card displaying the immediate next bin collection date, relative days pill badge, and interactive bin cards.
 */
@Composable
fun NextCollectionHeroCard(
    nextDate: LocalDate,
    daysRemaining: Long,
    events: List<CollectionEvent>,
    putOutBins: Set<Long>,
    onMarkPutOut: (Long, String) -> Unit,
    onViewBinDetail: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor = MaterialTheme.colorScheme.primaryContainer
    val onContainerColor = MaterialTheme.colorScheme.onPrimaryContainer

    val hasBankHolidayShift = events.any { it.isBankHolidayAdjusted }

    // Display "TODAY" or "TOMORROW" very loud if applicable
    val dayHeadline = when (daysRemaining) {
        0L -> "TODAY!"
        1L -> "TOMORROW!"
        else -> formatRelativeDays(daysRemaining).uppercase()
    }

    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = modifier
            .fillMaxWidth()
            .neoShadow(offset = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp)
        ) {
            // Header row: Label
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.Schedule,
                    contentDescription = null,
                    tint = onContainerColor,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "NEXT UP",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = onContainerColor,
                    letterSpacing = 2.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Big chunky relative day
            Text(
                text = dayHeadline,
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            
            // Sub date
            Text(
                text = formatBritishDate(nextDate).uppercase(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = onContainerColor.copy(alpha = 0.8f),
                letterSpacing = 1.sp
            )

            // Bank Holiday Shift Banner
            if (hasBankHolidayShift) {
                Spacer(modifier = Modifier.height(16.dp))
                BankHolidayShiftBanner(events = events)
            }

            Spacer(modifier = Modifier.height(24.dp))

            // List of bins for this next collection
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                events.forEach { event ->
                    val isPutOut = putOutBins.contains(event.binId)
                    HeroBinItemCard(
                        event = event,
                        isPutOut = isPutOut,
                        onMarkPutOut = { onMarkPutOut(event.binId, event.binName) },
                        onViewBinDetail = { onViewBinDetail(event.binId) }
                    )
                }
            }
        }
    }
}

/**
 * Explanatory banner notifying users when a collection date has been moved due to a UK bank holiday.
 */
@Composable
fun BankHolidayShiftBanner(events: List<CollectionEvent> = emptyList()) {
    val adjustedEvent = events.firstOrNull { it.isBankHolidayAdjusted }
    val shiftedDay = adjustedEvent?.collectionDate?.dayOfWeek?.getDisplayName(TextStyle.FULL, Locale.UK)

    val detailText = if (shiftedDay != null) {
        "SHIFTED TO $shiftedDay!"
    } else {
        "+1 DAY BANK HOLIDAY SHIFT!"
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.tertiary,
        contentColor = MaterialTheme.colorScheme.onTertiary,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier
            .fillMaxWidth()
            .neoShadow(offset = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.EventAvailable,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onTertiary,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = detailText.uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onTertiary
                )
            }
        }
    }
}

/**
 * Interactive bin item card embedded inside the next collection hero section with tactile styling and micro-animation.
 */
@Composable
fun HeroBinItemCard(
    event: CollectionEvent,
    isPutOut: Boolean,
    onMarkPutOut: () -> Unit,
    onViewBinDetail: () -> Unit
) {
    Card(
        onClick = onViewBinDetail,
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier
            .fillMaxWidth()
            .neoShadow(offset = 6.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Dual-Colour Visual Bin Swatch
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    WheelieBinVisualSwatch(
                        presetColor = event.presetColor,
                        colorHex = event.binColorHex,
                        lidPresetColor = event.lidPresetColor,
                        lidColorHex = event.lidColorHex,
                        size = 44.dp
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Bin Name & Note
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = event.binName.uppercase(),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (event.customNote.isNotBlank()) {
                        Text(
                            text = event.customNote.uppercase(),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Animated Put Out Button with Tactile Container Styling & Smooth State Change
            val animatedContainerColor by animateColorAsState(
                targetValue = if (isPutOut) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceVariant,
                animationSpec = tween(durationMillis = 200),
                label = "PutOutContainerColor"
            )
            val animatedContentColor by animateColorAsState(
                targetValue = if (isPutOut) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurfaceVariant,
                animationSpec = tween(durationMillis = 200),
                label = "PutOutContentColor"
            )

            Surface(
                onClick = onMarkPutOut,
                shape = RoundedCornerShape(8.dp),
                color = animatedContainerColor,
                contentColor = animatedContentColor,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier
                    .fillMaxWidth()
                    .neoShadow(
                        offset = if (isPutOut) 0.dp else 4.dp // Press effect!
                    )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp, horizontal = 16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AnimatedContent(
                        targetState = isPutOut,
                        transitionSpec = { fadeIn(tween(150)) togetherWith fadeOut(tween(150)) },
                        label = "PutOutStateTransition"
                    ) { putOut ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            if (putOut) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = null,
                                    tint = animatedContentColor,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "DONE! BINS OUT",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = animatedContentColor
                                )
                            } else {
                                Text(
                                    text = "PUT BINS OUT",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = animatedContentColor
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
 * Timetable section representing collections due on a future date.
 */
@Composable
fun UpcomingDateSection(
    date: LocalDate,
    events: List<CollectionEvent>,
    onViewBinDetail: (Long) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Date Header
        Surface(
            color = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .neoShadow(offset = 4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.CalendarToday,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = formatBritishDate(date).uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }

        // Cards for events on this date
        events.forEach { event ->
            UpcomingEventCard(
                event = event,
                onClick = { onViewBinDetail(event.binId) }
            )
        }
    }
}

/**
 * Compact card composable displaying a future collection event with dual-colour swatch and tactile styling.
 */
@Composable
fun UpcomingEventCard(
    event: CollectionEvent,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .neoShadow(offset = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Dual-colour swatch icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                WheelieBinVisualSwatch(
                    presetColor = event.presetColor,
                    colorHex = event.binColorHex,
                    lidPresetColor = event.lidPresetColor,
                    lidColorHex = event.lidColorHex,
                    size = 36.dp
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .padding(end = 8.dp)
            ) {
                Text(
                    text = event.binName.uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (event.customNote.isNotBlank()) {
                    Text(
                        text = event.customNote.uppercase(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (event.isBankHolidayAdjusted) {
                val origDay = event.originalDate.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.UK)
                val newDay = event.collectionDate.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.UK)
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = MaterialTheme.colorScheme.tertiary,
                    contentColor = MaterialTheme.colorScheme.onTertiary,
                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Text(
                        text = "+1D ($origDay➔$newDay)".uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

/**
 * Placeholder view displayed when no active bins or collection schedules exist.
 */
@Composable
fun EmptyScheduleView(
    onAddBinClicked: () -> Unit,
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
            text = "NO SCHEDULED COLLECTIONS",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Add your council bin schedules to see upcoming collections.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onAddBinClicked,
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier
                .height(56.dp)
                .neoShadow(offset = 6.dp)
        ) {
            Icon(imageVector = Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text(text = "ADD A BIN", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
        }
    }
}

/**
 * Jetpack Compose preview function for the dashboard screen.
 */
@Preview(showBackground = true)
@Composable
fun DashboardScreenPreview() {
    val sampleDate = LocalDate.now().plusDays(1)
    val sampleEvents = listOf(
        CollectionEvent(
            binId = 1,
            binName = "General Waste",
            binColorHex = BinColor.BLACK.defaultHex,
            presetColor = BinColor.BLACK,
            lidPresetColor = BinColor.BLUE,
            collectionDate = sampleDate,
            originalDate = sampleDate,
            isBankHolidayAdjusted = false,
            customNote = "Black bin with blue lid"
        ),
        CollectionEvent(
            binId = 2,
            binName = "Food Waste Caddy",
            binColorHex = BinColor.BROWN.defaultHex,
            presetColor = BinColor.BROWN,
            collectionDate = sampleDate,
            originalDate = sampleDate,
            isBankHolidayAdjusted = false,
            customNote = "Lock handle"
        )
    )

    BinMinderTheme {
        DashboardContent(
            uiState = DashboardUiState(
                isLoading = false,
                nextCollectionDate = sampleDate,
                nextCollectionEvents = sampleEvents,
                daysRemaining = 1,
                upcomingEventsGrouped = mapOf(
                    sampleDate.plusWeeks(1) to listOf(
                        CollectionEvent(
                            binId = 3,
                            binName = "Dry Mixed Recycling",
                            binColorHex = BinColor.BLUE.defaultHex,
                            presetColor = BinColor.BLUE,
                            collectionDate = sampleDate.plusWeeks(1),
                            originalDate = sampleDate.plusWeeks(1).minusDays(1),
                            isBankHolidayAdjusted = true,
                            customNote = "Paper and card"
                        )
                    )
                )
            ),
            snackbarHostState = remember { SnackbarHostState() },
            onMarkBinPutOut = { _, _ -> },
            onNavigateToAddBin = {},
            onNavigateToBinDetail = {},
            onNavigateToSettings = {}
        )
    }
}
