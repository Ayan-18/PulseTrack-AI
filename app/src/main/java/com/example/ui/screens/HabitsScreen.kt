package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.remote.GeneratedHabit
import com.example.ui.HabitUiModel
import com.example.ui.HabitsTabState
import com.example.ui.PulseUiState
import com.example.ui.components.DateNavigatorBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitsScreen(
    state: HabitsTabState,
    onToggleHabit: (Long, Boolean) -> Unit,
    onAddHabit: (String, String, String, String, String, Int) -> Unit,
    onDeleteHabit: (Long) -> Unit,
    onGenerateAiPlan: (String, (List<GeneratedHabit>) -> Unit) -> Unit,
    onAcceptAiPlan: (List<GeneratedHabit>) -> Unit,
    onPreviousDay: () -> Unit = {},
    onNextDay: () -> Unit = {},
    onResetToday: () -> Unit = {}
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var showAiGeneratorDialog by remember { mutableStateOf(false) }

    val completedCount = state.habits.count { it.isCompletedToday }
    val totalCount = state.habits.size
    val progress = if (totalCount > 0) completedCount.toFloat() / totalCount.toFloat() else 0f

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Date Navigator
            item {
                DateNavigatorBar(
                    selectedDate = state.selectedDate,
                    todayDate = state.todayDate,
                    onPreviousDay = onPreviousDay,
                    onNextDay = onNextDay,
                    onResetToday = onResetToday
                )
            }

            // Summary Card
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 8.dp)
                            ) {
                                Text(
                                    text = if (state.isSelectedDateToday) "Прогресс сегодня" else "Прогресс за день",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "$completedCount из $totalCount привычек закрыто",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "${(progress * 100).toInt()}%",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )

                        Spacer(modifier = Modifier.height(14.dp))
                        // AI Habit Generator Button
                        Button(
                            onClick = { showAiGeneratorDialog = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("open_ai_habit_generator")
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "AI-Генератор плана привычек (Gemini)",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                            )
                        }
                    }
                }
            }

            // Habits Section Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Ежедневные привычки",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp)
                    )
                    TextButton(onClick = { showAddDialog = true }) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Добавить", maxLines = 1, softWrap = false)
                    }
                }
            }

            // Habits List
            if (state.habits.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.TrackChanges,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Нет активных привычек",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                            Text(
                                text = "Добавьте свои или сгенерируйте персональный план через AI",
                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                        }
                    }
                }
            } else {
                items(state.habits, key = { it.habit.id }) { item ->
                    HabitCard(
                        habitUi = item,
                        onToggle = { onToggleHabit(item.habit.id, !item.isCompletedToday) },
                        onDelete = { onDeleteHabit(item.habit.id) }
                    )
                }
            }
        }

        // FAB to add habit manually
        FloatingActionButton(
            onClick = { showAddDialog = true },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = Color.White,
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 16.dp)
                .testTag("fab_add_habit")
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = "Добавить привычку")
        }
    }

    // Add Habit Dialog
    if (showAddDialog) {
        AddHabitDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { title, desc, cat, color, syncType, threshold ->
                onAddHabit(title, desc, cat, color, syncType, threshold)
                showAddDialog = false
            }
        )
    }

    // AI Generator Dialog
    if (showAiGeneratorDialog) {
        AiHabitPlanDialog(
            isLoading = state.isAiLoading,
            onDismiss = { showAiGeneratorDialog = false },
            onGenerate = { goal, onResults ->
                onGenerateAiPlan(goal, onResults)
            },
            onAccept = { accepted ->
                onAcceptAiPlan(accepted)
                showAiGeneratorDialog = false
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HabitCard(
    habitUi: HabitUiModel,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    val habit = habitUi.habit
    val cardBg = if (habitUi.isCompletedToday) {
        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
    } else {
        MaterialTheme.colorScheme.surface
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .testTag("habit_card_${habit.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Checkbox vs Auto-sync Smart Status Indicator
            if (habitUi.isAutoSynced) {
                if (habitUi.isCompletedToday) {
                    // Auto-completed badge
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF10B981),
                        modifier = Modifier
                            .size(28.dp)
                            .testTag("auto_completed_badge_${habit.id}")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Автоматически выполнено",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                } else {
                    // In-progress auto-sync ring/badge
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                        modifier = Modifier
                            .size(28.dp)
                            .testTag("auto_sync_badge_${habit.id}")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (habit.autoSyncType == "WATER") Icons.Default.WaterDrop else Icons.Default.ElectricBolt,
                                contentDescription = "Авто-синхронизация",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            } else {
                Checkbox(
                    checked = habitUi.isCompletedToday,
                    onCheckedChange = { onToggle() },
                    colors = CheckboxDefaults.colors(
                        checkedColor = MaterialTheme.colorScheme.secondary,
                        uncheckedColor = MaterialTheme.colorScheme.outline
                    ),
                    modifier = Modifier.testTag("checkbox_habit_${habit.id}")
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = habit.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = if (habitUi.isCompletedToday) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                        )
                    )
                }

                if (habit.description.isNotBlank()) {
                    Text(
                        text = habit.description,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        maxLines = 2
                    )
                }

                // If auto-synced, display live progress bar and text!
                if (habitUi.isAutoSynced) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (habitUi.isCompletedToday) "✓ Выполнено (${habitUi.progressStatusText})" else habitUi.progressStatusText,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (habitUi.isCompletedToday) Color(0xFF047857) else MaterialTheme.colorScheme.primary
                                ),
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 6.dp)
                            )
                            Text(
                                text = "${(habitUi.progressFraction * 100).toInt()}%",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (habitUi.isCompletedToday) Color(0xFF047857) else MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { habitUi.progressFraction },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = if (habitUi.isCompletedToday) Color(0xFF10B981) else MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Category Tag
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = habit.category,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    // Streak Pill
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFFFFEDD5)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocalFireDepartment,
                                contentDescription = null,
                                tint = Color(0xFFEA580C),
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = "${habitUi.streakDays} дн.",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color(0xFFC2410C),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                )
                            )
                        }
                    }

                    // Auto-sync Indicator
                    if (habitUi.isAutoSynced) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (habitUi.isCompletedToday) Color(0xFFD1FAE5) else Color(0xFFCCFBF1)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Icon(
                                    imageVector = if (habit.autoSyncType == "WATER") Icons.Default.WaterDrop else Icons.Default.Watch,
                                    contentDescription = null,
                                    tint = if (habitUi.isCompletedToday) Color(0xFF065F46) else Color(0xFF0F766E),
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                val label = if (habitUi.isCompletedToday) "Авто-выполнено" else "Синхронизируется"
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = if (habitUi.isCompletedToday) Color(0xFF065F46) else Color(0xFF0F766E),
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 10.sp
                                    )
                                )
                            }
                        }
                    }
                }
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Удалить привычку",
                    tint = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddHabitDialog(
    onDismiss: () -> Unit,
    onConfirm: (title: String, desc: String, cat: String, color: String, syncType: String, threshold: Int) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Здоровье") }
    var syncType by remember { mutableStateOf("NONE") }
    var thresholdText by remember { mutableStateOf("10000") }

    val categories = listOf("Здоровье", "Спорт", "Продуктивность", "Разум", "Другое")
    val syncOptions = listOf(
        "NONE" to "Без авто-синхронизации (вручную)",
        "STEPS" to "Браслет Xiaomi: Шаги (≥ 10 000)",
        "WORKOUT" to "Браслет Xiaomi: Любая тренировка",
        "SLEEP" to "Браслет Xiaomi: Длительность сна (≥ 7ч)",
        "DEEP_SLEEP" to "Браслет Xiaomi: Глубокий сон (≥ 90 мин)",
        "SPO2" to "Браслет Xiaomi: Кислород SpO2 (≥ 95%)",
        "STRESS" to "Браслет Xiaomi: Стресс (≤ 35 релакс)",
        "ACTIVE_MINUTES" to "Браслет Xiaomi: Активное время (≥ 45 мин)",
        "WATER" to "Дневник: Норма выпитой воды (≥ 2000 мл)"
    )

    var catExpanded by remember { mutableStateOf(false) }
    var syncExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Новая привычка", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Название привычки") },
                    placeholder = { Text("Например: 10 000 шагов") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_habit_title")
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Описание (опционально)") },
                    modifier = Modifier.fillMaxWidth()
                )

                // Category dropdown
                ExposedDropdownMenuBox(
                    expanded = catExpanded,
                    onExpandedChange = { catExpanded = it }
                ) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Категория") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = catExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(androidx.compose.material3.MenuAnchorType.PrimaryNotEditable, true)
                    )
                    ExposedDropdownMenu(
                        expanded = catExpanded,
                        onDismissRequest = { catExpanded = false }
                    ) {
                        categories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat) },
                                onClick = {
                                    category = cat
                                    catExpanded = false
                                }
                            )
                        }
                    }
                }

                // Auto-sync type dropdown
                ExposedDropdownMenuBox(
                    expanded = syncExpanded,
                    onExpandedChange = { syncExpanded = it }
                ) {
                    val label = syncOptions.firstOrNull { it.first == syncType }?.second ?: "Без авто"
                    OutlinedTextField(
                        value = label,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Связь с Xiaomi Band / Метриками") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = syncExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(androidx.compose.material3.MenuAnchorType.PrimaryNotEditable, true)
                    )
                    ExposedDropdownMenu(
                        expanded = syncExpanded,
                        onDismissRequest = { syncExpanded = false }
                    ) {
                        syncOptions.forEach { opt ->
                            DropdownMenuItem(
                                text = { Text(opt.second) },
                                onClick = {
                                    syncType = opt.first
                                    syncExpanded = false
                                    if (opt.first == "STEPS") thresholdText = "10000"
                                    if (opt.first == "WATER") thresholdText = "2000"
                                    if (opt.first == "SLEEP") thresholdText = "420"
                                    if (opt.first == "DEEP_SLEEP") thresholdText = "90"
                                    if (opt.first == "SPO2") thresholdText = "95"
                                    if (opt.first == "STRESS") thresholdText = "35"
                                    if (opt.first == "ACTIVE_MINUTES") thresholdText = "45"
                                }
                            )
                        }
                    }
                }

                if (syncType != "NONE" && syncType != "WORKOUT") {
                    OutlinedTextField(
                        value = thresholdText,
                        onValueChange = { thresholdText = it.filter { ch -> ch.isDigit() } },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        label = {
                            val hint = when (syncType) {
                                "STEPS" -> "Количество шагов (порог)"
                                "WATER" -> "Объем воды в мл (порог)"
                                "SLEEP" -> "Минуты сна (420 мин = 7ч)"
                                "DEEP_SLEEP" -> "Минуты глубокого сна (порог)"
                                "SPO2" -> "Процент SpO2 (порог ≥ %)"
                                "STRESS" -> "Максимальный стресс (порог ≤)"
                                "ACTIVE_MINUTES" -> "Минуты активности (порог)"
                                else -> "Порог"
                            }
                            Text(hint)
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        val threshold = thresholdText.toIntOrNull() ?: 0
                        onConfirm(title, description, category, "#4F46E5", syncType, threshold)
                    }
                },
                enabled = title.isNotBlank(),
                modifier = Modifier.testTag("confirm_add_habit_button")
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
fun AiHabitPlanDialog(
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onGenerate: (goal: String, (List<GeneratedHabit>) -> Unit) -> Unit,
    onAccept: (List<GeneratedHabit>) -> Unit
) {
    var goalText by remember { mutableStateOf("") }
    var generatedResults by remember { mutableStateOf<List<GeneratedHabit>>(emptyList()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("AI-Генератор привычек")
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Опишите вашу цель, и модель Gemini 3.5 Flash сгенерирует сбалансированный набор полезных привычек с интеграцией браслета Xiaomi.",
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )

                OutlinedTextField(
                    value = goalText,
                    onValueChange = { goalText = it },
                    label = { Text("Ваша цель") },
                    placeholder = { Text("Например: Улучшить физическую форму и сон") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_ai_goal")
                )

                if (isLoading) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Gemini анализирует цель...", style = MaterialTheme.typography.bodySmall)
                    }
                }

                if (generatedResults.isNotEmpty()) {
                    Text(
                        text = "Предложенные привычки:",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    generatedResults.forEach { item ->
                        Card(
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = item.title,
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = item.description,
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp)
                                )
                                if (item.autoSyncType != "NONE") {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "⚡ Связь с браслетом: ${item.autoSyncType}",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (generatedResults.isEmpty()) {
                Button(
                    onClick = {
                        if (goalText.isNotBlank()) {
                            onGenerate(goalText) { results ->
                                generatedResults = results
                            }
                        }
                    },
                    enabled = goalText.isNotBlank() && !isLoading,
                    modifier = Modifier.testTag("generate_ai_plan_button")
                ) {
                    Text("Сгенерировать")
                }
            } else {
                Button(
                    onClick = { onAccept(generatedResults) },
                    modifier = Modifier.testTag("accept_ai_plan_button")
                ) {
                    Text("Принять план (${generatedResults.size})")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Закрыть") }
        }
    )
}

@Composable
fun HabitsScreen(
    uiState: PulseUiState,
    onToggleHabit: (Long, Boolean) -> Unit,
    onAddHabit: (String, String, String, String, String, Int) -> Unit,
    onDeleteHabit: (Long) -> Unit,
    onGenerateAiPlan: (String, (List<GeneratedHabit>) -> Unit) -> Unit,
    onAcceptAiPlan: (List<GeneratedHabit>) -> Unit,
    onPreviousDay: () -> Unit = {},
    onNextDay: () -> Unit = {},
    onResetToday: () -> Unit = {}
) {
    HabitsScreen(
        state = HabitsTabState(
            habits = uiState.habits,
            selectedDate = uiState.selectedDate,
            isSelectedDateToday = uiState.isSelectedDateToday,
            todayDate = uiState.todayDate,
            isAiLoading = uiState.isAiLoading
        ),
        onToggleHabit = onToggleHabit,
        onAddHabit = onAddHabit,
        onDeleteHabit = onDeleteHabit,
        onGenerateAiPlan = onGenerateAiPlan,
        onAcceptAiPlan = onAcceptAiPlan,
        onPreviousDay = onPreviousDay,
        onNextDay = onNextDay,
        onResetToday = onResetToday
    )
}
