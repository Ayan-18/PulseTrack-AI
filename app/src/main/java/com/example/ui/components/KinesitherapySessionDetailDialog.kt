package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.domain.KinesitherapyExerciseItem
import com.example.data.local.entities.KinesitherapySessionEntity
import com.example.data.local.entities.UserProfileEntity
import com.example.util.DataExportManager
import com.example.util.ImageUtils

@Composable
fun KinesitherapySessionDetailDialog(
    session: KinesitherapySessionEntity,
    userProfile: UserProfileEntity? = null,
    onDismiss: () -> Unit,
    onDelete: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val context = LocalContext.current
    val exercises = remember(session.exercisesJson) {
        KinesitherapyExerciseItem.parseListFromJson(session.exercisesJson)
    }

    val availableBitmap by produceState<Bitmap?>(initialValue = null, session.availableExercisesPhotoBase64) {
        value = ImageUtils.loadPhotoBitmapAsync(context, session.availableExercisesPhotoBase64)
    }

    val performedBitmap by produceState<Bitmap?>(initialValue = null, session.performedExercisesPhotoBase64) {
        value = ImageUtils.loadPhotoBitmapAsync(context, session.performedExercisesPhotoBase64)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.92f)
                .testTag("kinesitherapy_detail_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFF0F766E),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.SelfImprovement,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = session.title,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "${session.date} • ${session.durationMinutes} мин",
                                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                )
                            }
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = {
                                val report = DataExportManager.generateSingleSessionReport(
                                    session = session,
                                    profile = userProfile
                                )
                                DataExportManager.sharePlainText(
                                    context = context,
                                    title = "Протокол сессии кинезиотерапии - ${session.title} (${session.date})",
                                    content = report
                                )
                            },
                            modifier = Modifier.testTag("button_share_single_session")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Поделиться этой сессией",
                                tint = if (isDark) MaterialTheme.colorScheme.primary else Color(0xFF0F766E)
                            )
                        }
                        IconButton(onClick = onDelete) {
                            Icon(imageVector = Icons.Default.DeleteOutline, contentDescription = "Удалить", tint = Color.Red)
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Закрыть")
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Score & Tonnage Summary
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isDark) MaterialTheme.colorScheme.surfaceVariant else Color(0xFFF0FDFA)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceAround,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "${session.aiScore}/10",
                                        style = MaterialTheme.typography.headlineMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = if (isDark) MaterialTheme.colorScheme.primary else Color(0xFF0F766E)
                                        )
                                    )
                                    Text(
                                        text = "Оценка AI",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFF134E4A)
                                        )
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .width(1.dp)
                                        .height(40.dp)
                                        .background(if (isDark) MaterialTheme.colorScheme.outlineVariant else Color(0xFF99F6E4))
                                )

                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "${session.totalTonnageKg.toInt()} кг",
                                        style = MaterialTheme.typography.headlineMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = if (isDark) MaterialTheme.colorScheme.secondary else Color(0xFF1D4ED8)
                                        )
                                    )
                                    Text(
                                        text = "Тоннаж нагрузки",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFF1E40AF)
                                        )
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .width(1.dp)
                                        .height(40.dp)
                                        .background(if (isDark) MaterialTheme.colorScheme.outlineVariant else Color(0xFF99F6E4))
                                )

                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    val delta = session.painLevelBefore - session.painLevelAfter
                                    Text(
                                        text = "${session.painLevelBefore} ➔ ${session.painLevelAfter}",
                                        style = MaterialTheme.typography.headlineMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = if (delta > 0) {
                                                if (isDark) Color(0xFF4ADE80) else Color(0xFF16A34A)
                                            } else {
                                                if (isDark) Color(0xFFFBBF24) else Color(0xFFD97706)
                                            }
                                        )
                                    )
                                    Text(
                                        text = "Боль по ВАШ",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFF78350F)
                                        )
                                    )
                                }
                            }
                        }
                    }

                    // Goal
                    item {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "🎯 Цель пациента",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = session.primaryGoal,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                                )
                            }
                        }
                    }

                    // Photos (if any)
                    val currentAvailable = availableBitmap
                    val currentPerformed = performedBitmap
                    if (currentAvailable != null || currentPerformed != null) {
                        item {
                            Text(
                                text = "📷 Прикрепленные фотографии",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                if (currentAvailable != null) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Программа всех упражнений:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(100.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(10.dp))
                                        ) {
                                            Image(
                                                bitmap = currentAvailable.asImageBitmap(),
                                                contentDescription = "Программа упражнений",
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        }
                                    }
                                }

                                if (currentPerformed != null) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Выполненные веса / тренажер:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(100.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(10.dp))
                                        ) {
                                            Image(
                                                bitmap = currentPerformed.asImageBitmap(),
                                                contentDescription = "Выполненные веса",
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 1. Clinical Evaluation
                    if (session.aiEvaluation.isNotBlank()) {
                        item {
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isDark) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.AutoAwesome,
                                            contentDescription = null,
                                            tint = if (isDark) MaterialTheme.colorScheme.primary else Color(0xFF0F766E),
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Клиническая оценка врача-кинезиотерапевта",
                                            style = MaterialTheme.typography.titleSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = if (isDark) MaterialTheme.colorScheme.onSurface else Color(0xFF0F766E)
                                            )
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = session.aiEvaluation,
                                        style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface)
                                    )
                                }
                            }
                        }
                    }

                    // 2. Load Analysis
                    if (session.aiLoadAnalysis.isNotBlank()) {
                        item {
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isDark) MaterialTheme.colorScheme.surfaceVariant else Color(0xFFEFF6FF)
                                )
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.FitnessCenter,
                                            contentDescription = null,
                                            tint = if (isDark) MaterialTheme.colorScheme.primary else Color(0xFF1D4ED8),
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Расчет нагрузки и баланс мышечных цепей",
                                            style = MaterialTheme.typography.titleSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = if (isDark) MaterialTheme.colorScheme.onSurface else Color(0xFF1D4ED8)
                                            )
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = session.aiLoadAnalysis,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            color = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFF1E3A8A)
                                        )
                                    )
                                }
                            }
                        }
                    }

                    // 3. Goal Progress
                    if (session.aiGoalRecommendations.isNotBlank()) {
                        item {
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isDark) MaterialTheme.colorScheme.surfaceVariant else Color(0xFFF0FDF4)
                                )
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(
                                        text = "🎯 Достижение цели",
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = if (isDark) MaterialTheme.colorScheme.onSurface else Color(0xFF15803D)
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = session.aiGoalRecommendations,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            color = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFF14532D)
                                        )
                                    )
                                }
                            }
                        }
                    }

                    // 4. Next Session Plan
                    if (session.aiNextSessionPlan.isNotBlank()) {
                        item {
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isDark) MaterialTheme.colorScheme.surfaceVariant else Color(0xFFFFFBEB)
                                )
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(
                                        text = "📋 План на следующее занятие и прогрессия весов",
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = if (isDark) MaterialTheme.colorScheme.onSurface else Color(0xFFB45309)
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = session.aiNextSessionPlan,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            color = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFF78350F)
                                        )
                                    )
                                }
                            }
                        }
                    }

                    // Exercises Performed List
                    item {
                        Text(
                            text = "Выполненные упражнения (${exercises.count { it.isDone }} из ${exercises.size}):",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    items(exercises) { ex ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (ex.isDone) {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            } else {
                                if (isDark) MaterialTheme.colorScheme.surfaceVariant else Color(0xFFF1F5F9)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = ex.name,
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                                    )
                                    Text(
                                        text = "${ex.category} • ${ex.machineName}",
                                        style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "${ex.weightKg} кг",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = if (isDark) MaterialTheme.colorScheme.primary else Color(0xFF0F766E)
                                        )
                                    )
                                    Text(
                                        text = "${ex.sets}x${ex.reps} (${ex.totalVolumeKg.toInt()} кг)",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
