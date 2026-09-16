package com.example.binminder

import android.app.Application
import android.util.Log
import com.example.binminder.di.AppContainer
import com.example.binminder.di.DefaultAppContainer
import com.example.binminder.worker.NotificationHelper

/**
 * Main application class for BinMinder.
 *
 * Initialises the application dependency injection container and sets up notification channels.
 * Proper grand setup for keeping your wheelie bin routines right on track!
 */
class BinMinderApplication : Application() {

    /**
     * Dependency container instance providing application-wide thread-safe singleton dependencies.
     */
    val container: AppContainer by lazy {
        try {
            DefaultAppContainer(this)
        } catch (exception: Exception) {
            Log.e("BinDay", "Error initializing AppContainer in BinMinderApplication", exception)
            throw exception
        }
    }

    override fun onCreate() {
        super.onCreate()
        try {
            NotificationHelper.createNotificationChannel(this)
        } catch (exception: Throwable) {
            Log.e("BinDay", "Error creating notification channel in BinMinderApplication", exception)
        }
    }
}
