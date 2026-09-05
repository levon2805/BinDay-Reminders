package com.example.binminder.ui.bins

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.binminder.data.model.Bin
import com.example.binminder.data.repository.BinRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

data class BinListUiState(
    val bins: List<Bin> = emptyList(),
    val isLoading: Boolean = true,
    val userMessage: String? = null,
    val binToDelete: Bin? = null
)

class BinListViewModel(
    private val repository: BinRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(BinListUiState())
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

    fun requestDeleteBin(bin: Bin) {
        _uiState.value = _uiState.value.copy(binToDelete = bin)
    }

    fun cancelDeleteBin() {
        _uiState.value = _uiState.value.copy(binToDelete = null)
    }

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

    fun resetDefaultBins() {
        viewModelScope.launch {
            repository.ensureDefaultBinsInitialized()
            _uiState.value = _uiState.value.copy(
                userMessage = "Reset default UK council wheelie bins."
            )
        }
    }

    fun dismissUserMessage() {
        _uiState.value = _uiState.value.copy(userMessage = null)
    }
}
