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
        assertNull(state.primaryCollectionDay)
        assertEquals(4, state.binSetups.size)
        assertFalse(state.isCompleted)
        assertFalse(state.isSearchingPostcode)
        assertNull(state.detectedCouncil)
        assertNull(state.searchError)
    }

    @Test
    fun testResetStateResetsToStep1() {
        viewModel.goToStep(4)
        assertEquals(4, viewModel.uiState.value.currentStep)

        viewModel.resetState()

        val state = viewModel.uiState.value
        assertEquals(1, state.currentStep)
        assertEquals("", state.postcodeOrCouncil)
        assertNull(state.primaryCollectionDay)
        assertFalse(state.isCompleted)
    }

    @Test
    fun testInitialDayIsNullRequiresSelection() {
        assertNull(viewModel.uiState.value.primaryCollectionDay)
        assertFalse("Should not be able to advance from Step 2 without day selection",
            viewModel.uiState.value.primaryCollectionDay != null)
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
    fun testCanAdvanceFromStep2RequiresDay() {
        viewModel.goToStep(2)

        assertFalse("Cannot advance without day selected", viewModel.canAdvanceFromCurrentStep())

        viewModel.setPrimaryCollectionDay(DayOfWeek.WEDNESDAY)

        assertTrue("Can advance after day selected", viewModel.canAdvanceFromCurrentStep())
    }

    @Test
    fun testCanAdvanceFromOtherStepsAlwaysTrue() {
        // Step 1 — always advanceable
        assertEquals(1, viewModel.uiState.value.currentStep)
        assertTrue(viewModel.canAdvanceFromCurrentStep())

        // Step 3 — always advanceable
        viewModel.goToStep(3)
        assertTrue(viewModel.canAdvanceFromCurrentStep())

        // Step 4 — always advanceable
        viewModel.goToStep(4)
        assertTrue(viewModel.canAdvanceFromCurrentStep())
    }

    @Test
    fun testSearchPostcodeIdentifiesCouncil() = runTest {
        viewModel.setPostcodeOrCouncil("M1 1AE")
        viewModel.searchPostcodeTimetable()

        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isSearchingPostcode)
        assertNull(state.searchError)
        assertNotNull(state.detectedCouncil)
        assertEquals("Manchester City Council", state.detectedCouncil?.councilName)
        assertEquals("Manchester City Council", state.postcodeOrCouncil)
    }

    @Test
    fun testSearchPostcodeDoesNotPopulateBinsOrDay() = runTest {
        viewModel.setPostcodeOrCouncil("M1 1AE")
        viewModel.searchPostcodeTimetable()

        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        // Day should remain null — user must set it themselves
        assertNull(state.primaryCollectionDay)
        // Bins should still be the defaults, not overwritten by lookup
        assertEquals(4, state.binSetups.size)
        assertEquals("General Waste", state.binSetups[0].binType)
        assertEquals("Dry Mixed Recycling", state.binSetups[1].binType)
        assertEquals("Garden Waste", state.binSetups[2].binType)
        assertEquals("Food Waste Caddy", state.binSetups[3].binType)
    }

    @Test
    fun testSearchPostcodeFailure() = runTest {
        fakeCouncilRepository.shouldSucceed = false
        viewModel.setPostcodeOrCouncil("INVALID_POSTCODE")
        viewModel.searchPostcodeTimetable()

        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isSearchingPostcode)
        assertNull(state.detectedCouncil)
        assertEquals("Could not identify your council. You can still set up your bins manually.", state.searchError)
    }

    @Test
    fun testCouncilWebSearchUrl() = runTest {
        viewModel.setPostcodeOrCouncil("M1 1AE")
        viewModel.searchPostcodeTimetable()

        testDispatcher.scheduler.advanceUntilIdle()

        val url = viewModel.getCouncilWebSearchUrl()
        assertNotNull(url)
        assertTrue("URL should be a Google search", url!!.startsWith("https://www.google.com/search?q="))
        assertTrue("URL should mention the council", url.contains("Manchester"))
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
    fun testSetBinColorCustomisation() {
        viewModel.setBinColor("Garden Waste", BinColor.PURPLE)
        val gardenBin = viewModel.uiState.value.binSetups.first { it.binType == "Garden Waste" }
        assertEquals(BinColor.PURPLE, gardenBin.presetColor)
        assertEquals("Standard purple wheelie bin for garden waste", gardenBin.customNote)
    }

    @Test
    fun testSetBinLidColorCustomisation() {
        viewModel.setBinLidColor("General Waste", BinColor.RED)
        val generalBin = viewModel.uiState.value.binSetups.first { it.binType == "General Waste" }
        assertEquals(BinColor.RED, generalBin.lidPresetColor)
        assertEquals("Standard black wheelie bin with red lid for general waste", generalBin.customNote)
    }

    @Test
    fun testDynamicNotesSync_OnBodyAndLidColourChange() {
        // Change body colour to BLACK and lid colour to BLUE for Dry Mixed Recycling
        viewModel.setBinColor("Dry Mixed Recycling", BinColor.BLACK)
        viewModel.setBinLidColor("Dry Mixed Recycling", BinColor.BLUE)

        val recyclingBin = viewModel.uiState.value.binSetups.first { it.binType == "Dry Mixed Recycling" }
        assertEquals("Standard black wheelie bin with blue lid for dry mixed recycling", recyclingBin.customNote)
    }

    @Test
    fun testEditableBinNamesInSetupWizard() {
        viewModel.renameBin("Food Waste Caddy", "Food Waste Bin")
        val foodBin = viewModel.uiState.value.binSetups.first { it.binType == "Food Waste Caddy" }
        assertEquals("Food Waste Bin", foodBin.displayName)
        assertEquals("Standard brown wheelie bin for food waste bin", foodBin.customNote)

        viewModel.renameBin("Dry Mixed Recycling", "Recycling Box")
        val recyclingBin = viewModel.uiState.value.binSetups.first { it.binType == "Dry Mixed Recycling" }
        assertEquals("Recycling Box", recyclingBin.displayName)
        assertEquals("Standard blue wheelie bin for recycling box", recyclingBin.customNote)
    }

    @Test
    fun testSetFortnightlyThisWeekBinSwapsCycle() {
        // Default bin setups:
        // General Waste: FORTNIGHTLY, startNextWeek = false
        // Dry Mixed Recycling: FORTNIGHTLY, startNextWeek = true
        // Garden Waste: FORTNIGHTLY, startNextWeek = false
        // Food Waste Caddy: WEEKLY, startNextWeek = false

        val initialGeneral = viewModel.uiState.value.binSetups.first { it.binType == "General Waste" }
        val initialRecycling = viewModel.uiState.value.binSetups.first { it.binType == "Dry Mixed Recycling" }
        assertFalse(initialGeneral.startNextWeek)
        assertTrue(initialRecycling.startNextWeek)

        // User selects Recycling to go out THIS week
        viewModel.setFortnightlyThisWeekBin("Dry Mixed Recycling")

        val updatedGeneral = viewModel.uiState.value.binSetups.first { it.binType == "General Waste" }
        val updatedRecycling = viewModel.uiState.value.binSetups.first { it.binType == "Dry Mixed Recycling" }

        assertTrue("General Waste should now start next week", updatedGeneral.startNextWeek)
        assertFalse("Recycling should now start this week", updatedRecycling.startNextWeek)

        // Selecting Recycling again (already this week) should leave cycle unchanged
        viewModel.setFortnightlyThisWeekBin("Dry Mixed Recycling")
        assertFalse(viewModel.uiState.value.binSetups.first { it.binType == "Dry Mixed Recycling" }.startNextWeek)
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
    fun testEveningAndMorningReminderTimeUpdates() {
        val eveningTime = LocalTime.of(20, 0)
        viewModel.updateEveningReminderTime(eveningTime)
        assertEquals(eveningTime, viewModel.uiState.value.eveningReminderTime)

        val morningTime = LocalTime.of(7, 30)
        viewModel.updateMorningReminderTime(morningTime)
        assertEquals(morningTime, viewModel.uiState.value.morningReminderTime)

        viewModel.setReminderEnabled(false)
        assertFalse(viewModel.uiState.value.reminderEnabled)
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

    @Test
    fun testCompleteSetupDefaultsDayToMondayIfNull() = runTest {
        viewModel.setPostcodeOrCouncil("Test Council")
        // Don't set a day — should default to Monday

        var callbackCalled = false
        viewModel.completeSetup {
            callbackCalled = true
        }

        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(callbackCalled)
        assertEquals(DayOfWeek.MONDAY, fakeRepository.savedPrimaryDay)
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
                        councilWebSearchUrl = "https://www.google.com/search?q=Manchester+City+Council+bin+collection+schedule"
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
        override suspend fun restoreStandardBins() {}

        override val notificationSettings: Flow<NotificationSettings> = flowOf(NotificationSettings())
        override suspend fun updateNotificationSettings(settings: NotificationSettings) {}
        override val putOutBins: Flow<Set<String>> = flowOf(emptySet())
        override suspend fun updatePutOutBins(binIds: Set<String>) {}

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
