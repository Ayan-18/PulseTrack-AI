package com.example.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.data.health.HealthConnectManager
import com.example.data.local.AppDatabase
import com.example.data.repository.PulseRepository

class HealthSyncWorker(
    private val ctx: Context,
    params: WorkerParameters
) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        return try {
            val db = AppDatabase.getInstance(ctx)
            val hcManager = HealthConnectManager(ctx)
            val repository = PulseRepository(dao = db.pulseDao(), healthConnectManager = hcManager)

            repository.syncHealthConnect()
            repository.checkAutoSyncHabits()

            Result.success()
        } catch (e: Exception) {
            // Gracefully succeed on missing permissions or errors without throwing fatal exceptions
            Result.success()
        }
    }
}
