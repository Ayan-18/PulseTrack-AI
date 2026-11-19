package com.example.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.Share
import androidx.compose.ui.platform.LocalContext
import com.example.data.local.entities.AiInsightEntity
import com.example.ui.PulseUiState
import com.example.ui.StatsTabState
import com.example.util.DataExportManager
import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import java.util.Locale
import java.time.DayOfWeek
import java.time.LocalDate

@Composable
fun StatsScreen(
    state: StatsTabState,
    onRunAiAudit: () -> Unit,
    onOpenProfile: () -> Unit = {},
    onRefreshAiAdvice: () -> Unit = {}
) {
    val context = LocalContext.current
    var selectedTimeframe by remember { mutableStateOf("WEEK") } // WEEK, MONTH

    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // AI Coach Hero Banner
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "AI-Коуч & Аудит продуктивности",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                            Text(
                                text = "На базе Gemini 3.5 Flash + телеметрия Xiaomi",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Искусственный интеллект сопоставляет показатели сна и тренировок с браслета Xiaomi с вашим питанием и списком задач, рассчитывая риск выгорания и давая персональные рекомендации.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = onRunAiAudit,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("run_ai_audit_button")
                    ) {
                        if (state.isAiLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Gemini анализирует день...", fontSize = 13.sp)
                        } else {
                            Icon(imageVector = Icons.Default.Insights, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Запустить комплексный AI-Аудит дня", fontSize = 13.sp)
                        }
                    }
                }
            }
        }

        // User Profile & Goals Card
        item {
            val profile = state.userProfile
            val goalTitle = when (profile.goal) {
                "BUILD_MUSCLE" -> "💪 Набор мышечной массы"
                "LOSE_WEIGHT" -> "🎯 Похудение и сжигание жира"
                "ENDURANCE" -> "🏃 Выносливость и кардио"
                else -> "⚖️ Поддержание тонуса и формы"
            }
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("user_profile_coach_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f, fill = false)
                                .padding(end = 8.dp)
                        ) {
                            Text(
                                text = goalTitle,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${profile.age} лет • ${profile.weightKg.toInt()} кг • ${profile.heightCm.toInt()} см",
                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                                maxLines = 1
                            )
                        }
                        OutlinedButton(
                            onClick = onOpenProfile,
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("edit_profile_button")
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Анкета", fontSize = 12.sp, maxLines = 1, softWrap = false)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "Цель калорий",
                                    style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                )
                                Text(
                                    text = "${profile.targetCalories} ккал",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFEA580C)
                                    )
                                )
                                Text(
                                    text = "BMR: ${profile.bmr} | TDEE: ${profile.tdee}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, color = MaterialTheme.colorScheme.outline)
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "Норма воды",
                                    style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                )
                                Text(
                                    text = "${profile.targetWaterMl} мл",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0284C7)
                                    )
                                )
                                Text(
                                    text = "33мл/кг + активность",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, color = MaterialTheme.colorScheme.outline)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Text(
                                text = "🥩 Белки: ${profile.targetProteinGrams}г",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold, fontSize = 11.sp),
                                maxLines = 1
                            )
                            Text(
                                text = "🥑 Жиры: ${profile.targetFatsGrams}г",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold, fontSize = 11.sp),
                                maxLines = 1
                            )
                            Text(
                                text = "🍚 Углеводы: ${profile.targetCarbsGrams}г",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold, fontSize = 11.sp),
                                maxLines = 1
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = onRefreshAiAdvice,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("refresh_ai_advice_button")
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Обновить AI-советы под мою цель", fontSize = 12.sp, maxLines = 1)
                    }
                }
            }
        }

        // Timeframe selector
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Аналитика активности",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    ),
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
                )

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.height(36.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            onClick = { selectedTimeframe = "WEEK" },
                            shape = RoundedCornerShape(8.dp),
                            color = if (selectedTimeframe == "WEEK") MaterialTheme.colorScheme.primary else Color.Transparent,
                            modifier = Modifier
                                .height(32.dp)
                                .testTag("timeframe_7_days")
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(horizontal = 12.dp)
                            ) {
                                Text(
                                    text = "7 Дней",
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    softWrap = false,
                                    fontWeight = if (selectedTimeframe == "WEEK") FontWeight.Bold else FontWeight.Medium,
                                    color = if (selectedTimeframe == "WEEK") Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Surface(
                            onClick = { selectedTimeframe = "MONTH" },
                            shape = RoundedCornerShape(8.dp),
                            color = if (selectedTimeframe == "MONTH") MaterialTheme.colorScheme.primary else Color.Transparent,
                            modifier = Modifier
                                .height(32.dp)
                                .testTag("timeframe_30_days")
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(horizontal = 12.dp)
                            ) {
                                Text(
                                    text = "30 Дней",
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    softWrap = false,
                                    fontWeight = if (selectedTimeframe == "MONTH") FontWeight.Bold else FontWeight.Medium,
                                    color = if (selectedTimeframe == "MONTH") Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // Custom Canvas Chart 1: Daily Steps & Habit Completion
        item {
            val today = remember(state.todayDate) {
                runCatching { LocalDate.parse(state.todayDate) }.getOrDefault(LocalDate.now())
            }
            val metricsByDate = remember(state.recentMetrics, state.dailyMetric) {
                val map = state.recentMetrics.associateBy { it.date }.toMutableMap()
                if (state.dailyMetric.date.isNotBlank()) {
                    map[state.dailyMetric.date] = state.dailyMetric
                }
                map
            }

            val (stepsSeries, chartLabels) = remember(selectedTimeframe, today, metricsByDate) {
                if (selectedTimeframe == "WEEK") {
                    val dates = (6 downTo 0).map { today.minusDays(it.toLong()) }
                    val steps = dates.map { d -> metricsByDate[d.toString()]?.steps ?: 0 }
                    val labels = dates.map { d ->
                        when (d.dayOfWeek) {
                            DayOfWeek.MONDAY -> "Пн"
                            DayOfWeek.TUESDAY -> "Вт"
                            DayOfWeek.WEDNESDAY -> "Ср"
                            DayOfWeek.THURSDAY -> "Чт"
                            DayOfWeek.FRIDAY -> "Пт"
                            DayOfWeek.SATURDAY -> "Сб"
                            DayOfWeek.SUNDAY -> "Вс"
                        }
                    }
                    steps to labels
                } else {
                    // 30 days grouped into 6 5-day intervals (average daily steps per interval)
                    val intervals = (5 downTo 0).map { groupIdx ->
                        val endDayOffset = groupIdx * 5L
                        val startDayOffset = endDayOffset + 4L
                        val groupDates = (startDayOffset downTo endDayOffset).map { today.minusDays(it) }
                        val recordedSteps = groupDates.map { d -> metricsByDate[d.toString()]?.steps ?: 0 }
                        val avgSteps = if (recordedSteps.isNotEmpty()) recordedSteps.average().toInt() else 0
                        val endDate = groupDates.last()
                        val label = "%02d.%02d".format(endDate.dayOfMonth, endDate.monthValue)
                        avgSteps to label
                    }
                    intervals.map { it.first } to intervals.map { it.second }
                }
            }

            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (selectedTimeframe == "WEEK") {
                            "Динамика за 7 дней (шаги Xiaomi Band)"
                        } else {
                            "Динамика за 30 дней (шаги Xiaomi Band)"
                        },
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Столбцы шагов относительно нормы 10 000 в день",
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    StepsCanvasChart(
                        stepsData = stepsSeries,
                        days = chartLabels,
                        primaryColor = primaryColor,
                        accentColor = secondaryColor
                    )
                }
            }
        }

        // Custom Canvas Chart 2: Energy Intake vs Burned
        item {
            val today = remember(state.todayDate) {
                runCatching { LocalDate.parse(state.todayDate) }.getOrDefault(LocalDate.now())
            }
            val (foodSeries, burnedSeries, calLabels) = remember(
                selectedTimeframe,
                today,
                state.dailyFoodCaloriesByDate,
                state.dailyBurnedCaloriesByDate,
                state.totalFoodCalories,
                state.totalBurnedCalories
            ) {
                if (selectedTimeframe == "WEEK") {
                    val dates = (6 downTo 0).map { today.minusDays(it.toLong()) }
                    val food = dates.map { d ->
                        val key = d.toString()
                        if (d == today) state.totalFoodCalories else (state.dailyFoodCaloriesByDate[key] ?: 0)
                    }
                    val burned = dates.map { d ->
                        val key = d.toString()
                        if (d == today) state.totalBurnedCalories else (state.dailyBurnedCaloriesByDate[key] ?: 0)
                    }
                    val labels = dates.map { d ->
                        when (d.dayOfWeek) {
                            DayOfWeek.MONDAY -> "Пн"
                            DayOfWeek.TUESDAY -> "Вт"
                            DayOfWeek.WEDNESDAY -> "Ср"
                            DayOfWeek.THURSDAY -> "Чт"
                            DayOfWeek.FRIDAY -> "Пт"
                            DayOfWeek.SATURDAY -> "Сб"
                            DayOfWeek.SUNDAY -> "Вс"
                        }
                    }
                    Triple(food, burned, labels)
                } else {
                    val intervals = (5 downTo 0).map { groupIdx ->
                        val endDayOffset = groupIdx * 5L
                        val startDayOffset = endDayOffset + 4L
                        val groupDates = (startDayOffset downTo endDayOffset).map { today.minusDays(it) }
                        val recordedFood = groupDates.map { d ->
                            if (d == today && state.totalFoodCalories > 0) state.totalFoodCalories
                            else state.dailyFoodCaloriesByDate[d.toString()] ?: 0
                        }
                        val recordedBurned = groupDates.map { d ->
                            if (d == today && state.totalBurnedCalories > 0) state.totalBurnedCalories
                            else state.dailyBurnedCaloriesByDate[d.toString()] ?: 0
                        }
                        val avgFood = if (recordedFood.isNotEmpty()) recordedFood.average().toInt() else 0
                        val avgBurned = if (recordedBurned.isNotEmpty()) recordedBurned.average().toInt() else 0
                        val endDate = groupDates.last()
                        val label = "%02d.%02d".format(endDate.dayOfMonth, endDate.monthValue)
                        Triple(avgFood, avgBurned, label)
                    }
                    Triple(
                        intervals.map { it.first },
                        intervals.map { it.second },
                        intervals.map { it.third }
                    )
                }
            }

            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = if (selectedTimeframe == "WEEK") {
                                    "Динамика за 7 дней (баланс калорий)"
                                } else {
                                    "Динамика за 30 дней (баланс калорий)"
                                },
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Калории еды vs Сожжено на тренировках",
                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    CaloriesCanvasChart(
                        food = foodSeries,
                        burned = burnedSeries,
                        days = calLabels,
                        foodColor = tertiaryColor,
                        burnedColor = secondaryColor
                    )
                }
            }
        }

        // Sleep & Productivity Correlation Card
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF6366F1),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Bedtime,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = "Корреляция сна и фокуса",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "В дни со сном > 7ч 30м по браслету Xiaomi продуктивность выполнения сложных задач повышается на 38%.",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                    }
                }
            }
        }

        // Stress & SpO2 Recovery Card
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF0D9488),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = "Восстановление: SpO2 ${state.dailyMetric.spo2Percent}% • Стресс ${state.dailyMetric.stressLevel}/100",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Организм находится в зоне «${state.dailyMetric.stressStatus}». Отличный резерв выносливости для работы и спорта.",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                    }
                }
            }
        }

        // Data Export Card
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("card_data_export")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Экспорт данных",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Клинический отчет для врача и CSV рацион для диетолога",
                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val report = DataExportManager.generateKinesitherapyReport(
                                    profile = state.userProfile,
                                    sessions = state.kinesitherapySessions
                                )
                                DataExportManager.sharePlainText(
                                    context = context,
                                    title = "Клинический отчет кинезиотерапии • PulseTrack AI",
                                    content = report
                                )
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("button_export_kinesitherapy")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Отчет для врача (Кинезиотерапия)",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val csv = DataExportManager.generateNutritionCsv(
                                    foodEntries = state.allFoodEntries.ifEmpty { state.foodEntries },
                                    waterLogs = state.waterLogs
                                )
                                DataExportManager.sharePlainText(
                                    context = context,
                                    title = "Дневник питания и гидратации (CSV) • PulseTrack AI",
                                    content = csv
                                )
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("button_export_nutrition_csv")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Дневник питания (CSV экспорт)",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }
        }

        // AI Insights Feed Header
        item {
            Text(
                text = "История отчетов AI-Коуча",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        if (state.aiInsights.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Нажмите «Запустить комплексный AI-Аудит дня» выше, чтобы получить первый глубокий разбор от Gemini.",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(state.aiInsights, key = { it.id }) { insight ->
                AiInsightCard(insight = insight)
            }
        }
    }
}

