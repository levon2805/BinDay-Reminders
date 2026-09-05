package com.example.binminder.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.binminder.data.model.AppThemeMode
import com.example.binminder.data.model.Bin
import com.example.binminder.data.model.NotificationSettings
import com.example.binminder.data.repository.BinRepository
import com.example.binminder.domain.ResetTimetableUseCase
import com.example.binminder.engine.BankHolidayCalculator
import com.example.binminder.engine.BankHolidayShiftPreview
import com.example.binminder.worker.NotificationScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.util.Locale

/**
 * UI state holding notification settings, theme preferences, and bank holiday previews.
 */
data class SettingsUiState(
    val notificationSettings: NotificationSettings = NotificationSettings(),
    val themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    val bankHolidayPreviews: List<BankHolidayShiftPreview> = emptyList(),
    val isLoading: Boolean = true,
    val userMessage: String? = null
)

/**
 * ViewModel managing application preferences, notification schedules, theme choices, and bank holiday shift previews.
 *
 * Uses domain use case [ResetTimetableUseCase] for resetting timetable configurations.
 */
class SettingsViewModel(
    private val repository: BinRepository,
    private val resetTimetableUseCase: ResetTimetableUseCase = ResetTimetableUseCase(repository)
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())

    /**
     * Observable flow of the settings UI state.
     */
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.notificationSettings.collectLatest { settings ->
                _uiState.value = _uiState.value.copy(
                    notificationSettings = settings,
                    themeMode = settings.themeMode,
                    isLoading = false
                )
            }
        }

        viewModelScope.launch {
            repository.themeMode.collectLatest { mode ->
                _uiState.value = _uiState.value.copy(
                    themeMode = mode
                )
            }
        }

        viewModelScope.launch {
            repository.allBins.collectLatest { bins ->
                val previews = generateBankHolidayShiftPreviews(bins)
                _uiState.value = _uiState.value.copy(
                    bankHolidayPreviews = previews
                )
            }
        }
    }

    /**
     * Updates the active theme mode preference.
     */
    fun setThemeMode(themeMode: AppThemeMode) {
        viewModelScope.launch {
            repository.setThemeMode(themeMode)
            _uiState.value = _uiState.value.copy(
                userMessage = "Theme updated to ${themeMode.label}."
            )
        }
    }

    /**
     * Enables or disables collection reminder notifications.
     */
    fun toggleReminders(enabled: Boolean, context: Context) {
        viewModelScope.launch {
            val updated = _uiState.value.notificationSettings.copy(reminderEnabled = enabled)
            repository.updateNotificationSettings(updated)
            val status = if (enabled) "enabled" else "disabled"
            _uiState.value = _uiState.value.copy(userMessage = "Collection reminders $status.")
        }
    }

    /**
     * Updates notification time and whether alerts fire the evening before collection.
     */
    fun updateReminderSchedule(time: LocalTime, eveningBefore: Boolean) {
        viewModelScope.launch {
            val updated = _uiState.value.notificationSettings.copy(
                reminderTime = time,
                reminderEveningBefore = eveningBefore
            )
            repository.updateNotificationSettings(updated)
            val formattedTime = time.format(DateTimeFormatter.ofPattern("HH:mm"))
            val timingText = if (eveningBefore) "Evening before" else "Morning of collection"
            _uiState.value = _uiState.value.copy(
                userMessage = "Reminder schedule updated to $formattedTime ($timingText)."
            )
        }
    }

    /**
     * Sends an immediate test notification to verify notification setup.
     */
    fun sendTestNotification(context: Context) {
        NotificationScheduler.sendImmediateTestNotification(context)
        _uiState.value = _uiState.value.copy(
            userMessage = "Test notification sent! Check your notification panel."
        )
    }

    /**
     * Restores default UK council bin profiles.
     */
    fun resetDefaultBins() {
        viewModelScope.launch {
            repository.ensureDefaultBinsInitialized()
            _uiState.value = _uiState.value.copy(
                userMessage = "Restored default UK wheelie bin profiles."
            )
        }
    }

    /**
     * Clears all saved bins and resets onboarding state to allow entering a new address via [ResetTimetableUseCase].
     */
    fun resetTimetableAndAddress(context: Context, onComplete: () -> Unit) {
        viewModelScope.launch {
            resetTimetableUseCase(context)
            _uiState.value = _uiState.value.copy(
                userMessage = "Timetable and address reset."
            )
            onComplete()
        }
    }

    /**
     * Resets onboarding state and clears bins for setup wizard re-run via [ResetTimetableUseCase].
     */
    fun resetOnboarding(onComplete: () -> Unit) {
        viewModelScope.launch {
            resetTimetableUseCase()
            _uiState.value = _uiState.value.copy(
                userMessage = "Resetting setup wizard..."
            )
            onComplete()
        }
    }

    /**
     * Handles notification permission denial feedback.
     */
    fun onNotificationPermissionDenied() {
        _uiState.value = _uiState.value.copy(
            userMessage = "Notification permission is required to receive bin reminders."
        )
    }

    /**
     * Clears current user message notification string.
     */
    fun dismissUserMessage() {
        _uiState.value = _uiState.value.copy(userMessage = null)
    }

    private fun generateBankHolidayShiftPreviews(bins: List<Bin>): List<BankHolidayShiftPreview> {
        val upcomingHolidays = BankHolidayCalculator.getNextUpcomingBankHolidays(LocalDate.now(), limit = 6)
        val activeBins = bins.filter { it.isEnabled && it.adjustForBankHolidays }
        val previews = mutableListOf<BankHolidayShiftPreview>()

        for (holiday in upcomingHolidays) {
            val holidayDate = holiday.date
            val weekStart = holidayDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

            if (activeBins.isNotEmpty()) {
                val seenBins = mutableSetOf<String>()
                for (bin in activeBins) {
                    val binNormalDayOfWeek = bin.startDate.dayOfWeek
                    val originalDate = weekStart.plusDays((binNormalDayOfWeek.value - 1).toLong())
                    val (shiftedDate, isAdjusted) = BankHolidayCalculator.adjustForBankHoliday(originalDate)

                    if (isAdjusted && !seenBins.contains(bin.name)) {
                        seenBins.add(bin.name)
                        previews.add(
                            BankHolidayShiftPreview(
                                holidayName = holiday.name,
                                holidayDate = holidayDate,
                                binName = bin.name,
                                originalDayName = originalDate.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.UK),
                                shiftedDayName = shiftedDate.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.UK),
                                originalDate = originalDate,
                                shiftedDate = shiftedDate
                            )
                        )
                    }
                }
            } else {
                val originalDate = weekStart
                val (shiftedDate, _) = BankHolidayCalculator.adjustForBankHoliday(originalDate)
                previews.add(
                    BankHolidayShiftPreview(
                        holidayName = holiday.name,
                        holidayDate = holidayDate,
                        binName = "Collection Day",
                        originalDayName = originalDate.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.UK),
                        shiftedDayName = shiftedDate.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.UK),
                        originalDate = originalDate,
                        shiftedDate = shiftedDate
                    )
                )
            }
        }
        return previews
    }
}
