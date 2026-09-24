package com.example.binminder.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.binminder.data.local.AppDatabase
import com.example.binminder.data.local.NotificationSettingsDataStore
import com.example.binminder.data.repository.BinRepositoryImpl
import com.example.binminder.engine.ScheduleEngine
import com.example.binminder.BinMinderApplication
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.LocalTime

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
        val repository = (appContext as? BinMinderApplication)?.container?.binRepository
            ?: run {
                val database = AppDatabase.getInstance(appContext)
                val dataStore = NotificationSettingsDataStore(appContext)
                BinRepositoryImpl(database.binDao(), dataStore, appContext)
            }


        val settings = repository.notificationSettings.first()
        if (!settings.reminderEnabled) {
            return Result.success()
        }

        val isExactDelivery = inputData.getBoolean("IS_EXACT_DELIVERY", false)
        if (!isExactDelivery) {
            Log.d(TAG, "NotificationWorker invoked without IS_EXACT_DELIVERY flag (likely a periodic/background sync). Rescheduling exact alarms and skipping direct notification posting.")
            NotificationScheduler.scheduleNotificationWorker(appContext)
            return Result.success()
        }

        val putOutBins = repository.putOutBins.first()
        val slot = inputData.getString("REMINDER_SLOT")
        val targetDateStr = inputData.getString("TARGET_DATE")
        val reminderTimeStr = inputData.getString("REMINDER_TIME")

        val targetDates = mutableListOf<Pair<LocalDate, Boolean>>() // Pair(targetDate, isEvening)

        val isEveningSlot = slot == "EVENING" || (slot == null && settings.eveningReminderTime != null)
        
        val triggeredTime: LocalTime? = reminderTimeStr?.let {
            runCatching { LocalTime.parse(it) }.getOrNull()
        }

        // Only apply the stale-alarm guard when we have an explicit REMINDER_TIME.
        // Legacy fallback workers (without REMINDER_TIME) should fire if not cancelled.
        if (triggeredTime != null) {
            val activeSet = if (isEveningSlot) settings.eveningReminderTimes else settings.morningReminderTimes
            if (!activeSet.contains(triggeredTime)) {
                Log.d(TAG, "Triggered time $triggeredTime ($slot) is no longer active in settings ($activeSet). Discarding WorkManager fallback notification.")
                NotificationScheduler.scheduleNotificationWorker(appContext)
                return Result.success()
            }
        }

        if (targetDateStr != null) {
            runCatching {
                val parsedDate = LocalDate.parse(targetDateStr)
                targetDates.add(parsedDate to isEveningSlot)
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
                // Generate unique notification ID per (date, slot, time) so multiple
                // reminders for the same date don't overwrite each other or collide in debounce
                val slotLabel = if (isEvening) "EVENING" else "MORNING"
                val notificationId = java.util.Objects.hash(targetDate, slotLabel, triggeredTime ?: "legacy") and 0x7FFFFFFF
                Log.d(TAG, "Posting high-priority reminder notification for $targetDate (${content.binNames})")
                NotificationHelper.postCollectionReminderNotification(
                    context = appContext,
                    title = content.title,
                    message = content.message,
                    notificationId = notificationId,
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
