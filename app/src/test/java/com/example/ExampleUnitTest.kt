package com.example

import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.domain.KinesitherapyExerciseItem
import com.example.data.domain.NutritionCalculator
import com.example.data.local.AppDatabase
import com.example.data.local.entities.FoodEntryEntity
import com.example.data.local.entities.HabitLogEntity
import com.example.data.local.entities.KinesitherapySessionEntity
import com.example.data.local.entities.TaskEntity
import com.example.data.local.entities.UserProfileEntity
import com.example.data.local.entities.WaterLogEntity
import com.example.data.remote.GeminiAiRepository
import com.example.ui.PulseTrackViewModel
import com.example.util.DataExportManager
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testNutritionCalculatorFormulas() {
        // Mifflin-St Jeor BMR for male 72kg, 176cm, 26yo
        // 10*72 + 6.25*176 - 5*26 + 5 = 720 + 1100 - 130 + 5 = 1695
        val bmr = NutritionCalculator.calculateBmr(72f, 176f, 26, "MALE")
        assertEquals(1695, bmr)

        // TDEE with moderate activity (1.55) -> 1695 * 1.55 = 2627
        val tdee = NutritionCalculator.calculateTdee(bmr, "MODERATE")
        assertEquals(2627, tdee)

        // Water requirement: 72kg * 33ml + 450ml = 2826 -> rounded to 50 = 2800
        val water = NutritionCalculator.calculateTargetWaterMl(72f, "MODERATE")
        assertTrue(water in 2500..3000)

        // Full profile for LOSE_WEIGHT (approx 500 kcal deficit)
        val profile = NutritionCalculator.calculateFullProfile(
            age = 26,
            weightKg = 72f,
            heightCm = 176f,
            gender = "MALE",
            activityLevel = "MODERATE",
            goal = "LOSE_WEIGHT"
        )
        assertTrue(profile.targetCalories < profile.tdee)
        assertTrue(profile.targetProteinGrams >= 120)
        assertTrue(profile.isOnboarded)
    }

    @Test
    fun testKinesitherapyExerciseVolumeAndSerialization() {
        val exercise = KinesitherapyExerciseItem(
            name = "Декомпрессия на МТБ",
            category = "ДЕКОМПРЕССИЯ СПИНЫ",
            weightKg = 25f,
            sets = 3,
            reps = 15,
            isDone = true
        )
        // 25kg * 3 * 15 = 1125kg
        assertEquals(1125f, exercise.totalVolumeKg)

        // Serialization
        val json = exercise.toJson()
        val restored = KinesitherapyExerciseItem.fromJson(json)
        assertEquals("Декомпрессия на МТБ", restored.name)
        assertEquals(25f, restored.weightKg)
        assertEquals(3, restored.sets)
        assertEquals(15, restored.reps)
        assertTrue(restored.isDone)

        // List serialization
        val list = listOf(exercise, exercise.copy(weightKg = 30f))
        val serialized = KinesitherapyExerciseItem.serializeList(list)
        val parsed = KinesitherapyExerciseItem.parseListFromJson(serialized)
        assertEquals(2, parsed.size)
        assertEquals(1125f + 1350f, parsed.sumOf { it.totalVolumeKg.toDouble() }.toFloat())
    }

    @Test
    fun testComputeStreakLogic() {
        val today = LocalDate.of(2026, 10, 16)
        val habitId = 1L

        // Case 1: Completed today + 2 previous days = 3 days streak
        val consecutiveLogs = listOf(
            HabitLogEntity(habitId = habitId, date = today.toString()),
            HabitLogEntity(habitId = habitId, date = today.minusDays(1).toString()),
            HabitLogEntity(habitId = habitId, date = today.minusDays(2).toString())
        )
        assertEquals(3, PulseTrackViewModel.computeStreak(habitId, consecutiveLogs, today))

        // Case 2: Not completed today yet, but completed yesterday and day before = 2 days active streak
        val yesterdayLogs = listOf(
            HabitLogEntity(habitId = habitId, date = today.minusDays(1).toString()),
            HabitLogEntity(habitId = habitId, date = today.minusDays(2).toString())
        )
        assertEquals(2, PulseTrackViewModel.computeStreak(habitId, yesterdayLogs, today))

        // Case 3: Broken streak (neither today nor yesterday completed) = 0
        val brokenLogs = listOf(
            HabitLogEntity(habitId = habitId, date = today.minusDays(2).toString()),
            HabitLogEntity(habitId = habitId, date = today.minusDays(3).toString())
        )
        assertEquals(0, PulseTrackViewModel.computeStreak(habitId, brokenLogs, today))
    }

    @Test
    fun testGeminiJsonCleaningAndParsing() {
        val repo = GeminiAiRepository()

        val fencedJson = """
            ```json
            {"calories": 450, "proteinGrams": 30}
            ```
        """.trimIndent()
        val cleaned = repo.cleanJsonString(fencedJson)
        assertEquals("""{"calories": 450, "proteinGrams": 30}""", cleaned)

        val fencedArray = """
            ```json
            ["Шаг 1: Сбор данных", "Шаг 2: Анализ", "Шаг 3: Отчет"]
            ```
        """.trimIndent()
        val parsedArray = repo.parseJsonStringArray(fencedArray)
        assertEquals(listOf("Шаг 1: Сбор данных", "Шаг 2: Анализ", "Шаг 3: Отчет"), parsedArray)
    }

    @Test
    fun testDateNavigatorBarFormatting() {
        val today = "2026-10-16"
        val yesterday = "2026-10-15"
        val pastDate = "2026-10-12"

        assertEquals("Сегодня", com.example.ui.components.formatDateLabel(today, today))
        assertEquals("Вчера", com.example.ui.components.formatDateLabel(yesterday, today))

        val pastFormatted = com.example.ui.components.formatDateLabel(pastDate, today)
        assertTrue(pastFormatted.contains("12") && pastFormatted.isNotEmpty())

        val invalid = "invalid-date"
        assertEquals("invalid-date", com.example.ui.components.formatDateLabel(invalid, today))
    }

    @Test
    fun testDataExportFormatting() {
        val profile = UserProfileEntity(
            age = 28,
            weightKg = 75f,
            heightCm = 180f,
            gender = "MALE",
            goal = "BUILD_MUSCLE"
        )

        val exercise = KinesitherapyExerciseItem(
            name = "Тяга верхнего блока на МТБ",
            category = "ДЕКОМПРЕССИЯ СПИНЫ",
            weightKg = 30f,
            sets = 3,
            reps = 15,
            isDone = true
        )

        val session = KinesitherapySessionEntity(
            date = "2026-10-16",
            title = "Декомпрессия грудного и поясничного отделов",
            primaryGoal = "Укрепление мышечного корсета",
            exercisesJson = KinesitherapyExerciseItem.serializeList(listOf(exercise)),
            totalTonnageKg = 1350f,
            exercisesCompletedCount = 1,
            exercisesTotalCount = 1,
            durationMinutes = 45,
            painLevelBefore = 6,
            painLevelAfter = 2,
            aiScore = 9,
            aiEvaluation = "Отличная симметрия и разгрузка дисков",
            aiLoadAnalysis = "Оптимальный тоннаж 1350 кг",
            aiGoalRecommendations = "Продолжать тяги к груди",
            aiNextSessionPlan = "Добавить ротацию плеча"
        )

        // 1. Test Clinical Kinesitherapy Report
        val report = DataExportManager.generateKinesitherapyReport(profile, listOf(session))

        assertTrue(report.contains("КЛИНИЧЕСКИЙ ОТЧЕТ ПО КИНЕЗИОТЕРАПИИ (МТБ)"))
        assertTrue(report.contains("28 лет") && report.contains("75.0 кг"))
        assertTrue(report.contains("Укрепление мышечного корсета"))
        assertTrue(report.contains("до 6/10 ➔ после 2/10"))
        assertTrue(report.contains("1350.0 кг"))
        assertTrue(report.contains("Тяга верхнего блока на МТБ"))
        assertTrue(report.contains("9/10"))
        assertTrue(report.contains("Отличная симметрия и разгрузка дисков"))

        // 2. Test Single Session Report
        val singleReport = DataExportManager.generateSingleSessionReport(session, profile)
        assertTrue(singleReport.contains("ПРОТОКОЛ ЗАНЯТИЯ КИНЕЗИОТЕРАПИИ (МТБ)"))
        assertTrue(singleReport.contains("Декомпрессия грудного и поясничного отделов"))
        assertTrue(singleReport.contains("Тяга верхнего блока на МТБ"))

        // 3. Test Nutrition CSV Export
        val foodEntries = listOf(
            FoodEntryEntity(
                mealType = "BREAKFAST",
                description = "Овсянка с ягодами, орехи",
                calories = 420,
                proteinGrams = 14,
                fatsGrams = 12,
                carbsGrams = 65,
                date = "2026-10-16",
                timestamp = 1792137600000L
            ),
            FoodEntryEntity(
                mealType = "LUNCH",
                description = "Куриная грудка гриль, гречка",
                calories = 580,
                proteinGrams = 52,
                fatsGrams = 10,
                carbsGrams = 70,
                date = "2026-10-16",
                timestamp = 1792152000000L
            )
        )

        val waterLogs = listOf(
            WaterLogEntity(amountMl = 500, date = "2026-10-16"),
            WaterLogEntity(amountMl = 350, date = "2026-10-16")
        )

        val csv = DataExportManager.generateNutritionCsv(foodEntries, waterLogs)

        assertTrue(csv.contains("Дата,Время,Прием пищи,Блюдо,Калории (ккал),Белки (г),Жиры (г),Углеводы (г)"))
        assertTrue(csv.contains("Завтрак") && csv.contains("\"Овсянка с ягодами, орехи\"") && csv.contains("420"))
        assertTrue(csv.contains("Обед") && csv.contains("\"Куриная грудка гриль, гречка\"") && csv.contains("580"))
        assertTrue(csv.contains("ИТОГО РАЦИОН,,,Всего,1000,66,22,135"))
        assertTrue(csv.contains("--- ИТОГИ ГИДРАТАЦИИ ---"))
        assertTrue(csv.contains("2026-10-16,850"))
        assertTrue(csv.contains("ИТОГО ВОДЫ,850"))
    }

    @Test
    fun testPulseWorkSchedulerAndNotificationConstants() {
        assertEquals("PulseHealthSyncWork", com.example.worker.PulseWorkScheduler.HEALTH_SYNC_WORK_NAME)
        assertEquals("PulseReminderWork", com.example.worker.PulseWorkScheduler.REMINDER_WORK_NAME)
        assertEquals("pulse_reminders", com.example.util.NotificationHelper.CHANNEL_ID)
        assertEquals("Напоминания о привычках и воде", com.example.util.NotificationHelper.CHANNEL_NAME)
    }

    @Test
    fun testRoomDatabaseMigrationsConfig() {
        // 1. Verify exactly 5 migrations exist
        assertEquals(5, AppDatabase.ALL_MIGRATIONS.size)

        // 2. Verify versions of each migration
        assertEquals(1, AppDatabase.MIGRATION_1_2.startVersion)
        assertEquals(2, AppDatabase.MIGRATION_1_2.endVersion)

        assertEquals(2, AppDatabase.MIGRATION_2_3.startVersion)
        assertEquals(3, AppDatabase.MIGRATION_2_3.endVersion)

        assertEquals(3, AppDatabase.MIGRATION_3_4.startVersion)
        assertEquals(4, AppDatabase.MIGRATION_3_4.endVersion)

        assertEquals(4, AppDatabase.MIGRATION_4_5.startVersion)
        assertEquals(5, AppDatabase.MIGRATION_4_5.endVersion)

        assertEquals(5, AppDatabase.MIGRATION_5_6.startVersion)
        assertEquals(6, AppDatabase.MIGRATION_5_6.endVersion)

        // 3. Verify SQL execution and target tables
        val executedSql = mutableListOf<String>()

        val dummyCursor = java.lang.reflect.Proxy.newProxyInstance(
            android.database.Cursor::class.java.classLoader,
            arrayOf(android.database.Cursor::class.java)
        ) { _, method, _ ->
            when (method.name) {
                "getColumnIndex" -> 1
                "moveToNext" -> false
                "close" -> null
                else -> null
            }
        } as android.database.Cursor

        val dummyDb = java.lang.reflect.Proxy.newProxyInstance(
            SupportSQLiteDatabase::class.java.classLoader,
            arrayOf(SupportSQLiteDatabase::class.java)
        ) { _, method, args ->
            when (method.name) {
                "execSQL" -> {
                    if (args != null && args.isNotEmpty()) {
                        executedSql.add(args[0].toString())
                    }
                    null
                }
                "query" -> dummyCursor
                else -> null
            }
        } as SupportSQLiteDatabase

        for (migration in AppDatabase.ALL_MIGRATIONS) {
            migration.migrate(dummyDb)
        }

        val allSql = executedSql.joinToString(";\n")
        assertTrue(allSql.contains("water_logs"))
        assertTrue(allSql.contains("user_profile"))
        assertTrue(allSql.contains("kinesitherapy_sessions"))
        assertTrue(allSql.contains("habit_logs"))
        assertTrue(allSql.contains("tasks"))
    }

    @Test
    fun testTabStatesIsolation() {
        val habitsState = com.example.ui.HabitsTabState()
        val workoutsState = com.example.ui.WorkoutsTabState()
        val nutritionState = com.example.ui.NutritionTabState()
        val tasksState = com.example.ui.TasksTabState()
        val statsState = com.example.ui.StatsTabState()

        // Verify default values
        assertTrue(habitsState.habits.isEmpty())
        assertTrue(workoutsState.workouts.isEmpty())
        assertTrue(nutritionState.foodEntries.isEmpty())
        assertTrue(tasksState.tasks.isEmpty())
        assertTrue(statsState.aiInsights.isEmpty())

        // Verify state independence: modifying tasks state does not affect nutrition, habits or stats
        val updatedTasksState = tasksState.copy(
            tasks = listOf(
                com.example.data.local.entities.TaskEntity(
                    id = 1,
                    title = "Утренняя разминка",
                    isCompleted = true
                )
            )
        )

        assertEquals(1, updatedTasksState.tasks.size)
        assertTrue(nutritionState.foodEntries.isEmpty())
        assertTrue(habitsState.habits.isEmpty())
        assertTrue(workoutsState.workouts.isEmpty())
        assertTrue(statsState.aiInsights.isEmpty())
    }

    @Test
    fun testTaskSubtasksHierarchy() {
        val rootTask = TaskEntity(
            id = 10,
            title = "Подготовка к марафону",
            parentTaskId = null,
            isCompleted = false
        )
        val subtask1 = TaskEntity(
            id = 11,
            title = "Купить кроссовки",
            parentTaskId = 10,
            isCompleted = true
        )
        val subtask2 = TaskEntity(
            id = 12,
            title = "Пробежать 5 км",
            parentTaskId = 10,
            isCompleted = false
        )
        val allTasks = listOf(rootTask, subtask1, subtask2)

        // Filter root tasks
        val rootTasks = allTasks.filter { it.parentTaskId == null }
        assertEquals(1, rootTasks.size)
        assertEquals(10L, rootTasks.first().id)

        // Group subtasks by parent
        val subtasksByParent = allTasks.filter { it.parentTaskId != null }.groupBy { it.parentTaskId!! }
        val rootSubtasks = subtasksByParent[rootTask.id].orEmpty()
        assertEquals(2, rootSubtasks.size)

        // Check progress calculation
        val completedSubtasks = rootSubtasks.count { it.isCompleted }
        assertEquals(1, completedSubtasks)
        assertEquals("1/2", "$completedSubtasks/${rootSubtasks.size}")
    }

    @Test
    fun testTimeframeToggleLogic() {
        val today = LocalDate.of(2026, 10, 16)

        // 7-day interval
        val weekDates = (6 downTo 0).map { today.minusDays(it.toLong()) }
        assertEquals(7, weekDates.size)
        assertEquals(LocalDate.of(2026, 10, 10), weekDates.first())
        assertEquals(today, weekDates.last())

        // 30-day interval: 6 groups of 5 days
        val monthIntervals = (5 downTo 0).map { groupIdx ->
            val endDayOffset = groupIdx * 5L
            val startDayOffset = endDayOffset + 4L
            val groupDates = (startDayOffset downTo endDayOffset).map { today.minusDays(it) }
            groupDates
        }
        assertEquals(6, monthIntervals.size)
        // Each interval has 5 days
        assertTrue(monthIntervals.all { it.size == 5 })
        // Total days covered is 30
        val allMonthDays = monthIntervals.flatten()
        assertEquals(30, allMonthDays.size)
        assertEquals(LocalDate.of(2026, 9, 17), allMonthDays.first()) // 2026-10-16 minus 29 days
        assertEquals(today, allMonthDays.last())

        // Test sample metrics aggregation for 30-day mode
        val sampleMetrics = mapOf(
            today.toString() to 10000,
            today.minusDays(1).toString() to 8000
        )
        val latestGroup = monthIntervals.last() // 4 downTo 0
        val recordedSteps = latestGroup.map { d -> sampleMetrics[d.toString()] ?: 0 }
        val avgSteps = if (recordedSteps.isNotEmpty()) recordedSteps.average().toInt() else 0
        // (10000 + 8000 + 0 + 0 + 0) / 5 = 18000 / 5 = 3600
        assertEquals(3600, avgSteps)
    }

    @Test
    fun testSmartContextualHabitFallback() {
        val repo = GeminiAiRepository()

        // 1. Goal with "спина" / "мтб"
        val backHabits = repo.generateFallbackHabits("Реабилитация спины и занятия на МТБ")
        assertEquals(3, backHabits.size)
        assertTrue(backHabits.any { it.title.contains("МТБ", ignoreCase = true) || it.title.contains("спин", ignoreCase = true) })
        assertTrue(backHabits.any { it.autoSyncType == "WORKOUT" })
        assertTrue(backHabits.any { it.autoSyncType == "SLEEP" })

        // 2. Goal with "похудение"
        val weightHabits = repo.generateFallbackHabits("Похудение и сушка к лету")
        assertEquals(3, weightHabits.size)
        assertTrue(weightHabits.any { it.autoSyncType == "STEPS" && it.autoSyncThreshold == 10000 })
        assertTrue(weightHabits.any { it.autoSyncType == "WATER" && it.autoSyncThreshold == 2500 })
        assertTrue(weightHabits.any { it.title.contains("калори", ignoreCase = true) || it.title.contains("рацион", ignoreCase = true) })

        // 3. Goal with "guitar"
        val guitarHabits = repo.generateFallbackHabits("play guitar")
        assertEquals(3, guitarHabits.size)
        assertTrue(guitarHabits.any { it.title.contains("Практика", ignoreCase = true) || it.title.contains("игры", ignoreCase = true) })
        assertTrue(guitarHabits.any { it.category == "Творчество" })

        // 4. API key validation
        assertTrue(GeminiAiRepository.isValidApiKeyFormat("AIzaSyD_DummyKeyFormatValidationTest12345"))
        assertTrue(GeminiAiRepository.isValidApiKeyFormat("AQ.DummyKeyFormatValidationTest1234567890"))
        assertFalse(GeminiAiRepository.isValidApiKeyFormat(""))
        assertFalse(GeminiAiRepository.isValidApiKeyFormat("MY_GEMINI_API_KEY"))
        assertFalse(GeminiAiRepository.isValidApiKeyFormat("YOUR_GEMINI_API_KEY_HERE"))
        assertFalse(GeminiAiRepository.isValidApiKeyFormat("short"))

        assertTrue(repo.isApiKeyConfigured())
        assertTrue(GeminiAiRepository.isValidApiKeyFormat(repo.getEffectiveApiKey()))

        // 5. Test JSON String Array parsing for task decomposition
        val jsonArrayStr = "[\"Шаг 1\", \"Шаг 2\", \"Шаг 3\"]"
        val parsedArray = repo.parseJsonStringArray(jsonArrayStr)
        assertEquals(3, parsedArray.size)
        assertEquals("Шаг 1", parsedArray[0])

        val jsonObjectStr = "{\"steps\": [\"Задача A\", \"Задача B\"]}"
        val parsedObj = repo.parseJsonStringArray(jsonObjectStr)
        assertEquals(2, parsedObj.size)
        assertEquals("Задача A", parsedObj[0])

        val markdownList = "1. Первое действие\n2. Второе действие\n- Третье действие"
        val parsedMarkdown = repo.parseJsonStringArray(markdownList)
        assertEquals(3, parsedMarkdown.size)
        assertEquals("Первое действие", parsedMarkdown[0])

        // 6. Test smart local food calculation engine
        val eggs = repo.calculateLocalFoodEstimate("2 яйца")
        assertEquals(220, eggs.calories)
        assertEquals(16, eggs.proteinGrams)

        val syrniki = repo.calculateLocalFoodEstimate("3 сырника со сметаной")
        assertEquals(360, syrniki.calories)
        assertEquals(27, syrniki.proteinGrams)

        val buckwheatChicken = repo.calculateLocalFoodEstimate("Гречка с куриным филе")
        assertEquals(420, buckwheatChicken.calories)
        assertEquals(40, buckwheatChicken.proteinGrams)
    }
}



