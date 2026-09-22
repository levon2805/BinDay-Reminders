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
    fun toggleReminders(enabled: Boolean) {
        viewModelScope.launch {
            val currentSettings = repository.notificationSettings.first()
            val updated = currentSettings.copy(reminderEnabled = enabled)
            repository.updateNotificationSettings(updated)
            val status = if (enabled) "enabled" else "disabled"
            _userMessage.value = "Collection reminders $status."
        }
    }

    /**
     * Updates evening reminder time (or clears all if null).
     */
    fun updateEveningReminderTime(time: LocalTime?) {
        viewModelScope.launch {
            val currentSettings = repository.notificationSettings.first()
            val oldPrimary = currentSettings.primaryEveningTime
            val extraTimes = if (oldPrimary != null) currentSettings.eveningReminderTimes - oldPrimary else currentSettings.eveningReminderTimes
            val updated = if (time == null) {
                // "None" selected: clear primary AND all advanced/extra evening times
                currentSettings.copy(
                    primaryEveningTime = null,
                    eveningReminderTimes = emptySet()
                )
            } else {
                currentSettings.copy(
                    primaryEveningTime = time,
                    eveningReminderTimes = extraTimes + time
                )
            }
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
     * Updates morning reminder time (or clears all if null).
     */
    fun updateMorningReminderTime(time: LocalTime?) {
        viewModelScope.launch {
            val currentSettings = repository.notificationSettings.first()
            val oldPrimary = currentSettings.primaryMorningTime
            val extraTimes = if (oldPrimary != null) currentSettings.morningReminderTimes - oldPrimary else currentSettings.morningReminderTimes
            val updated = if (time == null) {
                // "None" selected: clear primary AND all advanced/extra morning times
                currentSettings.copy(
                    primaryMorningTime = null,
                    morningReminderTimes = emptySet()
                )
            } else {
                currentSettings.copy(
                    primaryMorningTime = time,
                    morningReminderTimes = extraTimes + time
                )
            }
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
     * Adds an extra reminder time for evening before (if hour >= 12) or morning of (if hour < 12).
     */
    fun addExtraReminderTime(time: LocalTime) {
        viewModelScope.launch {
            val currentSettings = repository.notificationSettings.first()
            val updated = if (time.hour >= 12) {
                currentSettings.copy(eveningReminderTimes = currentSettings.eveningReminderTimes + time)
            } else {
                currentSettings.copy(morningReminderTimes = currentSettings.morningReminderTimes + time)
            }
            repository.updateNotificationSettings(updated)
            _userMessage.value = "Extra reminder added for ${time.format(DateTimeFormatter.ofPattern("HH:mm"))}."
        }
    }

    /**
     * Adds a reminder time for the day before collection.
     */
    fun addEveningReminderTime(time: LocalTime) {
        viewModelScope.launch {
            val currentSettings = repository.notificationSettings.first()
            val updated = currentSettings.copy(eveningReminderTimes = currentSettings.eveningReminderTimes + time)
            repository.updateNotificationSettings(updated)
            _userMessage.value = "Day Before reminder added for ${time.format(DateTimeFormatter.ofPattern("HH:mm"))}."
        }
    }

    /**
     * Adds a reminder time for the day of collection.
     */
    fun addMorningReminderTime(time: LocalTime) {
        viewModelScope.launch {
            val currentSettings = repository.notificationSettings.first()
            val updated = currentSettings.copy(morningReminderTimes = currentSettings.morningReminderTimes + time)
            repository.updateNotificationSettings(updated)
            _userMessage.value = "Day Of reminder added for ${time.format(DateTimeFormatter.ofPattern("HH:mm"))}."
        }
    }

    /**
     * Edits an existing day before reminder time in-place.
     */
    fun editEveningReminderTime(oldTime: LocalTime, newTime: LocalTime) {
        viewModelScope.launch {
            val currentSettings = repository.notificationSettings.first()
            val updatedTimes = (currentSettings.eveningReminderTimes - oldTime) + newTime
            val newPrimary = if (currentSettings.primaryEveningTime == oldTime) newTime else currentSettings.primaryEveningTime
            val updated = currentSettings.copy(
                primaryEveningTime = newPrimary,
                eveningReminderTimes = updatedTimes
            )
            repository.updateNotificationSettings(updated)
            _userMessage.value = "Day Before reminder updated to ${newTime.format(DateTimeFormatter.ofPattern("HH:mm"))}."
        }
    }

    /**
     * Edits an existing day of reminder time in-place.
     */
    fun editMorningReminderTime(oldTime: LocalTime, newTime: LocalTime) {
        viewModelScope.launch {
            val currentSettings = repository.notificationSettings.first()
            val updatedTimes = (currentSettings.morningReminderTimes - oldTime) + newTime
            val newPrimary = if (currentSettings.primaryMorningTime == oldTime) newTime else currentSettings.primaryMorningTime
            val updated = currentSettings.copy(
                primaryMorningTime = newPrimary,
                morningReminderTimes = updatedTimes
            )
            repository.updateNotificationSettings(updated)
            _userMessage.value = "Day Of reminder updated to ${newTime.format(DateTimeFormatter.ofPattern("HH:mm"))}."
        }
    }

    /**
     * Removes a day before reminder time.
     */
    fun removeEveningReminderTime(time: LocalTime) {
        viewModelScope.launch {
            val currentSettings = repository.notificationSettings.first()
            val updatedTimes = currentSettings.eveningReminderTimes - time
            val newPrimary = if (currentSettings.primaryEveningTime == time) null else currentSettings.primaryEveningTime
            val updated = currentSettings.copy(
                primaryEveningTime = newPrimary,
                eveningReminderTimes = updatedTimes
            )
            repository.updateNotificationSettings(updated)
            _userMessage.value = "Day Before reminder removed."
        }
    }

    /**
     * Removes a day of reminder time.
     */
    fun removeMorningReminderTime(time: LocalTime) {
        viewModelScope.launch {
            val currentSettings = repository.notificationSettings.first()
            val updatedTimes = currentSettings.morningReminderTimes - time
            val newPrimary = if (currentSettings.primaryMorningTime == time) null else currentSettings.primaryMorningTime
            val updated = currentSettings.copy(
                primaryMorningTime = newPrimary,
                morningReminderTimes = updatedTimes
            )
            repository.updateNotificationSettings(updated)
            _userMessage.value = "Day Of reminder removed."
        }
    }

    /**
     * Removes an extra reminder time.
     */
    fun removeExtraReminderTime(time: LocalTime) {
        viewModelScope.launch {
            val currentSettings = repository.notificationSettings.first()
            val updatedEvening = currentSettings.eveningReminderTimes - time
            val updatedMorning = currentSettings.morningReminderTimes - time
            val newPrimaryEvening = if (currentSettings.primaryEveningTime == time) null else currentSettings.primaryEveningTime
            val newPrimaryMorning = if (currentSettings.primaryMorningTime == time) null else currentSettings.primaryMorningTime
            val updated = currentSettings.copy(
                primaryEveningTime = newPrimaryEvening,
                primaryMorningTime = newPrimaryMorning,
                eveningReminderTimes = updatedEvening,
                morningReminderTimes = updatedMorning
            )
            repository.updateNotificationSettings(updated)
            _userMessage.value = "Extra reminder removed."
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
