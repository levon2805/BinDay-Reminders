package com.levdev.binday.worker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.levdev.binday.BinMinderApplication
import com.levdev.binday.data.local.AppDatabase
import com.levdev.binday.data.local.NotificationSettingsDataStore
import com.levdev.binday.data.repository.BinRepositoryImpl
import com.levdev.binday.domain.ToggleBinPutOutUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * Broadcast receiver that handles the "Mark as Put Out" action from notifications.
 */
class MarkBinPutOutReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action != "com.levdev.binday.ACTION_MARK_PUT_OUT") return
        
        val notificationId = intent.getIntExtra("NOTIFICATION_ID", -1)
        val targetDateStr = intent.getStringExtra("TARGET_DATE") ?: return
        
        val pendingResult = goAsync()
        val appContext = context.applicationContext

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repository = (appContext as? BinMinderApplication)?.container?.binRepository
                    ?: run {
                        val database = AppDatabase.getInstance(appContext)
                        val dataStore = NotificationSettingsDataStore(appContext)
                        BinRepositoryImpl(database.binDao(), dataStore, appContext)
                    }
                val toggleUseCase = ToggleBinPutOutUseCase()
                
                val targetDate = runCatching { LocalDate.parse(targetDateStr) }.getOrNull() ?: return@launch
                val currentPutOutBins = repository.putOutBins.first()
                val binIdsStr = intent.getStringExtra("BIN_IDS") ?: return@launch
                val binNamesStr = intent.getStringExtra("BIN_NAMES") ?: "the bins"
                
                val binIds = binIdsStr.split(",").mapNotNull { it.toLongOrNull() }
                
                var newPutOutBins = currentPutOutBins
                
                for (binId in binIds) {
                    val result = toggleUseCase(
                        binId = binId,
                        collectionDate = targetDate,
                        binName = "Bin", // Not used for the core logic
                        currentPutOutBins = newPutOutBins
                    )
                    newPutOutBins = result.updatedPutOutBins
                }
                
                // Update the repository
                repository.updatePutOutBins(newPutOutBins)
                
                // Cancel the notification since we've handled it
                if (notificationId != -1) {
                    NotificationHelper.cancelNotification(appContext, notificationId)
                }
                
                Log.d("MarkBinPutOutReceiver", "Marked $binNamesStr as put out from notification for $targetDate")
                
            } catch (e: Exception) {
                Log.e("MarkBinPutOutReceiver", "Error processing mark put out action", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
