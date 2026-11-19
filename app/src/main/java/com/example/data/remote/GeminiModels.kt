package com.example.data.remote

data class FoodEstimate(
    val calories: Int,
    val proteinGrams: Int,
    val fatsGrams: Int,
    val carbsGrams: Int,
    val summary: String
)

data class GeneratedHabit(
    val title: String,
    val description: String,
    val category: String,
    val autoSyncType: String = "NONE",
    val autoSyncThreshold: Int = 0
)

data class AiAuditReport(
    val title: String,
    val content: String,
    val burnoutRisk: String, // "LOW", "MODERATE", "HIGH"
    val energyBalanceTip: String
)
