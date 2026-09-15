package com.example.binminder.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.binminder.data.model.AppThemeMode
import com.example.binminder.data.model.Bin
import com.example.binminder.data.model.CollectionEvent
import com.example.binminder.data.model.NotificationSettings
import com.example.binminder.data.repository.BinRepository
import com.example.binminder.domain.ResetTimetableUseCase
import com.example.binminder.engine.BankHolidayCalculator
import com.example.binminder.engine.BankHolidayShiftPreview
import com.example.binminder.engine.ScheduleEngine
import com.example.binminder.worker.NotificationScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
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
    val nextCollectionDate: LocalDate? = null,
    val nextCollectionEvents: List<CollectionEvent> = emptyList(),
    val allBins: List<Bin> = emptyList(),
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
    private val resetTimetableUseCase: ResetTimetableUseCase = ResetTimetableUseCase(repository),
    started: SharingStarted = SharingStarted.WhileSubscribed(5000)
) : ViewModel() {

    private val _userMessage = MutableStateFlow<String?>(null)

    /**
     * Observable flow of the settings UI state.
     */
    val uiState: StateFlow<SettingsUiState> = combine(
        repository.notificationSettings,
        repository.themeMode,
        repository.allBins,
        _userMessage
    ) { settings, themeMode, bins, userMessage ->
        val previews = generateBankHolidayShiftPreviews(bins)
        val activeBins = bins.filter { it.isEnabled }
        val allEvents = ScheduleEngine.generateCollectionEvents(
            bins = activeBins,
            startDate = LocalDate.now(),
            endDate = LocalDate.now().plusWeeks(6)
        )
        val nextDate = allEvents.minOfOrNull { it.collectionDate }
        val nextEvents = if (nextDate != null) {
            allEvents.filter { it.collectionDate == nextDate }
        } else emptyList()

        SettingsUiState(
            notificationSettings = settings,
            themeMode = themeMode,
            bankHolidayPreviews = previews,
            nextCollectionDate = nextDate,
            nextCollectionEvents = nextEvents,
            allBins = bins,
            isLoading = false,
            userMessage = userMessage
        )
    }.stateIn(
        scope = viewModelScope,
        started = started,
        initialValue = SettingsUiState(isLoading = true)
    )

    /**
     * Updates the active theme mode preference.
     */
    fun setThemeMode(themeMode: AppThemeMode) {
        viewModelScope.launch {
            repository.setThemeMode(themeMode)
            _userMessage.value = "Theme updated to ${themeMode.label}."
        }
    }

    /**
     * Enables or disables collection reminder notifications.
     */
    fun toggleReminders(enabled: Boolean, context: Context) {
        viewModelScope.launch {
            val currentSettings = repository.notificationSettings.first()
            val updated = currentSettings.copy(reminderEnabled = enabled)
            repository.updateNotificationSettings(updated)
            val status = if (enabled) "enabled" else "disabled"
            _userMessage.value = "Collection reminders $status."
        }
    }

    /**
     * Updates evening reminder time (or null if disabled / NONE).
     */
    fun updateEveningReminderTime(time: LocalTime?) {
        viewModelScope.launch {
            val currentSettings = repository.notificationSettings.first()
            val updated = currentSettings.copy(eveningReminderTime = time)
            repository.updateNotificationSettings(updated)
            val message = if (time != null) {
                "Evening reminder set to ${time.format(DateTimeFormatter.ofPattern("HH:mm"))}."
            } else {
                "Evening reminder disabled."
            }
            _userMessage.value = message
        }
    }

    /**
     * Updates morning reminder time (or null if disabled / NONE).
     */
    fun updateMorningReminderTime(time: LocalTime?) {
        viewModelScope.launch {
            val currentSettings = repository.notificationSettings.first()
            val updated = currentSettings.copy(morningReminderTime = time)
            repository.updateNotificationSettings(updated)
            val message = if (time != null) {
                "Morning reminder set to ${time.format(DateTimeFormatter.ofPattern("HH:mm"))}."
            } else {
                "Morning reminder disabled."
            }
            _userMessage.value = message
        }
    }

    /**
     * Updates notification time and whether alerts fire the evening before collection.
     */
    fun updateReminderSchedule(time: LocalTime, eveningBefore: Boolean) {
        if (eveningBefore) {
            updateEveningReminderTime(time)
        } else {
            updateMorningReminderTime(time)
        }
    }

    /**
     * Sends an immediate test notification to verify notification setup.
     */
    fun sendTestNotification(context: Context) {
        NotificationScheduler.sendImmediateTestNotification(context)
        _userMessage.value = "Test notification sent! Check your notification panel."
    }

    /**
     * Restores standard UK council bin profiles.
     */
    fun resetDefaultBins() {
        viewModelScope.launch {
            repository.restoreStandardBins()
            _userMessage.value = "Restored standard UK wheelie bin profile."
        }
    }

    /**
     * Clears all saved bins and resets onboarding state to allow entering a new address via [ResetTimetableUseCase].
     */
    fun resetTimetableAndAddress(context: Context, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            resetTimetableUseCase(context)
            onComplete()
        }
    }

    /**
     * Resets onboarding state and clears bins for setup wizard re-run via [ResetTimetableUseCase].
     */
    fun resetOnboarding(onComplete: () -> Unit) {
        viewModelScope.launch {
            resetTimetableUseCase()
            onComplete()
        }
    }

    /**
     * Handles notification permission denial feedback.
     */
    fun onNotificationPermissionDenied() {
        _userMessage.value = "Notification permission is required to receive bin reminders."
    }

    /**
     * Clears current user message / toast notification state immediately when consumed.
     */
    fun onToastShown() {
        _userMessage.value = null
    }

    /**
     * Clears current user message notification string.
     */
    fun dismissUserMessage() {
        onToastShown()
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
