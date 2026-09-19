package com.example.binminder.ui.addedit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.binminder.data.model.Bin
import com.example.binminder.data.model.BinColor
import com.example.binminder.data.model.RecurrenceType
import com.example.binminder.data.repository.BinRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

import com.example.binminder.ui.theme.findMatchingBinColor
import com.example.binminder.ui.theme.getDefaultNotes
import com.example.binminder.ui.theme.isDefaultNote

/**
 * UI state holding form input field values and validation status for adding or editing a bin.
 */
data class AddEditBinUiState(
    val binId: Long? = null,
    val name: String = "",
    val presetColor: BinColor = BinColor.BLACK,
    val colorHex: String = BinColor.BLACK.defaultHex,
    val isCustomColor: Boolean = false,
    val lidPresetColor: BinColor? = null,
    val lidColorHex: String? = null,
    val isCustomLidColor: Boolean = false,
    val recurrence: RecurrenceType = RecurrenceType.FORTNIGHTLY,
    val startDate: LocalDate = LocalDate.now(),
    val adjustForBankHolidays: Boolean = true,
    val customNote: String = "",
    val isEnabled: Boolean = true,
    val isLoading: Boolean = false,
    val isSaved: Boolean = false,
    val errorMessage: String? = null
)

/**
 * ViewModel managing form input fields, validation, and database updates for wheelie bins.
 */
