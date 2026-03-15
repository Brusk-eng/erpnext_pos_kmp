package com.erpnext.pos.utils.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.erpnext.pos.AppContext

private const val CHANNEL_ID = "inventory_alerts"
private const val CHANNEL_NAME = "Inventory Alerts"
private const val INVENTORY_ALERT_WORK_NAME = "inventory-alerts-daily"

actual fun notifySystem(title: String, message: String) {
  val context = AppContext.get()
  val manager = context.getSystemService(NotificationManager::class.java)
  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
    val channel =
        NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT)
    manager.createNotificationChannel(channel)
  }
  val notification =
      NotificationCompat.Builder(context, CHANNEL_ID)
          .setSmallIcon(android.R.drawable.ic_dialog_alert)
          .setContentTitle(title)
          .setContentText(message)
          .setAutoCancel(true)
          .build()
  manager.notify(1001, notification)
}

actual fun scheduleDailyInventoryReminder(
    enabled: Boolean,
    title: String,
    message: String,
    hour: Int,
    minute: Int,
) {
  // Android uses WorkManager for daily alerts; nothing to do here.
}

actual fun configureInventoryAlertWorker(enabled: Boolean, hour: Int, minute: Int) {
  val context = AppContext.get()
  val workManager = WorkManager.getInstance(context)
  if (!enabled) {
    workManager.cancelUniqueWork(INVENTORY_ALERT_WORK_NAME)
    return
  }

  enqueueInventoryAlertWorker(
      workManager = workManager,
      hour = hour,
      minute = minute,
      policy = ExistingWorkPolicy.REPLACE,
  )
}

internal fun scheduleNextInventoryAlertWorker(hour: Int, minute: Int) {
  val workManager = WorkManager.getInstance(AppContext.get())
  enqueueInventoryAlertWorker(
      workManager = workManager,
      hour = hour,
      minute = minute,
      policy = ExistingWorkPolicy.APPEND_OR_REPLACE,
  )
}

private fun enqueueInventoryAlertWorker(
    workManager: WorkManager,
    hour: Int,
    minute: Int,
    policy: ExistingWorkPolicy,
) {
  val request =
      OneTimeWorkRequestBuilder<InventoryAlertsWorker>()
          .setInitialDelay(
              computeInventoryAlertDelayMillis(hour, minute),
              java.util.concurrent.TimeUnit.MILLISECONDS,
          )
          .build()

  workManager.enqueueUniqueWork(
      INVENTORY_ALERT_WORK_NAME,
      policy,
      request,
  )
}

private fun computeInventoryAlertDelayMillis(hour: Int, minute: Int): Long {
  val now = System.currentTimeMillis()
  val calendar =
      java.util.Calendar.getInstance().apply {
        set(java.util.Calendar.HOUR_OF_DAY, hour.coerceIn(0, 23))
        set(java.util.Calendar.MINUTE, minute.coerceIn(0, 59))
        set(java.util.Calendar.SECOND, 0)
        set(java.util.Calendar.MILLISECOND, 0)
        if (timeInMillis <= now) {
          add(java.util.Calendar.DAY_OF_YEAR, 1)
        }
      }
  return (calendar.timeInMillis - now).coerceAtLeast(1_000L)
}
