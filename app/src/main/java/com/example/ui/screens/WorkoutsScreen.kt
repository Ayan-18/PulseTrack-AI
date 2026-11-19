package com.example.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Pool
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.domain.KinesitherapyExerciseItem
import com.example.data.local.entities.WorkoutEntity
import com.example.ui.PulseUiState
import com.example.ui.WorkoutsTabState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutsScreen(
    state: WorkoutsTabState,
    onSyncBand: () -> Unit,
    onAddWorkout: (type: String, duration: Int, calories: Int, avgHr: Int, distance: Int) -> Unit,
    onDeleteWorkout: (Long) -> Unit,
    onSetKinesitherapySubTab: (Int) -> Unit = {},
    onExtractKinesitherapyFromPhoto: (String, (List<KinesitherapyExerciseItem>) -> Unit) -> Unit = { _, _ -> },
    onSaveKinesitherapySession: (
        title: String,
        goal: String,
        availablePhoto: String?,
        performedPhoto: String?,
        exercises: List<KinesitherapyExerciseItem>,
        painBefore: Int,
        painAfter: Int,
        duration: Int,
        notes: String
    ) -> Unit = { _, _, _, _, _, _, _, _, _ -> },
    onDeleteKinesitherapySession: (Long) -> Unit = {},
    onWriteDemoHealthConnect: (() -> Unit)? = null,
    onPurgeMockData: (() -> Unit)? = null
) {
    var showAddDialog by remember { mutableStateOf(false) }

    val totalWorkoutCals = state.workouts.sumOf { it.caloriesBurned }
    val totalMinutes = state.workouts.sumOf { it.durationMinutes }

    val sleepHours = state.dailyMetric.sleepMinutes / 60
    val sleepMins = state.dailyMetric.sleepMinutes % 60
    val deepSleepHours = state.dailyMetric.deepSleepMinutes / 60
    val deepSleepMins = state.dailyMetric.deepSleepMinutes % 60
    val remSleepHours = state.dailyMetric.remSleepMinutes / 60
    val remSleepMins = state.dailyMetric.remSleepMinutes % 60
    val lightSleepTotalMins = if (state.dailyMetric.lightSleepMinutes > 0) {
        state.dailyMetric.lightSleepMinutes
    } else {
        (state.dailyMetric.sleepMinutes - state.dailyMetric.deepSleepMinutes - state.dailyMetric.remSleepMinutes).coerceAtLeast(0)
    }
    val lightSleepHours = lightSleepTotalMins / 60
    val lightSleepMins = lightSleepTotalMins % 60

    // Pulse animation for heart rate
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(650, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Column(modifier = Modifier.fillMaxSize()) {
        // Segmented Sub-Tab Switcher
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(14.dp))
                .padding(4.dp)
        ) {
            Surface(
                onClick = { onSetKinesitherapySubTab(0) },
                shape = RoundedCornerShape(10.dp),
                color = if (state.kinesitherapySubTab == 0) MaterialTheme.colorScheme.surface else Color.Transparent,
                modifier = Modifier
                    .weight(1f)
                    .testTag("tab_xiaomi_band")
            ) {
                Row(
                    modifier = Modifier.padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Watch,
                        contentDescription = null,
                        tint = if (state.kinesitherapySubTab == 0) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Браслет Xiaomi",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = if (state.kinesitherapySubTab == 0) FontWeight.Bold else FontWeight.Normal,
                            color = if (state.kinesitherapySubTab == 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }

            Surface(
                onClick = { onSetKinesitherapySubTab(1) },
                shape = RoundedCornerShape(10.dp),
                color = if (state.kinesitherapySubTab == 1) MaterialTheme.colorScheme.surface else Color.Transparent,
                modifier = Modifier
                    .weight(1f)
                    .testTag("tab_kinesitherapy")
            ) {
                Row(
                    modifier = Modifier.padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.SelfImprovement,
                        contentDescription = null,
                        tint = if (state.kinesitherapySubTab == 1) Color(0xFF0F766E) else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Кинезиотерапия",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = if (state.kinesitherapySubTab == 1) FontWeight.Bold else FontWeight.Normal,
                            color = if (state.kinesitherapySubTab == 1) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        }

        if (state.kinesitherapySubTab == 1) {
            KinesitherapyView(
                state = state,
                onExtractExercisesFromPhoto = onExtractKinesitherapyFromPhoto,
                onSaveSession = onSaveKinesitherapySession,
                onDeleteSession = onDeleteKinesitherapySession
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 4.dp, bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
        // Device Banner Header
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f).padding(end = 8.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Watch,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Фитнес-браслет",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    maxLines = 1
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .background(Color(0xFF10B981), CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = "Google Health Connect",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = Color(0xFF0F766E),
                                            fontWeight = FontWeight.SemiBold
                                        ),
                                        maxLines = 1
                                    )
                                }
                            }
                        }

                        // Status Badge
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFDCFCE7)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Favorite,
                                    contentDescription = null,
                                    tint = Color(0xFF15803D),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Активен",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF15803D)
                                    )
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Sync & Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onSyncBand,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                            modifier = Modifier
                                .weight(1.35f)
                                .testTag("button_sync_health_connect")
                        ) {
                            if (state.isAiLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = Color.White
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Sync,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Считать из Health Connect",
                                fontSize = 11.sp,
                                maxLines = 1,
                                softWrap = false,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        }

                        OutlinedButton(
                            onClick = { showAddDialog = true },
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                            modifier = Modifier
                                .weight(0.85f)
                                .testTag("button_manual_workout")
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Тренировка", fontSize = 11.sp, maxLines = 1, softWrap = false)
                        }
                    }

                    if (onWriteDemoHealthConnect != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = onWriteDemoHealthConnect,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("button_test_health_connect")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bolt,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "Тест: Записать демо-данные в Health Connect и считать",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    if (onPurgeMockData != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        TextButton(
                            onClick = onPurgeMockData,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("button_purge_mock_data")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.outline
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "Очистить демо-данные (оставить только браслет)",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
            }
        }

        // Shortcut banner to Kinesitherapy
        item {
            val isDark = isSystemInDarkTheme()
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDark) Color(0xFF134E4A).copy(alpha = 0.45f) else Color(0xFFF0FDFA)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSetKinesitherapySubTab(1) }
                    .testTag("banner_shortcut_kinesitherapy")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF0F766E),
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.SelfImprovement,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Кинезиотерапия и ЛФК",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color(0xFF5EEAD4) else Color(0xFF0F766E)
                            )
                        )
                        Text(
                            text = "Скинуть фото программы, веса и получить расчет нагрузки AI",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = if (isDark) Color(0xFFCCFBF1) else Color(0xFF134E4A)
                            )
                        )
                    }
                    Text(
                        text = "Перейти ➔",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color(0xFF5EEAD4) else Color(0xFF0F766E)
                        )
                    )
                }
            }
        }

        // Section: Full Biometrics from Xiaomi Band
        item {
            Text(
                text = "Биометрические показатели браслета",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        }

        // 1. SpO2 (Blood Oxygen) & Stress Cards (Row)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // SpO2 Card
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFFE0F2FE),
                                modifier = Modifier.size(28.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Air,
                                        contentDescription = null,
                                        tint = Color(0xFF0284C7),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Кислород SpO2",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = "${state.dailyMetric.spo2Percent}%",
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0284C7)
                                )
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "в норме",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color(0xFF10B981),
                                    fontWeight = FontWeight.Bold
                                ),
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }
                        Text(
                            text = "Ночной минимум: 96%",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        )
                    }
                }

                // Stress Card
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val stressBg = if (state.dailyMetric.stressLevel < 35) Color(0xFFDCFCE7) else Color(0xFFFEF3C7)
                            val stressTint = if (state.dailyMetric.stressLevel < 35) Color(0xFF16A34A) else Color(0xFFD97706)
                            Surface(
                                shape = CircleShape,
                                color = stressBg,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Spa,
                                        contentDescription = null,
                                        tint = stressTint,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Уровень стресса",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = "${state.dailyMetric.stressLevel}",
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (state.dailyMetric.stressLevel < 35) Color(0xFF16A34A) else Color(0xFFD97706)
                                )
                            )
                            Text(
                                text = " / 100",
                                style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.outline),
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }
                        Text(
                            text = "Зона: ${state.dailyMetric.stressStatus}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = if (state.dailyMetric.stressLevel < 35) Color(0xFF16A34A) else Color(0xFFD97706),
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.sp
                            )
                        )
                    }
                }
            }
        }

        // 2. Comprehensive Sleep & Phases Card
        item {
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 8.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFFEDE9FE),
                                modifier = Modifier.size(32.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Bedtime,
                                        contentDescription = null,
                                        tint = Color(0xFF7C3AED),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Анализ сна и фаз",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Считывание браслета Xiaomi с 23:45 до 07:30",
                                    style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFEDE9FE)
                        ) {
                            Text(
                                text = "Оценка: ${state.dailyMetric.sleepScore}/100",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = Color(0xFF6D28D9),
                                    fontWeight = FontWeight.Bold
                                ),
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "${sleepHours}ч ${sleepMins}м",
                                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Общее время сна",
                                style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Phases breakdown - full width evenly spaced
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        PhasePill(title = "Глубокий", value = "${deepSleepHours}ч ${deepSleepMins}м", color = Color(0xFF4338CA), modifier = Modifier.weight(1f))
                        PhasePill(title = "Быстрый (REM)", value = "${remSleepHours}ч ${remSleepMins}м", color = Color(0xFF8B5CF6), modifier = Modifier.weight(1f))
                        PhasePill(title = "Легкий", value = "${lightSleepHours}ч ${lightSleepMins}м", color = Color(0xFFA78BFA), modifier = Modifier.weight(1f))
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Sleep Phase Multi-Color Bar
                    val totalSleepForBar = state.dailyMetric.sleepMinutes.coerceAtLeast(1).toFloat()
                    val deepWeight = (state.dailyMetric.deepSleepMinutes / totalSleepForBar).coerceIn(0.05f, 0.85f)
                    val remWeight = (state.dailyMetric.remSleepMinutes / totalSleepForBar).coerceIn(0.05f, 0.85f)
                    val lightWeight = (lightSleepTotalMins / totalSleepForBar).coerceIn(0.05f, 0.85f)
                    val awakeWeight = (1f - (deepWeight + remWeight + lightWeight)).coerceAtLeast(0.02f)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp))
                    ) {
                        Box(modifier = Modifier.weight(deepWeight).fillMaxSize().background(Color(0xFF4338CA)))
                        Box(modifier = Modifier.weight(remWeight).fillMaxSize().background(Color(0xFF8B5CF6)))
                        Box(modifier = Modifier.weight(lightWeight).fillMaxSize().background(Color(0xFFA78BFA)))
                        Box(modifier = Modifier.weight(awakeWeight).fillMaxSize().background(Color(0xFFE2E8F0)))
                    }
                }
            }
        }

        // 3. Heart Rate & Activity Time (Row)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Heart Rate Card with animated pulse
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFFFEE2E2),
                                modifier = Modifier
                                    .size(28.dp)
                                    .scale(pulseScale)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Favorite,
                                        contentDescription = null,
                                        tint = Color(0xFFEF4444),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Пульс (Live)",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = "${state.dailyMetric.avgHeartRate}",
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFEF4444)
                                )
                            )
                            Text(
                                text = " уд/мин",
                                style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.outline),
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }
                        Text(
                            text = "Покой: ${state.dailyMetric.minHeartRate} • Пик: ${state.dailyMetric.maxHeartRate}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        )
                    }
                }

                // Active Time & Vitality Card
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFFFEF3C7),
                                modifier = Modifier.size(28.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Bolt,
                                        contentDescription = null,
                                        tint = Color(0xFFD97706),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Активное время",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = "${state.dailyMetric.activeMinutes}",
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFD97706)
                                )
                            )
                            Text(
                                text = " мин",
                                style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.outline),
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }
                        Text(
                            text = "Разминка: ${state.dailyMetric.standingHours}/12 ч",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        )
                    }
                }
            }
        }

        // Workouts Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Тренировки и активности",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "С браслета: $totalMinutes мин • $totalWorkoutCals ккал сожжено",
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }
            }
        }

        // Workouts List
        if (state.workouts.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.FitnessCenter,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Сегодня тренировок еще не было",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Text(
                            text = "Нажмите «Считать все данные» для считывания тренировки с Xiaomi Band",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                    }
                }
            }
        } else {
            items(state.workouts, key = { it.id }) { workout ->
                WorkoutCard(
                    workout = workout,
                    onDelete = { onDeleteWorkout(workout.id) }
                )
            }
        }
    }
        }
    }

    if (showAddDialog) {
        AddWorkoutDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { type, duration, cals, hr, dist ->
                onAddWorkout(type, duration, cals, hr, dist)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun PhasePill(title: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                color = color
            ),
            maxLines = 1
        )
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            maxLines = 1
        )
    }
}

