package com.example.binminder

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.example.binminder.data.local.AppDatabase
import com.example.binminder.data.local.NotificationSettingsDataStore
import com.example.binminder.data.model.AppThemeMode
import com.example.binminder.data.repository.BinRepositoryImpl
import com.example.binminder.ui.navigation.MainScreen
import com.example.binminder.ui.theme.BinMinderTheme
import com.example.binminder.worker.NotificationHelper
import com.example.binminder.worker.NotificationScheduler
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = AppDatabase.getInstance(this)
        val notificationDataStore = NotificationSettingsDataStore(this)
        val repository = BinRepositoryImpl(database.binDao(), notificationDataStore, applicationContext)

        // Initialize Notification channel and schedule reminders
        NotificationHelper.createNotificationChannel(this)
        lifecycleScope.launch {
            val settings = repository.notificationSettings.first()
            NotificationScheduler.scheduleDailyReminder(this@MainActivity, settings)
        }

        setContent {
            val themeMode by repository.themeMode.collectAsState(initial = AppThemeMode.SYSTEM)
            val darkTheme = when (themeMode) {
                AppThemeMode.LIGHT -> false
                AppThemeMode.DARK -> true
                AppThemeMode.SYSTEM -> isSystemInDarkTheme()
            }

            BinMinderTheme(darkTheme = darkTheme) {
                MainScreen(
                    repository = repository,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
