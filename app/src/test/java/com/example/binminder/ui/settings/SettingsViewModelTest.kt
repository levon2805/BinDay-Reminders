package com.example.binminder.ui.settings

import android.content.Context
import android.content.ContextWrapper
import com.example.binminder.data.model.AppThemeMode
import com.example.binminder.data.model.Bin
import com.example.binminder.data.model.BinColor
import com.example.binminder.data.model.CollectionEvent
import com.example.binminder.data.model.NotificationSettings
import com.example.binminder.data.model.OnboardingBinSetup
import com.example.binminder.data.model.RecurrenceType
import com.example.binminder.data.repository.BinRepository
import com.example.binminder.engine.BankHolidayCalculator
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
import java.time.LocalTime

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakeBinRepository
    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeBinRepository()
        viewModel = SettingsViewModel(
            repository = fakeRepository,
            started = SharingStarted.Eagerly
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testInitialStateAndBankHolidayPreviews() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.notificationSettings.reminderEnabled)
        assertTrue("Bank holiday previews should not be empty", state.bankHolidayPreviews.isNotEmpty())
    }

    @Test
    fun testResetTimetableAndAddressFlow() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()

        var onCompleteCalled = false
        viewModel.resetTimetableAndAddress(mockContext()) {
            onCompleteCalled = true
        }

        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue("onComplete callback should be called", onCompleteCalled)
        assertTrue("clearAllBins should be called on repository", fakeRepository.clearAllBinsCalled)
        assertFalse("onboardingCompleted should be set to false", fakeRepository.onboardingCompletedState)
    }



    @Test
    fun testToggleReminders() = runTest {
        viewModel.toggleReminders(false)
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(fakeRepository.notificationSettingsState.reminderEnabled)
        assertEquals("Collection reminders disabled.", viewModel.uiState.value.userMessage)
    }

    @Test
    fun testUpdateReminderSchedule() = runTest {
        val newTime = LocalTime.of(21, 0)
        viewModel.updateEveningReminderTime(newTime)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(newTime, fakeRepository.notificationSettingsState.eveningReminderTime)
        assertTrue(viewModel.uiState.value.userMessage!!.contains("21:00"))
    }

    @Test
    fun testUpdateReminderScheduleCustomTime() = runTest {
        val customEveningTime = LocalTime.of(19, 30)
        viewModel.updateEveningReminderTime(customEveningTime)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(customEveningTime, fakeRepository.notificationSettingsState.eveningReminderTime)
        assertTrue(viewModel.uiState.value.userMessage!!.contains("19:30"))

        val customMorningTime = LocalTime.of(6, 45)
        viewModel.updateMorningReminderTime(customMorningTime)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(customMorningTime, fakeRepository.notificationSettingsState.morningReminderTime)
        assertTrue(viewModel.uiState.value.userMessage!!.contains("06:45"))

        viewModel.updateEveningReminderTime(null)
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(null, fakeRepository.notificationSettingsState.eveningReminderTime)
        assertEquals("Evening reminder disabled.", viewModel.uiState.value.userMessage)
    }

    @Test
    fun testInitialThemeModePreference() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(AppThemeMode.SYSTEM, viewModel.uiState.value.themeMode)
        assertEquals("System Default", viewModel.uiState.value.themeMode.label)
    }

    @Test
    fun testSetThemeModeToDark() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.setThemeMode(AppThemeMode.DARK)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(AppThemeMode.DARK, fakeRepository.themeModeState)
        assertEquals(AppThemeMode.DARK, viewModel.uiState.value.themeMode)
        assertEquals("Theme updated to Dark Theme.", viewModel.uiState.value.userMessage)
    }

    @Test
    fun testSetThemeModeToLight() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.setThemeMode(AppThemeMode.LIGHT)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(AppThemeMode.LIGHT, fakeRepository.themeModeState)
        assertEquals(AppThemeMode.LIGHT, viewModel.uiState.value.themeMode)
        assertEquals("Theme updated to Light Theme.", viewModel.uiState.value.userMessage)
    }

    @Test
    fun testBankHolidayCalculatorNamedHolidays() {
        val namedHolidays = BankHolidayCalculator.getNamedBankHolidaysForYear(2025)
        assertEquals(8, namedHolidays.size)

        val names = namedHolidays.map { it.name }
        assertTrue(names.contains("New Year's Day"))
        assertTrue(names.contains("Good Friday"))
        assertTrue(names.contains("Easter Monday"))
        assertTrue(names.contains("Early May Bank Holiday"))
        assertTrue(names.contains("Spring Bank Holiday"))
        assertTrue(names.contains("Summer Bank Holiday"))
        assertTrue(names.contains("Christmas Day"))
        assertTrue(names.contains("Boxing Day"))
    }

    @Test
    fun testBankHolidayCalculatorNextUpcomingHolidays() {
        val today = LocalDate.of(2025, 5, 1)
        val upcoming = BankHolidayCalculator.getNextUpcomingBankHolidays(today, limit = 5)

        assertEquals(5, upcoming.size)
        assertEquals("Early May Bank Holiday", upcoming[0].name)
        assertEquals(LocalDate.of(2025, 5, 5), upcoming[0].date)
        assertEquals("Spring Bank Holiday", upcoming[1].name)
        assertEquals(LocalDate.of(2025, 5, 26), upcoming[1].date)
    }

    private fun mockContext(): Context {
        return object : ContextWrapper(null) {
            override fun getApplicationContext(): Context = this
        }
    }

    private class FakeBinRepository : BinRepository {
        var clearAllBinsCalled = false
        var onboardingCompletedState = true
        var notificationSettingsState = NotificationSettings(reminderEnabled = true)
        var themeModeState = AppThemeMode.SYSTEM

        private val sampleBin = Bin(
            id = 1,
            name = "General Waste",
            colorHex = BinColor.BLACK.defaultHex,
            presetColor = BinColor.BLACK,
            recurrence = RecurrenceType.FORTNIGHTLY,
            startDate = LocalDate.of(2025, 5, 5),
            isEnabled = true,
            adjustForBankHolidays = true
        )

        private val binsFlow = MutableStateFlow(listOf(sampleBin))
        private val settingsFlow = MutableStateFlow(notificationSettingsState)
        private val themeModeFlow = MutableStateFlow(themeModeState)
        private val onboardingFlow = MutableStateFlow(onboardingCompletedState)

        override val allBins: Flow<List<Bin>> = binsFlow
        override suspend fun getBinsList(): List<Bin> = binsFlow.value
        override fun getBin(id: Long): Flow<Bin?> = flowOf(sampleBin)
        override suspend fun getBinSync(id: Long): Bin? = sampleBin
        override suspend fun insertBin(bin: Bin): Long = 1L
        override suspend fun updateBin(bin: Bin) {}
        override suspend fun deleteBin(bin: Bin) {}

        override suspend fun clearAllBins() {
            clearAllBinsCalled = true
            binsFlow.value = emptyList()
        }

        override suspend fun ensureDefaultBinsInitialized() {}

        var restoreStandardBinsCalled = false
        override suspend fun restoreStandardBins() {
            restoreStandardBinsCalled = true
        }

        override val notificationSettings: Flow<NotificationSettings> = settingsFlow

        override suspend fun updateNotificationSettings(settings: NotificationSettings) {
            notificationSettingsState = settings
            settingsFlow.value = settings
        }

        private val putOutBinsFlow = MutableStateFlow<Set<String>>(emptySet())
        override val putOutBins: Flow<Set<String>> = putOutBinsFlow

        override suspend fun updatePutOutBins(binIds: Set<String>) {
            putOutBinsFlow.value = binIds
        }

        override val themeMode: Flow<AppThemeMode> = themeModeFlow

        override suspend fun setThemeMode(themeMode: AppThemeMode) {
            themeModeState = themeMode
            themeModeFlow.value = themeMode
            notificationSettingsState = notificationSettingsState.copy(themeMode = themeMode)
            settingsFlow.value = notificationSettingsState
        }

        override val onboardingCompleted: Flow<Boolean> = onboardingFlow

        override suspend fun setOnboardingCompleted(completed: Boolean) {
            onboardingCompletedState = completed
            onboardingFlow.value = completed
        }

        override suspend fun completeOnboardingSetup(
            postcodeOrCouncil: String,
            primaryDay: DayOfWeek,
            binSetups: List<OnboardingBinSetup>,
            notificationSettings: NotificationSettings
        ) {
            setOnboardingCompleted(true)
        }

        override fun getUpcomingCollectionEvents(
            startDate: LocalDate,
            endDate: LocalDate
        ): Flow<List<CollectionEvent>> = flowOf(emptyList())
    }
}
