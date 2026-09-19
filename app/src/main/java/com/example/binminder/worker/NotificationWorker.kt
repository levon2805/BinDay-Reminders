package com.example.binminder.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.binminder.data.local.AppDatabase
import com.example.binminder.data.local.NotificationSettingsDataStore
import com.example.binminder.data.repository.BinRepositoryImpl
import com.example.binminder.engine.ScheduleEngine
import kotlinx.coroutines.flow.first
import java.time.LocalDate

/**
 * Background WorkManager worker that checks for upcoming bin collections and triggers notifications.
 */
class NotificationWorker(
    private val appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    private companion object {
        private const val TAG = "NotificationWorker"
    }

    /**
     * Executes background collection checks and triggers reminders when bins are due.
     */
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

        val putOutBins = repository.putOutBins.first()
        val slot = inputData.getString("REMINDER_SLOT")
        val targetDateStr = inputData.getString("TARGET_DATE")

        val targetDates = mutableListOf<Pair<LocalDate, Boolean>>() // Pair(targetDate, isEvening)

        if (targetDateStr != null) {
            runCatching {
                val parsedDate = LocalDate.parse(targetDateStr)
                val isEvening = slot == "EVENING"
                targetDates.add(parsedDate to isEvening)
            }
        }

        if (targetDates.isEmpty()) {
            if (slot == "EVENING") {
                targetDates.add(LocalDate.now().plusDays(1) to true)
            } else if (slot == "MORNING") {
                targetDates.add(LocalDate.now() to false)
            } else {
                if (settings.eveningReminderTime != null) {
                    targetDates.add(LocalDate.now().plusDays(1) to true)
                }
                if (settings.morningReminderTime != null) {
                    targetDates.add(LocalDate.now() to false)
                }
                if (targetDates.isEmpty()) {
                    val isEvening = settings.reminderEveningBefore
                    val date = if (isEvening) LocalDate.now().plusDays(1) else LocalDate.now()
                    targetDates.add(date to isEvening)
                }
            }
        }

        val bins = repository.getBinsList().filter { it.isEnabled }

        for ((targetDate, isEvening) in targetDates) {
            val events = ScheduleEngine.generateCollectionEvents(bins, targetDate, targetDate)
                .filter { it.collectionDate == targetDate }

            val unPutOutEvents = events.filter { !putOutBins.contains("${it.binId}_${it.collectionDate}") }

            if (unPutOutEvents.isEmpty()) {
                if (events.isNotEmpty()) {
                    Log.d(TAG, "Upcoming collection bins for $targetDate are marked put out (isPutOut == true). Skipping notification.")
                }
                continue
            }

            val content = NotificationHelper.formatNotificationContent(unPutOutEvents, isEvening)
            if (content != null) {
                Log.d(TAG, "Posting high-priority reminder notification for $targetDate (${content.binNames})")
                NotificationHelper.postCollectionReminderNotification(
                    context = appContext,
                    title = content.title,
                    message = content.message,
                    notificationId = targetDate.hashCode(),
                    binIds = content.unPutOutBinIds,
                    binNames = content.binNames,
                    targetDateStr = targetDate.toString()
                )
            }
        }

        // Reschedule the notification engine in case the OS killed the exact alarm 
        // and this fallback is keeping the schedule alive.
        NotificationScheduler.scheduleNotificationWorker(appContext)

        return Result.success()
    }
}
