package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.example.data.domain.NutritionCalculator
import com.example.data.local.entities.UserProfileEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingProfileDialog(
    initialProfile: UserProfileEntity,
    canDismiss: Boolean = true,
    onDismiss: () -> Unit,
    onSaveProfile: (
        age: Int,
        weightKg: Float,
        heightCm: Float,
        gender: String,
        activityLevel: String,
        goal: String
    ) -> Unit
) {
    var ageText by remember { mutableStateOf(initialProfile.age.toString()) }
    var weightText by remember { mutableStateOf(if (initialProfile.weightKg > 0) initialProfile.weightKg.toInt().toString() else "72") }
    var heightText by remember { mutableStateOf(if (initialProfile.heightCm > 0) initialProfile.heightCm.toInt().toString() else "176") }
    var gender by remember { mutableStateOf(initialProfile.gender.ifBlank { "MALE" }) }
    var activityLevel by remember { mutableStateOf(initialProfile.activityLevel.ifBlank { "MODERATE" }) }
    var goal by remember { mutableStateOf(initialProfile.goal.ifBlank { "LOSE_WEIGHT" }) }

    // Live calculation of preview
    val age = ageText.toIntOrNull() ?: 26
    val weight = weightText.toFloatOrNull() ?: 72f
    val height = heightText.toFloatOrNull() ?: 176f

    val previewProfile by remember(age, weight, height, gender, activityLevel, goal) {
        derivedStateOf {
            NutritionCalculator.calculateFullProfile(
                age = age.coerceIn(14, 100),
                weightKg = weight.coerceIn(30f, 250f),
                heightCm = height.coerceIn(100f, 240f),
                gender = gender,
                activityLevel = activityLevel,
                goal = goal
            )
        }
    }

    AlertDialog(
        onDismissRequest = {
            if (canDismiss) onDismiss()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .fillMaxWidth(0.95f)
            .padding(vertical = 16.dp)
            .testTag("onboarding_profile_dialog"),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Параметры тела и цели",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Персональный расчет норм калорий и воды",
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Gender selector
                Text(
                    text = "Пол",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FilterChip(
                        selected = gender.equals("MALE", ignoreCase = true),
                        onClick = { gender = "MALE" },
                        label = { Text("Мужской 👨") },
                        leadingIcon = if (gender.equals("MALE", ignoreCase = true)) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("chip_gender_male")
                    )
                    FilterChip(
                        selected = gender.equals("FEMALE", ignoreCase = true),
                        onClick = { gender = "FEMALE" },
                        label = { Text("Женский 👩") },
                        leadingIcon = if (gender.equals("FEMALE", ignoreCase = true)) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("chip_gender_female")
                    )
                }

                // Age, Weight, Height
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = ageText,
                        onValueChange = { if (it.length <= 3) ageText = it.filter { char -> char.isDigit() } },
                        label = { Text("Возраст, лет", maxLines = 1, softWrap = false) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_profile_age")
                    )

                    OutlinedTextField(
                        value = weightText,
                        onValueChange = { if (it.length <= 4) weightText = it.filter { char -> char.isDigit() || char == '.' } },
                        label = { Text("Вес, кг", maxLines = 1, softWrap = false) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_profile_weight")
                    )

                    OutlinedTextField(
                        value = heightText,
                        onValueChange = { if (it.length <= 3) heightText = it.filter { char -> char.isDigit() } },
                        label = { Text("Рост, см", maxLines = 1, softWrap = false) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_profile_height")
                    )
                }

                // Activity Level
                Text(
                    text = "Уровень физической активности",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                )
                val activityLevels = listOf(
                    "SEDENTARY" to "Сидячий (офис, мало шагов)",
                    "LIGHT" to "Легкий (1-2 тренировки / 8к шагов)",
                    "MODERATE" to "Умеренный (3-4 тренировки в нед.)",
                    "ACTIVE" to "Высокий (5+ интенсивных занятий)"
                )
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    activityLevels.forEach { (key, label) ->
                        val isSelected = activityLevel == key
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { activityLevel = key }
                                .testTag("activity_level_$key")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                }
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                    )
                                )
                            }
                        }
                    }
                }

                // Fitness Goal / Plan
                Text(
                    text = "Ваши планы и главная цель",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                )
                val goals = listOf(
                    Triple("LOSE_WEIGHT", "🎯 Похудеть", "Дефицит 20% калорий для сжигания жира"),
                    Triple("BUILD_MUSCLE", "💪 Набрать мышцы", "Профицит +12% и повышенный белок 2.0г/кг"),
                    Triple("MAINTAIN", "⚖️ Поддержание формы", "Баланс калорий и стабильный тонус"),
                    Triple("ENDURANCE", "🏃 Выносливость", "Кардио и оптимальная гидратация")
                )
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    goals.forEach { (key, title, subtitle) ->
                        val isSelected = goal == key
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { goal = key }
                                .testTag("goal_option_$key")
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = title,
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface
                                        )
                                    )
                                    if (isSelected) {
                                        Spacer(modifier = Modifier.weight(1f))
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.secondary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = subtitle,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }
                    }
                }

                // Scientific Live Calculation Card
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("live_calculation_card")
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Индивидуальный расчет (Mifflin-St Jeor / ВОЗ)",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.LocalFireDepartment,
                                        contentDescription = null,
                                        tint = Color(0xFFEA580C),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "${previewProfile.targetCalories} ккал",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                                Text(
                                    text = "Цель калорий в день",
                                    style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f))
                                )
                            }

                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.WaterDrop,
                                        contentDescription = null,
                                        tint = Color(0xFF0284C7),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "${previewProfile.targetWaterMl} мл",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                                Text(
                                    text = "Норма воды в день",
                                    style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f))
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Белки: ${previewProfile.targetProteinGrams}г",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold)
                                )
                                Text(
                                    text = "Жиры: ${previewProfile.targetFatsGrams}г",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold)
                                )
                                Text(
                                    text = "Углеводы: ${previewProfile.targetCarbsGrams}г",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSaveProfile(
                        age.coerceIn(14, 100),
                        weight.coerceIn(30f, 250f),
                        height.coerceIn(100f, 240f),
                        gender,
                        activityLevel,
                        goal
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("save_profile_button")
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Сохранить и получить AI-советы")
            }
        },
        dismissButton = if (canDismiss) {
            {
                TextButton(onClick = onDismiss, modifier = Modifier.testTag("dismiss_profile_button")) {
                    Text("Позже")
                }
            }
        } else null
    )
}
