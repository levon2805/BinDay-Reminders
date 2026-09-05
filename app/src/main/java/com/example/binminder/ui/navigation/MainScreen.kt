package com.example.binminder.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.binminder.data.repository.BinRepository
import com.example.binminder.ui.addedit.AddEditBinScreen
import com.example.binminder.ui.addedit.AddEditBinViewModel
import com.example.binminder.ui.bins.BinListScreen
import com.example.binminder.ui.bins.BinListViewModel
import com.example.binminder.ui.dashboard.DashboardScreen
import com.example.binminder.ui.dashboard.DashboardViewModel
import com.example.binminder.ui.factory.ViewModelFactory
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.binminder.ui.onboarding.OnboardingScreen
import com.example.binminder.ui.onboarding.OnboardingViewModel
import com.example.binminder.ui.settings.SettingsScreen
import com.example.binminder.ui.settings.SettingsViewModel

@Composable
fun MainScreen(
    repository: BinRepository,
    modifier: Modifier = Modifier
) {
    val factory = ViewModelFactory(repository)
    val backStack = rememberNavBackStack(Screen.Dashboard)

    val onboardingCompleted by repository.onboardingCompleted.collectAsState(initial = null)

    LaunchedEffect(onboardingCompleted) {
        if (onboardingCompleted == false) {
            if (backStack.lastOrNull() != Screen.Onboarding) {
                backStack.clear()
                backStack.add(Screen.Onboarding)
            }
        } else if (onboardingCompleted == true) {
            repository.ensureDefaultBinsInitialized()
        }
    }

    val currentScreen = backStack.lastOrNull() ?: Screen.Dashboard
    val isBottomBarVisible = currentScreen !is Screen.AddEditBin && currentScreen !is Screen.Onboarding

    Scaffold(
        bottomBar = {
            if (isBottomBarVisible) {
                NavigationBar {
                    BottomNavItem.entries.forEach { item ->
                        val isSelected = currentScreen == item.screen
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = {
                                if (!isSelected) {
                                    backStack.clear()
                                    backStack.add(item.screen)
                                }
                            },
                            icon = { Icon(imageVector = item.icon, contentDescription = item.title) },
                            label = { Text(text = item.title) }
                        )
                    }
                }
            }
        },
        modifier = modifier
    ) { innerPadding ->
        NavDisplay(
            backStack = backStack,
            onBack = {
                if (backStack.size > 1) {
                    backStack.removeLastOrNull()
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            entryProvider = entryProvider {
                entry<Screen.Onboarding> {
                    val onboardingViewModel: OnboardingViewModel = viewModel(factory = factory)
                    OnboardingScreen(
                        viewModel = onboardingViewModel,
                        onOnboardingComplete = {
                            backStack.clear()
                            backStack.add(Screen.Dashboard)
                        }
                    )
                }

                entry<Screen.Dashboard> {
                    val dashboardViewModel: DashboardViewModel = viewModel(factory = factory)
                    DashboardScreen(
                        viewModel = dashboardViewModel,
                        onNavigateToAddBin = {
                            backStack.add(Screen.AddEditBin())
                        },
                        onNavigateToBinDetail = { binId ->
                            backStack.add(Screen.AddEditBin(binId))
                        },
                        onNavigateToSettings = {
                            backStack.clear()
                            backStack.add(Screen.Settings)
                        }
                    )
                }

                entry<Screen.BinList> {
                    val binListViewModel: BinListViewModel = viewModel(factory = factory)
                    BinListScreen(
                        viewModel = binListViewModel,
                        onNavigateToAddBin = {
                            backStack.add(Screen.AddEditBin())
                        },
                        onNavigateToEditBin = { binId ->
                            backStack.add(Screen.AddEditBin(binId))
                        }
                    )
                }

                entry<Screen.AddEditBin> { key ->
                    val addEditViewModel: AddEditBinViewModel = viewModel(
                        key = "add_edit_${key.binId ?: 0L}",
                        factory = factory
                    )
                    AddEditBinScreen(
                        viewModel = addEditViewModel,
                        binId = key.binId,
                        onNavigateBack = {
                            if (backStack.size > 1) {
                                backStack.removeLastOrNull()
                            } else {
                                backStack.clear()
                                backStack.add(Screen.BinList)
                            }
                        }
                    )
                }

                entry<Screen.Settings> {
                    val settingsViewModel: SettingsViewModel = viewModel(factory = factory)
                    SettingsScreen(
                        viewModel = settingsViewModel,
                        onReRunSetupWizard = {
                            settingsViewModel.resetOnboarding {
                                backStack.clear()
                                backStack.add(Screen.Onboarding)
                            }
                        }
                    )
                }
            }
        )
    }
}
