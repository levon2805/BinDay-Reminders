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
import com.example.binminder.worker.NotificationScheduler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

class BinRepositoryImpl(
    private val binDao: BinDao,
    private val notificationSettingsDataStore: NotificationSettingsDataStore,
    context: Context
) : BinRepository {

    private val applicationContext = context.applicationContext

    private suspend fun triggerScheduleUpdate() {
        val settings = notificationSettingsDataStore.notificationSettings.first()
        NotificationScheduler.scheduleDailyReminder(applicationContext, settings)
    }

    override val allBins: Flow<List<Bin>> = binDao.getAllBins().map { entities ->
        entities.map { it.toDomain() }
    }

    override suspend fun getBinsList(): List<Bin> {
        return binDao.getAllBinsList().map { it.toDomain() }
    }

    override fun getBin(id: Long): Flow<Bin?> {
        return binDao.getBinById(id).map { it?.toDomain() }
    }

    override suspend fun getBinSync(id: Long): Bin? {
        return binDao.getBinByIdSync(id)?.toDomain()
    }

    override suspend fun insertBin(bin: Bin): Long {
        val id = binDao.insertBin(BinEntity.fromDomain(bin))
        triggerScheduleUpdate()
        return id
    }

    override suspend fun updateBin(bin: Bin) {
        binDao.updateBin(BinEntity.fromDomain(bin))
        triggerScheduleUpdate()
    }

    override suspend fun deleteBin(bin: Bin) {
        binDao.deleteBin(BinEntity.fromDomain(bin))
        triggerScheduleUpdate()
    }

    override suspend fun clearAllBins() {
        binDao.deleteAllBins()
        triggerScheduleUpdate()
    }

    override suspend fun ensureDefaultBinsInitialized() {
        if (binDao.getBinCount() == 0) {
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

    override val notificationSettings: Flow<NotificationSettings> =
        notificationSettingsDataStore.notificationSettings

    override suspend fun updateNotificationSettings(settings: NotificationSettings) {
        notificationSettingsDataStore.updateSettings(settings)
        NotificationScheduler.scheduleDailyReminder(applicationContext, settings)
    }

    override val themeMode: Flow<AppThemeMode> =
        notificationSettingsDataStore.themeMode

    override suspend fun setThemeMode(themeMode: AppThemeMode) {
        notificationSettingsDataStore.setThemeMode(themeMode)
    }

    override val onboardingCompleted: Flow<Boolean> =
        notificationSettingsDataStore.onboardingCompleted

    override suspend fun setOnboardingCompleted(completed: Boolean) {
        notificationSettingsDataStore.setOnboardingCompleted(completed)
    }

    override suspend fun completeOnboardingSetup(
        postcodeOrCouncil: String,
        primaryDay: DayOfWeek,
        binSetups: List<OnboardingBinSetup>,
        notificationSettings: NotificationSettings
    ) {
        notificationSettingsDataStore.saveOnboardingInfo(postcodeOrCouncil, primaryDay.name)

        binDao.deleteAllBins()

        val today = LocalDate.now()
        val firstPrimaryDay = today.with(TemporalAdjusters.nextOrSame(primaryDay))

        val newBins = binSetups.filter { it.isEnabled }.map { setup ->
            val startDate = if (setup.startNextWeek && setup.recurrence == RecurrenceType.FORTNIGHTLY) {
                firstPrimaryDay.plusWeeks(1)
            } else {
                firstPrimaryDay
            }

            Bin(
                name = setup.displayName,
                colorHex = setup.presetColor.defaultHex,
                presetColor = setup.presetColor,
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

        updateNotificationSettings(notificationSettings)
        setOnboardingCompleted(true)
    }

    override fun getUpcomingCollectionEvents(
        startDate: LocalDate,
        endDate: LocalDate
    ): Flow<List<CollectionEvent>> {
        return allBins.map { bins ->
            ScheduleEngine.generateCollectionEvents(bins, startDate, endDate)
        }
    }
}
