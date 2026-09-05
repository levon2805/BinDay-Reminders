package com.example.binminder.ui.factory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.binminder.data.repository.BinRepository
import com.example.binminder.ui.addedit.AddEditBinViewModel
import com.example.binminder.ui.bins.BinListViewModel
import com.example.binminder.ui.dashboard.DashboardViewModel
import com.example.binminder.ui.onboarding.OnboardingViewModel
import com.example.binminder.ui.settings.SettingsViewModel

/**
 * Factory for creating ViewModels with [BinRepository] dependency.
 */
class ViewModelFactory(
    private val repository: BinRepository,
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(OnboardingViewModel::class.java) -> {
                OnboardingViewModel(repository) as T
            }
            modelClass.isAssignableFrom(DashboardViewModel::class.java) -> {
                DashboardViewModel(repository) as T
            }
            modelClass.isAssignableFrom(BinListViewModel::class.java) -> {
                BinListViewModel(repository) as T
            }
            modelClass.isAssignableFrom(AddEditBinViewModel::class.java) -> {
                AddEditBinViewModel(repository) as T
            }
            modelClass.isAssignableFrom(SettingsViewModel::class.java) -> {
                SettingsViewModel(repository) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
