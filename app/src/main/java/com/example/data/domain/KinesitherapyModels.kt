package com.example.data.domain

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class KinesitherapyExerciseItem(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val category: String = "ДЕКОМПРЕССИЯ СПИНЫ",
    val weightKg: Float = 20f,
    val sets: Int = 3,
    val reps: Int = 15,
    val isDone: Boolean = true,
    val machineName: String = "МТБ-1 (Многофункциональный тренажер Бубновского)",
    val notes: String = ""
) {
    val totalVolumeKg: Float
        get() = if (isDone) weightKg * sets * reps else 0f

    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("name", name)
            put("category", category)
            put("weightKg", weightKg.toDouble())
            put("sets", sets)
            put("reps", reps)
            put("isDone", isDone)
            put("machineName", machineName)
            put("notes", notes)
        }
    }

    companion object {
        fun fromJson(obj: JSONObject): KinesitherapyExerciseItem {
            return KinesitherapyExerciseItem(
                id = obj.optString("id", UUID.randomUUID().toString()),
                name = obj.optString("name", "Упражнение"),
                category = obj.optString("category", "ДЕКОМПРЕССИЯ СПИНЫ"),
                weightKg = obj.optDouble("weightKg", 20.0).toFloat(),
                sets = obj.optInt("sets", 3),
                reps = obj.optInt("reps", 15),
                isDone = obj.optBoolean("isDone", true),
                machineName = obj.optString("machineName", "МТБ-1"),
                notes = obj.optString("notes", "")
            )
        }

        fun parseListFromJson(jsonString: String?): List<KinesitherapyExerciseItem> {
            if (jsonString.isNullOrBlank()) return emptyList()
            val list = mutableListOf<KinesitherapyExerciseItem>()
            try {
                val array = JSONArray(jsonString)
                for (i in 0 until array.length()) {
                    list.add(fromJson(array.getJSONObject(i)))
                }
            } catch (e: Exception) {
                // Return empty if parsing failed
            }
            return list
        }

        fun serializeList(exercises: List<KinesitherapyExerciseItem>): String {
            val array = JSONArray()
            for (item in exercises) {
                array.put(item.toJson())
            }
            return array.toString()
        }

        val DEFAULT_PRESETS = listOf(
            KinesitherapyExerciseItem(
                name = "Декомпрессия поясничного отдела (тяга снизу лежа на спине)",
                category = "ДЕКОМПРЕССИЯ СПИНЫ",
                weightKg = 25f,
                sets = 3,
                reps = 15,
                machineName = "МТБ-1 нижний блок",
                notes = "Выдох 'Хаа' на тяге, без осевой нагрузки на позвонки"
            ),
            KinesitherapyExerciseItem(
                name = "Тяга верхнего блока к груди узким хватом",
                category = "ДЕКОМПРЕССИЯ СПИНЫ",
                weightKg = 30f,
                sets = 3,
                reps = 15,
                machineName = "МТБ-1 верхний блок",
                notes = "Растяжение широчайших и разгрузка грудного отдела"
            ),
            KinesitherapyExerciseItem(
                name = "Гиперэкстензия горизонтальная с декомпрессией",
                category = "КОР И ПОЯСНИЦА",
                weightKg = 5f,
                sets = 3,
                reps = 15,
                machineName = "Скамья для кинезиотерапии",
                notes = "Укрепление паравертебральных мышц, фиксация таза"
            ),
            KinesitherapyExerciseItem(
                name = "Пуловер на спине на полу / скамье с рукоятью",
                category = "ГРУДЬ И ДЫХАНИЕ",
                weightKg = 15f,
                sets = 3,
                reps = 15,
                machineName = "МТБ-1 через верхний блок",
                notes = "Раскрытие диафрагмы и экскурсии грудной клетки"
            ),
            KinesitherapyExerciseItem(
                name = "Отведение ноги назад в манжете лежа на четвереньках",
                category = "ТАЗОБЕДРЕННЫЙ СУСТАВ",
                weightKg = 15f,
                sets = 3,
                reps = 18,
                machineName = "МТБ-1 нижний блок с манжетой",
                notes = "Активация большой ягодичной, стабилизация крестцового отдела"
            ),
            KinesitherapyExerciseItem(
                name = "Приведение бедра лежа на спине (петля)",
                category = "ТАЗОБЕДРЕННЫЙ СУСТАВ",
                weightKg = 12.5f,
                sets = 3,
                reps = 15,
                machineName = "МТБ нижний блок",
                notes = "Улучшение кровотока в органах малого таза"
            ),
            KinesitherapyExerciseItem(
                name = "Скручивания на пресс с фитболом и выдохом 'Хаа'",
                category = "КОР И ПОЯСНИЦА",
                weightKg = 0f,
                sets = 3,
                reps = 20,
                machineName = "Фитбол / Коврик",
                notes = "Снятие спазма поясницы, диафрагмальный выдох"
            )
        )
    }
}

data class KinesitherapyAiReport(
    val score: Int,
    val totalTonnageKg: Float,
    val evaluation: String,
    val loadAnalysis: String,
    val goalProgress: String,
    val nextSessionAdvice: String
)
