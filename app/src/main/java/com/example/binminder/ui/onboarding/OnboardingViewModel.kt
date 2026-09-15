package com.example.binminder.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.binminder.data.model.BinColor
import com.example.binminder.data.model.CouncilScheduleResult
import com.example.binminder.data.model.NotificationSettings
import com.example.binminder.data.model.OnboardingBinSetup
import com.example.binminder.data.model.RecurrenceType
import com.example.binminder.data.repository.BinRepository
import com.example.binminder.data.repository.CouncilLookupRepository
import com.example.binminder.data.repository.CouncilLookupRepositoryImpl
import com.example.binminder.domain.LookupCouncilScheduleUseCase
import com.example.binminder.ui.theme.getDefaultNotes
import com.example.binminder.ui.theme.isDefaultNote
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalTime

/**
 * UI state holding setup wizard progress, identified council info, and bin configuration parameters.
 */
data class OnboardingUiState(
    val currentStep: Int = 1,
    val postcodeOrCouncil: String = "",
    val isSearchingPostcode: Boolean = false,
    val detectedCouncil: CouncilScheduleResult? = null,
    val searchError: String? = null,
    val primaryCollectionDay: DayOfWeek? = null,
    val binSetups: List<OnboardingBinSetup> = defaultBinSetups(),
    val reminderTime: LocalTime = LocalTime.of(19, 0),
    val reminderEveningBefore: Boolean = true,
    val reminderEnabled: Boolean = true,
    val isCompleting: Boolean = false,
    val isCompleted: Boolean = false,
    val userMessage: String? = null
)

/**
 * Returns standard default bin configurations for initial setup.
 */
fun defaultBinSetups(): List<OnboardingBinSetup> = listOf(
    OnboardingBinSetup(
        binType = "General Waste",
        displayName = "General Waste",
        presetColor = BinColor.BLACK,
        lidPresetColor = null,
        recurrence = RecurrenceType.FORTNIGHTLY,
        isEnabled = true,
        startNextWeek = false,
        customNote = getDefaultNotes("General Waste", BinColor.BLACK, null)
    ),
    OnboardingBinSetup(
        binType = "Dry Mixed Recycling",
        displayName = "Dry Mixed Recycling",
        presetColor = BinColor.BLUE,
        lidPresetColor = null,
        recurrence = RecurrenceType.FORTNIGHTLY,
        isEnabled = true,
        startNextWeek = true,
        customNote = getDefaultNotes("Dry Mixed Recycling", BinColor.BLUE, null)
    ),
    OnboardingBinSetup(
        binType = "Garden Waste",
        displayName = "Garden Waste",
        presetColor = BinColor.GREEN,
        lidPresetColor = null,
        recurrence = RecurrenceType.FORTNIGHTLY,
        isEnabled = true,
        startNextWeek = false,
        customNote = getDefaultNotes("Garden Waste", BinColor.GREEN, null)
    ),
    OnboardingBinSetup(
        binType = "Food Waste Caddy",
        displayName = "Food Waste Caddy",
        presetColor = BinColor.BROWN,
        lidPresetColor = null,
        recurrence = RecurrenceType.WEEKLY,
        isEnabled = true,
        startNextWeek = false,
        customNote = getDefaultNotes("Food Waste Caddy", BinColor.BROWN, null)
    )
)

/**
 * ViewModel managing postcode council identification and guided manual setup wizard steps.
 *
 * Uses domain use case [LookupCouncilScheduleUseCase] to identify the user's local council
 * and provide a link to their council's website. Bin configuration is always done manually
 * by the user through the guided setup steps.
 */
