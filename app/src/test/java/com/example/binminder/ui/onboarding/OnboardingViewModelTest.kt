package com.example.binminder.ui.onboarding

import com.example.binminder.data.model.AppThemeMode
import com.example.binminder.data.model.Bin
import com.example.binminder.data.model.BinColor
import com.example.binminder.data.model.CollectionEvent
import com.example.binminder.data.model.CouncilScheduleResult
import com.example.binminder.data.model.NotificationSettings
import com.example.binminder.data.model.OnboardingBinSetup
import com.example.binminder.data.model.RecurrenceType
import com.example.binminder.data.repository.BinRepository
import com.example.binminder.data.repository.CouncilLookupRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakeBinRepository
    private lateinit var fakeCouncilRepository: FakeCouncilLookupRepository
    private lateinit var viewModel: OnboardingViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeBinRepository()
        fakeCouncilRepository = FakeCouncilLookupRepository()
        viewModel = OnboardingViewModel(fakeRepository, fakeCouncilRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testInitialState() {
        val state = viewModel.uiState.value
        assertEquals(1, state.currentStep)
        assertEquals("", state.postcodeOrCouncil)
        assertEquals(DayOfWeek.MONDAY, state.primaryCollectionDay)
        assertEquals(4, state.binSetups.size)
        assertFalse(state.isCompleted)
        assertFalse(state.isSearchingPostcode)
        assertNull(state.detectedSchedule)
        assertNull(state.searchError)
    }

    @Test
    fun testNavigationSteps() {
        viewModel.nextStep()
        assertEquals(2, viewModel.uiState.value.currentStep)

        viewModel.nextStep()
        assertEquals(3, viewModel.uiState.value.currentStep)

        viewModel.previousStep()
        assertEquals(2, viewModel.uiState.value.currentStep)

        viewModel.goToStep(4)
        assertEquals(4, viewModel.uiState.value.currentStep)
    }

    @Test
    fun testSearchPostcodeTimetableSuccess() = runTest {
        viewModel.setPostcodeOrCouncil("M1 1AE")
        viewModel.searchPostcodeTimetable()

        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isSearchingPostcode)
        assertNull(state.searchError)
        assertNotNull(state.detectedSchedule)
        assertEquals("Manchester City Council", state.detectedSchedule?.councilName)
        assertEquals("Manchester City Council", state.postcodeOrCouncil)
        assertEquals(DayOfWeek.TUESDAY, state.primaryCollectionDay)
        assertEquals(4, state.binSetups.size)

        state.binSetups.forEach { bin ->
            assertFalse("Bin display name '${bin.displayName}' should not contain '('", bin.displayName.contains("("))
        }
    }

    @Test
    fun testSearchPostcodeTimetableFailure() = runTest {
        fakeCouncilRepository.shouldSucceed = false
        viewModel.setPostcodeOrCouncil("INVALID_POSTCODE")
        viewModel.searchPostcodeTimetable()

        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isSearchingPostcode)
        assertNull(state.detectedSchedule)
        assertEquals("Could not auto-detect timetable. Switching to guided setup.", state.searchError)
    }

    @Test
    fun testSetPrimaryCollectionDayUpdatesDetectedSchedule() = runTest {
        viewModel.setPostcodeOrCouncil("M1 1AE")
        viewModel.searchPostcodeTimetable()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(DayOfWeek.TUESDAY, viewModel.uiState.value.primaryCollectionDay)
        assertEquals(DayOfWeek.TUESDAY, viewModel.uiState.value.detectedSchedule?.primaryCollectionDay)

        viewModel.setPrimaryCollectionDay(DayOfWeek.THURSDAY)

        assertEquals(DayOfWeek.THURSDAY, viewModel.uiState.value.primaryCollectionDay)
        assertEquals(DayOfWeek.THURSDAY, viewModel.uiState.value.detectedSchedule?.primaryCollectionDay)
    }

    @Test
    fun testAcceptAndApplyDetectedTimetableWithCustomDay() = runTest {
        viewModel.setPostcodeOrCouncil("M1 1AE")
        viewModel.searchPostcodeTimetable()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.setPrimaryCollectionDay(DayOfWeek.THURSDAY)

        var onCompleteCalled = false
        viewModel.acceptAndApplyDetectedTimetable {
            onCompleteCalled = true
        }

        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(onCompleteCalled)
        assertTrue(fakeRepository.completeSetupCalled)
        assertEquals("Manchester City Council", fakeRepository.savedPostcode)
        assertEquals(DayOfWeek.THURSDAY, fakeRepository.savedPrimaryDay)
    }

    @Test
    fun testContinueToGuidedSetup() {
        viewModel.continueToGuidedSetup()

        val state = viewModel.uiState.value
        assertNull(state.searchError)
        assertEquals(2, state.currentStep)
    }

    @Test
    fun testLocationAndCollectionDayUpdates() {
        viewModel.setPostcodeOrCouncil("SW1A 1AA")
        assertEquals("SW1A 1AA", viewModel.uiState.value.postcodeOrCouncil)

        viewModel.setPrimaryCollectionDay(DayOfWeek.WEDNESDAY)
        assertEquals(DayOfWeek.WEDNESDAY, viewModel.uiState.value.primaryCollectionDay)
    }

    @Test
    fun testBinSetupUpdates() {
        viewModel.toggleBinEnabled("Garden Waste")
        val gardenBin = viewModel.uiState.value.binSetups.first { it.binType == "Garden Waste" }
        assertFalse(gardenBin.isEnabled)

        viewModel.setBinRecurrence("General Waste", RecurrenceType.WEEKLY)
        val generalBin = viewModel.uiState.value.binSetups.first { it.binType == "General Waste" }
        assertEquals(RecurrenceType.WEEKLY, generalBin.recurrence)

        viewModel.setBinStartNextWeek("Dry Mixed Recycling", false)
        val recyclingBin = viewModel.uiState.value.binSetups.first { it.binType == "Dry Mixed Recycling" }
        assertFalse(recyclingBin.startNextWeek)
    }

    @Test
    fun testSetBinColorCustomization() {
        viewModel.setBinColor("Garden Waste", BinColor.PURPLE)
        val gardenBin = viewModel.uiState.value.binSetups.first { it.binType == "Garden Waste" }
        assertEquals(BinColor.PURPLE, gardenBin.presetColor)
    }

    @Test
    fun testSetFortnightlyThisWeekBinSwapsCycle() = runTest {
        viewModel.setPostcodeOrCouncil("M1 1AE")
        viewModel.searchPostcodeTimetable()
        testDispatcher.scheduler.advanceUntilIdle()

        // Initial setup from FakeCouncilLookupRepository:
        // General Waste: FORTNIGHTLY, startNextWeek = false
        // Recycling: FORTNIGHTLY, startNextWeek = true
        // Glass & Plastics: FORTNIGHTLY, startNextWeek = true
        // Food & Garden: WEEKLY, startNextWeek = false

        val initialGeneral = viewModel.uiState.value.binSetups.first { it.binType == "General Waste" }
        val initialRecycling = viewModel.uiState.value.binSetups.first { it.binType == "Recycling" }
        assertFalse(initialGeneral.startNextWeek)
        assertTrue(initialRecycling.startNextWeek)

        // User selects Recycling to go out THIS week
        viewModel.setFortnightlyThisWeekBin("Recycling")

        val updatedGeneral = viewModel.uiState.value.binSetups.first { it.binType == "General Waste" }
        val updatedRecycling = viewModel.uiState.value.binSetups.first { it.binType == "Recycling" }
        val updatedGlass = viewModel.uiState.value.binSetups.first { it.binType == "Glass & Plastics" }

        assertTrue("General Waste should now start next week", updatedGeneral.startNextWeek)
        assertFalse("Recycling should now start this week", updatedRecycling.startNextWeek)
        assertFalse("Glass & Plastics should now start this week", updatedGlass.startNextWeek)

        // Selecting Recycling again (already this week) should leave cycle unchanged
        viewModel.setFortnightlyThisWeekBin("Recycling")
        assertFalse(viewModel.uiState.value.binSetups.first { it.binType == "Recycling" }.startNextWeek)
    }

    @Test
    fun testReminderSettingsUpdate() {
        val newTime = LocalTime.of(20, 30)
        viewModel.setReminderSettings(newTime, eveningBefore = true, enabled = true)

        val state = viewModel.uiState.value
        assertEquals(newTime, state.reminderTime)
        assertTrue(state.reminderEveningBefore)
        assertTrue(state.reminderEnabled)
    }

    @Test
    fun testCompleteSetup() = runTest {
        viewModel.setPostcodeOrCouncil("Birmingham City Council")
        viewModel.setPrimaryCollectionDay(DayOfWeek.THURSDAY)

        var callbackCalled = false
        viewModel.completeSetup {
            callbackCalled = true
        }

        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(callbackCalled)
        assertTrue(fakeRepository.completeSetupCalled)
        assertEquals("Birmingham City Council", fakeRepository.savedPostcode)
        assertEquals(DayOfWeek.THURSDAY, fakeRepository.savedPrimaryDay)
    }

    private class FakeCouncilLookupRepository : CouncilLookupRepository {
        var shouldSucceed = true

        override suspend fun lookupPostcode(postcodeOrQuery: String): Result<CouncilScheduleResult> {
            return if (shouldSucceed) {
                Result.success(
                    CouncilScheduleResult(
                        postcode = postcodeOrQuery,
                        councilName = "Manchester City Council",
                        adminDistrict = "Manchester",
                        primaryCollectionDay = DayOfWeek.TUESDAY,
                        binSetups = listOf(
                            OnboardingBinSetup("General Waste", "General Waste", BinColor.GREY, RecurrenceType.FORTNIGHTLY, isEnabled = true, startNextWeek = false),
                            OnboardingBinSetup("Recycling", "Paper & Cardboard", BinColor.BLUE, RecurrenceType.FORTNIGHTLY, isEnabled = true, startNextWeek = true),
                            OnboardingBinSetup("Glass & Plastics", "Glass & Cans", BinColor.BROWN, RecurrenceType.FORTNIGHTLY, isEnabled = true, startNextWeek = true),
                            OnboardingBinSetup("Food & Garden", "Organics", BinColor.GREEN, RecurrenceType.WEEKLY, isEnabled = true, startNextWeek = false)
                        )
                    )
                )
            } else {
                Result.failure(Exception("Failed lookup"))
            }
        }
    }

    private class FakeBinRepository : BinRepository {
        var completeSetupCalled = false
        var savedPostcode = ""
        var savedPrimaryDay: DayOfWeek? = null

        override val allBins: Flow<List<Bin>> = flowOf(emptyList())
        override suspend fun getBinsList(): List<Bin> = emptyList()
        override fun getBin(id: Long): Flow<Bin?> = flowOf(null)
        override suspend fun getBinSync(id: Long): Bin? = null
        override suspend fun insertBin(bin: Bin): Long = 1L
        override suspend fun updateBin(bin: Bin) {}
        override suspend fun deleteBin(bin: Bin) {}
        override suspend fun clearAllBins() {}
        override suspend fun ensureDefaultBinsInitialized() {}

        override val notificationSettings: Flow<NotificationSettings> = flowOf(NotificationSettings())
        override suspend fun updateNotificationSettings(settings: NotificationSettings) {}

        override val themeMode: Flow<AppThemeMode> = flowOf(AppThemeMode.SYSTEM)
        override suspend fun setThemeMode(themeMode: AppThemeMode) {}

        override val onboardingCompleted: Flow<Boolean> = MutableStateFlow(false)
        override suspend fun setOnboardingCompleted(completed: Boolean) {}

        override suspend fun completeOnboardingSetup(
            postcodeOrCouncil: String,
            primaryDay: DayOfWeek,
            binSetups: List<OnboardingBinSetup>,
            notificationSettings: NotificationSettings
        ) {
            completeSetupCalled = true
            savedPostcode = postcodeOrCouncil
            savedPrimaryDay = primaryDay
        }

        override fun getUpcomingCollectionEvents(
            startDate: LocalDate,
            endDate: LocalDate
        ): Flow<List<CollectionEvent>> = flowOf(emptyList())
    }
}
