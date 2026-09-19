package com.example.binminder.ui.bins

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.binminder.data.model.Bin
import com.example.binminder.data.repository.BinRepository
import com.example.binminder.domain.ResetTimetableUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * UI state holding the list of saved wheelie bins and deletion dialogue state.
 */
data class BinListUiState(
    val bins: List<Bin> = emptyList(),
    val isLoading: Boolean = true,
    val userMessage: String? = null,
    val binToDelete: Bin? = null
)

/**
 * ViewModel managing bin list display, enabling/disabling bins, and bin deletion.
 *
 * Uses domain use case [ResetTimetableUseCase] for resetting default bins if needed.
 */
class BinListViewModel(
    private val repository: BinRepository,
    private val resetTimetableUseCase: ResetTimetableUseCase = ResetTimetableUseCase(repository),
    started: SharingStarted = SharingStarted.WhileSubscribed(5000)
) : ViewModel() {

    private val _userMessage = MutableStateFlow<String?>(null)
    private val _binToDelete = MutableStateFlow<Bin?>(null)

    /**
     * Observable flow of the bin list UI state.
     */
    val uiState: StateFlow<BinListUiState> = combine(
        repository.allBins,
        _userMessage,
        _binToDelete
    ) { bins, userMessage, binToDelete ->
        BinListUiState(
            bins = bins,
            isLoading = false,
            userMessage = userMessage,
            binToDelete = binToDelete
        )
    }.stateIn(
        scope = viewModelScope,
        started = started,
        initialValue = BinListUiState(isLoading = true)
    )

    /**
     * Toggles whether a bin is enabled for collection schedule generation.
     */
    fun toggleBinEnabled(bin: Bin) {
        viewModelScope.launch {
            val updatedBin = bin.copy(isEnabled = !bin.isEnabled)
            repository.updateBin(updatedBin)
            val status = if (updatedBin.isEnabled) "enabled" else "disabled"
            _userMessage.value = "'${bin.name}' $status."
        }
    }

    /**
     * Opens the deletion confirmation prompt for the specified bin.
     */
    fun requestDeleteBin(bin: Bin) {
        _binToDelete.value = bin
    }

    /**
     * Cancels pending bin deletion and closes the confirmation dialogue.
     */
    fun cancelDeleteBin() {
        _binToDelete.value = null
    }

    /**
     * Confirms and deletes the requested bin from persistent storage.
     */
    fun confirmDeleteBin() {
        val bin = _binToDelete.value ?: return
        viewModelScope.launch {
            repository.deleteBin(bin)
            _binToDelete.value = null
            _userMessage.value = "Deleted '${bin.name}' wheelie bin."
        }
    }

    /**
     * Restores default UK council bin profiles.
     */
    fun resetDefaultBins() {
        viewModelScope.launch {
            repository.restoreStandardBins()
            _userMessage.value = "Restored standard UK wheelie bin profile."
        }
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

    /**
     * Clears all stored bins from the application.
     */
    fun removeAllBins() {
        viewModelScope.launch {
            repository.clearAllBins()
            _userMessage.value = "All wheelie bins removed."
        }
    }

    /**
     * Clears current user message notification string.
     */
    fun dismissUserMessage() {
        _userMessage.value = null
    }
}
