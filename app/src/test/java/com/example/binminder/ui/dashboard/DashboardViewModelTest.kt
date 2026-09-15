package com.example.binminder.ui.dashboard

import com.example.binminder.data.model.AppThemeMode
import com.example.binminder.data.model.Bin
import com.example.binminder.data.model.BinColor
import com.example.binminder.data.model.CollectionEvent
import com.example.binminder.data.model.NotificationSettings
import com.example.binminder.data.model.OnboardingBinSetup
import com.example.binminder.data.model.RecurrenceType
import com.example.binminder.data.repository.BinRepository
import com.example.binminder.domain.GetUpcomingCollectionsUseCase
import com.example.binminder.domain.ToggleBinPutOutUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakeBinRepository
    private lateinit var viewModel: DashboardViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeBinRepository()
        viewModel = DashboardViewModel(
            repository = fakeRepository,
            getUpcomingCollectionsUseCase = GetUpcomingCollectionsUseCase(fakeRepository),
            toggleBinPutOutUseCase = ToggleBinPutOutUseCase(),
            started = SharingStarted.Eagerly
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testInitialStateLoadsSchedule() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.nextCollectionEvents.isNotEmpty())
        assertEquals("General Waste", state.nextCollectionEvents[0].binName)
    }

    @Test
    fun testMarkBinPutOutTogglesState() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.markBinPutOut(1L, "General Waste")
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.putOutBins.contains(1L))
        assertEquals("Marked 'General Waste' bin as put out for collection.", viewModel.uiState.value.userMessage)

        viewModel.markBinPutOut(1L, "General Waste")
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.uiState.value.putOutBins.contains(1L))
        assertEquals("Unmarked 'General Waste' bin.", viewModel.uiState.value.userMessage)
    }

    private class FakeBinRepository : BinRepository {
        val today: LocalDate = LocalDate.now()

        val sampleBin = Bin(
            id = 1,
            name = "General Waste",
            colorHex = BinColor.BLACK.defaultHex,
            presetColor = BinColor.BLACK,
            recurrence = RecurrenceType.WEEKLY,
            startDate = today,
            isEnabled = true,
            adjustForBankHolidays = false
        )

        private val binsFlow = MutableStateFlow(listOf(sampleBin))

        override val allBins: Flow<List<Bin>> = binsFlow
        override suspend fun getBinsList(): List<Bin> = binsFlow.value
        override fun getBin(id: Long): Flow<Bin?> = flowOf(sampleBin)
        override suspend fun getBinSync(id: Long): Bin? = sampleBin
        override suspend fun insertBin(bin: Bin): Long = 1L
        override suspend fun updateBin(bin: Bin) {}
        override suspend fun deleteBin(bin: Bin) {}
        override suspend fun clearAllBins() {}
        override suspend fun ensureDefaultBinsInitialized() {}
        override suspend fun restoreStandardBins() {}

        override val notificationSettings: Flow<NotificationSettings> = flowOf(NotificationSettings())
        override suspend fun updateNotificationSettings(settings: NotificationSettings) {}
        override val themeMode: Flow<AppThemeMode> = flowOf(AppThemeMode.SYSTEM)
        override suspend fun setThemeMode(themeMode: AppThemeMode) {}
        override val onboardingCompleted: Flow<Boolean> = flowOf(true)
        override suspend fun setOnboardingCompleted(completed: Boolean) {}

        override suspend fun completeOnboardingSetup(
            postcodeOrCouncil: String,
            primaryDay: DayOfWeek,
            binSetups: List<OnboardingBinSetup>,
            notificationSettings: NotificationSettings
        ) {}

        override fun getUpcomingCollectionEvents(
            startDate: LocalDate,
            endDate: LocalDate
        ): Flow<List<CollectionEvent>> {
            val event = CollectionEvent(
                binId = 1L,
                binName = "General Waste",
                binColorHex = BinColor.BLACK.defaultHex,
                presetColor = BinColor.BLACK,
                collectionDate = today.plusDays(1),
                originalDate = today.plusDays(1),
                isBankHolidayAdjusted = false
            )
            return flowOf(listOf(event))
        }
    }
}