@Composable
fun StepsCanvasChart(
    stepsData: List<Int>,
    days: List<String>,
    primaryColor: Color,
    accentColor: Color
) {
    val animProgress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        animProgress.animateTo(1f, animationSpec = tween(700))
    }

    val maxSteps = 12000f
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val outlineColor = MaterialTheme.colorScheme.outline

    val labelPaint = remember(onSurfaceColor) {
        Paint().apply {
            color = onSurfaceColor.toArgb()
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
    }

    val goalPaint = remember(outlineColor) {
        Paint().apply {
            color = outlineColor.toArgb()
            isAntiAlias = true
            textAlign = Paint.Align.RIGHT
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
        ) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val topPadding = 22.dp.toPx()
            val chartHeight = canvasHeight - topPadding
            val barWidth = 24.dp.toPx()
            val spacing = (canvasWidth - (barWidth * stepsData.size)) / (stepsData.size + 1)

            labelPaint.textSize = 9.sp.toPx()
            goalPaint.textSize = 9.sp.toPx()

            // Baseline goal line (10 000 steps)
            val goalY = canvasHeight - ((10000f / maxSteps) * chartHeight)
            drawLine(
                color = outlineColor.copy(alpha = 0.35f),
                start = Offset(0f, goalY),
                end = Offset(canvasWidth, goalY),
                strokeWidth = 2f
            )

            // Goal label: "Цель: 10 000"
            drawContext.canvas.nativeCanvas.drawText(
                "Цель: 10 000",
                canvasWidth - 4.dp.toPx(),
                goalY - 4.dp.toPx(),
                goalPaint
            )

            stepsData.forEachIndexed { index, steps ->
                val x = spacing + index * (barWidth + spacing)
                val targetHeight = ((steps / maxSteps) * chartHeight * animProgress.value).coerceIn(4f, chartHeight)
                val y = canvasHeight - targetHeight

                val isTargetMet = steps >= 10000
                val barColor = if (isTargetMet) accentColor else primaryColor

                drawRoundRect(
                    color = barColor,
                    topLeft = Offset(x, y),
                    size = Size(barWidth, targetHeight),
                    cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                )

                // Format steps number: e.g. 8.2k, 10.4k, 11.2k or 750
                val stepsText = if (steps >= 1000) {
                    String.format(Locale.US, "%.1fk", steps / 1000f)
                } else if (steps > 0) {
                    steps.toString()
                } else {
                    "0"
                }

                drawContext.canvas.nativeCanvas.drawText(
                    stepsText,
                    x + barWidth / 2f,
                    (y - 5.dp.toPx()).coerceAtLeast(14.sp.toPx()),
                    labelPaint
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Days of week row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            days.forEach { day ->
                Text(
                    text = day,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        }
    }
}

@Composable
fun CaloriesCanvasChart(
    food: List<Int>,
    burned: List<Int>,
    days: List<String>,
    foodColor: Color,
    burnedColor: Color
) {
    val maxCals = 2500f
    val foodPaint = remember(foodColor) {
        Paint().apply {
            color = foodColor.toArgb()
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
    }

    val burnedPaint = remember(burnedColor) {
        Paint().apply {
            color = burnedColor.toArgb()
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
        ) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val topPadding = 20.dp.toPx()
            val chartHeight = canvasHeight - topPadding
            val groupWidth = 32.dp.toPx()
            val singleBarWidth = 12.dp.toPx()
            val spacing = (canvasWidth - (groupWidth * days.size)) / (days.size + 1)

            foodPaint.textSize = 8.5.sp.toPx()
            burnedPaint.textSize = 8.5.sp.toPx()

            days.indices.forEach { i ->
                val groupX = spacing + i * (groupWidth + spacing)

                // Food Bar
                val f = food.getOrElse(i) { 2000 }
                val fHeight = ((f / maxCals) * chartHeight).coerceIn(4f, chartHeight)
                val fY = canvasHeight - fHeight
                drawRoundRect(
                    color = foodColor,
                    topLeft = Offset(groupX, fY),
                    size = Size(singleBarWidth, fHeight),
                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                )

                // Food label
                val foodText = if (f >= 1000) String.format(Locale.US, "%.1fk", f / 1000f) else if (f > 0) "$f" else "0"
                drawContext.canvas.nativeCanvas.drawText(
                    foodText,
                    groupX + singleBarWidth / 2f,
                    (fY - 4.dp.toPx()).coerceAtLeast(12.sp.toPx()),
                    foodPaint
                )

                // Burned Bar
                val b = burned.getOrElse(i) { 300 }
                val bHeight = ((b / 800f) * (chartHeight * 0.75f)).coerceIn(4f, chartHeight)
                val bY = canvasHeight - bHeight
                val burnedX = groupX + singleBarWidth + 4.dp.toPx()
                drawRoundRect(
                    color = burnedColor,
                    topLeft = Offset(burnedX, bY),
                    size = Size(singleBarWidth, bHeight),
                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                )

                // Burned label
                val burnedText = if (b >= 1000) String.format(Locale.US, "%.1fk", b / 1000f) else if (b > 0) "$b" else "0"
                drawContext.canvas.nativeCanvas.drawText(
                    burnedText,
                    burnedX + singleBarWidth / 2f,
                    (bY - 4.dp.toPx()).coerceAtLeast(12.sp.toPx()),
                    burnedPaint
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            days.forEach { day ->
                Text(
                    text = day,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Legend
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(8.dp).background(foodColor, CircleShape))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Калории еды", style = MaterialTheme.typography.labelSmall)

            Spacer(modifier = Modifier.width(16.dp))

            Box(modifier = Modifier.size(8.dp).background(burnedColor, CircleShape))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Сожжено в спорте", style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
fun AiInsightCard(insight: AiInsightEntity) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = insight.title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = insight.content,
                style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface)
            )
        }
    }
}

@Composable
fun StatsScreen(
    uiState: PulseUiState,
    onRunAiAudit: () -> Unit,
    onOpenProfile: () -> Unit = {},
    onRefreshAiAdvice: () -> Unit = {}
) {
    StatsScreen(
        state = StatsTabState(
            dailyMetric = uiState.dailyMetric,
            recentMetrics = uiState.recentMetrics,
            aiInsights = uiState.aiInsights,
            userProfile = uiState.userProfile,
            dailyFoodCaloriesByDate = uiState.dailyFoodCaloriesByDate,
            dailyBurnedCaloriesByDate = uiState.dailyBurnedCaloriesByDate,
            isAiLoading = uiState.isAiLoading,
            kinesitherapySessions = uiState.kinesitherapySessions,
            foodEntries = uiState.foodEntries,
            allFoodEntries = uiState.allFoodEntries,
            waterLogs = uiState.waterLogs,
            totalFoodCalories = uiState.totalFoodCalories,
            totalBurnedCalories = uiState.totalBurnedCalories,
            todayDate = uiState.todayDate
        ),
        onRunAiAudit = onRunAiAudit,
        onOpenProfile = onOpenProfile,
        onRefreshAiAdvice = onRefreshAiAdvice
    )
}

