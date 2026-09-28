package com.example.binminder.ui.dashboard

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.binminder.data.model.Bin
import com.example.binminder.data.model.CollectionEvent
import com.example.binminder.data.repository.BinRepository
import com.example.binminder.domain.GetUpcomingCollectionsUseCase
import com.example.binminder.domain.ResetTimetableUseCase
import com.example.binminder.domain.ToggleBinPutOutUseCase
import com.example.binminder.worker.NotificationScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Data state holding timetable collection information and user messaging for the dashboard screen.
 */
data class DashboardUiState(
    val isLoading: Boolean = true,
    val nextCollectionDate: LocalDate? = null,
    val nextCollectionEvents: List<CollectionEvent> = emptyList(),
    val daysRemaining: Long = 0,
    val upcomingEventsGrouped: Map<LocalDate, List<CollectionEvent>> = emptyMap(),
    val putOutBins: Set<String> = emptySet(),
    val allBins: List<Bin> = emptyList(),
    val userMessage: String? = null
)

/**
 * ViewModel managing collection timetable state and user interactions on the main dashboard screen.
 *
 * Uses domain use cases [GetUpcomingCollectionsUseCase] and [ToggleBinPutOutUseCase] for clean UDF state updates.
 */
class DashboardViewModel(
    private val repository: BinRepository,
    getUpcomingCollectionsUseCase: GetUpcomingCollectionsUseCase = GetUpcomingCollectionsUseCase(repository),
    private val toggleBinPutOutUseCase: ToggleBinPutOutUseCase = ToggleBinPutOutUseCase(),
    private val resetTimetableUseCase: ResetTimetableUseCase,
    started: SharingStarted = SharingStarted.WhileSubscribed(5000)
) : ViewModel() {

    private val _userMessage = MutableStateFlow<String?>(null)

    init {
        viewModelScope.launch {
            runCatching {
                repository.ensureDefaultBinsInitialized()
            }
        }
    }

    /**
     * Observable flow of the current immutable dashboard UI state.
     */
    val uiState: StateFlow<DashboardUiState> = combine(
        getUpcomingCollectionsUseCase(LocalDate.now(), LocalDate.now().plusWeeks(8)),
        repository.allBins,
        repository.putOutBins,
        _userMessage
    ) { events, allBins, putOutBins, userMessage ->
        val today = LocalDate.now()
        val validEvents = events.filter { !it.collectionDate.isBefore(today) }

        if (validEvents.isEmpty()) {
            DashboardUiState(
                isLoading = false,
                putOutBins = putOutBins,
                allBins = allBins,
                userMessage = userMessage
            )
        } else {
            val grouped = validEvents.groupBy { it.collectionDate }
            val dates = grouped.keys.sorted()
            val earliestDate = dates.firstOrNull()

            val nextEvents = mutableListOf<CollectionEvent>()
            var lastHeroDate = earliestDate

            if (earliestDate != null) {
                nextEvents.addAll(grouped[earliestDate].orEmpty())
                
                if (dates.size > 1) {
                    val secondDate = dates[1]
                    if (ChronoUnit.DAYS.between(earliestDate, secondDate) == 1L) {
                        nextEvents.addAll(grouped[secondDate].orEmpty())
                        lastHeroDate = secondDate
                    }
                }
            }

            val days = if (earliestDate != null) ChronoUnit.DAYS.between(today, earliestDate) else 0L

            val remainingGrouped = grouped.filterKeys { it != earliestDate && it != lastHeroDate }

            DashboardUiState(
                isLoading = false,
                nextCollectionDate = earliestDate,
                nextCollectionEvents = nextEvents,
                daysRemaining = days,
                upcomingEventsGrouped = remainingGrouped,
                putOutBins = putOutBins,
                allBins = allBins,
                userMessage = userMessage
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = started,
        initialValue = DashboardUiState(isLoading = true)
    )

    /**
     * Toggles whether a specific bin has been put out on the kerb for collection using [ToggleBinPutOutUseCase].
     */
    fun markBinPutOut(binId: Long, collectionDate: LocalDate, binName: String, context: Context? = null) {
        viewModelScope.launch {
            val currentPutOutBins = repository.putOutBins.first()
            val result = toggleBinPutOutUseCase(
                binId = binId,
                collectionDate = collectionDate,
                binName = binName,
                currentPutOutBins = currentPutOutBins
            )

            _userMessage.value = result.userMessage
            repository.updatePutOutBins(result.updatedPutOutBins)
            if (context != null) {
                if (result.updatedPutOutBins.contains("${binId}_$collectionDate")) {
                    // Bin was marked as put out: suppress/cancel system tray notification
                    NotificationScheduler.cancelOrSuppressNotificationForToday(context)
                }
                NotificationScheduler.scheduleNotificationWorkerSuspend(context)
            }
        }
    }

    /**
     * Clears the current user message snackbar notification.
     */
    fun dismissUserMessage() {
        _userMessage.value = null
    }

    /**
     * Clears saved bins and resets onboarding state for setup wizard re-run via [ResetTimetableUseCase].
     */
    fun resetTimetableAndAddress(context: Context, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            resetTimetableUseCase(context)
            onComplete()
        }
    }
}