class OnboardingViewModel(
    private val repository: BinRepository,
    private val councilLookupRepository: CouncilLookupRepository = CouncilLookupRepositoryImpl(),
    private val lookupCouncilScheduleUseCase: LookupCouncilScheduleUseCase = LookupCouncilScheduleUseCase(councilLookupRepository)
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())

    /**
     * Observable state flow for onboarding wizard UI.
     */
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    /**
     * Resets the onboarding wizard state back to Step 1 (Postcode Search).
     */
    fun resetState() {
        _uiState.value = OnboardingUiState()
    }

    /**
     * Updates the postcode or council search input string.
     */
    fun setPostcodeOrCouncil(query: String) {
        _uiState.update { state ->
            val updatedCouncil = if (state.detectedCouncil != null &&
                query != state.detectedCouncil.councilName &&
                query != state.detectedCouncil.postcode
            ) null else state.detectedCouncil

            state.copy(
                postcodeOrCouncil = query,
                detectedCouncil = updatedCouncil,
                searchError = null
            )
        }
    }

    /**
     * Performs a postcode lookup to identify the local council and generate a website search URL.
     * Does not populate bin setups or collection day — those are set manually by the user.
     */
    fun searchPostcodeTimetable() {
        val query = _uiState.value.postcodeOrCouncil.trim()
        if (query.isBlank()) return

        _uiState.update { it.copy(isSearchingPostcode = true, searchError = null) }

        viewModelScope.launch {
            val result = lookupCouncilScheduleUseCase(query)
            if (result.isSuccess) {
                val council = result.getOrNull()!!
                _uiState.update { state ->
                    state.copy(
                        isSearchingPostcode = false,
                        detectedCouncil = council,
                        postcodeOrCouncil = council.councilName,
                        searchError = null
                    )
                }
            } else {
                _uiState.update { state ->
                    state.copy(
                        isSearchingPostcode = false,
                        detectedCouncil = null,
                        searchError = "Could not identify your council. You can still set up your bins manually."
                    )
                }
            }
        }
    }

    /**
     * Returns the council website search URL if a council has been identified, or null.
     */
    fun getCouncilWebSearchUrl(): String? {
        return _uiState.value.detectedCouncil?.councilWebSearchUrl
    }

    /**
     * Sets the primary weekly collection day.
     */
    fun setPrimaryCollectionDay(day: DayOfWeek) {
        _uiState.update { state ->
            state.copy(primaryCollectionDay = day)
        }
    }

    /**
     * Toggles whether a specific bin type is enabled during setup.
     */
    fun toggleBinEnabled(binType: String) {
        _uiState.update { state ->
            val updated = state.binSetups.map { setup ->
                if (setup.binType == binType) setup.copy(isEnabled = !setup.isEnabled) else setup
            }
            state.copy(binSetups = updated)
        }
    }

    /**
     * Updates recurrence frequency for a specific bin setup.
     */
    fun setBinRecurrence(binType: String, recurrence: RecurrenceType) {
        _uiState.update { state ->
            val updated = state.binSetups.map { setup ->
                if (setup.binType == binType) setup.copy(recurrence = recurrence) else setup
            }
            state.copy(binSetups = updated)
        }
    }

    /**
     * Configures whether a fortnightly bin collection starts on the upcoming week or next week.
     */
    fun setBinStartNextWeek(binType: String, startNextWeek: Boolean) {
        _uiState.update { state ->
            val updated = state.binSetups.map { setup ->
                if (setup.binType == binType) setup.copy(startNextWeek = startNextWeek) else setup
            }
            state.copy(binSetups = updated)
        }
    }

    /**
     * Updates the chosen body colour preset for a specific bin setup, syncing default notes if unmodified.
     */
    fun setBinColor(binType: String, color: BinColor) {
        _uiState.update { state ->
            val updated = state.binSetups.map { setup ->
                if (setup.binType == binType) {
                    val newNote = if (isDefaultNote(setup.customNote, setup.displayName)) {
                        getDefaultNotes(setup.displayName, color, setup.lidPresetColor)
                    } else {
                        setup.customNote
                    }
                    setup.copy(presetColor = color, customNote = newNote)
                } else setup
            }
            state.copy(binSetups = updated)
        }
    }

    /**
     * Updates the chosen lid colour preset for a specific bin setup, syncing default notes if unmodified.
     */
    fun setBinLidColor(binType: String, lidColor: BinColor?) {
        _uiState.update { state ->
            val updated = state.binSetups.map { setup ->
                if (setup.binType == binType) {
                    val newNote = if (isDefaultNote(setup.customNote, setup.displayName)) {
                        getDefaultNotes(setup.displayName, setup.presetColor, lidColor)
                    } else {
                        setup.customNote
                    }
                    setup.copy(lidPresetColor = lidColor, customNote = newNote)
                } else setup
            }
            state.copy(binSetups = updated)
        }
    }

    /**
     * Updates the collection day for a specific bin setup.
     */
    fun setBinCollectionDay(binType: String, day: DayOfWeek) {
        _uiState.update { state ->
            val updated = state.binSetups.map { setup ->
                if (setup.binType == binType) setup.copy(collectionDay = day) else setup
            }
            state.copy(binSetups = updated)
        }
    }

    /**
     * Renames a bin's display name, syncing default notes if unmodified.
     */
    fun renameBin(binType: String, newName: String) {
        _uiState.update { state ->
            val updated = state.binSetups.map { setup ->
                if (setup.binType == binType) {
                    val newNote = if (isDefaultNote(setup.customNote, setup.displayName)) {
                        getDefaultNotes(newName, setup.presetColor, setup.lidPresetColor)
                    } else {
                        setup.customNote
                    }
                    setup.copy(displayName = newName, customNote = newNote)
                } else setup
            }
            state.copy(binSetups = updated)
        }
    }

    /**
     * Adds a new custom bin setup to the list.
     */
    fun addCustomBin() {
        _uiState.update { state ->
            val customBinCount = state.binSetups.count { it.binType.startsWith("Custom Bin") }
            val newBinName = "Custom Bin ${customBinCount + 1}"
            val newBin = OnboardingBinSetup(
                binType = newBinName,
                displayName = newBinName,
                presetColor = BinColor.BLACK,
                lidPresetColor = null,
                recurrence = RecurrenceType.FORTNIGHTLY,
                isEnabled = true,
                startNextWeek = false,
                customNote = getDefaultNotes(newBinName, BinColor.BLACK, null)
            )
            state.copy(binSetups = state.binSetups + newBin)
        }
    }

    /**
     * Specifies which fortnightly bin is due on the immediate upcoming collection day.
     */
    fun setFortnightlyThisWeekBin(selectedBinType: String) {
        _uiState.update { state ->
            val targetBin = state.binSetups.find { it.binType == selectedBinType }
            if (targetBin != null && targetBin.recurrence == RecurrenceType.FORTNIGHTLY) {
                if (targetBin.startNextWeek) {
                    val updated = state.binSetups.map { setup ->
                        if (setup.recurrence == RecurrenceType.FORTNIGHTLY) {
                            setup.copy(startNextWeek = !setup.startNextWeek)
                        } else {
                            setup
                        }
                    }
                    state.copy(binSetups = updated)
                } else {
                    state
                }
            } else {
                state
            }
        }
    }

    /**
     * Updates reminder notification timing and enabled state during setup.
     */
    fun setReminderSettings(time: LocalTime, eveningBefore: Boolean, enabled: Boolean) {
        _uiState.update { state ->
            state.copy(
                reminderTime = time,
                reminderEveningBefore = eveningBefore,
                reminderEnabled = enabled
            )
        }
    }

    /**
     * Advances to the next step in the setup wizard.
     */
    fun nextStep() {
        _uiState.update { state ->
            if (state.currentStep < 4) state.copy(currentStep = state.currentStep + 1) else state
        }
    }

    /**
     * Returns to the previous step in the setup wizard.
     */
    fun previousStep() {
        _uiState.update { state ->
            if (state.currentStep > 1) state.copy(currentStep = state.currentStep - 1) else state
        }
    }

    /**
     * Navigates directly to a specific step index in the setup wizard.
     */
    fun goToStep(step: Int) {
        if (step in 1..4) {
            _uiState.update { it.copy(currentStep = step) }
        }
    }

    /**
     * Returns whether the user can advance past the current step.
     * Step 2 requires a collection day to be selected.
     */
    fun canAdvanceFromCurrentStep(): Boolean {
        val state = _uiState.value
        return when (state.currentStep) {
            2 -> state.primaryCollectionDay != null
            else -> true
        }
    }

    /**
     * Finalises onboarding choices, populates initial database bins, and schedules notifications.
     */
    fun completeSetup(onComplete: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isCompleting = true) }

            val settings = NotificationSettings(
                reminderEnabled = _uiState.value.reminderEnabled,
                reminderTime = _uiState.value.reminderTime,
                reminderEveningBefore = _uiState.value.reminderEveningBefore
            )

            repository.completeOnboardingSetup(
                postcodeOrCouncil = _uiState.value.postcodeOrCouncil,
                primaryDay = _uiState.value.primaryCollectionDay ?: DayOfWeek.MONDAY,
                binSetups = _uiState.value.binSetups,
                notificationSettings = settings
            )

            _uiState.update { it.copy(isCompleting = false, isCompleted = true) }
            onComplete()
        }
    }

    /**
     * Clears current user message notification string.
     */
    fun dismissUserMessage() {
        _uiState.update { it.copy(userMessage = null) }
    }
}
