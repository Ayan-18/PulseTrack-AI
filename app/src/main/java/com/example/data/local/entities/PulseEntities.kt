package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String = "",
    val category: String = "Здоровье", // Здоровье, Спорт, Продуктивность, Разум
    val targetDaysPerWeek: Int = 7,
    val colorHex: String = "#4F46E5",
    val autoSyncType: String = "NONE", // NONE, STEPS, WORKOUT, SLEEP, WATER, SPO2, STRESS, ACTIVE_MINUTES
    val autoSyncThreshold: Int = 0,    // e.g. 10000 steps, 30 min workout, 95% SpO2, max 40 stress
    val isArchived: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "habit_logs",
    foreignKeys = [
        ForeignKey(
            entity = HabitEntity::class,
            parentColumns = ["id"],
            childColumns = ["habitId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["habitId", "date"], unique = true)]
)
data class HabitLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val habitId: Long,
    val date: String, // YYYY-MM-DD
    val completedAt: Long = System.currentTimeMillis(),
    val isAutoCompleted: Boolean = false
)

@Entity(
    tableName = "tasks",
    foreignKeys = [
        ForeignKey(
            entity = TaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["parentTaskId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["parentTaskId"])]
)
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val dueDate: String? = null,
    val priority: String = "MEDIUM", // HIGH, MEDIUM, LOW
    val isCompleted: Boolean = false,
    val parentTaskId: Long? = null,
    val category: String = "Общее",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val content: String,
    val tags: String = "",
    val updatedAt: Long = System.currentTimeMillis(),
    val isPinned: Boolean = false
)

@Entity(tableName = "workouts")
data class WorkoutEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val workoutType: String, // Бег, Ходьба, Силовая, Велосипед, Плавание, HIIT, Йога
    val date: String,        // YYYY-MM-DD
    val durationMinutes: Int,
    val caloriesBurned: Int,
    val avgHeartRate: Int,
    val distanceMeters: Int = 0,
    val source: String = "Xiaomi Smart Band 10",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "food_entries")
data class FoodEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val mealType: String, // BREAKFAST, LUNCH, DINNER, SNACK
    val description: String,
    val calories: Int,
    val proteinGrams: Int = 0,
    val fatsGrams: Int = 0,
    val carbsGrams: Int = 0,
    val date: String,     // YYYY-MM-DD
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "water_logs")
data class WaterLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amountMl: Int,
    val date: String,     // YYYY-MM-DD
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "daily_metrics")
data class DailyMetricEntity(
    @PrimaryKey val date: String, // YYYY-MM-DD
    val steps: Int = 0,
    val sleepMinutes: Int = 0,
    val deepSleepMinutes: Int = 0,
    val remSleepMinutes: Int = 0,
    val lightSleepMinutes: Int = 0,
    val sleepScore: Int = 85,
    val avgHeartRate: Int = 0,
    val minHeartRate: Int = 54,
    val maxHeartRate: Int = 142,
    val spo2Percent: Int = 98,        // Кислород в крови (SpO2: 95-100%)
    val stressLevel: Int = 28,        // Уровень стресса (0-100)
    val stressStatus: String = "Низкий", // Спокойно, Умеренный, Высокий
    val activeMinutes: Int = 45,      // Время активности в минутах
    val standingHours: Int = 9,       // Часы разминки / стояния
    val vitalityScore: Int = 78,      // Индекс жизненной силы / PAI
    val waterGoalMl: Int = 2000,
    val calorieGoal: Int = 2200,
    val lastSyncedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "ai_insights")
data class AiInsightEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val content: String,
    val category: String, // AUDIT, BURNOUT, HABIT_RECOMMENDATION, NUTRITION, STRESS, SPO2
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey val id: Long = 1,
    val age: Int = 26,
    val weightKg: Float = 72f,
    val heightCm: Float = 176f,
    val gender: String = "MALE", // MALE, FEMALE
    val activityLevel: String = "MODERATE", // SEDENTARY, LIGHT, MODERATE, ACTIVE, VERY_ACTIVE
    val goal: String = "LOSE_WEIGHT", // LOSE_WEIGHT, BUILD_MUSCLE, MAINTAIN, ENDURANCE
    val bmr: Int = 1680,
    val tdee: Int = 2350,
    val targetCalories: Int = 1880,
    val targetWaterMl: Int = 2400,
    val targetProteinGrams: Int = 145,
    val targetFatsGrams: Int = 65,
    val targetCarbsGrams: Int = 180,
    val isOnboarded: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "kinesitherapy_sessions")
data class KinesitherapySessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,
    val title: String = "Занятие кинезиотерапией на МТБ",
    val primaryGoal: String = "Реабилитация и укрепление мышечного корсета",
    val availableExercisesPhotoBase64: String? = null,
    val performedExercisesPhotoBase64: String? = null,
    val exercisesJson: String = "[]",
    val totalTonnageKg: Float = 0f,
    val exercisesCompletedCount: Int = 0,
    val exercisesTotalCount: Int = 0,
    val durationMinutes: Int = 50,
    val painLevelBefore: Int = 4,
    val painLevelAfter: Int = 1,
    val aiScore: Int = 9,
    val aiEvaluation: String = "",
    val aiLoadAnalysis: String = "",
    val aiGoalRecommendations: String = "",
    val aiNextSessionPlan: String = "",
    val userNotes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
