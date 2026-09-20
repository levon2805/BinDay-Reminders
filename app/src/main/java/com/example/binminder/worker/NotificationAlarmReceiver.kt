package com.example.binminder.worker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.binminder.data.local.AppDatabase
import com.example.binminder.data.local.NotificationSettingsDataStore
import com.example.binminder.data.repository.BinRepositoryImpl
import com.example.binminder.engine.ScheduleEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime

/**
 * BroadcastReceiver triggered by AlarmManager exact alarms for bin collection reminders.
 *
 * Checks Room DB and bin put-out status (isPutOut) to immediately post high-priority
 * heads-up notifications when bins are not yet marked as put out.
 */
class NotificationAlarmReceiver : BroadcastReceiver() {

    private companion object {
        private const val TAG = "NotificationAlarmReceiver"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        val pendingResult = goAsync()
        val appContext = context.applicationContext

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val database = AppDatabase.getInstance(appContext)
                val dataStore = NotificationSettingsDataStore(appContext)
                val repository = BinRepositoryImpl(database.binDao(), dataStore, appContext)


                val settings = repository.notificationSettings.first()
                if (!settings.reminderEnabled) {
                    return@launch
                }

                val slotStr = intent?.getStringExtra("REMINDER_SLOT")
                val targetDateStr = intent?.getStringExtra("TARGET_DATE")
                val reminderTimeStr = intent?.getStringExtra("REMINDER_TIME")

                val isEvening = slotStr == "EVENING" || (slotStr == null && settings.eveningReminderTime != null)

                val triggeredTime: LocalTime? = reminderTimeStr?.let {
                    runCatching { LocalTime.parse(it) }.getOrNull()
                } ?: if (isEvening) settings.eveningReminderTime else settings.morningReminderTime

                val activeSet = if (isEvening) settings.eveningReminderTimes else settings.morningReminderTimes

                if (triggeredTime == null || !activeSet.contains(triggeredTime)) {
                    Log.d(TAG, "Discarding cancelled alarm: Triggered time $triggeredTime ($slotStr) is no longer active in settings ($activeSet).")
                    return@launch
                }

                val targetDate = if (targetDateStr != null) {
                    runCatching { LocalDate.parse(targetDateStr) }.getOrDefault(
                        if (isEvening) LocalDate.now().plusDays(1) else LocalDate.now()
                    )
                } else {
                    if (isEvening) LocalDate.now().plusDays(1) else LocalDate.now()
                }

                val putOutBins = repository.putOutBins.first()
                val bins = repository.getBinsList().filter { it.isEnabled }

                val events = ScheduleEngine.generateCollectionEvents(bins, targetDate, targetDate)
                    .filter { it.collectionDate == targetDate }

                val unPutOutEvents = events.filter { !putOutBins.contains("${it.binId}_${it.collectionDate}") }

                if (unPutOutEvents.isNotEmpty()) {
                    val content = NotificationHelper.formatNotificationContent(unPutOutEvents, isEvening)
                    if (content != null) {
                        Log.d(TAG, "Posting high-priority heads-up reminder notification for $targetDate (${content.binNames})")
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
                } else if (events.isNotEmpty()) {
                    Log.d(TAG, "All collection bins for $targetDate are marked put out (isPutOut == true). Skipping notification.")
                }

                // Recalculate target times and set next exact alarms / WorkManager tasks
                NotificationScheduler.scheduleNotificationWorker(appContext)
            } catch (e: Exception) {
                Log.e(TAG, "Error handling alarm broadcast", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
