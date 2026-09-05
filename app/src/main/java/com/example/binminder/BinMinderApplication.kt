package com.example.binminder

import android.app.Application
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
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer(this)
        NotificationHelper.createNotificationChannel(this)
    }
}
