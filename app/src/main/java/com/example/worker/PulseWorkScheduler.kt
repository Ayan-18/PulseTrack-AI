package com.example.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object PulseWorkScheduler {

    const val HEALTH_SYNC_WORK_NAME = "PulseHealthSyncWork"
    const val REMINDER_WORK_NAME = "PulseReminderWork"

    fun schedulePeriodicTasks(context: Context) {
        val workManager = WorkManager.getInstance(context)

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .build()

        // 1. Periodic background sync with Health Connect (every 4 hours)
        val healthSyncRequest = PeriodicWorkRequestBuilder<HealthSyncWorker>(
            4, TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .build()

        workManager.enqueueUniquePeriodicWork(
            HEALTH_SYNC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            healthSyncRequest
        )

        // 2. Periodic reminders for water & habits (every 3 hours)
        val reminderRequest = PeriodicWorkRequestBuilder<ReminderWorker>(
            3, TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .build()

        workManager.enqueueUniquePeriodicWork(
            REMINDER_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            reminderRequest
        )
    }
}
