package com.example.binminder.ui.factory

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.binminder.di.AppContainer
import com.example.binminder.ui.addedit.AddEditBinViewModel
import com.example.binminder.ui.bins.BinListViewModel
import com.example.binminder.ui.dashboard.DashboardViewModel
import com.example.binminder.ui.onboarding.OnboardingViewModel
import com.example.binminder.ui.settings.SettingsViewModel

/**
 * Factory for instantiating ViewModels using clean dependency injection via [AppContainer].
 *
 * Keeps all ViewModels right tidy with their domain use cases and repository singletons!
 */
class ViewModelFactory(
    private val appContainer: AppContainer
) : ViewModelProvider.Factory {

    /**
     * Creates a new instance of the requested ViewModel type using container dependencies.
     */
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return try {
            when {
                modelClass.isAssignableFrom(OnboardingViewModel::class.java) ||
                OnboardingViewModel::class.java.isAssignableFrom(modelClass) -> {
                    OnboardingViewModel(
                        repository = appContainer.binRepository,
                        councilLookupRepository = appContainer.councilLookupRepository,
                        lookupCouncilScheduleUseCase = appContainer.lookupCouncilScheduleUseCase
                    ) as T
                }
                modelClass.isAssignableFrom(DashboardViewModel::class.java) ||
                DashboardViewModel::class.java.isAssignableFrom(modelClass) -> {
                    DashboardViewModel(
                        repository = appContainer.binRepository,
                        getUpcomingCollectionsUseCase = appContainer.getUpcomingCollectionsUseCase,
                        toggleBinPutOutUseCase = appContainer.toggleBinPutOutUseCase
                    ) as T
                }
                modelClass.isAssignableFrom(BinListViewModel::class.java) ||
                BinListViewModel::class.java.isAssignableFrom(modelClass) -> {
                    BinListViewModel(
                        repository = appContainer.binRepository,
                        resetTimetableUseCase = appContainer.resetTimetableUseCase
                    ) as T
                }
                modelClass.isAssignableFrom(AddEditBinViewModel::class.java) ||
                AddEditBinViewModel::class.java.isAssignableFrom(modelClass) -> {
                    AddEditBinViewModel(
                        repository = appContainer.binRepository
                    ) as T
                }
                modelClass.isAssignableFrom(SettingsViewModel::class.java) ||
                SettingsViewModel::class.java.isAssignableFrom(modelClass) -> {
                    SettingsViewModel(
                        repository = appContainer.binRepository,
                        resetTimetableUseCase = appContainer.resetTimetableUseCase
                    ) as T
                }
                else -> {
                    Log.e("BinDay", "Unhandled ViewModel class requested in ViewModelFactory: ${modelClass.name}. Attempting fallback construction.")
                    try {
                        modelClass.getDeclaredConstructor().newInstance()
                    } catch (e: Exception) {
                        Log.e("BinDay", "Reflective instantiation failed for ViewModel class: ${modelClass.name}", e)
                        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}", e)
                    }
                }
            }
        } catch (exception: Exception) {
            Log.e("BinDay", "Error creating ViewModel instance for ${modelClass.name}", exception)
            throw exception
        }
    }
}
