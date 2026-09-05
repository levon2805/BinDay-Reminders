package com.example.binminder.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.binminder.data.model.AppThemeMode
import com.example.binminder.data.model.NotificationSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalTime

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "notification_settings")

class NotificationSettingsDataStore(context: Context) {

    private val applicationContext = context.applicationContext

    private object Keys {
        val REMINDER_ENABLED = booleanPreferencesKey("reminder_enabled")
        val REMINDER_HOUR = intPreferencesKey("reminder_hour")
        val REMINDER_MINUTE = intPreferencesKey("reminder_minute")
        val REMINDER_EVENING_BEFORE = booleanPreferencesKey("reminder_evening_before")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val POSTCODE_OR_COUNCIL = stringPreferencesKey("postcode_or_council")
        val PRIMARY_COLLECTION_DAY = stringPreferencesKey("primary_collection_day")
        val THEME_MODE = stringPreferencesKey("theme_mode")
    }

    val notificationSettings: Flow<NotificationSettings> = applicationContext.dataStore.data.map { prefs ->
        val enabled = prefs[Keys.REMINDER_ENABLED] ?: true
        val hour = prefs[Keys.REMINDER_HOUR] ?: 19
        val minute = prefs[Keys.REMINDER_MINUTE] ?: 0
        val eveningBefore = prefs[Keys.REMINDER_EVENING_BEFORE] ?: true
        val themeModeRaw = prefs[Keys.THEME_MODE]
        val mode = themeModeRaw?.let { runCatching { AppThemeMode.valueOf(it) }.getOrNull() } ?: AppThemeMode.SYSTEM

        NotificationSettings(
            reminderEnabled = enabled,
            reminderTime = LocalTime.of(hour, minute),
            reminderEveningBefore = eveningBefore,
            themeMode = mode
        )
    }

    val themeMode: Flow<AppThemeMode> = applicationContext.dataStore.data.map { prefs ->
        val raw = prefs[Keys.THEME_MODE]
        raw?.let { runCatching { AppThemeMode.valueOf(it) }.getOrNull() } ?: AppThemeMode.SYSTEM
    }

    val onboardingCompleted: Flow<Boolean> = applicationContext.dataStore.data.map { prefs ->
        prefs[Keys.ONBOARDING_COMPLETED] ?: false
    }

    val postcodeOrCouncil: Flow<String> = applicationContext.dataStore.data.map { prefs ->
        prefs[Keys.POSTCODE_OR_COUNCIL] ?: ""
    }

    val primaryCollectionDay: Flow<String> = applicationContext.dataStore.data.map { prefs ->
        prefs[Keys.PRIMARY_COLLECTION_DAY] ?: "MONDAY"
    }

    suspend fun setThemeMode(themeMode: AppThemeMode) {
        applicationContext.dataStore.edit { prefs ->
            prefs[Keys.THEME_MODE] = themeMode.name
        }
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        applicationContext.dataStore.edit { prefs ->
            prefs[Keys.ONBOARDING_COMPLETED] = completed
        }
    }

    suspend fun saveOnboardingInfo(postcodeOrCouncil: String, primaryDay: String) {
        applicationContext.dataStore.edit { prefs ->
            prefs[Keys.POSTCODE_OR_COUNCIL] = postcodeOrCouncil
            prefs[Keys.PRIMARY_COLLECTION_DAY] = primaryDay
        }
    }

    suspend fun updateSettings(settings: NotificationSettings) {
        applicationContext.dataStore.edit { prefs ->
            prefs[Keys.REMINDER_ENABLED] = settings.reminderEnabled
            prefs[Keys.REMINDER_HOUR] = settings.reminderTime.hour
            prefs[Keys.REMINDER_MINUTE] = settings.reminderTime.minute
            prefs[Keys.REMINDER_EVENING_BEFORE] = settings.reminderEveningBefore
            prefs[Keys.THEME_MODE] = settings.themeMode.name
        }
    }
}
