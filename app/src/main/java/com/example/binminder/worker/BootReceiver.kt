package com.example.binminder.worker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * BroadcastReceiver triggered when device reboots or application package is replaced.
 *
 * Automatically reschedules exact alarms and WorkManager fallback tasks for active bin reminders.
 */
class BootReceiver : BroadcastReceiver() {

    private companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null) return
        val action = intent.action

        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            action == "android.intent.action.QUICKBOOT_POWERON" ||
            action == Intent.ACTION_LOCKED_BOOT_COMPLETED
        ) {
            Log.d(TAG, "Received boot or package replaced broadcast action: $action. Rescheduling alarms.")
            val pendingResult = goAsync()
            val appContext = context.applicationContext

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    NotificationScheduler.scheduleNotificationWorker(appContext)
                } catch (e: Exception) {
                    Log.e(TAG, "Error rescheduling notifications on boot", e)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
