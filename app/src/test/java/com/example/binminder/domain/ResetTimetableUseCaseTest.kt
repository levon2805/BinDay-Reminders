package com.example.binminder.domain

import com.example.binminder.data.model.AppThemeMode
import com.example.binminder.data.model.Bin
import com.example.binminder.data.model.CollectionEvent
import com.example.binminder.data.model.NotificationSettings
import com.example.binminder.data.model.OnboardingBinSetup
import com.example.binminder.data.repository.BinRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class ResetTimetableUseCaseTest {

    @Test
    fun testResetTimetableClearsBinsAndResetsOnboarding() = runTest {
        var clearAllBinsCalled = false
        var onboardingCompletedValue: Boolean? = true

        val fakeRepo = object : FakeBinRepository() {
            override suspend fun clearAllBins() {
                clearAllBinsCalled = true
            }

            override suspend fun setOnboardingCompleted(completed: Boolean) {
                onboardingCompletedValue = completed
            }
        }

        val useCase = ResetTimetableUseCase(fakeRepo)
        val result = useCase()

        assertTrue(result.isSuccess)
        assertTrue(clearAllBinsCalled)
        assertFalse(onboardingCompletedValue!!)
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
