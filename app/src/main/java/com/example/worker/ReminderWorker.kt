package com.example.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.data.local.AppDatabase
import com.example.data.local.entities.UserProfileEntity
import com.example.util.NotificationHelper
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.LocalTime

class ReminderWorker(
    private val ctx: Context,
    params: WorkerParameters
) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        return try {
            // 1. Silent mode check: Reminders only between 09:00 and 21:30 (no night alerts)
            val now = LocalTime.now()
            val startDay = LocalTime.of(9, 0)
            val endDay = LocalTime.of(21, 30)
            if (now.isBefore(startDay) || now.isAfter(endDay)) {
                return Result.success()
            }

            val today = LocalDate.now().toString()
            val db = AppDatabase.getInstance(ctx)
            val dao = db.pulseDao()

            // 2. Check hydration balance
            val profile = dao.getUserProfile().first() ?: UserProfileEntity()
            val waterLogs = dao.getWaterLogsForDate(today).first()
            val totalWater = waterLogs.sumOf { it.amountMl }
            val targetWater = if (profile.targetWaterMl > 0) profile.targetWaterMl else 2000

            if (totalWater < targetWater) {
                NotificationHelper.showWaterReminder(ctx, totalWater, targetWater)
            }

            // 3. Check pending uncompleted habits
            val habits = dao.getAllHabits().first().filter { !it.isArchived }
            val logsToday = dao.getHabitLogsForDate(today).first().map { it.habitId }.toSet()
            val pendingCount = habits.count { it.id !in logsToday }

            if (pendingCount > 0) {
                NotificationHelper.showHabitReminder(ctx, pendingCount)
            }

            Result.success()
        } catch (e: Exception) {
            // Gracefully succeed without throwing fatal exceptions
            Result.success()
        }
    }
}
