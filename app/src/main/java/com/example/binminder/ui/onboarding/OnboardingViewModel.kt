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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalTime

data class OnboardingUiState(
    val currentStep: Int = 1,
    val postcodeOrCouncil: String = "",
    val isSearchingPostcode: Boolean = false,
    val detectedSchedule: CouncilScheduleResult? = null,
    val searchError: String? = null,
    val primaryCollectionDay: DayOfWeek = DayOfWeek.MONDAY,
    val binSetups: List<OnboardingBinSetup> = defaultBinSetups(),
    val reminderTime: LocalTime = LocalTime.of(19, 0),
    val reminderEveningBefore: Boolean = true,
    val reminderEnabled: Boolean = true,
    val isCompleting: Boolean = false,
    val isCompleted: Boolean = false,
    val userMessage: String? = null
)

fun defaultBinSetups(): List<OnboardingBinSetup> = listOf(
    OnboardingBinSetup(
        binType = "General Waste",
        displayName = "General Waste",
        presetColor = BinColor.BLACK,
        recurrence = RecurrenceType.FORTNIGHTLY,
        isEnabled = true,
        startNextWeek = false,
        customNote = "Black bin for non-recyclable household waste."
    ),
    OnboardingBinSetup(
        binType = "Dry Mixed Recycling",
        displayName = "Dry Mixed Recycling",
        presetColor = BinColor.BLUE,
        recurrence = RecurrenceType.FORTNIGHTLY,
        isEnabled = true,
        startNextWeek = true,
        customNote = "Blue bin for paper, cardboard, plastic bottles, and cans."
    ),
    OnboardingBinSetup(
        binType = "Garden Waste",
        displayName = "Garden Waste",
        presetColor = BinColor.GREEN,
        recurrence = RecurrenceType.FORTNIGHTLY,
        isEnabled = true,
        startNextWeek = false,
        customNote = "Green bin for grass cuttings and garden clippings."
    ),
    OnboardingBinSetup(
        binType = "Food Waste Caddy",
        displayName = "Food Waste Caddy",
        presetColor = BinColor.BROWN,
        recurrence = RecurrenceType.WEEKLY,
        isEnabled = true,
        startNextWeek = false,
        customNote = "Brown caddy for kitchen food leftovers."
    )
)

class OnboardingViewModel(
    private val repository: BinRepository,
    private val councilLookupRepository: CouncilLookupRepository = CouncilLookupRepositoryImpl()
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    fun setPostcodeOrCouncil(query: String) {
        _uiState.update { state ->
            val updatedSchedule = if (state.detectedSchedule != null &&
                query != state.detectedSchedule.councilName &&
                query != state.detectedSchedule.postcode
            ) null else state.detectedSchedule

            state.copy(
                postcodeOrCouncil = query,
                detectedSchedule = updatedSchedule,
                searchError = null
            )
        }
    }

    fun searchPostcodeTimetable() {
        val query = _uiState.value.postcodeOrCouncil.trim()
        if (query.isBlank()) return

        _uiState.update { it.copy(isSearchingPostcode = true, searchError = null) }

        viewModelScope.launch {
            val result = councilLookupRepository.lookupPostcode(query)
            if (result.isSuccess) {
                val schedule = result.getOrNull()!!
                _uiState.update { state ->
                    state.copy(
                        isSearchingPostcode = false,
                        detectedSchedule = schedule,
                        postcodeOrCouncil = schedule.councilName,
                        primaryCollectionDay = schedule.primaryCollectionDay,
                        binSetups = schedule.binSetups,
                        searchError = null
                    )
                }
            } else {
                _uiState.update { state ->
                    state.copy(
                        isSearchingPostcode = false,
                        detectedSchedule = null,
                        searchError = "Could not auto-detect timetable. Switching to guided setup."
                    )
                }
            }
        }
    }

    fun acceptAndApplyDetectedTimetable(onComplete: () -> Unit) {
        completeSetup(onComplete)
    }

    fun customiseDetectedTimetable() {
        nextStep()
    }

    fun continueToGuidedSetup() {
        _uiState.update { it.copy(searchError = null) }
        nextStep()
    }

    fun setPrimaryCollectionDay(day: DayOfWeek) {
        _uiState.update { state ->
            val updatedSchedule = state.detectedSchedule?.copy(primaryCollectionDay = day)
            state.copy(
                primaryCollectionDay = day,
                detectedSchedule = updatedSchedule
            )
        }
    }

    fun toggleBinEnabled(binType: String) {
        _uiState.update { state ->
            val updated = state.binSetups.map { setup ->
                if (setup.binType == binType) setup.copy(isEnabled = !setup.isEnabled) else setup
            }
            state.copy(binSetups = updated)
        }
    }

    fun setBinRecurrence(binType: String, recurrence: RecurrenceType) {
        _uiState.update { state ->
            val updated = state.binSetups.map { setup ->
                if (setup.binType == binType) setup.copy(recurrence = recurrence) else setup
            }
            state.copy(binSetups = updated)
        }
    }

    fun setBinStartNextWeek(binType: String, startNextWeek: Boolean) {
        _uiState.update { state ->
            val updated = state.binSetups.map { setup ->
                if (setup.binType == binType) setup.copy(startNextWeek = startNextWeek) else setup
            }
            state.copy(binSetups = updated)
        }
    }

    fun setBinColor(binType: String, color: BinColor) {
        _uiState.update { state ->
            val updated = state.binSetups.map { setup ->
                if (setup.binType == binType) setup.copy(presetColor = color) else setup
            }
            state.copy(binSetups = updated)
        }
    }

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

    fun setReminderSettings(time: LocalTime, eveningBefore: Boolean, enabled: Boolean) {
        _uiState.update { state ->
            state.copy(
                reminderTime = time,
                reminderEveningBefore = eveningBefore,
                reminderEnabled = enabled
            )
        }
    }

    fun nextStep() {
        _uiState.update { state ->
            if (state.currentStep < 4) state.copy(currentStep = state.currentStep + 1) else state
        }
    }

    fun previousStep() {
        _uiState.update { state ->
            if (state.currentStep > 1) state.copy(currentStep = state.currentStep - 1) else state
        }
    }

    fun goToStep(step: Int) {
        if (step in 1..4) {
            _uiState.update { it.copy(currentStep = step) }
        }
    }

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
                primaryDay = _uiState.value.primaryCollectionDay,
                binSetups = _uiState.value.binSetups,
                notificationSettings = settings
            )

            _uiState.update { it.copy(isCompleting = false, isCompleted = true) }
            onComplete()
        }
    }

    fun dismissUserMessage() {
        _uiState.update { it.copy(userMessage = null) }
    }
}
