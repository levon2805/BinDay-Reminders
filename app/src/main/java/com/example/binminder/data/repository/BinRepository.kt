package com.example.binminder.data.repository

import com.example.binminder.data.model.AppThemeMode
import com.example.binminder.data.model.Bin
import com.example.binminder.data.model.CollectionEvent
import com.example.binminder.data.model.NotificationSettings
import com.example.binminder.data.model.OnboardingBinSetup
import kotlinx.coroutines.flow.Flow
import java.time.DayOfWeek
import java.time.LocalDate

interface BinRepository {
    val allBins: Flow<List<Bin>>
    suspend fun getBinsList(): List<Bin>
    fun getBin(id: Long): Flow<Bin?>
    suspend fun getBinSync(id: Long): Bin?
    suspend fun insertBin(bin: Bin): Long
    suspend fun updateBin(bin: Bin)
    suspend fun deleteBin(bin: Bin)
    suspend fun clearAllBins()
    suspend fun ensureDefaultBinsInitialized()

    val notificationSettings: Flow<NotificationSettings>
    suspend fun updateNotificationSettings(settings: NotificationSettings)

    val themeMode: Flow<AppThemeMode>
    suspend fun setThemeMode(themeMode: AppThemeMode)

    val onboardingCompleted: Flow<Boolean>
    suspend fun setOnboardingCompleted(completed: Boolean)
    suspend fun completeOnboardingSetup(
        postcodeOrCouncil: String,
        primaryDay: DayOfWeek,
        binSetups: List<OnboardingBinSetup>,
        notificationSettings: NotificationSettings
    )

    fun getUpcomingCollectionEvents(startDate: LocalDate, endDate: LocalDate): Flow<List<CollectionEvent>>
}
