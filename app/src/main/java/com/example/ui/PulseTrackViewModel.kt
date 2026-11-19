package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.entities.AiInsightEntity
import com.example.data.local.entities.DailyMetricEntity
import com.example.data.local.entities.FoodEntryEntity
import com.example.data.local.entities.HabitEntity
import com.example.data.local.entities.HabitLogEntity
import com.example.data.local.entities.NoteEntity
import com.example.data.local.entities.TaskEntity
import com.example.data.local.entities.WaterLogEntity
import com.example.data.local.entities.WorkoutEntity
import com.example.data.domain.KinesitherapyAiReport
import com.example.data.domain.KinesitherapyExerciseItem
import com.example.data.domain.NutritionCalculator
import com.example.data.local.entities.KinesitherapySessionEntity
import com.example.data.local.entities.UserProfileEntity
import com.example.data.remote.FoodEstimate
import com.example.data.remote.GeneratedHabit
import com.example.data.health.HealthConnectManager
import com.example.data.health.HealthSyncResult
import com.example.data.repository.PulseRepository
import com.example.util.ImageUtils
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class HabitUiModel(
    val habit: HabitEntity,
    val isCompletedToday: Boolean,
    val isAutoCompletedToday: Boolean = false,
    val streakDays: Int = 0,
    val isAutoSynced: Boolean = false,
    val currentProgress: Int = 0,
    val targetProgress: Int = 0,
    val progressFraction: Float = 0f,
    val progressStatusText: String = ""
)

data class HabitsTabState(
    val habits: List<HabitUiModel> = emptyList(),
    val selectedDate: String = "",
    val isSelectedDateToday: Boolean = true,
    val todayDate: String = "",
    val isAiLoading: Boolean = false
)

data class WorkoutsTabState(
    val workouts: List<WorkoutEntity> = emptyList(),
    val kinesitherapySessions: List<KinesitherapySessionEntity> = emptyList(),
    val kinesitherapySubTab: Int = 0,
    val userProfile: UserProfileEntity = UserProfileEntity(),
    val todayDate: String = "",
    val dailyMetric: DailyMetricEntity = DailyMetricEntity(date = ""),
    val isAiLoading: Boolean = false
)

data class NutritionTabState(
    val foodEntries: List<FoodEntryEntity> = emptyList(),
    val allFoodEntries: List<FoodEntryEntity> = emptyList(),
    val waterLogs: List<WaterLogEntity> = emptyList(),
    val totalFoodCalories: Int = 0,
    val totalWaterMl: Int = 0,
    val userProfile: UserProfileEntity = UserProfileEntity(),
    val selectedDate: String = "",
    val isSelectedDateToday: Boolean = true,
    val totalBurnedCalories: Int = 0,
    val todayDate: String = "",
    val isAiLoading: Boolean = false,
    val dailyMetric: DailyMetricEntity = DailyMetricEntity(date = "")
)

data class TasksTabState(
    val tasks: List<TaskEntity> = emptyList(),
    val notes: List<NoteEntity> = emptyList(),
    val isAiLoading: Boolean = false
)

data class StatsTabState(
    val dailyMetric: DailyMetricEntity = DailyMetricEntity(date = ""),
    val recentMetrics: List<DailyMetricEntity> = emptyList(),
    val aiInsights: List<AiInsightEntity> = emptyList(),
    val userProfile: UserProfileEntity = UserProfileEntity(),
    val dailyFoodCaloriesByDate: Map<String, Int> = emptyMap(),
    val dailyBurnedCaloriesByDate: Map<String, Int> = emptyMap(),
    val isAiLoading: Boolean = false,
    val kinesitherapySessions: List<KinesitherapySessionEntity> = emptyList(),
    val foodEntries: List<FoodEntryEntity> = emptyList(),
    val allFoodEntries: List<FoodEntryEntity> = emptyList(),
    val waterLogs: List<WaterLogEntity> = emptyList(),
    val totalFoodCalories: Int = 0,
    val totalBurnedCalories: Int = 0,
    val todayDate: String = ""
)

data class PulseUiState(
    val currentTab: Int = 0,
    val kinesitherapySubTab: Int = 0,
    val todayDate: String = "",
    val selectedDate: String = todayDate,
    val isSelectedDateToday: Boolean = true,
    val userProfile: UserProfileEntity = UserProfileEntity(),
    val showOnboarding: Boolean = false,
    val habits: List<HabitUiModel> = emptyList(),
    val tasks: List<TaskEntity> = emptyList(),
    val notes: List<NoteEntity> = emptyList(),
    val workouts: List<WorkoutEntity> = emptyList(),
    val foodEntries: List<FoodEntryEntity> = emptyList(),
    val allFoodEntries: List<FoodEntryEntity> = emptyList(),
    val waterLogs: List<WaterLogEntity> = emptyList(),
    val dailyMetric: DailyMetricEntity = DailyMetricEntity(date = ""),
    val recentMetrics: List<DailyMetricEntity> = emptyList(),
    val dailyFoodCaloriesByDate: Map<String, Int> = emptyMap(),
    val dailyBurnedCaloriesByDate: Map<String, Int> = emptyMap(),
    val aiInsights: List<AiInsightEntity> = emptyList(),
    val kinesitherapySessions: List<KinesitherapySessionEntity> = emptyList(),
    val totalFoodCalories: Int = 0,
    val totalBurnedCalories: Int = 0,
    val totalWaterMl: Int = 0,
    val isAiLoading: Boolean = false,
    val userNotification: String? = null
)

class PulseTrackViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: PulseRepository

    init {
        val db = AppDatabase.getInstance(application)
        val hcManager = com.example.data.health.HealthConnectManager(application)
        repository = PulseRepository(db.pulseDao(), healthConnectManager = hcManager)
        val prefs = application.getSharedPreferences("pulse_prefs", android.content.Context.MODE_PRIVATE)
        val savedKey = prefs.getString("gemini_api_key", null)
        if (!savedKey.isNullOrBlank()) {
            if (com.example.data.remote.GeminiAiRepository.isKnownDeadKey(savedKey)) {
                prefs.edit().remove("gemini_api_key").apply()
            } else {
                repository.setCustomApiKey(savedKey)
            }
        }
        viewModelScope.launch {
            repository.seedInitialDataIfNeeded()
            repository.checkAutoSyncHabits(todayDate)
        }
    }

    private val currentTabFlow = MutableStateFlow(0)
    private val kinesitherapySubTabFlow = MutableStateFlow(0)
    private val isAiLoadingFlow = MutableStateFlow(false)
    private val userNotificationFlow = MutableStateFlow<String?>(null)
    private val manualShowOnboardingFlow = MutableStateFlow<Boolean?>(null)

    private val todayDate = repository.getTodayDate()
    private val selectedDateFlow = MutableStateFlow(todayDate)

    private data class UiControlsSlice(
        val currentTab: Int,
        val kinesitherapySubTab: Int,
        val isAiLoading: Boolean,
        val notification: String?,
        val manualOnboarding: Boolean?
    )

    private data class OrganizerSlice(
        val habits: List<HabitEntity>,
        val habitLogs: List<HabitLogEntity>,
        val tasks: List<TaskEntity>,
        val notes: List<NoteEntity>
    )

    private data class ActivityNutritionSlice(
        val selectedDate: String,
        val allWorkouts: List<WorkoutEntity>,
        val allFoodEntries: List<FoodEntryEntity>,
        val waterLogs: List<WaterLogEntity>,
        val kinesitherapySessions: List<KinesitherapySessionEntity>
    )

    private data class TelemetryAiSlice(
        val dailyMetric: DailyMetricEntity,
        val recentMetrics: List<DailyMetricEntity>,
        val aiInsights: List<AiInsightEntity>,
        val profile: UserProfileEntity
    )

    private val uiControlsFlow = combine(
        currentTabFlow,
        kinesitherapySubTabFlow,
        isAiLoadingFlow,
        userNotificationFlow,
        manualShowOnboardingFlow
    ) { currentTab, kinesitherapySubTab, isAiLoading, notification, manualOnboarding ->
        UiControlsSlice(currentTab, kinesitherapySubTab, isAiLoading, notification, manualOnboarding)
    }

    private val habitsOnlyFlow = combine(
        repository.allHabits,
        repository.allHabitLogs
    ) { habits, habitLogs ->
        Pair(habits, habitLogs)
    }

    private val tasksOnlyFlow = combine(
        repository.allTasks,
        repository.allNotes
    ) { tasks, notes ->
        Pair(tasks, notes)
    }

    private val organizerFlow = combine(
        habitsOnlyFlow,
        tasksOnlyFlow
    ) { (habits, habitLogs), (tasks, notes) ->
        OrganizerSlice(habits, habitLogs, tasks, notes)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val activityNutritionFlow = combine(
        selectedDateFlow,
        repository.allWorkouts,
        repository.allFoodEntries,
        repository.kinesitherapySessions,
        selectedDateFlow.flatMapLatest { date -> repository.getWaterLogsForDate(date) }
    ) { selectedDate, allWorkouts, allFoodEntries, kinesitherapySessions, waterLogs ->
        ActivityNutritionSlice(selectedDate, allWorkouts, allFoodEntries, waterLogs, kinesitherapySessions)
    }

    private val telemetryAiFlow = combine(
        repository.getDailyMetric(todayDate),
        repository.recentDailyMetrics,
        repository.allAiInsights,
        repository.userProfile
    ) { dailyMetric, recentMetrics, aiInsights, profile ->
        TelemetryAiSlice(
            dailyMetric = dailyMetric ?: DailyMetricEntity(date = todayDate),
            recentMetrics = recentMetrics,
            aiInsights = aiInsights,
            profile = profile ?: UserProfileEntity()
        )
    }

    val uiState: StateFlow<PulseUiState> = combine(
        uiControlsFlow,
        organizerFlow,
        activityNutritionFlow,
        telemetryAiFlow
    ) { controls, organizer, activityNutrition, telemetry ->
        val showOnboarding = controls.manualOnboarding ?: (!telemetry.profile.isOnboarded)

        val selectedDate = activityNutrition.selectedDate
        val isSelectedDateToday = selectedDate == todayDate

        val workouts = activityNutrition.allWorkouts.filter { it.date == selectedDate }
        val foodEntries = activityNutrition.allFoodEntries.filter { it.date == selectedDate }
        val dailyFoodByDate = activityNutrition.allFoodEntries
            .groupBy { it.date }
            .mapValues { (_, list) -> list.sumOf { it.calories } }
        val dailyBurnedByDate = activityNutrition.allWorkouts
            .groupBy { it.date }
            .mapValues { (_, list) -> list.sumOf { it.caloriesBurned } }

        val totalFoodCals = foodEntries.sumOf { it.calories }
        val totalBurned = workouts.sumOf { it.caloriesBurned }
        val totalWater = activityNutrition.waterLogs.sumOf { it.amountMl }
        val dailyMetric = if (isSelectedDateToday) {
            telemetry.dailyMetric
        } else {
            telemetry.recentMetrics.find { it.date == selectedDate } ?: DailyMetricEntity(date = selectedDate)
        }

        val habitUiModels = buildHabitUiModels(
            habits = organizer.habits,
            habitLogs = organizer.habitLogs,
            selectedDate = selectedDate,
            dailyMetric = dailyMetric,
            totalWater = totalWater,
            workouts = workouts
        )

        PulseUiState(
            currentTab = controls.currentTab,
            kinesitherapySubTab = controls.kinesitherapySubTab,
            todayDate = todayDate,
            selectedDate = selectedDate,
            isSelectedDateToday = isSelectedDateToday,
            userProfile = telemetry.profile,
            showOnboarding = showOnboarding,
            habits = habitUiModels,
            tasks = organizer.tasks,
            notes = organizer.notes,
            workouts = workouts,
            foodEntries = foodEntries,
            allFoodEntries = activityNutrition.allFoodEntries,
            waterLogs = activityNutrition.waterLogs,
            dailyMetric = dailyMetric,
            recentMetrics = telemetry.recentMetrics,
            dailyFoodCaloriesByDate = dailyFoodByDate,
            dailyBurnedCaloriesByDate = dailyBurnedByDate,
            aiInsights = telemetry.aiInsights,
            kinesitherapySessions = activityNutrition.kinesitherapySessions,
            totalFoodCalories = totalFoodCals,
            totalBurnedCalories = totalBurned,
            totalWaterMl = totalWater,
            isAiLoading = controls.isAiLoading,
            userNotification = controls.notification
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PulseUiState(todayDate = todayDate, selectedDate = todayDate, isSelectedDateToday = true)
    )

    private fun buildHabitUiModels(
        habits: List<HabitEntity>,
        habitLogs: List<HabitLogEntity>,
        selectedDate: String,
        dailyMetric: DailyMetricEntity,
        totalWater: Int,
        workouts: List<WorkoutEntity>
    ): List<HabitUiModel> {
        val targetLogs = habitLogs.filter { it.date == selectedDate }
        return habits.map { habit ->
            val isDoneInDb = targetLogs.any { it.habitId == habit.id }
            val streak = computeStreak(habit.id, habitLogs)
            val isAuto = habit.autoSyncType != "NONE"

            var currentVal = 0
            val targetVal = habit.autoSyncThreshold
            var progressText = ""
            var shouldAutoDone = false

            when (habit.autoSyncType) {
                "STEPS" -> {
                    currentVal = dailyMetric.steps
                    progressText = "$currentVal / $targetVal шагов"
                    shouldAutoDone = targetVal > 0 && currentVal >= targetVal
                }
                "WATER" -> {
                    currentVal = totalWater
                    progressText = "$currentVal / $targetVal мл воды"
                    shouldAutoDone = targetVal > 0 && currentVal >= targetVal
                }
                "SLEEP" -> {
                    currentVal = dailyMetric.sleepMinutes
                    val hours = currentVal / 60
                    val mins = currentVal % 60
                    val targetHours = targetVal / 60
                    progressText = "${hours}ч ${mins}м / ${targetHours}ч сна"
                    shouldAutoDone = targetVal > 0 && currentVal >= targetVal
                }
                "DEEP_SLEEP" -> {
                    currentVal = dailyMetric.deepSleepMinutes
                    progressText = "$currentVal / $targetVal мин глуб. сна"
                    shouldAutoDone = targetVal > 0 && currentVal >= targetVal
                }
                "ACTIVE_MINUTES" -> {
                    currentVal = dailyMetric.activeMinutes
                    progressText = "$currentVal / $targetVal мин активности"
                    shouldAutoDone = targetVal > 0 && currentVal >= targetVal
                }
                "WORKOUT" -> {
                    currentVal = workouts.sumOf { it.durationMinutes }
                    progressText = if (workouts.isNotEmpty()) "${workouts.size} трен. ($currentVal мин)" else "0 / $targetVal мин"
                    shouldAutoDone = workouts.isNotEmpty() && (targetVal == 0 || currentVal >= targetVal)
                }
                "SPO2" -> {
                    currentVal = dailyMetric.spo2Percent
                    progressText = "$currentVal% / $targetVal% SpO2"
                    shouldAutoDone = targetVal > 0 && currentVal >= targetVal
                }
                "STRESS" -> {
                    currentVal = dailyMetric.stressLevel
                    progressText = "Стресс $currentVal (цель ≤ $targetVal)"
                    shouldAutoDone = targetVal > 0 && currentVal <= targetVal
                }
                else -> {
                    currentVal = if (isDoneInDb) 1 else 0
                    progressText = if (isDoneInDb) "Выполнено" else "В процессе"
                    shouldAutoDone = false
                }
            }

            val fraction = if (targetVal > 0) (currentVal.toFloat() / targetVal).coerceIn(0f, 1f) else if (isDoneInDb) 1f else 0f
            val isCompletedFinal = isDoneInDb || shouldAutoDone
            val isAutoLog = targetLogs.any { it.habitId == habit.id && it.isAutoCompleted } || shouldAutoDone

            HabitUiModel(
                habit = habit,
                isCompletedToday = isCompletedFinal,
                isAutoCompletedToday = isAutoLog,
                streakDays = streak,
                isAutoSynced = isAuto,
                currentProgress = currentVal,
                targetProgress = targetVal,
                progressFraction = fraction,
                progressStatusText = progressText
            )
        }
    }

    val habitsTabState: StateFlow<HabitsTabState> = combine(
        habitsOnlyFlow,
        activityNutritionFlow,
        telemetryAiFlow,
        isAiLoadingFlow
    ) { (habits, habitLogs), activityNutrition, telemetry, isAiLoading ->
        val selectedDate = activityNutrition.selectedDate
        val isToday = selectedDate == todayDate
        val dailyMetric = if (isToday) {
            telemetry.dailyMetric
        } else {
            telemetry.recentMetrics.find { it.date == selectedDate } ?: DailyMetricEntity(date = selectedDate)
        }
        val workouts = activityNutrition.allWorkouts.filter { it.date == selectedDate }
        val totalWater = activityNutrition.waterLogs.sumOf { it.amountMl }
        val habitUiModels = buildHabitUiModels(
            habits = habits,
            habitLogs = habitLogs,
            selectedDate = selectedDate,
            dailyMetric = dailyMetric,
            totalWater = totalWater,
            workouts = workouts
        )
        HabitsTabState(
            habits = habitUiModels,
            selectedDate = selectedDate,
            isSelectedDateToday = isToday,
            todayDate = todayDate,
            isAiLoading = isAiLoading
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HabitsTabState(selectedDate = todayDate, isSelectedDateToday = true, todayDate = todayDate)
    )

    val workoutsTabState: StateFlow<WorkoutsTabState> = combine(
        activityNutritionFlow,
        kinesitherapySubTabFlow,
        repository.userProfile,
        repository.getDailyMetric(todayDate),
        isAiLoadingFlow
    ) { activityNutrition, kinesitherapySubTab, profile, dailyMetric, isAiLoading ->
        val selectedDate = activityNutrition.selectedDate
        WorkoutsTabState(
            workouts = activityNutrition.allWorkouts.filter { it.date == selectedDate },
            kinesitherapySessions = activityNutrition.kinesitherapySessions,
            kinesitherapySubTab = kinesitherapySubTab,
            userProfile = profile ?: UserProfileEntity(),
            todayDate = todayDate,
            dailyMetric = dailyMetric ?: DailyMetricEntity(date = todayDate),
            isAiLoading = isAiLoading
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = WorkoutsTabState(todayDate = todayDate)
    )

    val nutritionTabState: StateFlow<NutritionTabState> = combine(
        activityNutritionFlow,
        repository.userProfile,
        repository.getDailyMetric(todayDate),
        isAiLoadingFlow
    ) { activityNutrition, profile, dailyMetric, isAiLoading ->
        val selectedDate = activityNutrition.selectedDate
        val foodEntries = activityNutrition.allFoodEntries.filter { it.date == selectedDate }
        val totalFoodCals = foodEntries.sumOf { it.calories }
        val totalWater = activityNutrition.waterLogs.sumOf { it.amountMl }
        val isToday = selectedDate == todayDate
        val totalBurned = activityNutrition.allWorkouts.filter { it.date == selectedDate }.sumOf { it.caloriesBurned }

        NutritionTabState(
            foodEntries = foodEntries,
            allFoodEntries = activityNutrition.allFoodEntries,
            waterLogs = activityNutrition.waterLogs,
            totalFoodCalories = totalFoodCals,
            totalWaterMl = totalWater,
            userProfile = profile ?: UserProfileEntity(),
            selectedDate = selectedDate,
            isSelectedDateToday = isToday,
            totalBurnedCalories = totalBurned,
            todayDate = todayDate,
            isAiLoading = isAiLoading,
            dailyMetric = dailyMetric ?: DailyMetricEntity(date = todayDate)
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = NutritionTabState(selectedDate = todayDate, isSelectedDateToday = true, todayDate = todayDate)
    )

    val tasksTabState: StateFlow<TasksTabState> = combine(
        tasksOnlyFlow,
        isAiLoadingFlow
    ) { (tasks, notes), isAiLoading ->
        TasksTabState(tasks = tasks, notes = notes, isAiLoading = isAiLoading)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TasksTabState()
    )

    val statsTabState: StateFlow<StatsTabState> = combine(
        telemetryAiFlow,
        activityNutritionFlow,
        isAiLoadingFlow
    ) { telemetry, activityNutrition, isAiLoading ->
        val allFood = activityNutrition.allFoodEntries
        val allWorkouts = activityNutrition.allWorkouts
        val sessions = activityNutrition.kinesitherapySessions
        val dailyFoodByDate = allFood
            .groupBy { it.date }
            .mapValues { (_, list) -> list.sumOf { it.calories } }
        val dailyBurnedByDate = allWorkouts
            .groupBy { it.date }
            .mapValues { (_, list) -> list.sumOf { it.caloriesBurned } }
        val totalFoodCals = allFood.filter { it.date == todayDate }.sumOf { it.calories }
        val totalBurned = allWorkouts.filter { it.date == todayDate }.sumOf { it.caloriesBurned }

        StatsTabState(
            dailyMetric = telemetry.dailyMetric,
            recentMetrics = telemetry.recentMetrics,
            aiInsights = telemetry.aiInsights,
            userProfile = telemetry.profile,
            dailyFoodCaloriesByDate = dailyFoodByDate,
            dailyBurnedCaloriesByDate = dailyBurnedByDate,
            isAiLoading = isAiLoading,
            kinesitherapySessions = sessions,
            foodEntries = allFood.filter { it.date == todayDate },
            allFoodEntries = allFood,
            waterLogs = activityNutrition.waterLogs,
            totalFoodCalories = totalFoodCals,
            totalBurnedCalories = totalBurned,
            todayDate = todayDate
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = StatsTabState(todayDate = todayDate)
    )

    fun setTab(tabIndex: Int) {
        currentTabFlow.value = tabIndex
    }

    fun setKinesitherapySubTab(subTab: Int) {
        kinesitherapySubTabFlow.value = subTab
    }

    fun setSelectedDate(date: String) {
        val target = try { LocalDate.parse(date) } catch (e: Exception) { return }
        val today = try { LocalDate.parse(todayDate) } catch (e: Exception) { LocalDate.now() }
        if (!target.isAfter(today)) {
            selectedDateFlow.value = date
        }
    }

    fun selectPreviousDay() {
        val current = try { LocalDate.parse(selectedDateFlow.value) } catch (e: Exception) { LocalDate.now() }
        selectedDateFlow.value = current.minusDays(1).toString()
    }

    fun selectNextDay() {
        val current = try { LocalDate.parse(selectedDateFlow.value) } catch (e: Exception) { LocalDate.now() }
        val today = try { LocalDate.parse(todayDate) } catch (e: Exception) { LocalDate.now() }
        val next = current.plusDays(1)
        if (!next.isAfter(today)) {
            selectedDateFlow.value = next.toString()
        }
    }

    fun resetToToday() {
        selectedDateFlow.value = todayDate
    }

    fun extractKinesitherapyExercisesFromPhoto(
        base64Image: String,
        onResult: (List<KinesitherapyExerciseItem>) -> Unit
    ) {
        viewModelScope.launch {
            isAiLoadingFlow.value = true
            try {
                val exercises = repository.extractExercisesFromPhoto(base64Image)
                onResult(exercises)
                userNotificationFlow.value = "AI распознал ${exercises.size} упражнений из программы!"
            } catch (e: Exception) {
                userNotificationFlow.value = "Ошибка распознавания фото: ${e.message}"
            } finally {
                isAiLoadingFlow.value = false
            }
        }
    }

    fun analyzeAndSaveKinesitherapySession(
        date: String,
        title: String,
        primaryGoal: String,
        availableExercisesPhotoBase64: String?,
        performedExercisesPhotoBase64: String?,
        exercises: List<KinesitherapyExerciseItem>,
        painBefore: Int,
        painAfter: Int,
        durationMinutes: Int,
        userNotes: String,
        onComplete: (KinesitherapyAiReport) -> Unit
    ) {
        viewModelScope.launch {
            isAiLoadingFlow.value = true
            try {
                val report = repository.analyzeKinesitherapy(
                    availableExercisesPhotoBase64 = availableExercisesPhotoBase64,
                    performedExercisesPhotoBase64 = performedExercisesPhotoBase64,
                    exercises = exercises,
                    userGoal = primaryGoal,
                    painBefore = painBefore,
                    painAfter = painAfter,
                    userNotes = userNotes
                )

                val completedExercises = exercises.count { it.isDone }
                val appContext = getApplication<Application>()
                val availablePhotoPath = availableExercisesPhotoBase64?.let {
                    ImageUtils.saveBase64ImageToInternalStorage(appContext, it, "avail") ?: it
                }
                val performedPhotoPath = performedExercisesPhotoBase64?.let {
                    ImageUtils.saveBase64ImageToInternalStorage(appContext, it, "perf") ?: it
                }
                val sessionEntity = KinesitherapySessionEntity(
                    date = date.ifBlank { todayDate },
                    title = title.ifBlank { "Занятие кинезиотерапией на МТБ" },
                    primaryGoal = primaryGoal,
                    availableExercisesPhotoBase64 = availablePhotoPath,
                    performedExercisesPhotoBase64 = performedPhotoPath,
                    exercisesJson = KinesitherapyExerciseItem.serializeList(exercises),
                    totalTonnageKg = report.totalTonnageKg,
                    exercisesCompletedCount = completedExercises,
                    exercisesTotalCount = exercises.size,
                    durationMinutes = durationMinutes,
                    painLevelBefore = painBefore,
                    painLevelAfter = painAfter,
                    aiScore = report.score,
                    aiEvaluation = report.evaluation,
                    aiLoadAnalysis = report.loadAnalysis,
                    aiGoalRecommendations = report.goalProgress,
                    aiNextSessionPlan = report.nextSessionAdvice,
                    userNotes = userNotes
                )

                repository.saveKinesitherapySession(sessionEntity)
                userNotificationFlow.value = "AI завершил аудит кинезиотерапии! Оценка: ${report.score}/10, Тоннаж: ${report.totalTonnageKg.toInt()} кг"
                onComplete(report)
            } catch (e: Exception) {
                userNotificationFlow.value = "Ошибка анализа кинезиотерапии: ${e.message}"
            } finally {
                isAiLoadingFlow.value = false
            }
        }
    }

    fun deleteKinesitherapySession(id: Long) {
        viewModelScope.launch {
            val session = uiState.value.kinesitherapySessions.find { it.id == id }
            ImageUtils.deleteInternalPhotoIfExists(session?.availableExercisesPhotoBase64)
            ImageUtils.deleteInternalPhotoIfExists(session?.performedExercisesPhotoBase64)
            repository.deleteKinesitherapySession(id)
            userNotificationFlow.value = "Запись кинезиотерапии удалена"
        }
    }

    fun dismissNotification() {
        userNotificationFlow.value = null
    }

    fun openOnboarding() {
        manualShowOnboardingFlow.value = true
    }

    fun closeOnboarding() {
        manualShowOnboardingFlow.value = false
    }

    fun saveUserProfile(
        age: Int,
        weightKg: Float,
        heightCm: Float,
        gender: String,
        activityLevel: String,
        goal: String
    ) {
        viewModelScope.launch {
            isAiLoadingFlow.value = true
            try {
                val fullProfile = NutritionCalculator.calculateFullProfile(
                    age = age,
                    weightKg = weightKg,
                    heightCm = heightCm,
                    gender = gender,
                    activityLevel = activityLevel,
                    goal = goal
                )
                repository.saveUserProfile(fullProfile)
                manualShowOnboardingFlow.value = false
                userNotificationFlow.value = "Профиль сохранен! Норма воды: ${fullProfile.targetWaterMl} мл, калории: ${fullProfile.targetCalories} ккал."
            } catch (e: Exception) {
                userNotificationFlow.value = "Ошибка сохранения: ${e.message}"
            } finally {
                isAiLoadingFlow.value = false
            }
        }
    }

    fun refreshAiAdvice() {
        viewModelScope.launch {
            isAiLoadingFlow.value = true
            try {
                repository.generatePersonalizedAdvice()
                userNotificationFlow.value = "AI-советы обновлены под вашу цель!"
            } finally {
                isAiLoadingFlow.value = false
            }
        }
    }

    // Habits actions
    fun toggleHabit(habitId: Long, isCompleted: Boolean) {
        val targetDate = selectedDateFlow.value
        val habitUi = uiState.value.habits.find { it.habit.id == habitId }
        if (habitUi != null && habitUi.isAutoSynced && targetDate == todayDate) {
            if (habitUi.isCompletedToday) {
                userNotificationFlow.value = "✓ Привычка «${habitUi.habit.title}» выполнена автоматически фитнес-браслетом / учетом воды!"
            } else {
                userNotificationFlow.value = "⚡ Привычка считается автоматически (${habitUi.progressStatusText}). Выполните норму на браслете или в питании!"
            }
            return
        }
        viewModelScope.launch {
            repository.toggleHabitCompletion(habitId, targetDate, isCompleted)
        }
    }

    fun addHabit(
        title: String,
        description: String,
        category: String,
        colorHex: String,
        autoSyncType: String = "NONE",
        threshold: Int = 0
    ) {
        viewModelScope.launch {
            repository.insertHabit(
                HabitEntity(
                    title = title,
                    description = description,
                    category = category,
                    colorHex = colorHex,
                    autoSyncType = autoSyncType,
                    autoSyncThreshold = threshold
                )
            )
            userNotificationFlow.value = "Привычка «$title» добавлена!"
        }
    }

    fun deleteHabit(habitId: Long) {
        viewModelScope.launch {
            repository.deleteHabit(habitId)
        }
    }

    // AI Habit Plan Generation
    fun generateHabitPlan(goal: String, onComplete: (List<GeneratedHabit>) -> Unit) {
        viewModelScope.launch {
            isAiLoadingFlow.value = true
            try {
                val (plan, isAi) = repository.generateHabitPlanWithStatus(goal)
                onComplete(plan)
                if (isAi) {
                    userNotificationFlow.value = "✓ План привычек под цель сформирован Gemini AI!"
                } else {
                    userNotificationFlow.value = "⚠️ Gemini AI: проверьте API-ключ (.env) или сеть. Сформирован умный план под вашу цель."
                }
            } catch (e: Exception) {
                val fallback = repository.generateFallbackHabits(goal)
                onComplete(fallback)
                userNotificationFlow.value = "⚠️ Gemini AI: проверьте API-ключ (.env) или сеть. Сформирован умный план под вашу цель."
            } finally {
                isAiLoadingFlow.value = false
            }
        }
    }

    fun acceptHabitPlan(habits: List<GeneratedHabit>) {
        viewModelScope.launch {
            habits.forEach { gen ->
                repository.insertHabit(
                    HabitEntity(
                        title = gen.title,
                        description = gen.description,
                        category = gen.category,
                        autoSyncType = gen.autoSyncType,
                        autoSyncThreshold = gen.autoSyncThreshold,
                        colorHex = when (gen.category) {
                            "Спорт" -> "#0D9488"
                            "Здоровье" -> "#0284C7"
                            else -> "#6366F1"
                        }
                    )
                )
            }
            userNotificationFlow.value = "Добавлено ${habits.size} привычки из AI-плана!"
        }
    }

    // Tasks actions
    fun toggleTask(task: TaskEntity) {
        viewModelScope.launch {
            repository.updateTask(task.copy(isCompleted = !task.isCompleted))
        }
    }

    fun addTask(title: String, priority: String, category: String, dueDate: String?) {
        viewModelScope.launch {
            repository.insertTask(
                TaskEntity(
                    title = title,
                    priority = priority,
                    category = category,
                    dueDate = dueDate
                )
            )
            userNotificationFlow.value = "Задача сохранена!"
        }
    }

    fun deleteTask(taskId: Long) {
        viewModelScope.launch {
            repository.deleteTask(taskId)
        }
    }

    fun decomposeTask(task: TaskEntity) {
        viewModelScope.launch {
            isAiLoadingFlow.value = true
            try {
                val subtasks = repository.decomposeTaskWithAi(task.id, task.title)
                userNotificationFlow.value = "AI разбил задачу на ${subtasks.size} подзадачи!"
            } finally {
                isAiLoadingFlow.value = false
            }
        }
    }

    // Notes actions
    fun addNote(title: String, content: String, tags: String, isPinned: Boolean) {
        viewModelScope.launch {
            repository.insertNote(
                NoteEntity(
                    title = title,
                    content = content,
                    tags = tags,
                    isPinned = isPinned
                )
            )
            userNotificationFlow.value = "Заметка сохранена!"
        }
    }

    fun deleteNote(noteId: Long) {
        viewModelScope.launch {
            repository.deleteNote(noteId)
        }
    }

    // Workouts & Health Connect
    fun syncHealthConnect(onRequestPermissions: ((Set<String>) -> Unit)? = null) {
        viewModelScope.launch {
            isAiLoadingFlow.value = true
            try {
                when (val result = repository.syncHealthConnect()) {
                    is HealthSyncResult.Success -> {
                        userNotificationFlow.value = result.message
                    }
                    is HealthSyncResult.NeedsPermission -> {
                        onRequestPermissions?.invoke(result.permissions)
                        userNotificationFlow.value = if (onRequestPermissions != null) {
                            "Запрос разрешений Health Connect..."
                        } else {
                            "Требуется разрешение Health Connect для чтения данных браслета."
                        }
                    }
                    is HealthSyncResult.NoData -> {
                        userNotificationFlow.value = result.message
                    }
                    is HealthSyncResult.UpdateRequired -> {
                        userNotificationFlow.value = result.message.ifBlank {
                            "Требуется установить или обновить Google Health Connect из Google Play."
                        }
                    }
                    is HealthSyncResult.NotAvailable -> {
                        userNotificationFlow.value = result.message.ifBlank {
                            "Health Connect недоступен на устройстве. Установите или обновите Google Health Connect."
                        }
                    }
                    is HealthSyncResult.Error -> {
                        userNotificationFlow.value = result.message
                    }
                }
            } catch (e: Exception) {
                userNotificationFlow.value = "Ошибка Health Connect: ${e.message}"
            } finally {
                isAiLoadingFlow.value = false
            }
        }
    }

    fun onHealthConnectPermissionsResult(granted: Set<String>) {
        viewModelScope.launch {
            if (granted.isNotEmpty()) {
                userNotificationFlow.value = "Разрешения Health Connect получены! Очищаем демо-данные и читаем браслет..."
                repository.purgeMockData()
                syncHealthConnect(null)
            } else {
                userNotificationFlow.value = "Разрешения Health Connect не были предоставлены."
            }
        }
    }

    fun purgeMockData() {
        viewModelScope.launch {
            repository.purgeMockData()
            userNotificationFlow.value = "✓ Демо-данные удалены! Отображаются только реальные показатели браслета."
        }
    }

    fun writeDemoHealthConnectDataAndSync(onRequestPermissions: ((Set<String>) -> Unit)? = null) {
        viewModelScope.launch {
            isAiLoadingFlow.value = true
            try {
                when (val result = repository.writeDemoDataToHealthConnect()) {
                    is HealthSyncResult.Success -> {
                        userNotificationFlow.value = "✓ Записано в Health Connect и прочитано: ${result.message}"
                    }
                    is HealthSyncResult.NeedsPermission -> {
                        onRequestPermissions?.invoke(result.permissions)
                        userNotificationFlow.value = if (onRequestPermissions != null) {
                            "Для записи теста требуется разрешение Health Connect..."
                        } else {
                            "Требуются разрешения Health Connect."
                        }
                    }
                    is HealthSyncResult.UpdateRequired -> {
                        userNotificationFlow.value = result.message.ifBlank {
                            "Требуется установить или обновить Google Health Connect из Google Play."
                        }
                    }
                    is HealthSyncResult.NotAvailable -> {
                        userNotificationFlow.value = result.message.ifBlank {
                            "Health Connect недоступен на устройстве. Установите или обновите Google Health Connect."
                        }
                    }
                    is HealthSyncResult.NoData -> {
                        userNotificationFlow.value = result.message
                    }
                    is HealthSyncResult.Error -> {
                        userNotificationFlow.value = result.message
                    }
                }
            } catch (e: Exception) {
                userNotificationFlow.value = "Ошибка Health Connect: ${e.message}"
            } finally {
                isAiLoadingFlow.value = false
            }
        }
    }

    fun addManualWorkout(type: String, durationMin: Int, calories: Int, avgHr: Int, distanceM: Int) {
        viewModelScope.launch {
            repository.addWorkout(
                WorkoutEntity(
                    workoutType = type,
                    date = todayDate,
                    durationMinutes = durationMin,
                    caloriesBurned = calories,
                    avgHeartRate = avgHr,
                    distanceMeters = distanceM,
                    source = "Xiaomi Smart Band 10"
                )
            )
            userNotificationFlow.value = "Тренировка зафиксирована!"
        }
    }

    fun deleteWorkout(workoutId: Long) {
        viewModelScope.launch {
            repository.deleteWorkout(workoutId)
        }
    }

    // Nutrition & Water
    fun addWater(amountMl: Int) {
        viewModelScope.launch {
            val targetDate = selectedDateFlow.value
            repository.addWaterLog(amountMl, targetDate)
            userNotificationFlow.value = "+$amountMl мл воды записано!"
        }
    }

    fun setCustomApiKey(key: String) {
        val trimmed = key.trim()
        repository.setCustomApiKey(trimmed)
        getApplication<Application>().getSharedPreferences("pulse_prefs", android.content.Context.MODE_PRIVATE)
            .edit().putString("gemini_api_key", trimmed).apply()
        userNotificationFlow.value = if (trimmed.isNotBlank()) "Ключ Gemini API сохранен!" else "Пользовательский ключ удален."
    }

    fun getEffectiveApiKey(): String = repository.getEffectiveApiKey()

    fun estimateAndAddFood(
        description: String,
        onEstimated: (FoodEstimate) -> Unit
    ) {
        viewModelScope.launch {
            isAiLoadingFlow.value = true
            try {
                val est = repository.estimateFoodWithAi(description)
                val err = repository.getLastAiError()
                if (err != null) {
                    userNotificationFlow.value = err
                }
                onEstimated(est)
            } catch (e: Exception) {
                userNotificationFlow.value = "Ошибка оценки питания: ${e.message}"
            } finally {
                isAiLoadingFlow.value = false
            }
        }
    }

    fun estimateAndAddFoodFromPhoto(
        base64Image: String,
        mimeType: String = "image/jpeg",
        userNote: String = "",
        onEstimated: (FoodEstimate) -> Unit
    ) {
        viewModelScope.launch {
            isAiLoadingFlow.value = true
            try {
                val est = repository.estimateFoodFromPhotoWithAi(base64Image, mimeType, userNote)
                val err = repository.getLastAiError()
                if (err != null) {
                    userNotificationFlow.value = err
                }
                onEstimated(est)
            } catch (e: Exception) {
                userNotificationFlow.value = "Ошибка оценки по фото: ${e.message}"
            } finally {
                isAiLoadingFlow.value = false
            }
        }
    }

    fun saveFoodEntry(
        mealType: String,
        description: String,
        calories: Int,
        protein: Int,
        fats: Int,
        carbs: Int
    ) {
        viewModelScope.launch {
            val targetDate = selectedDateFlow.value
            repository.addFoodEntry(
                FoodEntryEntity(
                    mealType = mealType,
                    description = description,
                    calories = calories,
                    proteinGrams = protein,
                    fatsGrams = fats,
                    carbsGrams = carbs,
                    date = targetDate
                )
            )
            userNotificationFlow.value = "Прием пищи добавлен (+$calories ккал)!"
        }
    }

    fun deleteFoodEntry(entryId: Long) {
        viewModelScope.launch {
            repository.deleteFoodEntry(entryId)
        }
    }

    // AI Daily Audit
    fun runAiDailyAudit() {
        viewModelScope.launch {
            isAiLoadingFlow.value = true
            try {
                repository.generateDailyAudit()
                userNotificationFlow.value = "AI-Аудит готов! Проверьте новые рекомендации."
            } finally {
                isAiLoadingFlow.value = false
            }
        }
    }

    companion object {
        internal fun computeStreak(
            habitId: Long,
            allLogs: List<HabitLogEntity>,
            today: LocalDate = LocalDate.now()
        ): Int {
            val datesWithHabit = allLogs
                .filter { it.habitId == habitId }
                .map { it.date }
                .toSet()

            if (datesWithHabit.isEmpty()) return 0

            var streak = 0
            var checkDate = today

            // If today is completed, streak includes today
            if (checkDate.toString() in datesWithHabit) {
                streak++
                checkDate = checkDate.minusDays(1)
            } else {
                // Check if yesterday was completed
                checkDate = checkDate.minusDays(1)
                if (checkDate.toString() !in datesWithHabit) return 0
            }

            while (checkDate.toString() in datesWithHabit) {
                streak++
                checkDate = checkDate.minusDays(1)
            }

            return streak
        }
    }
}
