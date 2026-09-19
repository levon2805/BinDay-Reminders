package com.example.binminder.ui.addedit

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

@OptIn(ExperimentalCoroutinesApi::class)
class AddEditBinViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakeBinRepository
    private lateinit var viewModel: AddEditBinViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeBinRepository()
        viewModel = AddEditBinViewModel(fakeRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testInitialStateNewBin() {
        viewModel.loadBin(null)
        val state = viewModel.uiState.value

        assertNull(state.binId)
        assertEquals("", state.name)
        assertEquals(BinColor.BLACK, state.presetColor)
        assertNull(state.lidPresetColor)
        assertEquals(RecurrenceType.FORTNIGHTLY, state.recurrence)
        assertTrue(state.adjustForBankHolidays)
        assertEquals("Standard black wheelie bin for waste", state.customNote)
    }

    @Test
    fun testDynamicNotesUpdate_OnNameChange() {
        viewModel.loadBin(null)
        viewModel.onNameChange("General Waste")

        assertEquals("General Waste", viewModel.uiState.value.name)
        assertEquals("Standard black wheelie bin for general waste", viewModel.uiState.value.customNote)

        viewModel.onNameChange("Recycling Box")
        assertEquals("Recycling Box", viewModel.uiState.value.name)
        assertEquals("Standard black wheelie bin for recycling box", viewModel.uiState.value.customNote)
    }

    @Test
    fun testDynamicNotesUpdate_OnColorAndLidChange() {
        viewModel.loadBin(null)
        viewModel.onNameChange("Dry Mixed Recycling")

        viewModel.onPresetColorSelected(BinColor.BLUE)
        assertEquals(BinColor.BLUE, viewModel.uiState.value.presetColor)
        assertEquals("Standard blue wheelie bin for dry mixed recycling", viewModel.uiState.value.customNote)

        viewModel.onLidPresetColorSelected(BinColor.RED)
        assertEquals(BinColor.RED, viewModel.uiState.value.lidPresetColor)
        assertEquals("Standard blue wheelie bin with red lid for dry mixed recycling", viewModel.uiState.value.customNote)
    }

    @Test
    fun testPreserveCustomUserNote_OnColorOrNameChange() {
        viewModel.loadBin(null)
        viewModel.onNameChange("General Waste")
        viewModel.onCustomNoteChange("Leave by side gate next to garage")

        viewModel.onPresetColorSelected(BinColor.GREEN)
        viewModel.onLidPresetColorSelected(BinColor.YELLOW)
        viewModel.onNameChange("Garden Waste")

        // Custom note typed by user should be preserved!
        assertEquals("Leave by side gate next to garage", viewModel.uiState.value.customNote)
    }

    @Test
    fun testColorPickerIntegration_CustomHexChange() {
        viewModel.loadBin(null)
        viewModel.onNameChange("Custom Bin")

        // Colour picker applies custom body hex matching BLUE
        viewModel.onCustomHexChange("#1E88E5")
        assertEquals("#1E88E5", viewModel.uiState.value.colorHex)
        assertEquals(BinColor.BLUE, viewModel.uiState.value.presetColor)
        assertEquals("Standard blue wheelie bin for custom bin", viewModel.uiState.value.customNote)

        // Colour picker applies custom lid hex matching RED
        viewModel.onCustomLidHexChange("#D32F2F")
        assertEquals("#D32F2F", viewModel.uiState.value.lidColorHex)
        assertEquals(BinColor.RED, viewModel.uiState.value.lidPresetColor)
        assertEquals("Standard blue wheelie bin with red lid for custom bin", viewModel.uiState.value.customNote)
    }

    @Test
    fun testSaveBinSuccess() = runTest {
        viewModel.loadBin(null)
        viewModel.onNameChange("Cardboard Caddy")
        viewModel.onPresetColorSelected(BinColor.BLUE)

        viewModel.saveBin()

        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isSaved)
        assertFalse(viewModel.uiState.value.isLoading)
        assertNotNull(fakeRepository.insertedBin)
        assertEquals("Cardboard Caddy", fakeRepository.insertedBin?.name)
        assertEquals(BinColor.BLUE, fakeRepository.insertedBin?.presetColor)
    }

    @Test
    fun testSaveBinValidationErrorWhenNameEmpty() {
        viewModel.loadBin(null)
        viewModel.onNameChange("   ")

        viewModel.saveBin()

        assertFalse(viewModel.uiState.value.isSaved)
        assertEquals("Bin name cannot be empty.", viewModel.uiState.value.errorMessage)
    }

    private class FakeBinRepository : BinRepository {
        var insertedBin: Bin? = null

        override val allBins: Flow<List<Bin>> = flowOf(emptyList())
        override suspend fun getBinsList(): List<Bin> = emptyList()
        override fun getBin(id: Long): Flow<Bin?> = flowOf(null)
        override suspend fun getBinSync(id: Long): Bin? = null

        override suspend fun insertBin(bin: Bin): Long {
            insertedBin = bin
            return 1L
        }

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
        ) {}

        override fun getUpcomingCollectionEvents(
            startDate: LocalDate,
            endDate: LocalDate
        ): Flow<List<CollectionEvent>> = flowOf(emptyList())
    }
}
