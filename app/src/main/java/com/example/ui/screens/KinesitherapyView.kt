package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.domain.KinesitherapyExerciseItem
import com.example.data.local.entities.KinesitherapySessionEntity
import com.example.ui.PulseUiState
import com.example.ui.WorkoutsTabState
import com.example.ui.components.KinesitherapySessionDetailDialog
import com.example.ui.components.KinesitherapyWorkoutDialog
import androidx.compose.material.icons.filled.Share
import androidx.compose.ui.platform.LocalContext
import com.example.util.DataExportManager
import com.example.util.ImageUtils

@Composable
fun KinesitherapyView(
    state: WorkoutsTabState,
    onExtractExercisesFromPhoto: (base64: String, onResult: (List<KinesitherapyExerciseItem>) -> Unit) -> Unit,
    onSaveSession: (
        title: String,
        goal: String,
        availablePhoto: String?,
        performedPhoto: String?,
        exercises: List<KinesitherapyExerciseItem>,
        painBefore: Int,
        painAfter: Int,
        duration: Int,
        notes: String
    ) -> Unit,
    onDeleteSession: (Long) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedSessionForDetail by remember { mutableStateOf<KinesitherapySessionEntity?>(null) }

    val context = LocalContext.current
    val sessions = state.kinesitherapySessions
    val totalTonnage = sessions.sumOf { it.totalTonnageKg.toDouble() }.toFloat()
    val avgScore = if (sessions.isNotEmpty()) (sessions.map { it.aiScore }.average() * 10).toInt() / 10.0 else 0.0
    val isDark = isSystemInDarkTheme()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Hero Header: Kinesitherapy Overview & Action
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
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
                                color = Color(0xFF0F766E),
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.SelfImprovement,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Кинезиотерапия & МТБ",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    maxLines = 1
                                )
                                Text(
                                    text = "ЛФК, декомпрессия и баланс глубоких мышц",
                                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                                    maxLines = 1
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFCCFBF1)
                        ) {
                            Text(
                                text = "${sessions.size} сессий",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F766E)
                                ),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Скиньте фото карты упражнений из зала, отметьте поднятые веса — AI рассчитает суммарный тоннаж, оценит декомпрессию позвоночника и подскажет, как достичь цели без травм.",
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { showAddDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F766E)),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("button_new_kinesitherapy_session")
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Записать тренировку и оценить в AI", fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, softWrap = false)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = {
                            val report = DataExportManager.generateKinesitherapyReport(
                                profile = state.userProfile,
                                sessions = sessions
                            )
                            DataExportManager.sharePlainText(
                                context = context,
                                title = "Клинический отчет кинезиотерапии • PulseTrack AI",
                                content = report
                            )
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("button_share_kinesitherapy_report")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = null,
                            tint = if (isDark) MaterialTheme.colorScheme.primary else Color(0xFF0F766E),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Поделиться отчетом с врачом",
                            color = if (isDark) MaterialTheme.colorScheme.onSurface else Color(0xFF0F766E),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }

        // Stats Row: Tonnage, Score, Relief
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Tonnage Card
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDark) Color(0xFF1E3A8A).copy(alpha = 0.4f) else Color(0xFFEFF6FF)
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.FitnessCenter,
                                contentDescription = null,
                                tint = if (isDark) Color(0xFF93C5FD) else Color(0xFF1D4ED8),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Тоннаж",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = if (isDark) Color(0xFFBFDBFE) else Color(0xFF1E40AF),
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (totalTonnage >= 1000) "${(totalTonnage / 1000f * 10).toInt() / 10.0} т" else "${totalTonnage.toInt()} кг",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color(0xFFDBEAFE) else Color(0xFF1E3A8A)
                            )
                        )
                        Text(
                            text = "Общий объем",
                            style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF3B82F6))
                        )
                    }
                }

                // AI Score Card
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDark) Color(0xFF14532D).copy(alpha = 0.4f) else Color(0xFFF0FDF4)
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = if (isDark) Color(0xFF86EFAC) else Color(0xFF15803D),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Оценка AI",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = if (isDark) Color(0xFFBBF7D0) else Color(0xFF166534),
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (avgScore > 0) "$avgScore/10" else "—",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color(0xFFDCFCE7) else Color(0xFF14532D)
                            )
                        )
                        Text(
                            text = "Эффективность",
                            style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF22C55E))
                        )
                    }
                }

                // Pain Relief Card
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDark) Color(0xFF78350F).copy(alpha = 0.4f) else Color(0xFFFFFBEB)
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.TrendingDown,
                                contentDescription = null,
                                tint = if (isDark) Color(0xFFFCD34D) else Color(0xFFB45309),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Боль ВАШ",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = if (isDark) Color(0xFFFDE68A) else Color(0xFF92400E),
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        val painRelief = if (sessions.isNotEmpty()) {
                            val diff = sessions.map { it.painLevelBefore - it.painLevelAfter }.average()
                            (diff * 10).toInt() / 10.0
                        } else 0.0
                        Text(
                            text = if (painRelief > 0) "-$painRelief" else "—",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color(0xFFFEF3C7) else Color(0xFF78350F)
                            )
                        )
                        Text(
                            text = "Снижение спазма",
                            style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFFF59E0B))
                        )
                    }
                }
            }
        }

        // Section Title: History of Kinesitherapy Sessions
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "История тренировок по кинезиотерапии",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
                )
                Text(
                    text = "${sessions.size} записей",
                    style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                    maxLines = 1,
                    softWrap = false
                )
            }
        }

        // Empty state
        if (sessions.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFF0FDFA),
                            modifier = Modifier.size(56.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.SelfImprovement,
                                    contentDescription = null,
                                    tint = Color(0xFF0F766E),
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "Нет записей кинезиотерапии",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Нажмите «Записать тренировку», прикрепите фото карты с упражнениями (или используйте демо-программу) и укажите свои рабочие веса. AI проведет оценку сессии и рассчитает нагрузку!",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        )
                    }
                }
            }
        } else {
            items(sessions, key = { it.id }) { session ->
                KinesitherapySessionCard(
                    session = session,
                    onOpenDetail = { selectedSessionForDetail = session },
                    onDelete = { onDeleteSession(session.id) }
                )
            }
        }
    }

    // Add Workout Dialog
    if (showAddDialog) {
        val userGoal = state.userProfile.goal
        val readableGoal = when (userGoal.uppercase()) {
            "BUILD_MUSCLE" -> "Укрепление мышечного корсета и стабилизаторов"
            "LOSE_WEIGHT" -> "Снижение веса и разгрузка суставов"
            else -> "Реабилитация позвоночника и декомпрессия"
        }

        KinesitherapyWorkoutDialog(
            initialGoal = readableGoal,
            isLoading = state.isAiLoading,
            onDismiss = { showAddDialog = false },
            onExtractExercisesFromPhoto = onExtractExercisesFromPhoto,
            onSubmit = { title, goal, availablePhoto, performedPhoto, exercises, painBefore, painAfter, duration, notes ->
                onSaveSession(
                    title,
                    goal,
                    availablePhoto,
                    performedPhoto,
                    exercises,
                    painBefore,
                    painAfter,
                    duration,
                    notes
                )
                showAddDialog = false
            }
        )
    }

    // Detail Dialog
    selectedSessionForDetail?.let { session ->
        KinesitherapySessionDetailDialog(
            session = session,
            userProfile = state.userProfile,
            onDismiss = { selectedSessionForDetail = null },
            onDelete = {
                onDeleteSession(session.id)
                selectedSessionForDetail = null
            }
        )
    }
}

