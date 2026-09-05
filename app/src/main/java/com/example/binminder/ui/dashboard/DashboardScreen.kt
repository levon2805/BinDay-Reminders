package com.example.binminder.ui.dashboard

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.EventAvailable
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.binminder.data.model.BinColor
import com.example.binminder.data.model.CollectionEvent
import com.example.binminder.ui.theme.BinMinderTheme
import com.example.binminder.ui.theme.formatBritishDate
import com.example.binminder.ui.theme.formatRelativeDays
import com.example.binminder.ui.theme.getContrastingTextColor
import com.example.binminder.ui.theme.parseBinColor
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
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.userMessage) {
        uiState.userMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.dismissUserMessage()
        }
    }

    DashboardContent(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onMarkBinPutOut = { binId, name -> viewModel.markBinPutOut(binId, name) },
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
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "BinMinder",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "UK Council Collection Schedule",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = Icons.Rounded.Notifications,
                            contentDescription = "Notification Settings"
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
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Spacer(modifier = Modifier.height(4.dp))
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
                            Text(
                                text = "Upcoming Timetable",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                            )
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
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }
    }
}

/**
 * Featured hero card displaying the immediate next bin collection date and interactive items.
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

    ElevatedCard(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = containerColor),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            // Header row: Badge + Days remaining
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Schedule,
                        contentDescription = null,
                        tint = onContainerColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "NEXT COLLECTION",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = onContainerColor,
                        letterSpacing = 1.sp
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Text(
                        text = formatRelativeDays(daysRemaining),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Date Headline
            Text(
                text = formatBritishDate(nextDate),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = onContainerColor
            )

            // Bank Holiday Shift Banner
            if (hasBankHolidayShift) {
                Spacer(modifier = Modifier.height(12.dp))
                BankHolidayShiftBanner(events = events)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = if (events.size > 1) "Wheelie Bins to put out:" else "Wheelie Bin to put out:",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = onContainerColor.copy(alpha = 0.8f)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // List of bins for this next collection
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
    val originalDay = adjustedEvent?.originalDate?.dayOfWeek?.getDisplayName(TextStyle.FULL, Locale.UK)
    val shiftedDay = adjustedEvent?.collectionDate?.dayOfWeek?.getDisplayName(TextStyle.FULL, Locale.UK)

    val detailText = if (originalDay != null && shiftedDay != null) {
        "Collection day shifts from $originalDay ➔ $shiftedDay (+1 day shift)"
    } else {
        "Adjusted automatically by +1 day for UK Bank Holiday"
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.EventAvailable,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = "+1 Day Bank Holiday Shift",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
                Text(
                    text = detailText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                )
            }
        }
    }
}

/**
 * Interactive bin item card embedded inside the next collection hero section.
 */
@Composable
fun HeroBinItemCard(
    event: CollectionEvent,
    isPutOut: Boolean,
    onMarkPutOut: () -> Unit,
    onViewBinDetail: () -> Unit
) {
    val binBgColor = parseBinColor(event.binColorHex, event.presetColor)
    val binTextColor = getContrastingTextColor(binBgColor)

    Card(
        onClick = onViewBinDetail,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Bin Colour Swatch
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(binBgColor)
                        .border(1.5.dp, Color.White.copy(alpha = 0.6f), CircleShape),
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

                // Bin Name & Note taking full horizontal width
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = event.binName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (event.customNote.isNotBlank()) {
                        Text(
                            text = event.customNote,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Put Out Quick Action Button beneath bin details
            if (isPutOut) {
                FilledTonalButton(
                    onClick = onMarkPutOut,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Put Out ✓",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                OutlinedButton(
                    onClick = onMarkPutOut,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Mark Put Out",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
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
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Date Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(vertical = 4.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.CalendarToday,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = formatBritishDate(date),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
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
 * Compact card composable displaying a future collection event.
 */
@Composable
fun UpcomingEventCard(
    event: CollectionEvent,
    onClick: () -> Unit
) {
    val binBgColor = parseBinColor(event.binColorHex, event.presetColor)

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left colour strip / circle
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(binBgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Delete,
                    contentDescription = null,
                    tint = getContrastingTextColor(binBgColor),
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .padding(end = 8.dp)
            ) {
                Text(
                    text = event.binName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (event.customNote.isNotBlank()) {
                    Text(
                        text = event.customNote,
                        style = MaterialTheme.typography.bodySmall,
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
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                ) {
                    Text(
                        text = "+1d Bank Holiday ($origDay ➔ $newDay)",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
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
            text = "No Active Wheelie Bins",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Add your council wheelie bin schedules to display your upcoming timetable.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = onAddBinClicked,
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(imageVector = Icons.Rounded.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "Add Wheelie Bin")
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
            collectionDate = sampleDate,
            originalDate = sampleDate,
            isBankHolidayAdjusted = false,
            customNote = "Put lid down"
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