class AddEditBinViewModel(
    private val repository: BinRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddEditBinUiState())

    /**
     * Observable state flow for the bin form UI.
     */
    val uiState: StateFlow<AddEditBinUiState> = _uiState.asStateFlow()

    /**
     * Loads existing bin properties for editing or resets fields for creating a new bin.
     */
    fun loadBin(id: Long?) {
        if (id == null || id == 0L) {
            _uiState.value = AddEditBinUiState(
                customNote = getDefaultNotes("", BinColor.BLACK, null)
            )
            return
        }

        _uiState.value = _uiState.value.copy(isLoading = true, binId = id)

        viewModelScope.launch {
            val bin = repository.getBinSync(id)
            if (bin != null) {
                _uiState.value = AddEditBinUiState(
                    binId = bin.id,
                    name = bin.name,
                    presetColor = bin.presetColor,
                    colorHex = bin.colorHex,
                    isCustomColor = bin.presetColor == BinColor.CUSTOM,
                    lidPresetColor = bin.lidPresetColor,
                    lidColorHex = bin.lidColorHex,
                    isCustomLidColor = bin.lidPresetColor == BinColor.CUSTOM,
                    recurrence = bin.recurrence,
                    startDate = bin.startDate,
                    adjustForBankHolidays = bin.adjustForBankHolidays,
                    customNote = bin.customNote,
                    isEnabled = bin.isEnabled,
                    isLoading = false
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Wheelie bin profile not found."
                )
            }
        }
    }

    /**
     * Updates the name input field in state, syncing default notes if unmodified.
     */
    fun onNameChange(name: String) {
        val currentNote = _uiState.value.customNote
        val currentName = _uiState.value.name
        val bodyColor = _uiState.value.presetColor
        val lidColor = _uiState.value.lidPresetColor
        val newNote = if (isDefaultNote(currentNote, currentName)) {
            getDefaultNotes(name, bodyColor, lidColor)
        } else {
            currentNote
        }
        _uiState.value = _uiState.value.copy(
            name = name,
            customNote = newNote,
            errorMessage = null
        )
    }

    /**
     * Updates the selected preset bin body colour, syncing default notes if unmodified.
     */
    fun onPresetColorSelected(preset: BinColor) {
        val currentNote = _uiState.value.customNote
        val currentName = _uiState.value.name
        val lidColor = _uiState.value.lidPresetColor
        val hex = if (preset == BinColor.CUSTOM) {
            _uiState.value.colorHex
        } else {
            preset.defaultHex
        }
        val newNote = if (isDefaultNote(currentNote, currentName)) {
            getDefaultNotes(currentName, preset, lidColor)
        } else {
            currentNote
        }
        _uiState.value = _uiState.value.copy(
            presetColor = preset,
            colorHex = hex,
            isCustomColor = preset == BinColor.CUSTOM,
            customNote = newNote
        )
    }

    /**
     * Updates custom hex body colour input in state, syncing default notes if unmodified.
     */
    fun onCustomHexChange(hex: String) {
        val currentNote = _uiState.value.customNote
        val currentName = _uiState.value.name
        val lidColor = _uiState.value.lidPresetColor
        val preset = findMatchingBinColor(hex)
        val newNote = if (isDefaultNote(currentNote, currentName)) {
            getDefaultNotes(currentName, preset, lidColor)
        } else {
            currentNote
        }
        _uiState.value = _uiState.value.copy(
            colorHex = hex,
            presetColor = preset,
            isCustomColor = preset == BinColor.CUSTOM,
            customNote = newNote
        )
    }

    /**
     * Updates the selected preset lid colour, syncing default notes if unmodified.
     */
    fun onLidPresetColorSelected(preset: BinColor?) {
        val currentNote = _uiState.value.customNote
        val currentName = _uiState.value.name
        val bodyColor = _uiState.value.presetColor
        val hex = if (preset == null) {
            null
        } else if (preset == BinColor.CUSTOM) {
            _uiState.value.lidColorHex ?: BinColor.CUSTOM.defaultHex
        } else {
            preset.defaultHex
        }
        val newNote = if (isDefaultNote(currentNote, currentName)) {
            getDefaultNotes(currentName, bodyColor, preset)
        } else {
            currentNote
        }
        _uiState.value = _uiState.value.copy(
            lidPresetColor = preset,
            lidColorHex = hex,
            isCustomLidColor = preset == BinColor.CUSTOM,
            customNote = newNote
        )
    }

    /**
     * Updates custom hex lid colour input in state, syncing default notes if unmodified.
     */
    fun onCustomLidHexChange(hex: String) {
        val currentNote = _uiState.value.customNote
        val currentName = _uiState.value.name
        val bodyColor = _uiState.value.presetColor
        val preset = findMatchingBinColor(hex)
        val newNote = if (isDefaultNote(currentNote, currentName)) {
            getDefaultNotes(currentName, bodyColor, preset)
        } else {
            currentNote
        }
        _uiState.value = _uiState.value.copy(
            lidColorHex = hex,
            lidPresetColor = preset,
            isCustomLidColor = preset == BinColor.CUSTOM,
            customNote = newNote
        )
    }

    /**
     * Updates the selected collection recurrence frequency.
     */
    fun onRecurrenceSelected(type: RecurrenceType) {
        _uiState.value = _uiState.value.copy(recurrence = type)
    }

    /**
     * Updates the starting collection date in state.
     */
    fun onStartDateSelected(date: LocalDate) {
        _uiState.value = _uiState.value.copy(startDate = date)
    }

    /**
     * Updates the bank holiday adjustment preference toggle.
     */
    fun onAdjustForBankHolidaysChange(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(adjustForBankHolidays = enabled)
    }

    /**
     * Updates kerbside notes or instructions in state.
     */
    fun onCustomNoteChange(note: String) {
        _uiState.value = _uiState.value.copy(customNote = note)
    }

    /**
     * Validates and persists bin details to local storage.
     */
    fun saveBin() {
        val state = _uiState.value
        if (state.name.isBlank()) {
            _uiState.value = state.copy(errorMessage = "Bin name cannot be empty.")
            return
        }

        _uiState.value = state.copy(isLoading = true)

        viewModelScope.launch {
            val binToSave = Bin(
                id = state.binId ?: 0L,
                name = state.name.trim(),
                colorHex = state.colorHex,
                presetColor = state.presetColor,
                lidColorHex = state.lidColorHex,
                lidPresetColor = state.lidPresetColor,
                recurrence = state.recurrence,
                repeatIntervalWeeks = state.recurrence.intervalWeeks,
                startDate = state.startDate,
                customNote = state.customNote.trim(),
                isEnabled = state.isEnabled,
                adjustForBankHolidays = state.adjustForBankHolidays
            )

            if (state.binId == null || state.binId == 0L) {
                repository.insertBin(binToSave)
            } else {
                repository.updateBin(binToSave)
            }

            _uiState.value = _uiState.value.copy(
                isLoading = false,
                isSaved = true
            )
        }
    }

    /**
     * Resets the save completion flag after navigation has been triggered.
     */
    fun resetSaveState() {
        _uiState.value = _uiState.value.copy(isSaved = false)
    }

    /**
     * Clears current error message notification string.
     */
    fun clearErrorMessage() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}
