package com.example.binminder.data.repository

import com.example.binminder.data.model.AppThemeMode
import com.example.binminder.data.model.Bin
import com.example.binminder.data.model.CollectionEvent
import com.example.binminder.data.model.NotificationSettings
import com.example.binminder.data.model.OnboardingBinSetup
import kotlinx.coroutines.flow.Flow
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * Repository interface defining operations for managing bins, collection schedules, and settings.
 * 
 * Acts as the single source of truth for bin details and user application preferences.
 */
interface BinRepository {
    /**
     * Reactive flow of all saved bins.
     */
    val allBins: Flow<List<Bin>>

    /**
     * Gets a snapshot list of all current bins.
     */
    suspend fun getBinsList(): List<Bin>

    /**
     * Observes a single bin by its unique ID.
     */
    fun getBin(id: Long): Flow<Bin?>

    /**
     * Fetches a single bin synchronously by its unique ID.
     */
    suspend fun getBinSync(id: Long): Bin?

    /**
     * Adds a new bin and returns its newly assigned database ID.
     */
    suspend fun insertBin(bin: Bin): Long

    /**
     * Updates details for an existing bin.
     */
    suspend fun updateBin(bin: Bin)

    /**
     * Deletes a specific bin from storage.
     */
    suspend fun deleteBin(bin: Bin)

    /**
     * Clears all stored bins from the application.
     */
    suspend fun clearAllBins()

    /**
     * Ensures initial default bins are populated if the database is currently empty.
     */
    suspend fun ensureDefaultBinsInitialized()

    /**
     * Clears existing bins and restores the standard UK council wheelie bin profile.
     */
    suspend fun restoreStandardBins()

    /**
     * Reactive flow of user notification and reminder preferences.
     */
    val notificationSettings: Flow<NotificationSettings>

    /**
     * Updates user notification preferences and reschedules background reminders.
     */
    suspend fun updateNotificationSettings(settings: NotificationSettings)

    /**
     * Reactive flow of put out bin IDs.
     */
    val putOutBins: Flow<Set<String>>

    /**
     * Updates set of put out bin IDs.
     */
    suspend fun updatePutOutBins(binIds: Set<String>)

    /**
     * Reactive flow of the active application theme preference.
     */
    val themeMode: Flow<AppThemeMode>

    /**
     * Updates the active application theme mode preference.
     */
    suspend fun setThemeMode(themeMode: AppThemeMode)

    /**
     * Reactive flow indicating whether first-time onboarding is complete.
     */
    val onboardingCompleted: Flow<Boolean>

    /**
     * Updates the onboarding completion status flag.
     */
    suspend fun setOnboardingCompleted(completed: Boolean)

    /**
     * Saves user onboarding choices, sets up default bins, and marks setup as finished.
     */
    suspend fun completeOnboardingSetup(
        postcodeOrCouncil: String,
        primaryDay: DayOfWeek,
        binSetups: List<OnboardingBinSetup>,
        notificationSettings: NotificationSettings
    )

    /**
     * Generates upcoming collection events for all active bins within a specified date window.
     */
    fun getUpcomingCollectionEvents(startDate: LocalDate, endDate: LocalDate): Flow<List<CollectionEvent>>
}
