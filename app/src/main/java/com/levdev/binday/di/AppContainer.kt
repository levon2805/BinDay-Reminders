package com.levdev.binday.di

import android.content.Context
import com.levdev.binday.data.local.AppDatabase
import com.levdev.binday.data.local.NotificationSettingsDataStore
import com.levdev.binday.data.repository.BinRepository
import com.levdev.binday.data.repository.BinRepositoryImpl
import com.levdev.binday.data.repository.CouncilLookupRepository
import com.levdev.binday.data.repository.CouncilLookupRepositoryImpl
import com.levdev.binday.data.service.CouncilLookupService
import com.levdev.binday.data.service.CouncilLookupServiceImpl
import com.levdev.binday.domain.GetUpcomingCollectionsUseCase
import com.levdev.binday.domain.LookupCouncilScheduleUseCase
import com.levdev.binday.domain.ResetTimetableUseCase
import com.levdev.binday.domain.ToggleBinPutOutUseCase
import com.levdev.binday.engine.BankHolidayCalculator
import com.levdev.binday.engine.ScheduleEngine
import com.levdev.binday.worker.NotificationScheduler

/**
 * Application Dependency Container interface providing clean, thread-safe singleton dependencies.
 *
 * A right proper container to keep your repositories, services, and domain use cases sorted!
 */
interface AppContainer {
    val database: AppDatabase
    val notificationSettingsDataStore: NotificationSettingsDataStore
    val councilLookupService: CouncilLookupService
    val binRepository: BinRepository
    val councilLookupRepository: CouncilLookupRepository
    val bankHolidayCalculator: BankHolidayCalculator
    val scheduleEngine: ScheduleEngine
    val notificationScheduler: NotificationScheduler

    // Domain Use Cases
    val getUpcomingCollectionsUseCase: GetUpcomingCollectionsUseCase
    val lookupCouncilScheduleUseCase: LookupCouncilScheduleUseCase
    val toggleBinPutOutUseCase: ToggleBinPutOutUseCase
    val resetTimetableUseCase: ResetTimetableUseCase
}

/**
 * Default thread-safe implementation of [AppContainer] providing application singletons.
 */
class DefaultAppContainer(
    context: Context
) : AppContainer {

    private val applicationContext = context.applicationContext

    override val database: AppDatabase by lazy {
        AppDatabase.getInstance(applicationContext)
    }

    override val notificationSettingsDataStore: NotificationSettingsDataStore by lazy {
        NotificationSettingsDataStore(applicationContext)
    }

    override val councilLookupService: CouncilLookupService by lazy {
        CouncilLookupServiceImpl()
    }

    override val binRepository: BinRepository by lazy {
        BinRepositoryImpl(
            binDao = database.binDao(),
            notificationSettingsDataStore = notificationSettingsDataStore,
            context = applicationContext
        )
    }

    override val councilLookupRepository: CouncilLookupRepository by lazy {
        CouncilLookupRepositoryImpl(
            service = councilLookupService
        )
    }

    override val bankHolidayCalculator: BankHolidayCalculator
        get() = BankHolidayCalculator

    override val scheduleEngine: ScheduleEngine
        get() = ScheduleEngine

    override val notificationScheduler: NotificationScheduler
        get() = NotificationScheduler

    override val getUpcomingCollectionsUseCase: GetUpcomingCollectionsUseCase by lazy {
        GetUpcomingCollectionsUseCase(binRepository)
    }

    override val lookupCouncilScheduleUseCase: LookupCouncilScheduleUseCase by lazy {
        LookupCouncilScheduleUseCase(councilLookupRepository)
    }

    override val toggleBinPutOutUseCase: ToggleBinPutOutUseCase by lazy {
        ToggleBinPutOutUseCase()
    }

    override val resetTimetableUseCase: ResetTimetableUseCase by lazy {
        ResetTimetableUseCase(binRepository)
    }
}
