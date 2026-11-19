package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entities.AiInsightEntity
import com.example.data.local.entities.DailyMetricEntity
import com.example.data.local.entities.FoodEntryEntity
import com.example.data.local.entities.HabitEntity
import com.example.data.local.entities.HabitLogEntity
import com.example.data.local.entities.NoteEntity
import com.example.data.local.entities.TaskEntity
import com.example.data.local.entities.KinesitherapySessionEntity
import com.example.data.local.entities.UserProfileEntity
import com.example.data.local.entities.WaterLogEntity
import com.example.data.local.entities.WorkoutEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PulseDao {

    // Habits
    @Query("SELECT * FROM habits WHERE isArchived = 0 ORDER BY id ASC")
    fun getAllHabits(): Flow<List<HabitEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabit(habit: HabitEntity): Long

    @Update
    suspend fun updateHabit(habit: HabitEntity)

    @Query("DELETE FROM habits WHERE id = :id")
    suspend fun deleteHabit(id: Long)

    // Habit Logs
    @Query("SELECT * FROM habit_logs WHERE date = :date")
    fun getHabitLogsForDate(date: String): Flow<List<HabitLogEntity>>

    @Query("SELECT * FROM habit_logs ORDER BY date DESC")
    fun getAllHabitLogs(): Flow<List<HabitLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabitLog(log: HabitLogEntity): Long

    @Query("DELETE FROM habit_logs WHERE habitId = :habitId AND date = :date")
    suspend fun deleteHabitLog(habitId: Long, date: String)

    // Tasks
    @Query("SELECT * FROM tasks ORDER BY isCompleted ASC, createdAt DESC")
    fun getAllTasks(): Flow<List<TaskEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTasks(tasks: List<TaskEntity>)

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteTask(id: Long)

    // Notes
    @Query("SELECT * FROM notes ORDER BY isPinned DESC, updatedAt DESC")
    fun getAllNotes(): Flow<List<NoteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: NoteEntity): Long

    @Update
    suspend fun updateNote(note: NoteEntity)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteNote(id: Long)

    // Workouts (Xiaomi Smart Band 10)
    @Query("SELECT * FROM workouts WHERE date = :date ORDER BY timestamp DESC")
    fun getWorkoutsForDate(date: String): Flow<List<WorkoutEntity>>

    @Query("SELECT * FROM workouts ORDER BY date DESC, timestamp DESC")
    fun getAllWorkouts(): Flow<List<WorkoutEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkout(workout: WorkoutEntity): Long

    @Query("DELETE FROM workouts WHERE id = :id")
    suspend fun deleteWorkout(id: Long)

    // Food Entries
    @Query("SELECT * FROM food_entries WHERE date = :date ORDER BY timestamp DESC")
    fun getFoodEntriesForDate(date: String): Flow<List<FoodEntryEntity>>

    @Query("SELECT * FROM food_entries ORDER BY date DESC, timestamp DESC")
    fun getAllFoodEntries(): Flow<List<FoodEntryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFoodEntry(entry: FoodEntryEntity): Long

    @Query("DELETE FROM food_entries WHERE id = :id")
    suspend fun deleteFoodEntry(id: Long)

    // Water Logs
    @Query("SELECT * FROM water_logs WHERE date = :date ORDER BY timestamp DESC")
    fun getWaterLogsForDate(date: String): Flow<List<WaterLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWaterLog(log: WaterLogEntity): Long

    // Daily Metrics
    @Query("SELECT * FROM daily_metrics WHERE date = :date")
    fun getDailyMetric(date: String): Flow<DailyMetricEntity?>

    @Query("SELECT * FROM daily_metrics ORDER BY date DESC LIMIT 30")
    fun getRecentDailyMetrics(): Flow<List<DailyMetricEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDailyMetric(metric: DailyMetricEntity)

    // AI Insights
    @Query("SELECT * FROM ai_insights ORDER BY createdAt DESC")
    fun getAllAiInsights(): Flow<List<AiInsightEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAiInsight(insight: AiInsightEntity): Long

    // User Profile & Onboarding
    @Query("SELECT * FROM user_profile WHERE id = 1")
    fun getUserProfile(): Flow<UserProfileEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertUserProfile(profile: UserProfileEntity)

    // Kinesitherapy Sessions
    @Query("SELECT * FROM kinesitherapy_sessions ORDER BY date DESC, createdAt DESC")
    fun getAllKinesitherapySessions(): Flow<List<KinesitherapySessionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertKinesitherapySession(session: KinesitherapySessionEntity): Long

    @Query("DELETE FROM kinesitherapy_sessions WHERE id = :id")
    suspend fun deleteKinesitherapySession(id: Long)

    // Purge mock/demo data when real Health Connect connects
    @Query("DELETE FROM workouts WHERE source = 'Xiaomi Smart Band 10'")
    suspend fun deleteMockWorkouts()

    @Query("DELETE FROM food_entries WHERE description LIKE '%Дневной рацион%' OR description LIKE '%Овсяная каша с черникой%' OR description LIKE '%Куриное филе на гриле%'")
    suspend fun deleteMockFoodEntries()

    @Query("DELETE FROM daily_metrics WHERE lastSyncedAt = 0 AND date != :today")
    suspend fun deleteMockPastDailyMetrics(today: String)
}