@Composable
fun KinesitherapyView(
    uiState: PulseUiState,
    onExtractExercisesFromPhoto: (base64: String, onResult: (List<KinesitherapyExerciseItem>) -> Unit) -> Unit,
    onSaveSession: (
        title: String,
        goal: String,
        availablePhoto: String?,
        performedPhoto: String?,
        exercises: List<KinesitherapyExerciseItem>,
        painBefore: Int,
        painAfter: Int,
        duration: Int,
        notes: String
    ) -> Unit,
    onDeleteSession: (Long) -> Unit
) {
    KinesitherapyView(
        state = WorkoutsTabState(
            kinesitherapySessions = uiState.kinesitherapySessions,
            userProfile = uiState.userProfile,
            isAiLoading = uiState.isAiLoading
        ),
        onExtractExercisesFromPhoto = onExtractExercisesFromPhoto,
        onSaveSession = onSaveSession,
        onDeleteSession = onDeleteSession
    )
}

@Composable
private fun KinesitherapySessionCard(
    session: KinesitherapySessionEntity,
    onOpenDetail: () -> Unit,
    onDelete: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    val exercises = remember(session.exercisesJson) {
        KinesitherapyExerciseItem.parseListFromJson(session.exercisesJson)
    }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenDetail() }
            .testTag("kinesitherapy_session_card_${session.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = session.title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "${session.date} • ${session.durationMinutes} мин",
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFF0FDFA)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFF0F766E), modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${session.aiScore}/10",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F766E)
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Metrics chips row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Tonnage chip
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFEFF6FF)
                ) {
                    Text(
                        text = "🏋 ${session.totalTonnageKg.toInt()} кг тоннаж",
                        style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF1D4ED8), fontWeight = FontWeight.SemiBold),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                // Completed exercises chip
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = "✓ ${session.exercisesCompletedCount} из ${session.exercisesTotalCount} упр.",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                // Pain delta chip
                val painDelta = session.painLevelBefore - session.painLevelAfter
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (painDelta > 0) Color(0xFFDCFCE7) else Color(0xFFFEF3C7)
                ) {
                    Text(
                        text = "Боль: ${session.painLevelBefore} ➔ ${session.painLevelAfter}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = if (painDelta > 0) Color(0xFF15803D) else Color(0xFFB45309),
                            fontWeight = FontWeight.SemiBold
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // AI Evaluation Snippet
            if (session.aiEvaluation.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF8FAFC),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFF0F766E), modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Вердикт врача-кинезиотерапевта:",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Color(0xFF0F766E))
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (expanded) session.aiEvaluation else session.aiEvaluation.take(130) + if (session.aiEvaluation.length > 130) "..." else "",
                            style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF334155))
                        )

                        AnimatedVisibility(visible = expanded) {
                            Column(modifier = Modifier.padding(top = 8.dp)) {
                                if (session.aiLoadAnalysis.isNotBlank()) {
                                    Text(
                                        text = "Нагрузка: ${session.aiLoadAnalysis}",
                                        style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF1E3A8A), fontWeight = FontWeight.Medium)
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                }
                                if (session.aiNextSessionPlan.isNotBlank()) {
                                    Text(
                                        text = "План: ${session.aiNextSessionPlan}",
                                        style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF78350F))
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Action row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { expanded = !expanded }
                ) {
                    Text(
                        text = if (expanded) "Свернуть" else "Подробнее",
                        style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF0F766E), fontWeight = FontWeight.Bold)
                    )
                    Icon(
                        imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = Color(0xFF0F766E),
                        modifier = Modifier.size(16.dp)
                    )
                }

                Row {
                    TextButton(onClick = onOpenDetail) {
                        Text("Полный аудит", fontSize = 12.sp)
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(imageVector = Icons.Default.DeleteOutline, contentDescription = "Удалить", tint = Color.Gray, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}
