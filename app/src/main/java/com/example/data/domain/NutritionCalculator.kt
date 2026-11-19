package com.example.data.domain

import com.example.data.local.entities.UserProfileEntity

object NutritionCalculator {

    fun calculateBmr(weightKg: Float, heightCm: Float, age: Int, gender: String): Int {
        // Mifflin-St Jeor formula
        val base = (10f * weightKg) + (6.25f * heightCm) - (5f * age)
        return if (gender.equals("FEMALE", ignoreCase = true) || gender.equals("Женский", ignoreCase = true)) {
            (base - 161f).toInt().coerceAtLeast(1000)
        } else {
            (base + 5f).toInt().coerceAtLeast(1200)
        }
    }

    fun getActivityMultiplier(level: String): Float = when (level.uppercase()) {
        "SEDENTARY", "СИДЯЧИЙ" -> 1.2f
        "LIGHT", "ЛЕГКИЙ", "ЛЕГКАЯ" -> 1.375f
        "MODERATE", "УМЕРЕННЫЙ", "УМЕРЕННАЯ" -> 1.55f
        "ACTIVE", "ВЫСОКИЙ", "ВЫСОКАЯ" -> 1.725f
        "VERY_ACTIVE", "СПОРТСМЕН" -> 1.9f
        else -> 1.45f
    }

    fun calculateTdee(bmr: Int, activityLevel: String): Int {
        return (bmr * getActivityMultiplier(activityLevel)).toInt()
    }

    fun calculateTargetCalories(tdee: Int, goal: String, gender: String): Int {
        val minCal = if (gender.equals("FEMALE", ignoreCase = true) || gender.equals("Женский", ignoreCase = true)) 1200 else 1500
        val target = when (goal.uppercase()) {
            "LOSE_WEIGHT", "ПОХУДЕТЬ", "СНИЖЕНИЕ ВЕСА" -> (tdee * 0.80f).toInt() // 20% deficit
            "BUILD_MUSCLE", "НАБРАТЬ МЫШЦЫ", "НАБОР МАССЫ" -> (tdee * 1.12f).toInt() // 12% surplus
            "ENDURANCE", "ВЫНОСЛИВОСТЬ" -> (tdee * 1.05f).toInt()
            else -> tdee // MAINTAIN / ПОДДЕРЖАНИЕ
        }
        return target.coerceAtLeast(minCal)
    }

    fun calculateTargetWaterMl(weightKg: Float, activityLevel: String): Int {
        val baseWater = weightKg * 33f
        val activityBonus = when (activityLevel.uppercase()) {
            "SEDENTARY", "СИДЯЧИЙ" -> 0
            "LIGHT", "ЛЕГКИЙ", "ЛЕГКАЯ" -> 250
            "MODERATE", "УМЕРЕННЫЙ", "УМЕРЕННАЯ" -> 450
            "ACTIVE", "ВЫСОКИЙ", "ВЫСОКАЯ" -> 700
            "VERY_ACTIVE", "СПОРТСМЕН" -> 1000
            else -> 400
        }
        val total = ((baseWater + activityBonus) / 50).toInt() * 50
        return total.coerceIn(1500, 4500)
    }

    fun calculateMacros(targetCalories: Int, weightKg: Float, goal: String): Triple<Int, Int, Int> {
        val proteinPerKg = when (goal.uppercase()) {
            "BUILD_MUSCLE", "НАБРАТЬ МЫШЦЫ", "НАБОР МАССЫ" -> 2.0f
            "LOSE_WEIGHT", "ПОХУДЕТЬ", "СНИЖЕНИЕ ВЕСА" -> 1.8f
            else -> 1.5f
        }
        val proteinG = (weightKg * proteinPerKg).toInt()
        val fatsG = (weightKg * 0.9f).toInt()
        val caloriesFromProteinAndFat = (proteinG * 4) + (fatsG * 9)
        val remainingCalories = (targetCalories - caloriesFromProteinAndFat).coerceAtLeast(100)
        val carbsG = (remainingCalories / 4)
        return Triple(proteinG, fatsG, carbsG)
    }

    fun calculateFullProfile(
        age: Int,
        weightKg: Float,
        heightCm: Float,
        gender: String,
        activityLevel: String,
        goal: String
    ): UserProfileEntity {
        val bmr = calculateBmr(weightKg, heightCm, age, gender)
        val tdee = calculateTdee(bmr, activityLevel)
        val targetCalories = calculateTargetCalories(tdee, goal, gender)
        val targetWaterMl = calculateTargetWaterMl(weightKg, activityLevel)
        val (protein, fats, carbs) = calculateMacros(targetCalories, weightKg, goal)

        return UserProfileEntity(
            id = 1,
            age = age,
            weightKg = weightKg,
            heightCm = heightCm,
            gender = gender,
            activityLevel = activityLevel,
            goal = goal,
            bmr = bmr,
            tdee = tdee,
            targetCalories = targetCalories,
            targetWaterMl = targetWaterMl,
            targetProteinGrams = protein,
            targetFatsGrams = fats,
            targetCarbsGrams = carbs,
            isOnboarded = true,
            updatedAt = System.currentTimeMillis()
        )
    }
}
