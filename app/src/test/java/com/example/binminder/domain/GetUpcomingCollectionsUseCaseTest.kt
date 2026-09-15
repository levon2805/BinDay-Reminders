package com.example.binminder.domain

import com.example.binminder.data.model.AppThemeMode
import com.example.binminder.data.model.Bin
import com.example.binminder.data.model.BinColor
import com.example.binminder.data.model.CollectionEvent
import com.example.binminder.data.model.NotificationSettings
import com.example.binminder.data.model.OnboardingBinSetup
import com.example.binminder.data.repository.BinRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class GetUpcomingCollectionsUseCaseTest {

    @Test
    fun testGetUpcomingCollectionsReturnsEventsFromRepository() = runTest {
        val today = LocalDate.of(2025, 6, 1)
        val nextWeek = today.plusDays(7)
        val eventDate = today.plusDays(2)
        val expectedEvent = CollectionEvent(
            binId = 101,
            binName = "General Waste",
            binColorHex = BinColor.BLACK.defaultHex,
            presetColor = BinColor.BLACK,
            collectionDate = eventDate,
            originalDate = eventDate,
            isBankHolidayAdjusted = false,
            customNote = "Put out early"
        )

        val fakeRepo = object : FakeBinRepository() {
            override fun getUpcomingCollectionEvents(
                startDate: LocalDate,
                endDate: LocalDate
            ): Flow<List<CollectionEvent>> {
                return flowOf(listOf(expectedEvent))
            }
        }

        val useCase = GetUpcomingCollectionsUseCase(fakeRepo)
        val result = useCase(today, nextWeek).first()

        assertEquals(1, result.size)
        assertEquals(expectedEvent, result[0])
    }

    private open class FakeBinRepository : BinRepository {
        override val allBins: Flow<List<Bin>> = flowOf(emptyList())
        override suspend fun getBinsList(): List<Bin> = emptyList()
        override fun getBin(id: Long): Flow<Bin?> = flowOf(null)
        override suspend fun getBinSync(id: Long): Bin? = null
        override suspend fun insertBin(bin: Bin): Long = 0L
        override suspend fun updateBin(bin: Bin) {}
        override suspend fun deleteBin(bin: Bin) {}
        override suspend fun clearAllBins() {}
        override suspend fun ensureDefaultBinsInitialized() {}
        override suspend fun restoreStandardBins() {}
        override val notificationSettings: Flow<NotificationSettings> = flowOf(NotificationSettings())
        override suspend fun updateNotificationSettings(settings: NotificationSettings) {}
        override val themeMode: Flow<AppThemeMode> = flowOf(AppThemeMode.SYSTEM)
        override suspend fun setThemeMode(themeMode: AppThemeMode) {}
        override val onboardingCompleted: Flow<Boolean> = flowOf(false)
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
