package com.example.binminder.ui.bins

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.binminder.data.model.Bin
import com.example.binminder.data.repository.BinRepository
import com.example.binminder.domain.ResetTimetableUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
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
    private val resetTimetableUseCase: ResetTimetableUseCase = ResetTimetableUseCase(repository)
) : ViewModel() {

    private val _uiState = MutableStateFlow(BinListUiState())

    /**
     * Observable flow of the bin list UI state.
     */
    val uiState: StateFlow<BinListUiState> = _uiState.asStateFlow()

    init {
        loadBins()
    }

    private fun loadBins() {
        viewModelScope.launch {
            repository.allBins.collectLatest { bins ->
                _uiState.value = _uiState.value.copy(
                    bins = bins,
                    isLoading = false
                )
            }
        }
    }

    /**
     * Toggles whether a bin is enabled for collection schedule generation.
     */
    fun toggleBinEnabled(bin: Bin) {
        viewModelScope.launch {
            val updatedBin = bin.copy(isEnabled = !bin.isEnabled)
            repository.updateBin(updatedBin)
            val status = if (updatedBin.isEnabled) "enabled" else "disabled"
            _uiState.value = _uiState.value.copy(
                userMessage = "'${bin.name}' $status."
            )
        }
    }

    /**
     * Opens the deletion confirmation prompt for the specified bin.
     */
    fun requestDeleteBin(bin: Bin) {
        _uiState.value = _uiState.value.copy(binToDelete = bin)
    }

    /**
     * Cancels pending bin deletion and closes the confirmation dialogue.
     */
    fun cancelDeleteBin() {
        _uiState.value = _uiState.value.copy(binToDelete = null)
    }

    /**
     * Confirms and deletes the requested bin from persistent storage.
     */
    fun confirmDeleteBin() {
        val bin = _uiState.value.binToDelete ?: return
        viewModelScope.launch {
            repository.deleteBin(bin)
            _uiState.value = _uiState.value.copy(
                binToDelete = null,
                userMessage = "Deleted '${bin.name}' wheelie bin."
            )
        }
    }

    /**
     * Restores default UK council bin profiles if needed.
     */
    fun resetDefaultBins() {
        viewModelScope.launch {
            repository.ensureDefaultBinsInitialized()
            _uiState.value = _uiState.value.copy(
                userMessage = "Reset default UK council wheelie bins."
            )
        }
    }

    /**
     * Clears current user message notification string.
     */
    fun dismissUserMessage() {
        _uiState.value = _uiState.value.copy(userMessage = null)
    }
}