@Composable
fun WorkoutCard(
    workout: WorkoutEntity,
    onDelete: () -> Unit
) {
    val icon: ImageVector = when {
        "бег" in workout.workoutType.lowercase() -> Icons.AutoMirrored.Filled.DirectionsRun
        "ходьба" in workout.workoutType.lowercase() -> Icons.AutoMirrored.Filled.DirectionsWalk
        "вело" in workout.workoutType.lowercase() -> Icons.AutoMirrored.Filled.DirectionsBike
        "плав" in workout.workoutType.lowercase() -> Icons.Default.Pool
        "йога" in workout.workoutType.lowercase() -> Icons.Default.SelfImprovement
        else -> Icons.Default.FitnessCenter
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = workout.workoutType,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "${workout.durationMinutes} мин",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                    )
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.outline)
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.LocalFireDepartment,
                            contentDescription = null,
                            tint = Color(0xFFEA580C),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "${workout.caloriesBurned} ккал",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFEA580C)
                            )
                        )
                    }
                    if (workout.avgHeartRate > 0) {
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.outline)
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = null,
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = "${workout.avgHeartRate}",
                                style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFEF4444))
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Watch,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = workout.source,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    )
                }
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Удалить",
                    tint = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddWorkoutDialog(
    onDismiss: () -> Unit,
    onConfirm: (type: String, duration: Int, calories: Int, avgHr: Int, distance: Int) -> Unit
) {
    var workoutType by remember { mutableStateOf("Бег на улице") }
    var durationText by remember { mutableStateOf("30") }
    var caloriesText by remember { mutableStateOf("250") }
    var hrText by remember { mutableStateOf("145") }
    var distanceText by remember { mutableStateOf("3500") }

    val types = listOf(
        "Бег на улице",
        "Быстрая ходьба",
        "Силовая тренировка",
        "Велотренажер",
        "Плавание",
        "HIIT интервальная",
        "Йога и растяжка"
    )
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Записать активность", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it }
                ) {
                    OutlinedTextField(
                        value = workoutType,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Вид тренировки") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        types.forEach { t ->
                            DropdownMenuItem(
                                text = { Text(t) },
                                onClick = {
                                    workoutType = t
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = durationText,
                        onValueChange = { durationText = it.filter { ch -> ch.isDigit() } },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        label = { Text("Длит-сть (мин)") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = caloriesText,
                        onValueChange = { caloriesText = it.filter { ch -> ch.isDigit() } },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        label = { Text("Калории (ккал)") },
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = hrText,
                        onValueChange = { hrText = it.filter { ch -> ch.isDigit() } },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        label = { Text("Средний пульс") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = distanceText,
                        onValueChange = { distanceText = it.filter { ch -> ch.isDigit() } },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        label = { Text("Дистанция (м)") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val duration = durationText.toIntOrNull() ?: 30
                    val calories = caloriesText.toIntOrNull() ?: 250
                    val hr = hrText.toIntOrNull() ?: 140
                    val dist = distanceText.toIntOrNull() ?: 0
                    onConfirm(workoutType, duration, calories, hr, dist)
                }
            ) {
                Text("Сохранить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        }
    )
}

@Composable
fun WorkoutsScreen(
    uiState: PulseUiState,
    onSyncBand: () -> Unit,
    onAddWorkout: (type: String, duration: Int, calories: Int, avgHr: Int, distance: Int) -> Unit,
    onDeleteWorkout: (Long) -> Unit,
    onSetKinesitherapySubTab: (Int) -> Unit = {},
    onExtractKinesitherapyFromPhoto: (String, (List<KinesitherapyExerciseItem>) -> Unit) -> Unit = { _, _ -> },
    onSaveKinesitherapySession: (
        title: String,
        goal: String,
        availablePhoto: String?,
        performedPhoto: String?,
        exercises: List<KinesitherapyExerciseItem>,
        painBefore: Int,
        painAfter: Int,
        duration: Int,
        notes: String
    ) -> Unit = { _, _, _, _, _, _, _, _, _ -> },
    onDeleteKinesitherapySession: (Long) -> Unit = {},
    onWriteDemoHealthConnect: (() -> Unit)? = null
) {
    WorkoutsScreen(
        state = WorkoutsTabState(
            workouts = uiState.workouts,
            kinesitherapySessions = uiState.kinesitherapySessions,
            kinesitherapySubTab = uiState.kinesitherapySubTab,
            userProfile = uiState.userProfile,
            todayDate = uiState.todayDate,
            dailyMetric = uiState.dailyMetric,
            isAiLoading = uiState.isAiLoading
        ),
        onSyncBand = onSyncBand,
        onAddWorkout = onAddWorkout,
        onDeleteWorkout = onDeleteWorkout,
        onSetKinesitherapySubTab = onSetKinesitherapySubTab,
        onExtractKinesitherapyFromPhoto = onExtractKinesitherapyFromPhoto,
        onSaveKinesitherapySession = onSaveKinesitherapySession,
        onDeleteKinesitherapySession = onDeleteKinesitherapySession,
        onWriteDemoHealthConnect = onWriteDemoHealthConnect
    )
}

