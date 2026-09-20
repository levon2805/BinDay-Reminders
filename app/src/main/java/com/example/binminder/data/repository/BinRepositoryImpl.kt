package com.example.binminder.data.repository

import android.content.Context
import com.example.binminder.data.local.BinDao
import com.example.binminder.data.local.BinEntity
import com.example.binminder.data.local.NotificationSettingsDataStore
import com.example.binminder.data.model.AppThemeMode
import com.example.binminder.data.model.Bin
import com.example.binminder.data.model.BinColor
import com.example.binminder.data.model.CollectionEvent
import com.example.binminder.data.model.NotificationSettings
import com.example.binminder.data.model.OnboardingBinSetup
import com.example.binminder.data.model.RecurrenceType
import com.example.binminder.engine.ScheduleEngine
import com.example.binminder.worker.NotificationHelper
import com.example.binminder.worker.NotificationScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/**
 * Concrete implementation of [BinRepository] coordinating Room database and DataStore settings.
 * 
 * Ensures all I/O calls execute safely on [Dispatchers.IO] with robust error handling.
 * Proper grand data layer logic to keep your wheelie bin details saved tidy and fast!
 */
class BinRepositoryImpl(
    private val binDao: BinDao,
    private val notificationSettingsDataStore: NotificationSettingsDataStore,
    context: Context
) : BinRepository {

    private val applicationContext = context.applicationContext

    private suspend fun triggerScheduleUpdate() = withContext(Dispatchers.IO) {
        runCatching {
            NotificationScheduler.scheduleNotificationWorker(applicationContext)
        }
    }

    /**
     * Observes all bins from local database as a reactive flow on [Dispatchers.IO].
     */
    override val allBins: Flow<List<Bin>> = binDao.getAllBins().map { entities ->
        entities.map { it.toDomain() }
    }.flowOn(Dispatchers.IO)

    /**
     * Retrieves a direct snapshot list of all saved bins safely on [Dispatchers.IO].
     */
    override suspend fun getBinsList(): List<Bin> = withContext(Dispatchers.IO) {
        runCatching {
            binDao.getAllBinsList().map { it.toDomain() }
        }.getOrDefault(emptyList())
    }

    /**
     * Observes a specific bin by ID as a reactive flow on [Dispatchers.IO].
     */
    override fun getBin(id: Long): Flow<Bin?> = binDao.getBinById(id).map {
        it?.toDomain()
    }.flowOn(Dispatchers.IO)

    /**
     * Fetches a specific bin by ID synchronously on [Dispatchers.IO].
     */
    override suspend fun getBinSync(id: Long): Bin? = withContext(Dispatchers.IO) {
        runCatching {
            binDao.getBinByIdSync(id)?.toDomain()
        }.getOrNull()
    }

    /**
     * Saves a new bin and triggers background reminder updates on [Dispatchers.IO].
     */
    override suspend fun insertBin(bin: Bin): Long = withContext(Dispatchers.IO) {
        val id = binDao.insertBin(BinEntity.fromDomain(bin))
        triggerScheduleUpdate()
        id
    }

    /**
     * Updates an existing bin and triggers background reminder updates on [Dispatchers.IO].
     */
    override suspend fun updateBin(bin: Bin): Unit = withContext(Dispatchers.IO) {
        binDao.updateBin(BinEntity.fromDomain(bin))
        triggerScheduleUpdate()
    }

    /**
     * Deletes a bin and triggers background reminder updates on [Dispatchers.IO].
     */
    override suspend fun deleteBin(bin: Bin): Unit = withContext(Dispatchers.IO) {
        binDao.deleteBin(BinEntity.fromDomain(bin))
        triggerScheduleUpdate()
    }

    /**
     * Clears all saved bins and updates scheduled notifications on [Dispatchers.IO].
     */
    override suspend fun clearAllBins(): Unit = withContext(Dispatchers.IO) {
        binDao.deleteAllBins()
        triggerScheduleUpdate()
    }

    /**
     * Populates sensible default UK bin choices if local storage is empty on [Dispatchers.IO].
     */
    override suspend fun ensureDefaultBinsInitialized(): Unit = withContext(Dispatchers.IO) {
        runCatching {
            val isOnboardingCompleted = notificationSettingsDataStore.onboardingCompleted.first()
            if (!isOnboardingCompleted && binDao.getBinCount() == 0) {
                val today = LocalDate.now()
                val currentMonday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

                val defaultBins = listOf(
                    Bin(
                        name = "General Waste",
                        colorHex = BinColor.BLACK.defaultHex,
                        presetColor = BinColor.BLACK,
                        recurrence = RecurrenceType.FORTNIGHTLY,
                        startDate = currentMonday,
                        customNote = "Black bin for non-recyclable household waste. Put out by 7:00 AM.",
                        isEnabled = true,
                        adjustForBankHolidays = true
                    ),
                    Bin(
                        name = "Dry Mixed Recycling",
                        colorHex = BinColor.BLUE.defaultHex,
                        presetColor = BinColor.BLUE,
                        recurrence = RecurrenceType.FORTNIGHTLY,
                        startDate = currentMonday.plusWeeks(1),
                        customNote = "Blue bin for paper, cardboard, plastic bottles, and cans.",
                        isEnabled = true,
                        adjustForBankHolidays = true
                    ),
                    Bin(
                        name = "Garden Waste",
                        colorHex = BinColor.GREEN.defaultHex,
                        presetColor = BinColor.GREEN,
                        recurrence = RecurrenceType.FORTNIGHTLY,
                        startDate = currentMonday,
                        customNote = "Green bin for grass cuttings, leaves, and small hedge trimmings.",
                        isEnabled = true,
                        adjustForBankHolidays = true
                    ),
                    Bin(
                        name = "Food Waste Caddy",
                        colorHex = BinColor.BROWN.defaultHex,
                        presetColor = BinColor.BROWN,
                        recurrence = RecurrenceType.WEEKLY,
                        startDate = currentMonday,
                        customNote = "Brown caddy for food scraps and kitchen leftovers.",
                        isEnabled = true,
                        adjustForBankHolidays = true
                    )
                )

                binDao.insertBins(defaultBins.map { BinEntity.fromDomain(it) })
                triggerScheduleUpdate()
            }
        }
    }

    /**
     * Clears existing bins and restores the standard UK council wheelie bin profile on [Dispatchers.IO].
     */
    override suspend fun restoreStandardBins(): Unit = withContext(Dispatchers.IO) {
        binDao.deleteAllBins()
        val today = LocalDate.now()
        val currentMonday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

        val defaultBins = listOf(
            Bin(
                name = "General Waste",
                colorHex = BinColor.BLACK.defaultHex,
                presetColor = BinColor.BLACK,
                recurrence = RecurrenceType.FORTNIGHTLY,
                startDate = currentMonday,
                customNote = "Black bin for non-recyclable household waste. Put out by 7:00 AM.",
                isEnabled = true,
                adjustForBankHolidays = true
            ),
            Bin(
                name = "Dry Mixed Recycling",
                colorHex = BinColor.BLUE.defaultHex,
                presetColor = BinColor.BLUE,
                recurrence = RecurrenceType.FORTNIGHTLY,
                startDate = currentMonday.plusWeeks(1),
                customNote = "Blue bin for paper, cardboard, plastic bottles, and cans.",
                isEnabled = true,
                adjustForBankHolidays = true
            ),
            Bin(
                name = "Garden Waste",
                colorHex = BinColor.GREEN.defaultHex,
                presetColor = BinColor.GREEN,
                recurrence = RecurrenceType.FORTNIGHTLY,
                startDate = currentMonday,
                customNote = "Green bin for grass cuttings, leaves, and small hedge trimmings.",
                isEnabled = true,
                adjustForBankHolidays = true
            ),
            Bin(
                name = "Food Waste Caddy",
                colorHex = BinColor.BROWN.defaultHex,
                presetColor = BinColor.BROWN,
                recurrence = RecurrenceType.WEEKLY,
                startDate = currentMonday,
                customNote = "Brown caddy for food scraps and kitchen leftovers.",
                isEnabled = true,
                adjustForBankHolidays = true
            )
        )

        binDao.insertBins(defaultBins.map { BinEntity.fromDomain(it) })
        triggerScheduleUpdate()
    }

    /**
     * Observes notification preference changes as a flow on [Dispatchers.IO].
     */
    override val notificationSettings: Flow<NotificationSettings> =
        notificationSettingsDataStore.notificationSettings.flowOn(Dispatchers.IO)

    /**
     * Saves updated notification preferences and reschedules background reminders on [Dispatchers.IO].
     */
    override suspend fun updateNotificationSettings(settings: NotificationSettings): Unit = withContext(Dispatchers.IO) {
        notificationSettingsDataStore.updateSettings(settings)
        NotificationHelper.clearDebounceCache(applicationContext)
        NotificationScheduler.scheduleNotificationWorker(applicationContext, settings)
    }

    /**
     * Observes set of put out bin IDs as a flow on [Dispatchers.IO].
     */
    override val putOutBins: Flow<Set<String>> =
        notificationSettingsDataStore.putOutBinIds.flowOn(Dispatchers.IO)

    /**
     * Updates set of put out bin IDs on [Dispatchers.IO] and immediately reschedules notification engine.
     */
    override suspend fun updatePutOutBins(binIds: Set<String>): Unit = withContext(Dispatchers.IO) {
        notificationSettingsDataStore.setPutOutBinIds(binIds)
        NotificationScheduler.scheduleNotificationWorker(applicationContext)
    }

    /**
     * Observes active theme choices as a flow on [Dispatchers.IO].
     */
    override val themeMode: Flow<AppThemeMode> =
        notificationSettingsDataStore.themeMode.flowOn(Dispatchers.IO)

    /**
     * Saves the chosen app theme option on [Dispatchers.IO].
     */
    override suspend fun setThemeMode(themeMode: AppThemeMode): Unit = withContext(Dispatchers.IO) {
        notificationSettingsDataStore.setThemeMode(themeMode)
    }

    /**
     * Observes onboarding completion status on [Dispatchers.IO].
     */
    override val onboardingCompleted: Flow<Boolean> =
        notificationSettingsDataStore.onboardingCompleted.flowOn(Dispatchers.IO)

    /**
     * Sets whether setup onboarding is completed on [Dispatchers.IO].
     */
    override suspend fun setOnboardingCompleted(completed: Boolean): Unit = withContext(Dispatchers.IO) {
        notificationSettingsDataStore.setOnboardingCompleted(completed)
    }

    /**
     * Configures initial bins and preferences based on user choices during onboarding on [Dispatchers.IO].
     */
    override suspend fun completeOnboardingSetup(
        postcodeOrCouncil: String,
        primaryDay: DayOfWeek,
        binSetups: List<OnboardingBinSetup>,
        notificationSettings: NotificationSettings
    ): Unit = withContext(Dispatchers.IO) {
        val currentTheme = notificationSettingsDataStore.themeMode.first()
        val settingsWithPreservedTheme = if (notificationSettings.themeMode == AppThemeMode.SYSTEM && currentTheme != AppThemeMode.SYSTEM) {
            notificationSettings.copy(themeMode = currentTheme)
        } else {
            notificationSettings
        }
        notificationSettingsDataStore.saveOnboardingInfo(postcodeOrCouncil, primaryDay.name)

        binDao.deleteAllBins()

        val today = LocalDate.now()

        val newBins = binSetups.filter { it.isEnabled }.map { setup ->
            val binDay = setup.collectionDay ?: primaryDay
            val firstBinDay = today.with(TemporalAdjusters.nextOrSame(binDay))

            val startDate = if (setup.startNextWeek && setup.recurrence.intervalWeeks > 1) {
                firstBinDay.plusWeeks(1)
            } else {
                firstBinDay
            }

            Bin(
                name = setup.displayName,
                colorHex = setup.presetColor.defaultHex,
                presetColor = setup.presetColor,
                lidColorHex = setup.lidPresetColor?.defaultHex,
                lidPresetColor = setup.lidPresetColor,
                recurrence = setup.recurrence,
                startDate = startDate,
                customNote = setup.customNote,
                isEnabled = true,
                adjustForBankHolidays = true
            )
        }

        if (newBins.isNotEmpty()) {
            binDao.insertBins(newBins.map { BinEntity.fromDomain(it) })
        }

        updateNotificationSettings(settingsWithPreservedTheme)
        setOnboardingCompleted(true)
    }

    /**
     * Calculates future collection dates across active bins within a date range on [Dispatchers.IO].
     */
    override fun getUpcomingCollectionEvents(
        startDate: LocalDate,
        endDate: LocalDate
    ): Flow<List<CollectionEvent>> {
        return allBins.map { bins ->
            ScheduleEngine.generateCollectionEvents(bins, startDate, endDate)
        }.flowOn(Dispatchers.IO)
    }
}
