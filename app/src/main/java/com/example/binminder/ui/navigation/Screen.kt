package com.example.binminder.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
sealed interface Screen : NavKey {
    @Serializable
    data object Onboarding : Screen

    @Serializable
    data object Dashboard : Screen

    @Serializable
    data object BinList : Screen

    @Serializable
    data class AddEditBin(val binId: Long? = null) : Screen

    @Serializable
    data object Settings : Screen
}

enum class BottomNavItem(
    val screen: Screen,
    val title: String,
    val icon: ImageVector
) {
    TIMETABLE(Screen.Dashboard, "Timetable", Icons.Rounded.Schedule),
    BINS(Screen.BinList, "Wheelie Bins", Icons.Rounded.Delete),
    SETTINGS(Screen.Settings, "Settings", Icons.Rounded.Settings)
}
