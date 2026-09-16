package com.example.binminder.ui.navigation

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.binminder.di.AppContainer
import com.example.binminder.ui.addedit.AddEditBinScreen
import com.example.binminder.ui.addedit.AddEditBinViewModel
import com.example.binminder.ui.bins.BinListScreen
import com.example.binminder.ui.bins.BinListViewModel
import com.example.binminder.ui.dashboard.DashboardScreen
import com.example.binminder.ui.dashboard.DashboardViewModel
import com.example.binminder.ui.factory.ViewModelFactory
import com.example.binminder.ui.onboarding.OnboardingScreen
import com.example.binminder.ui.onboarding.OnboardingViewModel
import com.example.binminder.ui.settings.SettingsScreen
import com.example.binminder.ui.settings.SettingsViewModel

/**
 * Main scaffold hosting navigation display and bottom navigation bar.
 * 
 * Directs users between the timetable dashboard, bin management list, settings,
 * and the first-time onboarding wizard.
 */
@Composable
fun MainScreen(
    appContainer: AppContainer,
    modifier: Modifier = Modifier
) {
    val repository = appContainer.binRepository
    val onboardingCompletedState by repository.onboardingCompleted.collectAsStateWithLifecycle(initialValue = null)

    when (val completed = onboardingCompletedState) {
        null -> {
            // Render a clean loading box / splash indicator while resolving onboarding status
            Box(
                modifier = modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
        else -> {
            val startDestination = if (completed) Screen.Dashboard else Screen.Onboarding
            MainNavigationContent(
                appContainer = appContainer,
                startDestination = startDestination,
                modifier = modifier
            )
        }
    }
}

@Composable
private fun MainNavigationContent(
    appContainer: AppContainer,
    startDestination: Screen,
    modifier: Modifier = Modifier
) {
    val repository = appContainer.binRepository
    val factory = remember(appContainer) { ViewModelFactory(appContainer) }
    val backStack = rememberNavBackStack(startDestination)

    LaunchedEffect(startDestination) {
        runCatching {
            if (startDestination == Screen.Dashboard) {
                repository.ensureDefaultBinsInitialized()
            }
        }.onFailure { exception ->
            Log.e("BinDay", "Error initializing default bins in MainScreen", exception)
        }
    }

    val currentScreen = backStack.lastOrNull() ?: startDestination
    val isBottomBarVisible = currentScreen !is Screen.AddEditBin && currentScreen !is Screen.Onboarding

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (isBottomBarVisible) {
                NavigationBar {
                    BottomNavItem.entries.forEach { item ->
                        val isSelected = currentScreen == item.screen
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = {
                                try {
                                    if (!isSelected) {
                                        backStack.clear()
                                        backStack.add(item.screen)
                                    }
                                } catch (exception: Exception) {
                                    Log.e("BinDay", "Error navigating to ${item.title} in MainScreen", exception)
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
                try {
                    if (backStack.size > 1) {
                        backStack.removeLastOrNull()
                    }
                } catch (exception: Exception) {
                    Log.e("BinDay", "Error during onBack in MainScreen NavDisplay", exception)
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
                            try {
                                backStack.clear()
                                backStack.add(Screen.Dashboard)
                            } catch (exception: Exception) {
                                Log.e("BinDay", "Error navigating to Dashboard on onboarding complete", exception)
                            }
                        }
                    )
                }

                entry<Screen.Dashboard> {
                    val dashboardViewModel: DashboardViewModel = viewModel(factory = factory)
                    DashboardScreen(
                        viewModel = dashboardViewModel,
                        onNavigateToAddBin = {
                            try {
                                backStack.add(Screen.AddEditBin())
                            } catch (exception: Exception) {
                                Log.e("BinDay", "Error navigating to AddBin", exception)
                            }
                        },
                        onNavigateToBinDetail = { binId ->
                            try {
                                backStack.add(Screen.AddEditBin(binId))
                            } catch (exception: Exception) {
                                Log.e("BinDay", "Error navigating to BinDetail", exception)
                            }
                        },
                        onNavigateToSettings = {
                            try {
                                backStack.clear()
                                backStack.add(Screen.Settings)
                            } catch (exception: Exception) {
                                Log.e("BinDay", "Error navigating to Settings", exception)
                            }
                        }
                    )
                }

                entry<Screen.BinList> {
                    val binListViewModel: BinListViewModel = viewModel(factory = factory)
                    BinListScreen(
                        viewModel = binListViewModel,
                        onNavigateToAddBin = {
                            try {
                                backStack.add(Screen.AddEditBin())
                            } catch (exception: Exception) {
                                Log.e("BinDay", "Error navigating to AddBin from BinList", exception)
                            }
                        },
                        onNavigateToEditBin = { binId ->
                            try {
                                backStack.add(Screen.AddEditBin(binId))
                            } catch (exception: Exception) {
                                Log.e("BinDay", "Error navigating to EditBin from BinList", exception)
                            }
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
                            try {
                                if (backStack.size > 1) {
                                    backStack.removeLastOrNull()
                                } else {
                                    backStack.clear()
                                    backStack.add(Screen.BinList)
                                }
                            } catch (exception: Exception) {
                                Log.e("BinDay", "Error navigating back from AddEditBin", exception)
                            }
                        }
                    )
                }

                entry<Screen.Settings> {
                    val settingsViewModel: SettingsViewModel = viewModel(factory = factory)
                    SettingsScreen(
                        viewModel = settingsViewModel,
                        onReRunSetupWizard = {
                            try {
                                backStack.clear()
                                backStack.add(Screen.Onboarding)
                            } catch (exception: Exception) {
                                Log.e("BinDay", "Error re-running setup wizard from Settings", exception)
                            }
                        }
                    )
                }
            }
        )
    }
}
