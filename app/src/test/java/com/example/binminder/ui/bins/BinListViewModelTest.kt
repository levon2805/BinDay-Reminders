package com.example.binminder.ui.bins

import com.example.binminder.data.model.AppThemeMode
import com.example.binminder.data.model.Bin
import com.example.binminder.data.model.BinColor
import com.example.binminder.data.model.CollectionEvent
import com.example.binminder.data.model.NotificationSettings
import com.example.binminder.data.model.OnboardingBinSetup
import com.example.binminder.data.model.RecurrenceType
import com.example.binminder.data.repository.BinRepository
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class BinListViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakeBinRepository
    private lateinit var viewModel: BinListViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeBinRepository()
        viewModel = BinListViewModel(
            repository = fakeRepository,
            started = SharingStarted.Eagerly
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testInitialStateLoadsBins() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(1, state.bins.size)
        assertEquals("General Waste", state.bins[0].name)
    }

    @Test
    fun testToggleBinEnabled() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()

        val bin = fakeRepository.sampleBin
        viewModel.toggleBinEnabled(bin)
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(fakeRepository.updatedBin?.isEnabled ?: true)
        assertEquals("'General Waste' disabled.", viewModel.uiState.value.userMessage)
    }

    @Test
    fun testRequestAndConfirmDeleteBin() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()

        val bin = fakeRepository.sampleBin
        viewModel.requestDeleteBin(bin)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(bin, viewModel.uiState.value.binToDelete)

        viewModel.confirmDeleteBin()
        testDispatcher.scheduler.advanceUntilIdle()

        assertNull(viewModel.uiState.value.binToDelete)
        assertTrue(fakeRepository.deleteBinCalled)
        assertEquals("Deleted 'General Waste' wheelie bin.", viewModel.uiState.value.userMessage)
    }

    @Test
    fun testResetDefaultBins() = runTest {
        viewModel.resetDefaultBins()
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(fakeRepository.restoreStandardBinsCalled)
        assertEquals("Restored standard UK wheelie bin profile.", viewModel.uiState.value.userMessage)
    }

    private class FakeBinRepository : BinRepository {
        val sampleBin = Bin(
            id = 1,
            name = "General Waste",
            colorHex = BinColor.BLACK.defaultHex,
            presetColor = BinColor.BLACK,
            recurrence = RecurrenceType.FORTNIGHTLY,
            startDate = LocalDate.of(2025, 5, 5),
            isEnabled = true,
            adjustForBankHolidays = true
        )

        var updatedBin: Bin? = null
        var deleteBinCalled = false
        private val binsFlow = MutableStateFlow(listOf(sampleBin))

        override val allBins: Flow<List<Bin>> = binsFlow
        override suspend fun getBinsList(): List<Bin> = binsFlow.value
        override fun getBin(id: Long): Flow<Bin?> = flowOf(sampleBin)
        override suspend fun getBinSync(id: Long): Bin? = sampleBin
        override suspend fun insertBin(bin: Bin): Long = 1L

        override suspend fun updateBin(bin: Bin) {
            updatedBin = bin
            binsFlow.value = listOf(bin)
        }

        override suspend fun deleteBin(bin: Bin) {
            deleteBinCalled = true
            binsFlow.value = emptyList()
        }

        override suspend fun clearAllBins() {
            binsFlow.value = emptyList()
        }

        override suspend fun ensureDefaultBinsInitialized() {}

        var restoreStandardBinsCalled = false
        override suspend fun restoreStandardBins() {
            restoreStandardBinsCalled = true
        }

        override val notificationSettings: Flow<NotificationSettings> = flowOf(NotificationSettings())
        override suspend fun updateNotificationSettings(settings: NotificationSettings) {}
        override val putOutBins: Flow<Set<String>> = flowOf(emptySet())
        override suspend fun updatePutOutBins(binIds: Set<String>) {}
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
        ): Flow<List<CollectionEvent>> = flowOf(emptyList())
    }
}
