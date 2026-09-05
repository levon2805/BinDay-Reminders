package com.example.binminder.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.binminder.data.model.CollectionEvent
import com.example.binminder.data.repository.BinRepository
import com.example.binminder.domain.GetUpcomingCollectionsUseCase
import com.example.binminder.domain.ToggleBinPutOutUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
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
    val putOutBins: Set<Long> = emptySet(),
    val userMessage: String? = null
)

/**
 * ViewModel managing collection timetable state and user interactions on the main dashboard screen.
 *
 * Uses domain use cases [GetUpcomingCollectionsUseCase] and [ToggleBinPutOutUseCase] for clean UDF state updates.
 */
class DashboardViewModel(
    private val repository: BinRepository,
    private val getUpcomingCollectionsUseCase: GetUpcomingCollectionsUseCase = GetUpcomingCollectionsUseCase(repository),
    private val toggleBinPutOutUseCase: ToggleBinPutOutUseCase = ToggleBinPutOutUseCase()
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())

    /**
     * Observable flow of the current immutable dashboard UI state.
     */
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.ensureDefaultBinsInitialized()
            loadSchedule()
        }
    }

    private fun loadSchedule() {
        val today = LocalDate.now()
        val endDate = today.plusWeeks(8)

        viewModelScope.launch {
            getUpcomingCollectionsUseCase(today, endDate).collectLatest { events ->
                val validEvents = events.filter { !it.collectionDate.isBefore(today) }

                if (validEvents.isEmpty()) {
                    _uiState.value = DashboardUiState(isLoading = false)
                    return@collectLatest
                }

                val grouped = validEvents.groupBy { it.collectionDate }
                val earliestDate = grouped.keys.minOrNull()

                val nextEvents = if (earliestDate != null) grouped[earliestDate].orEmpty() else emptyList()
                val days = if (earliestDate != null) ChronoUnit.DAYS.between(today, earliestDate) else 0L

                val remainingGrouped = grouped.filterKeys { it != earliestDate }

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    nextCollectionDate = earliestDate,
                    nextCollectionEvents = nextEvents,
                    daysRemaining = days,
                    upcomingEventsGrouped = remainingGrouped
                )
            }
        }
    }

    /**
     * Toggles whether a specific bin has been put out on the kerb for collection using [ToggleBinPutOutUseCase].
     */
    fun markBinPutOut(binId: Long, binName: String) {
        val result = toggleBinPutOutUseCase(
            binId = binId,
            binName = binName,
            currentPutOutBins = _uiState.value.putOutBins
        )

        _uiState.value = _uiState.value.copy(
            putOutBins = result.updatedPutOutBins,
            userMessage = result.userMessage
        )
    }

    /**
     * Clears the current user message snackbar notification.
     */
    fun dismissUserMessage() {
        _uiState.value = _uiState.value.copy(userMessage = null)
    }
}
