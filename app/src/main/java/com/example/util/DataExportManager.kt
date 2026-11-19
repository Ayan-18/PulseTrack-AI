package com.example.util

import android.content.Context
import android.content.Intent
import com.example.data.domain.KinesitherapyExerciseItem
import com.example.data.local.entities.FoodEntryEntity
import com.example.data.local.entities.KinesitherapySessionEntity
import com.example.data.local.entities.UserProfileEntity
import com.example.data.local.entities.WaterLogEntity
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object DataExportManager {

    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())

    fun generateKinesitherapyReport(
        profile: UserProfileEntity,
        sessions: List<KinesitherapySessionEntity>
    ): String {
        val sb = StringBuilder()
        sb.appendLine("================================================================================")
        sb.appendLine("  КЛИНИЧЕСКИЙ ОТЧЕТ ПО КИНЕЗИОТЕРАПИИ (МТБ) • PulseTrack AI")
        sb.appendLine("================================================================================")
        sb.appendLine()
        sb.appendLine("ДАННЫЕ ПАЦИЕНТА:")
        sb.appendLine("• Возраст: ${profile.age} лет")
        sb.appendLine("• Пол: ${if (profile.gender.uppercase() == "MALE") "Мужской" else "Женский"}")
        sb.appendLine("• Вес: ${profile.weightKg} кг | Рост: ${profile.heightCm} см")
        val readableGoal = when (profile.goal.uppercase()) {
            "BUILD_MUSCLE" -> "Укрепление мышечного корсета и стабилизаторов"
            "LOSE_WEIGHT" -> "Снижение веса и разгрузка суставов"
            "MAINTAIN" -> "Поддержание мобильности и тонуса"
            "ENDURANCE" -> "Развитие выносливости"
            else -> profile.goal
        }
        sb.appendLine("• Исходная цель: $readableGoal")
        sb.appendLine()

        val totalTonnage = sessions.sumOf { it.totalTonnageKg.toDouble() }
        val avgAiScore = if (sessions.isNotEmpty()) {
            String.format(Locale.US, "%.1f", sessions.map { it.aiScore }.average())
        } else "0.0"
        val totalPainReduction = sessions.sumOf { (it.painLevelBefore - it.painLevelAfter).coerceAtLeast(0) }
        val avgPainReduction = if (sessions.isNotEmpty()) {
            String.format(Locale.US, "%.1f", totalPainReduction.toDouble() / sessions.size)
        } else "0.0"

        sb.appendLine("ОБЩАЯ СТАТИСТИКА КУРСА:")
        sb.appendLine("• Всего сессий: ${sessions.size}")
        sb.appendLine("• Суммарный тоннаж: ${String.format(Locale.US, "%.1f", totalTonnage)} кг")
        sb.appendLine("• Средняя оценка AI: $avgAiScore / 10")
        sb.appendLine("• Среднее снижение боли по ВАШ: $avgPainReduction баллов")
        sb.appendLine()
        sb.appendLine("================================================================================")
        sb.appendLine("  ХРОНОЛОГИЯ СЕССИЙ (ОТ НОВЫХ К СТАРЫМ)")
        sb.appendLine("================================================================================")

        if (sessions.isEmpty()) {
            sb.appendLine("Записи о пройденных сессиях кинезиотерапии отсутствуют.")
        } else {
            val sorted = sessions.sortedWith(
                compareByDescending<KinesitherapySessionEntity> { it.date }.thenByDescending { it.createdAt }
            )
            sorted.forEachIndexed { index, session ->
                sb.appendLine()
                sb.appendLine("--------------------------------------------------------------------------------")
                sb.appendLine("[Сессия #${sorted.size - index}] ${session.date} • ${session.title}")
                sb.appendLine("--------------------------------------------------------------------------------")
                sb.appendLine("• Основная цель: ${session.primaryGoal}")
                sb.appendLine("• Длительность: ${session.durationMinutes} мин | Тоннаж: ${session.totalTonnageKg} кг")
                val painDiff = session.painLevelBefore - session.painLevelAfter
                val painDiffText = if (painDiff > 0) "Снижение на $painDiff баллов" else if (painDiff == 0) "Без изменений" else "Усиление на ${-painDiff} баллов"
                sb.appendLine("• Динамика боли: до ${session.painLevelBefore}/10 ➔ после ${session.painLevelAfter}/10 ($painDiffText)")
                sb.appendLine("• Упражнений выполнено: ${session.exercisesCompletedCount} из ${session.exercisesTotalCount}")

                val exercises = KinesitherapyExerciseItem.parseListFromJson(session.exercisesJson)
                if (exercises.isNotEmpty()) {
                    sb.appendLine("• Протокол упражнений:")
                    exercises.forEach { ex ->
                        val status = if (ex.isDone) "✓" else "✗"
                        sb.appendLine("    $status ${ex.name} (${ex.category}): ${ex.weightKg} кг, ${ex.sets} подх. x ${ex.reps} повт. (Тоннаж: ${ex.totalVolumeKg} кг)")
                        if (ex.notes.isNotBlank()) {
                            sb.appendLine("      Примечание: ${ex.notes}")
                        }
                    }
                }

                sb.appendLine("• Оценка AI: ${session.aiScore}/10")
                if (session.aiEvaluation.isNotBlank()) {
                    sb.appendLine("• Клинический разбор: ${session.aiEvaluation}")
                }
                if (session.aiLoadAnalysis.isNotBlank()) {
                    sb.appendLine("• Анализ нагрузки: ${session.aiLoadAnalysis}")
                }
                if (session.aiGoalRecommendations.isNotBlank()) {
                    sb.appendLine("• Рекомендации: ${session.aiGoalRecommendations}")
                }
                if (session.aiNextSessionPlan.isNotBlank()) {
                    sb.appendLine("• План на следующее занятие: ${session.aiNextSessionPlan}")
                }
                if (session.userNotes.isNotBlank()) {
                    sb.appendLine("• Заметки пациента: ${session.userNotes}")
                }
            }
        }
        sb.appendLine()
        sb.appendLine("================================================================================")
        sb.appendLine("  Отчет сформирован автоматически в приложении PulseTrack AI")
        sb.appendLine("================================================================================")
        return sb.toString()
    }

    fun generateSingleSessionReport(
        session: KinesitherapySessionEntity,
        profile: UserProfileEntity? = null
    ): String {
        val sb = StringBuilder()
        sb.appendLine("================================================================================")
        sb.appendLine("  ПРОТОКОЛ ЗАНЯТИЯ КИНЕЗИОТЕРАПИИ (МТБ) • PulseTrack AI")
        sb.appendLine("================================================================================")
        sb.appendLine("• Дата: ${session.date} | Длительность: ${session.durationMinutes} мин")
        sb.appendLine("• Название: ${session.title}")
        sb.appendLine("• Направленность: ${session.primaryGoal}")
        if (profile != null) {
            sb.appendLine("• Пациент: Возраст: ${profile.age} лет, Вес: ${profile.weightKg} кг, Рост: ${profile.heightCm} см")
        }
        sb.appendLine()
        val painDiff = session.painLevelBefore - session.painLevelAfter
        val painDiffText = if (painDiff > 0) "Снижение на $painDiff баллов" else if (painDiff == 0) "Без изменений" else "Усиление на ${-painDiff} баллов"
        sb.appendLine("ПОКАЗАТЕЛИ СЕССИИ:")
        sb.appendLine("• Динамика боли: до ${session.painLevelBefore}/10 ➔ после ${session.painLevelAfter}/10 ($painDiffText)")
        sb.appendLine("• Суммарный тоннаж: ${session.totalTonnageKg} кг")
        sb.appendLine("• Выполнено упражнений: ${session.exercisesCompletedCount} из ${session.exercisesTotalCount}")
        sb.appendLine("• Оценка эффективности AI: ${session.aiScore}/10")
        sb.appendLine()

        val exercises = KinesitherapyExerciseItem.parseListFromJson(session.exercisesJson)
        if (exercises.isNotEmpty()) {
            sb.appendLine("ПРОТОКОЛ УПРАЖНЕНИЙ:")
            exercises.forEachIndexed { i, ex ->
                val status = if (ex.isDone) "[Выполнено]" else "[Пропущено]"
                sb.appendLine("${i + 1}. ${ex.name} — ${ex.weightKg} кг, ${ex.sets} подх. x ${ex.reps} повт. (Объем: ${ex.totalVolumeKg} кг) $status")
                if (ex.machineName.isNotBlank() && ex.machineName != "МТБ-1") {
                    sb.appendLine("   Тренажер: ${ex.machineName}")
                }
            }
            sb.appendLine()
        }

        sb.appendLine("АНАЛИЗ И РЕКОМЕНДАЦИИ AI:")
        if (session.aiEvaluation.isNotBlank()) sb.appendLine("• Оценка: ${session.aiEvaluation}")
        if (session.aiLoadAnalysis.isNotBlank()) sb.appendLine("• Разбор нагрузки: ${session.aiLoadAnalysis}")
        if (session.aiGoalRecommendations.isNotBlank()) sb.appendLine("• Рекомендации: ${session.aiGoalRecommendations}")
        if (session.aiNextSessionPlan.isNotBlank()) sb.appendLine("• План на следующее занятие: ${session.aiNextSessionPlan}")
        if (session.userNotes.isNotBlank()) sb.appendLine("• Заметки пациента: ${session.userNotes}")

        sb.appendLine()
        sb.appendLine("================================================================================")
        return sb.toString()
    }

    fun generateNutritionCsv(
        foodEntries: List<FoodEntryEntity>,
        waterLogs: List<WaterLogEntity>
    ): String {
        val sb = StringBuilder()
        sb.appendLine("Дата,Время,Прием пищи,Блюдо,Калории (ккал),Белки (г),Жиры (г),Углеводы (г)")

        val sortedEntries = foodEntries.sortedWith(
            compareBy<FoodEntryEntity> { it.date }.thenBy { it.timestamp }
        )

        for (entry in sortedEntries) {
            val time = try {
                Instant.ofEpochMilli(entry.timestamp)
                    .atZone(ZoneId.systemDefault())
                    .format(timeFormatter)
            } catch (e: Exception) {
                "--:--"
            }
            val meal = when (entry.mealType.uppercase()) {
                "BREAKFAST" -> "Завтрак"
                "LUNCH" -> "Обед"
                "DINNER" -> "Ужин"
                "SNACK" -> "Перекус"
                else -> entry.mealType
            }
            val escapedDescription = if (entry.description.contains(",") || entry.description.contains("\"") || entry.description.contains("\n")) {
                "\"${entry.description.replace("\"", "\"\"")}\""
            } else {
                entry.description
            }

            sb.appendLine("${entry.date},$time,$meal,$escapedDescription,${entry.calories},${entry.proteinGrams},${entry.fatsGrams},${entry.carbsGrams}")
        }

        val totalCals = foodEntries.sumOf { it.calories }
        val totalProtein = foodEntries.sumOf { it.proteinGrams }
        val totalFats = foodEntries.sumOf { it.fatsGrams }
        val totalCarbs = foodEntries.sumOf { it.carbsGrams }

        sb.appendLine()
        sb.appendLine("ИТОГО РАЦИОН,,,Всего,$totalCals,$totalProtein,$totalFats,$totalCarbs")

        sb.appendLine()
        sb.appendLine("--- ИТОГИ ГИДРАТАЦИИ ---")
        sb.appendLine("Дата,Выпито воды (мл)")
        val waterByDate = waterLogs.groupBy { it.date }.toSortedMap()

        if (waterByDate.isEmpty()) {
            sb.appendLine("Нет данных о воде,0")
        } else {
            waterByDate.forEach { (date, logs) ->
                sb.appendLine("$date,${logs.sumOf { it.amountMl }}")
            }
        }
        val totalWater = waterLogs.sumOf { it.amountMl }
        sb.appendLine("ИТОГО ВОДЫ,$totalWater")

        return sb.toString()
    }

    fun sharePlainText(context: Context, title: String, content: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, content)
        }
        val chooser = Intent.createChooser(intent, title).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }
}
