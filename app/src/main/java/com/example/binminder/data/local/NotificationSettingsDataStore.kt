package com.example.binminder.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.binminder.data.model.AppThemeMode
import com.example.binminder.data.model.NotificationSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalTime

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "notification_settings")

/**
 * DataStore manager for persistent user preferences and application state.
 * 
 * Handles notification settings, theme preferences, onboarding state, and council location info.
 */
class NotificationSettingsDataStore(context: Context) {

    private val applicationContext = context.applicationContext

    private object Keys {
        val REMINDER_ENABLED = booleanPreferencesKey("reminder_enabled")
        val EVENING_REMINDER_TIME = stringPreferencesKey("evening_reminder_time")
        val MORNING_REMINDER_TIME = stringPreferencesKey("morning_reminder_time")
        val PUT_OUT_BIN_IDS = stringSetPreferencesKey("put_out_bin_ids")
        val REMINDER_HOUR = intPreferencesKey("reminder_hour")
        val REMINDER_MINUTE = intPreferencesKey("reminder_minute")
        val REMINDER_EVENING_BEFORE = booleanPreferencesKey("reminder_evening_before")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val POSTCODE_OR_COUNCIL = stringPreferencesKey("postcode_or_council")
        val PRIMARY_COLLECTION_DAY = stringPreferencesKey("primary_collection_day")
        val THEME_MODE = stringPreferencesKey("theme_mode")
    }

    /**
     * Observes current notification settings including reminder time and theme mode.
     */
    val notificationSettings: Flow<NotificationSettings> = applicationContext.dataStore.data.map { prefs ->
        val enabled = prefs[Keys.REMINDER_ENABLED] ?: true
        val themeModeRaw = prefs[Keys.THEME_MODE]
        val mode = themeModeRaw?.let { runCatching { AppThemeMode.valueOf(it) }.getOrNull() } ?: AppThemeMode.SYSTEM

        val eveningStr = prefs[Keys.EVENING_REMINDER_TIME]
        val eveningTime = if (eveningStr != null) {
            if (eveningStr == "NONE") null else runCatching { LocalTime.parse(eveningStr) }.getOrNull()
        } else {
            val legacyEvening = prefs[Keys.REMINDER_EVENING_BEFORE] ?: true
            if (legacyEvening) {
                val hour = prefs[Keys.REMINDER_HOUR] ?: 19
                val minute = prefs[Keys.REMINDER_MINUTE] ?: 0
                LocalTime.of(hour, minute)
            } else null
        }

        val morningStr = prefs[Keys.MORNING_REMINDER_TIME]
        val morningTime = if (morningStr != null) {
            if (morningStr == "NONE") null else runCatching { LocalTime.parse(morningStr) }.getOrNull()
        } else {
            val legacyEvening = prefs[Keys.REMINDER_EVENING_BEFORE] ?: false
            if (!legacyEvening) {
                val hour = prefs[Keys.REMINDER_HOUR] ?: 7
                val minute = prefs[Keys.REMINDER_MINUTE] ?: 0
                LocalTime.of(hour, minute)
            } else null
        }

        NotificationSettings(
            reminderEnabled = enabled,
            eveningReminderTime = eveningTime,
            morningReminderTime = morningTime,
            themeMode = mode
        )
    }

    /**
     * Observes the set of bin IDs currently marked as put out for collection.
     */
    val putOutBinIds: Flow<Set<Long>> = applicationContext.dataStore.data.map { prefs ->
        val stringSet = prefs[Keys.PUT_OUT_BIN_IDS] ?: emptySet()
        stringSet.mapNotNull { it.toLongOrNull() }.toSet()
    }

    /**
     * Updates the saved put out bin IDs set.
     */
    suspend fun setPutOutBinIds(binIds: Set<Long>) {
        applicationContext.dataStore.edit { prefs ->
            prefs[Keys.PUT_OUT_BIN_IDS] = binIds.map { it.toString() }.toSet()
        }
    }

    /**
     * Observes the active app theme mode setting.
     */
    val themeMode: Flow<AppThemeMode> = applicationContext.dataStore.data.map { prefs ->
        val raw = prefs[Keys.THEME_MODE]
        raw?.let { runCatching { AppThemeMode.valueOf(it) }.getOrNull() } ?: AppThemeMode.SYSTEM
    }

    /**
     * Observes whether the first-time setup onboarding flow has been completed.
     */
    val onboardingCompleted: Flow<Boolean> = applicationContext.dataStore.data.map { prefs ->
        prefs[Keys.ONBOARDING_COMPLETED] ?: false
    }

    /**
     * Observes saved postcode or council name information.
     */
    val postcodeOrCouncil: Flow<String> = applicationContext.dataStore.data.map { prefs ->
        prefs[Keys.POSTCODE_OR_COUNCIL] ?: ""
    }

    /**
     * Observes the primary weekly collection day preference.
     */
    val primaryCollectionDay: Flow<String> = applicationContext.dataStore.data.map { prefs ->
        prefs[Keys.PRIMARY_COLLECTION_DAY] ?: "MONDAY"
    }

    /**
     * Updates the saved theme preference option.
     */
    suspend fun setThemeMode(themeMode: AppThemeMode) {
        applicationContext.dataStore.edit { prefs ->
            prefs[Keys.THEME_MODE] = themeMode.name
        }
    }

    /**
     * Updates the onboarding completion status flag.
     */
    suspend fun setOnboardingCompleted(completed: Boolean) {
        applicationContext.dataStore.edit { prefs ->
            prefs[Keys.ONBOARDING_COMPLETED] = completed
        }
    }

    /**
     * Saves user council and postcode preferences gathered during onboarding.
     */
    suspend fun saveOnboardingInfo(postcodeOrCouncil: String, primaryDay: String) {
        applicationContext.dataStore.edit { prefs ->
            prefs[Keys.POSTCODE_OR_COUNCIL] = postcodeOrCouncil
            prefs[Keys.PRIMARY_COLLECTION_DAY] = primaryDay
        }
    }

    /**
     * Updates notification settings including reminder schedule and theme choices.
     */
    suspend fun updateSettings(settings: NotificationSettings) {
        applicationContext.dataStore.edit { prefs ->
            prefs[Keys.REMINDER_ENABLED] = settings.reminderEnabled
            prefs[Keys.EVENING_REMINDER_TIME] = settings.eveningReminderTime?.toString() ?: "NONE"
            prefs[Keys.MORNING_REMINDER_TIME] = settings.morningReminderTime?.toString() ?: "NONE"
            prefs[Keys.THEME_MODE] = settings.themeMode.name
        }
    }
}
