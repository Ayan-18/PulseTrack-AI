package com.example.data.repository

import com.example.data.local.PulseDao
import com.example.data.local.entities.AiInsightEntity
import com.example.data.local.entities.DailyMetricEntity
import com.example.data.local.entities.FoodEntryEntity
import com.example.data.local.entities.HabitEntity
import com.example.data.local.entities.HabitLogEntity
import com.example.data.local.entities.NoteEntity
import com.example.data.domain.KinesitherapyAiReport
import com.example.data.domain.KinesitherapyExerciseItem
import com.example.data.local.entities.KinesitherapySessionEntity
import com.example.data.local.entities.TaskEntity
import com.example.data.local.entities.UserProfileEntity
import com.example.data.local.entities.WaterLogEntity
import com.example.data.local.entities.WorkoutEntity
import com.example.data.remote.AiAuditReport
import com.example.data.remote.FoodEstimate
import com.example.data.health.HealthConnectManager
import com.example.data.health.HealthSyncResult
import com.example.data.remote.GeminiAiRepository
import com.example.data.remote.GeneratedHabit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.time.LocalDate

class PulseRepository(
    private val dao: PulseDao,
    private val aiRepository: GeminiAiRepository = GeminiAiRepository(),
    private val healthConnectManager: HealthConnectManager? = null
) {
    fun getTodayDate(): String = LocalDate.now().toString()

    // Habits & Logs
    val allHabits: Flow<List<HabitEntity>> = dao.getAllHabits()
    val allHabitLogs: Flow<List<HabitLogEntity>> = dao.getAllHabitLogs()

    fun getHabitLogsForDate(date: String = getTodayDate()): Flow<List<HabitLogEntity>> =
        dao.getHabitLogsForDate(date)

    suspend fun insertHabit(habit: HabitEntity): Long {
        val id = dao.insertHabit(habit)
        if (habit.autoSyncType != "NONE") {
            checkAutoSyncHabits()
        }
        return id
    }
    suspend fun updateHabit(habit: HabitEntity) = dao.updateHabit(habit)
    suspend fun deleteHabit(id: Long) = dao.deleteHabit(id)

    suspend fun toggleHabitCompletion(habitId: Long, date: String = getTodayDate(), isCompleted: Boolean) {
        if (isCompleted) {
            dao.insertHabitLog(
                HabitLogEntity(
                    habitId = habitId,
                    date = date,
                    completedAt = System.currentTimeMillis(),
                    isAutoCompleted = false
                )
            )
        } else {
            dao.deleteHabitLog(habitId, date)
        }
    }

    // Tasks
    val allTasks: Flow<List<TaskEntity>> = dao.getAllTasks()
    suspend fun insertTask(task: TaskEntity): Long = dao.insertTask(task)
    suspend fun updateTask(task: TaskEntity) = dao.updateTask(task)
    suspend fun deleteTask(id: Long) = dao.deleteTask(id)

    suspend fun decomposeTaskWithAi(taskId: Long, taskTitle: String): List<String> {
        val subtasks = aiRepository.decomposeTask(taskTitle)
        val entities = subtasks.map { title ->
            TaskEntity(
                title = title,
                priority = "MEDIUM",
                isCompleted = false,
                parentTaskId = taskId,
                category = "Подзадача"
            )
        }
        dao.insertTasks(entities)
        return subtasks
    }

    // Notes
    val allNotes: Flow<List<NoteEntity>> = dao.getAllNotes()
    suspend fun insertNote(note: NoteEntity): Long = dao.insertNote(note)
    suspend fun updateNote(note: NoteEntity) = dao.updateNote(note)
    suspend fun deleteNote(id: Long) = dao.deleteNote(id)

    // Workouts (Xiaomi Smart Band 10)
    fun getWorkoutsForDate(date: String = getTodayDate()): Flow<List<WorkoutEntity>> =
        dao.getWorkoutsForDate(date)
    val allWorkouts: Flow<List<WorkoutEntity>> = dao.getAllWorkouts()

    suspend fun addWorkout(workout: WorkoutEntity) {
        dao.insertWorkout(workout)
        // Check auto-sync habits
        checkAutoSyncHabits(date = workout.date)
    }

    suspend fun deleteWorkout(id: Long) = dao.deleteWorkout(id)

    // Nutrition & Food
    fun getFoodEntriesForDate(date: String = getTodayDate()): Flow<List<FoodEntryEntity>> =
        dao.getFoodEntriesForDate(date)
    val allFoodEntries: Flow<List<FoodEntryEntity>> = dao.getAllFoodEntries()

    suspend fun addFoodEntry(entry: FoodEntryEntity) = dao.insertFoodEntry(entry)
    suspend fun deleteFoodEntry(id: Long) = dao.deleteFoodEntry(id)

    suspend fun estimateFoodWithAi(description: String): FoodEstimate =
        aiRepository.estimateNutrition(description)

    suspend fun estimateFoodFromPhotoWithAi(
        base64Image: String,
        mimeType: String = "image/jpeg",
        note: String = ""
    ): FoodEstimate = aiRepository.estimateNutritionFromImage(base64Image, mimeType, note)

    fun getLastAiError(): String? = aiRepository.lastApiError
    fun clearLastAiError() { aiRepository.lastApiError = null }
    fun setCustomApiKey(key: String) { aiRepository.customApiKey = key.trim() }
    fun getEffectiveApiKey(): String = aiRepository.getEffectiveApiKey()

    // Water
    fun getWaterLogsForDate(date: String = getTodayDate()): Flow<List<WaterLogEntity>> =
        dao.getWaterLogsForDate(date)

    suspend fun addWaterLog(amountMl: Int, date: String = getTodayDate()) {
        dao.insertWaterLog(WaterLogEntity(amountMl = amountMl, date = date))
        checkAutoSyncHabits(date = date)
    }

    // Daily Metrics
    fun getDailyMetric(date: String = getTodayDate()): Flow<DailyMetricEntity?> =
        dao.getDailyMetric(date)
    val recentDailyMetrics: Flow<List<DailyMetricEntity>> = dao.getRecentDailyMetrics()

    suspend fun updateDailyMetric(metric: DailyMetricEntity) {
        dao.upsertDailyMetric(metric)
        checkAutoSyncHabits(date = metric.date)
    }

    // AI Insights
    val allAiInsights: Flow<List<AiInsightEntity>> = dao.getAllAiInsights()

    suspend fun generateDailyAudit(): AiAuditReport {
        val today = getTodayDate()
        val metric = dao.getDailyMetric(today).first() ?: DailyMetricEntity(
            date = today,
            steps = 8450,
            sleepMinutes = 465,
            deepSleepMinutes = 110,
            spo2Percent = 98,
            stressLevel = 28,
            activeMinutes = 50
        )
        val profile = dao.getUserProfile().first() ?: UserProfileEntity()
        val workouts = dao.getWorkoutsForDate(today).first()
        val food = dao.getFoodEntriesForDate(today).first()
        val waterLogs = dao.getWaterLogsForDate(today).first()
        val tasks = dao.getAllTasks().first()
        val habits = dao.getAllHabits().first()
        val habitLogs = dao.getHabitLogsForDate(today).first()

        val totalWorkoutCalories = workouts.sumOf { it.caloriesBurned }
        val totalFoodCalories = food.sumOf { it.calories }
        val totalWater = waterLogs.sumOf { it.amountMl }
        val completedTasks = tasks.count { it.isCompleted }
        val pendingTasks = tasks.count { !it.isCompleted }
        val habitsCompleted = habitLogs.size

        val report = aiRepository.generateDailyAudit(
            steps = metric.steps,
            sleepMinutes = metric.sleepMinutes,
            deepSleepMinutes = metric.deepSleepMinutes,
            spo2Percent = metric.spo2Percent,
            stressLevel = metric.stressLevel,
            activeMinutes = metric.activeMinutes,
            workoutsCount = workouts.size,
            burnedCalories = totalWorkoutCalories,
            foodCalories = totalFoodCalories,
            waterMl = totalWater,
            completedTasks = completedTasks,
            pendingTasks = pendingTasks,
            habitsCompleted = habitsCompleted,
            totalHabits = habits.size,
            bmr = profile.bmr
        )

        dao.insertAiInsight(
            AiInsightEntity(
                title = report.title,
                content = "${report.content}\n\n💡 Совет по энергии: ${report.energyBalanceTip}",
                category = "AUDIT"
            )
        )
        return report
    }

    suspend fun generateHabitPlan(goal: String): List<GeneratedHabit> =
        aiRepository.generateHabitPlan(goal)

    suspend fun generateHabitPlanWithStatus(goal: String): Pair<List<GeneratedHabit>, Boolean> =
        aiRepository.generateHabitPlanWithStatus(goal)

    fun generateFallbackHabits(goal: String): List<GeneratedHabit> =
        aiRepository.generateFallbackHabits(goal)

    // User Profile & Onboarding
    val userProfile: Flow<UserProfileEntity?> = dao.getUserProfile()

    suspend fun saveUserProfile(profile: UserProfileEntity) {
        dao.upsertUserProfile(profile)
        val today = getTodayDate()
        val currentMetric = dao.getDailyMetric(today).first() ?: DailyMetricEntity(date = today)
        dao.upsertDailyMetric(
            currentMetric.copy(
                calorieGoal = profile.targetCalories,
                waterGoalMl = profile.targetWaterMl
            )
        )
        // Adjust default WATER habit threshold to match user's personalized requirement
        val habits = dao.getAllHabits().first()
        for (habit in habits) {
            if (habit.autoSyncType == "WATER") {
                dao.updateHabit(
                    habit.copy(
                        title = "Выпить ${profile.targetWaterMl} мл воды",
                        description = "Персональная норма гидратации",
                        autoSyncThreshold = profile.targetWaterMl
                    )
                )
            }
        }
        checkAutoSyncHabits(today)
        generatePersonalizedAdvice()
    }

    suspend fun generatePersonalizedAdvice(): String {
        val today = getTodayDate()
        val profile = dao.getUserProfile().first() ?: UserProfileEntity()
        val metric = dao.getDailyMetric(today).first() ?: DailyMetricEntity(date = today)
        val workouts = dao.getWorkoutsForDate(today).first()
        val waterLogs = dao.getWaterLogsForDate(today).first()

        val totalWorkoutCalories = workouts.sumOf { it.caloriesBurned }
        val totalWater = waterLogs.sumOf { it.amountMl }

        val advice = aiRepository.generatePersonalizedAdvice(
            profile = profile,
            steps = metric.steps,
            sleepMinutes = metric.sleepMinutes,
            waterMl = totalWater,
            burnedCalories = totalWorkoutCalories
        )

        val goalRu = when (profile.goal.uppercase()) {
            "LOSE_WEIGHT", "ПОХУДЕТЬ", "СНИЖЕНИЕ ВЕСА" -> "Похудение"
            "BUILD_MUSCLE", "НАБРАТЬ МЫШЦЫ", "НАБОР МАССЫ" -> "Набор мышц"
            else -> "Баланс"
        }

        dao.insertAiInsight(
            AiInsightEntity(
                title = "Персональный совет AI: $goalRu",
                content = advice,
                category = "HABIT_RECOMMENDATION"
            )
        )
        return advice
    }

    // Kinesitherapy
    val kinesitherapySessions: Flow<List<KinesitherapySessionEntity>> = dao.getAllKinesitherapySessions()

    suspend fun extractExercisesFromPhoto(base64Image: String): List<KinesitherapyExerciseItem> {
        return aiRepository.extractExercisesFromProgramPhoto(base64Image)
    }

    suspend fun saveKinesitherapySession(session: KinesitherapySessionEntity): Long {
        val id = dao.upsertKinesitherapySession(session)
        // Also log as a workout so it adds to daily activity and burns calories!
        val today = getTodayDate()
        val cals = (session.durationMinutes * 6.5f + session.totalTonnageKg * 0.05f).toInt()
        dao.insertWorkout(
            WorkoutEntity(
                workoutType = "Кинезиотерапия",
                durationMinutes = session.durationMinutes,
                caloriesBurned = cals,
                avgHeartRate = 108,
                date = session.date.ifBlank { today }
            )
        )
        // Check auto sync habits
        checkAutoSyncHabits(today)
        return id
    }

    suspend fun deleteKinesitherapySession(id: Long) {
        dao.deleteKinesitherapySession(id)
    }

    suspend fun analyzeKinesitherapy(
        availableExercisesPhotoBase64: String?,
        performedExercisesPhotoBase64: String?,
        exercises: List<KinesitherapyExerciseItem>,
        userGoal: String,
        painBefore: Int,
        painAfter: Int,
        userNotes: String
    ): KinesitherapyAiReport {
        val profile = dao.getUserProfile().first()
        val report = aiRepository.analyzeKinesitherapySession(
            availableExercisesPhotoBase64 = availableExercisesPhotoBase64,
            performedExercisesPhotoBase64 = performedExercisesPhotoBase64,
            exercises = exercises,
            userGoal = userGoal,
            userProfile = profile,
            painBefore = painBefore,
            painAfter = painAfter,
            userNotes = userNotes
        )
        // Save an AI insight
        dao.insertAiInsight(
            AiInsightEntity(
                title = "Кинезиотерапия: Оценка ${report.score}/10 (Тоннаж ${report.totalTonnageKg.toInt()} кг)",
                content = "${report.evaluation}\n\n${report.loadAnalysis}\n\n${report.goalProgress}\n\n${report.nextSessionAdvice}",
                category = "AUDIT"
            )
        )
        return report
    }

    // Check habits configured for auto-sync with Xiaomi Smart Band or Water
    suspend fun checkAutoSyncHabits(date: String = getTodayDate()) {
        val habits = dao.getAllHabits().first()
        val currentLogs = dao.getHabitLogsForDate(date).first().map { it.habitId }.toSet()

        val metric = dao.getDailyMetric(date).first() ?: DailyMetricEntity(date = date)
        val workouts = dao.getWorkoutsForDate(date).first()
        val waterLogs = dao.getWaterLogsForDate(date).first()
        val totalWater = waterLogs.sumOf { it.amountMl }

        for (habit in habits) {
            if (habit.id in currentLogs) continue

            val shouldComplete = when (habit.autoSyncType) {
                "STEPS" -> habit.autoSyncThreshold > 0 && metric.steps >= habit.autoSyncThreshold
                "SLEEP" -> habit.autoSyncThreshold > 0 && metric.sleepMinutes >= habit.autoSyncThreshold
                "DEEP_SLEEP" -> habit.autoSyncThreshold > 0 && metric.deepSleepMinutes >= habit.autoSyncThreshold
                "SPO2" -> habit.autoSyncThreshold > 0 && metric.spo2Percent >= habit.autoSyncThreshold
                "STRESS" -> habit.autoSyncThreshold > 0 && metric.stressLevel <= habit.autoSyncThreshold
                "ACTIVE_MINUTES" -> habit.autoSyncThreshold > 0 && metric.activeMinutes >= habit.autoSyncThreshold
                "WATER" -> habit.autoSyncThreshold > 0 && totalWater >= habit.autoSyncThreshold
                "WORKOUT" -> workouts.isNotEmpty() && (habit.autoSyncThreshold == 0 || workouts.sumOf { it.durationMinutes } >= habit.autoSyncThreshold)
                else -> false
            }

            if (shouldComplete) {
                dao.insertHabitLog(
                    HabitLogEntity(
                        habitId = habit.id,
                        date = date,
                        completedAt = System.currentTimeMillis(),
                        isAutoCompleted = true
                    )
                )
            }
        }
    }

    // Health Connect Integration
    suspend fun syncHealthConnect(): HealthSyncResult {
        val manager = healthConnectManager ?: return HealthSyncResult.NotAvailable(
            "Health Connect не инициализирован в системе."
        )

        // 1. Check SDK availability
        when (manager.checkAvailability()) {
            is com.example.data.health.HealthConnectAvailability.UpdateRequired -> {
                return HealthSyncResult.UpdateRequired(
                    "Для работы требуется установить или обновить приложение Health Connect из Google Play."
                )
            }
            is com.example.data.health.HealthConnectAvailability.NotSupported -> {
                return HealthSyncResult.NotAvailable(
                    "Health Connect не поддерживается на данном устройстве (требуется Android 9+ с Google Play или Android 14+)."
                )
            }
            else -> {}
        }

        // 2. Check permissions
        if (!manager.hasReadPermissions()) {
            val missing = manager.getMissingReadPermissions()
            return HealthSyncResult.NeedsPermission(missing)
        }

        // 3. Read real snapshot from Health Connect
        val snapshot = manager.readTodaySnapshot()
        if (!snapshot.isFromHealthConnect) {
            return HealthSyncResult.Error(
                snapshot.statusMessage.ifBlank { "Не удалось прочитать данные из Health Connect." }
            )
        }

        val today = getTodayDate()
        // Automatically purge mock/demo data now that Health Connect is connected
        purgeMockData(today)

        val currentMetric = dao.getDailyMetric(today).first() ?: DailyMetricEntity(
            date = today,
            steps = 0,
            sleepMinutes = 0,
            avgHeartRate = 0,
            spo2Percent = 0,
            stressLevel = 0,
            stressStatus = "—",
            activeMinutes = 0,
            standingHours = 0,
            vitalityScore = 0
        )

        // Check if Health Connect had any records for today
        val hasAnyData = snapshot.steps != null ||
                snapshot.avgHeartRate != null ||
                snapshot.sleepMinutes != null ||
                snapshot.spo2Percent != null ||
                snapshot.activeCalories != null ||
                snapshot.workouts.isNotEmpty()

        if (!hasAnyData) {
            // Real integration: DO NOT generate fake simulated data!
            return HealthSyncResult.NoData(
                "Health Connect подключен! Демо-данные очищены. Новых записей с браслета за сегодня пока нет. Синхронизируйте ваш браслет (Mi Fitness, Zepp Life или Zepp) с Health Connect."
            )
        }

        // Update with actual records from Health Connect
        val updatedMetric = currentMetric.copy(
            steps = snapshot.steps ?: currentMetric.steps,
            avgHeartRate = snapshot.avgHeartRate ?: currentMetric.avgHeartRate,
            minHeartRate = snapshot.minHeartRate ?: currentMetric.minHeartRate,
            maxHeartRate = snapshot.maxHeartRate ?: currentMetric.maxHeartRate,
            sleepMinutes = snapshot.sleepMinutes ?: currentMetric.sleepMinutes,
            spo2Percent = snapshot.spo2Percent ?: currentMetric.spo2Percent,
            activeMinutes = if (snapshot.workouts.isNotEmpty()) {
                snapshot.workouts.sumOf { it.durationMinutes }
            } else {
                currentMetric.activeMinutes
            },
            lastSyncedAt = System.currentTimeMillis()
        )
        dao.upsertDailyMetric(updatedMetric)

        // Insert actual workouts retrieved from Health Connect
        var newWorkoutsCount = 0
        val existingWorkouts = dao.getWorkoutsForDate(today).first()
        snapshot.workouts.forEach { hw ->
            val alreadyExists = existingWorkouts.any {
                it.workoutType == hw.workoutType && it.durationMinutes == hw.durationMinutes
            }
            if (!alreadyExists) {
                dao.insertWorkout(
                    WorkoutEntity(
                        workoutType = hw.workoutType,
                        date = today,
                        durationMinutes = hw.durationMinutes,
                        caloriesBurned = hw.caloriesBurned,
                        avgHeartRate = hw.avgHeartRate,
                        distanceMeters = hw.distanceMeters,
                        source = "Health Connect"
                    )
                )
                newWorkoutsCount++
            }
        }

        checkAutoSyncHabits(today)

        // Read real historical 7 days steps and active calories from Health Connect to populate analytics
        try {
            val historyMap = manager.readHistoricalDaysMetrics(7)
            for ((dateKey, pair) in historyMap) {
                val (steps, calories) = pair
                if (steps > 0 || calories > 0) {
                    val existing = dao.getDailyMetric(dateKey).first()
                    if (existing != null) {
                        dao.upsertDailyMetric(
                            existing.copy(
                                steps = if (steps > 0) steps else existing.steps,
                                lastSyncedAt = System.currentTimeMillis()
                            )
                        )
                    } else {
                        dao.upsertDailyMetric(
                            DailyMetricEntity(
                                date = dateKey,
                                steps = steps,
                                lastSyncedAt = System.currentTimeMillis()
                            )
                        )
                    }

                    if (calories > 0 && dateKey != today) {
                        val existingWorkoutsForDate = dao.getWorkoutsForDate(dateKey).first()
                        val hasHcWorkout = existingWorkoutsForDate.any { it.source == "Health Connect" }
                        if (!hasHcWorkout) {
                            dao.insertWorkout(
                                WorkoutEntity(
                                    workoutType = "Активность с браслета",
                                    date = dateKey,
                                    durationMinutes = (calories / 5).coerceIn(15, 120),
                                    caloriesBurned = calories,
                                    avgHeartRate = 110,
                                    source = "Health Connect"
                                )
                            )
                        }
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.w("PulseRepository", "Failed to sync historical metrics: ${e.message}")
        }

        val parts = mutableListOf<String>()
        snapshot.steps?.let { parts.add("$it шагов") }
        snapshot.avgHeartRate?.let { parts.add("пульс $it уд/мин") }
        snapshot.sleepMinutes?.let { parts.add("сон ${it / 60}ч ${it % 60}м") }
        if (newWorkoutsCount > 0) parts.add("тренировок: $newWorkoutsCount")

        val summary = if (parts.isNotEmpty()) parts.joinToString(" • ") else "данные обновлены"
        return HealthSyncResult.Success(
            message = "Синхронизировано с Health Connect: $summary",
            steps = snapshot.steps,
            heartRate = snapshot.avgHeartRate,
            workoutsCount = newWorkoutsCount,
            isRealData = true
        )
    }

    suspend fun writeDemoDataToHealthConnect(): HealthSyncResult {
        val manager = healthConnectManager ?: return HealthSyncResult.NotAvailable("Health Connect не инициализирован")
        when (manager.checkAvailability()) {
            is com.example.data.health.HealthConnectAvailability.UpdateRequired -> {
                return HealthSyncResult.UpdateRequired(
                    "Для работы требуется установить или обновить приложение Health Connect из Google Play."
                )
            }
            is com.example.data.health.HealthConnectAvailability.NotSupported -> {
                return HealthSyncResult.NotAvailable(
                    "Health Connect не поддерживается на данном устройстве (требуется Android 9+ с Google Play или Android 14+)."
                )
            }
            else -> {}
        }

        if (!manager.hasAllPermissions()) {
            val missing = manager.getMissingAllPermissions()
            return HealthSyncResult.NeedsPermission(missing.ifEmpty { manager.allPermissions })
        }

        val written = manager.writeDemoHealthData(
            steps = 4250,
            bpm = 72,
            workoutTitle = "Ходьба с браслетом",
            durationMin = 30
        )
        return if (written) {
            syncHealthConnect()
        } else {
            HealthSyncResult.Error("Не удалось записать тестовые данные в Health Connect. Проверьте разрешения.")
        }
    }

    suspend fun purgeMockData(today: String = getTodayDate()) {
        try {
            dao.deleteMockWorkouts()
            dao.deleteMockFoodEntries()
            dao.deleteMockPastDailyMetrics(today)
            val current = dao.getDailyMetric(today).first()
            if (current != null && current.lastSyncedAt == 0L && current.steps == 7450) {
                dao.upsertDailyMetric(
                    current.copy(
                        steps = 0,
                        sleepMinutes = 0,
                        deepSleepMinutes = 0,
                        remSleepMinutes = 0,
                        lightSleepMinutes = 0,
                        sleepScore = 0,
                        avgHeartRate = 0,
                        minHeartRate = 0,
                        maxHeartRate = 0,
                        spo2Percent = 0,
                        stressLevel = 0,
                        stressStatus = "—",
                        activeMinutes = 0,
                        standingHours = 0,
                        vitalityScore = 0
                    )
                )
            }
            android.util.Log.i("PulseRepository", "Все mock/demo данные успешно удалены.")
        } catch (e: Exception) {
            android.util.Log.w("PulseRepository", "Error purging mock data: ${e.message}")
        }
    }

    // Seed Initial Starter Data
    suspend fun seedInitialDataIfNeeded() {
        val existingProfile = dao.getUserProfile().first()
        val existingHabits = dao.getAllHabits().first()
        if (existingProfile != null || existingHabits.isNotEmpty()) return

        dao.upsertUserProfile(UserProfileEntity(id = 1, isOnboarded = false))

        val today = getTodayDate()

        // 1. Starter Habits
        val habit1 = HabitEntity(
            title = "10 000 шагов в день",
            description = "Двигательная норма, синхронизируемая с Xiaomi Band",
            category = "Спорт",
            colorHex = "#0D9488",
            autoSyncType = "STEPS",
            autoSyncThreshold = 10000
        )
        val habit2 = HabitEntity(
            title = "Выпить 2 литра воды",
            description = "Гидратация и бодрость в течение дня",
            category = "Здоровье",
            colorHex = "#0284C7",
            autoSyncType = "WATER",
            autoSyncThreshold = 2000
        )
        val habit3 = HabitEntity(
            title = "Сон 7+ часов",
            description = "Восстановление нервной системы и когнитивных функций",
            category = "Здоровье",
            colorHex = "#6366F1",
            autoSyncType = "SLEEP",
            autoSyncThreshold = 420
        )
        val habit4 = HabitEntity(
            title = "Фокус-сессия 45 минут",
            description = "Глубокая концентрация над главной рабочей задачей",
            category = "Продуктивность",
            colorHex = "#EA580C",
            autoSyncType = "NONE",
            autoSyncThreshold = 0
        )

        val h1Id = dao.insertHabit(habit1)
        val h2Id = dao.insertHabit(habit2)
        val h3Id = dao.insertHabit(habit3)
        dao.insertHabit(habit4)

        // Seed some history logs for streak demo
        val todayLocal = LocalDate.now()
        for (i in 1..4) {
            val pastDate = todayLocal.minusDays(i.toLong()).toString()
            dao.insertHabitLog(HabitLogEntity(habitId = h1Id, date = pastDate, isAutoCompleted = true))
            dao.insertHabitLog(HabitLogEntity(habitId = h2Id, date = pastDate, isAutoCompleted = false))
            dao.insertHabitLog(HabitLogEntity(habitId = h3Id, date = pastDate, isAutoCompleted = true))
        }

        // 2. Starter Tasks
        dao.insertTask(
            TaskEntity(
                title = "Подготовить презентацию проекта",
                priority = "HIGH",
                category = "Работа",
                dueDate = today
            )
        )
        dao.insertTask(
            TaskEntity(
                title = "Разобрать входящую почту и сообщения",
                priority = "MEDIUM",
                category = "Организация",
                dueDate = today
            )
        )
        dao.insertTask(
            TaskEntity(
                title = "Заказать спортивные витамины и магний",
                priority = "LOW",
                category = "Здоровье"
            )
        )

        // 3. Starter Notes
        dao.insertNote(
            NoteEntity(
                title = "Стратегия тренировок и питания",
                content = "1. Тренироваться 3 раза в неделю (пн, ср, пт).\n2. Держать норму белка 1.5г на 1кг веса.\n3. Не есть тяжелую пищу за 3 часа до сна для улучшения фаз глубокого сна по браслету.",
                tags = "Спорт, Питание",
                isPinned = true
            )
        )

        // 4. Starter Xiaomi Band Metrics (past 6 days + today for real analytics history)
        val pastSteps = listOf(8200, 10400, 7500, 11200, 9400, 6800)
        val pastFoodCals = listOf(1950, 2100, 1850, 2200, 1980, 2300)
        val pastBurnedCals = listOf(350, 480, 200, 520, 310, 420)
        for (idx in pastSteps.indices) {
            val daysAgo = (6 - idx).toLong()
            val histDate = todayLocal.minusDays(daysAgo).toString()
            dao.upsertDailyMetric(
                DailyMetricEntity(
                    date = histDate,
                    steps = pastSteps[idx],
                    sleepMinutes = 440 + (idx * 8),
                    deepSleepMinutes = 100 + (idx * 3),
                    remSleepMinutes = 90,
                    lightSleepMinutes = 250 + (idx * 5),
                    sleepScore = 84 + (idx % 5),
                    avgHeartRate = 65 + (idx % 4),
                    spo2Percent = 98,
                    stressLevel = 26 + (idx * 2),
                    stressStatus = "Спокойно",
                    activeMinutes = 40 + (idx * 5)
                )
            )
            dao.insertFoodEntry(
                FoodEntryEntity(
                    mealType = "LUNCH",
                    description = "Дневной рацион ($histDate)",
                    calories = pastFoodCals[idx],
                    proteinGrams = 130,
                    fatsGrams = 65,
                    carbsGrams = 190,
                    date = histDate
                )
            )
            dao.insertWorkout(
                WorkoutEntity(
                    workoutType = "Активность с браслетом",
                    date = histDate,
                    durationMinutes = 35,
                    caloriesBurned = pastBurnedCals[idx],
                    avgHeartRate = 128,
                    distanceMeters = 3200,
                    source = "Xiaomi Smart Band 10"
                )
            )
        }

        dao.upsertDailyMetric(
            DailyMetricEntity(
                date = today,
                steps = 7450,
                sleepMinutes = 465, // 7h 45m
                deepSleepMinutes = 110,
                remSleepMinutes = 95,
                lightSleepMinutes = 260,
                sleepScore = 88,
                avgHeartRate = 67,
                minHeartRate = 54,
                maxHeartRate = 142,
                spo2Percent = 98,
                stressLevel = 28,
                stressStatus = "Спокойно",
                activeMinutes = 52,
                standingHours = 10,
                vitalityScore = 82,
                waterGoalMl = 2000,
                calorieGoal = 2200
            )
        )

        // 5. Starter Workout from Band
        dao.insertWorkout(
            WorkoutEntity(
                workoutType = "Утренняя пробежка",
                date = today,
                durationMinutes = 32,
                caloriesBurned = 285,
                avgHeartRate = 142,
                distanceMeters = 4200,
                source = "Xiaomi Smart Band 10"
            )
        )

        // 6. Starter Food & Water
        dao.insertFoodEntry(
            FoodEntryEntity(
                mealType = "BREAKFAST",
                description = "Овсяная каша с черникой, вареное яйцо и зеленый чай",
                calories = 380,
                proteinGrams = 18,
                fatsGrams = 11,
                carbsGrams = 52,
                date = today
            )
        )
        dao.insertFoodEntry(
            FoodEntryEntity(
                mealType = "LUNCH",
                description = "Куриное филе на гриле, киноа и овощной салат с оливковым маслом",
                calories = 540,
                proteinGrams = 42,
                fatsGrams = 14,
                carbsGrams = 48,
                date = today
            )
        )

        dao.insertWaterLog(WaterLogEntity(amountMl = 500, date = today))
        dao.insertWaterLog(WaterLogEntity(amountMl = 350, date = today))
        dao.insertWaterLog(WaterLogEntity(amountMl = 250, date = today))

        // 7. Starter AI Insight
        dao.insertAiInsight(
            AiInsightEntity(
                title = "Оптимальный баланс продуктивности и восстановления",
                content = "Браслет Xiaomi зафиксировал 7ч 45м сна (хорошая фаза глубокого сна). Благодаря утренней пробежке сожжено 285 ккал. Рекомендуется выпить еще 900 мл воды и завершить высокоприоритетную презентацию до 16:00, когда фокус максимален.",
                category = "AUDIT"
            )
        )

        checkAutoSyncHabits(today)
    }
}
