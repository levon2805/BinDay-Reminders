package com.example.binminder.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.binminder.data.model.CollectionEvent
import com.example.binminder.data.repository.BinRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.ChronoUnit

data class DashboardUiState(
    val isLoading: Boolean = true,
    val nextCollectionDate: LocalDate? = null,
    val nextCollectionEvents: List<CollectionEvent> = emptyList(),
    val daysRemaining: Long = 0,
    val upcomingEventsGrouped: Map<LocalDate, List<CollectionEvent>> = emptyMap(),
    val putOutBins: Set<Long> = emptySet(),
    val userMessage: String? = null
)

class DashboardViewModel(
    private val repository: BinRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
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
            repository.getUpcomingCollectionEvents(today, endDate).collectLatest { events ->
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

    fun markBinPutOut(binId: Long, binName: String) {
        val currentPutOut = _uiState.value.putOutBins.toMutableSet()
        val isNowPutOut = if (currentPutOut.contains(binId)) {
            currentPutOut.remove(binId)
            false
        } else {
            currentPutOut.add(binId)
            true
        }

        val message = if (isNowPutOut) {
            "Marked '$binName' bin as put out for collection."
        } else {
            "Unmarked '$binName' bin."
        }

        _uiState.value = _uiState.value.copy(
            putOutBins = currentPutOut,
            userMessage = message
        )
    }

    fun dismissUserMessage() {
        _uiState.value = _uiState.value.copy(userMessage = null)
    }
}
