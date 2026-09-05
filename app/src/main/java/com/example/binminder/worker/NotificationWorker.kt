package com.example.binminder.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.binminder.data.local.AppDatabase
import com.example.binminder.data.local.NotificationSettingsDataStore
import com.example.binminder.data.repository.BinRepositoryImpl
import com.example.binminder.engine.ScheduleEngine
import kotlinx.coroutines.flow.first
import java.time.LocalDate

class NotificationWorker(
    private val appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val database = AppDatabase.getInstance(appContext)
        val dataStore = NotificationSettingsDataStore(appContext)
        val repository = BinRepositoryImpl(database.binDao(), dataStore, appContext)

        // Ensure defaults are populated if app started in background
        repository.ensureDefaultBinsInitialized()

        val settings = repository.notificationSettings.first()
        if (!settings.reminderEnabled) {
            return Result.success()
        }

        val targetDate = if (settings.reminderEveningBefore) {
            LocalDate.now().plusDays(1)
        } else {
            LocalDate.now()
        }

        val bins = repository.getBinsList()
        val events = ScheduleEngine.generateCollectionEvents(bins, targetDate, targetDate)
            .filter { it.collectionDate == targetDate }

        if (events.isNotEmpty()) {
            val binNames = events.joinToString(separator = " and ") { it.binName }
            val title = if (settings.reminderEveningBefore) {
                "Tomorrow's Bin Collection"
            } else {
                "Today's Bin Collection"
            }

            val isPlural = events.size > 1
            val message = if (isPlural) {
                "Please remember to put out your $binNames bins."
            } else {
                "Please remember to put out your $binNames bin."
            }

            NotificationHelper.postCollectionReminderNotification(
                context = appContext,
                title = title,
                message = message,
                notificationId = targetDate.hashCode()
            )
        }

        return Result.success()
    }
}
