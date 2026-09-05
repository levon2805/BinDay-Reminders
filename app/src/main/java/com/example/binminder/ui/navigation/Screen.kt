package com.example.binminder.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * Sealed interface defining navigation route destinations in the application.
 */
@Serializable
sealed interface Screen : NavKey {
    /**
     * Route for first-time onboarding setup screen.
     */
    @Serializable
    data object Onboarding : Screen

    /**
     * Route for collection timetable dashboard screen.
     */
    @Serializable
    data object Dashboard : Screen

    /**
     * Route for bin collection list management screen.
     */
    @Serializable
    data object BinList : Screen

    /**
     * Route for adding or editing a specific bin screen.
     */
    @Serializable
    data class AddEditBin(val binId: Long? = null) : Screen

    /**
     * Route for application settings and preferences screen.
     */
    @Serializable
    data object Settings : Screen
}

/**
 * Enum defining bottom navigation bar destinations with icons and labels.
 */
enum class BottomNavItem(
    val screen: Screen,
    val title: String,
    val icon: ImageVector
) {
    TIMETABLE(Screen.Dashboard, "Timetable", Icons.Rounded.Schedule),
    BINS(Screen.BinList, "Wheelie Bins", Icons.Rounded.Delete),
    SETTINGS(Screen.Settings, "Settings", Icons.Rounded.Settings)
}
