package com.example.ui.components

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.domain.KinesitherapyExerciseItem
import com.example.util.ImageUtils
import kotlinx.coroutines.launch

@Composable
fun KinesitherapyWorkoutDialog(
    initialGoal: String,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onExtractExercisesFromPhoto: (base64: String, onResult: (List<KinesitherapyExerciseItem>) -> Unit) -> Unit,
    onSubmit: (
        title: String,
        goal: String,
        availablePhotoBase64: String?,
        performedPhotoBase64: String?,
        exercises: List<KinesitherapyExerciseItem>,
        painBefore: Int,
        painAfter: Int,
        durationMinutes: Int,
        notes: String
    ) -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var title by remember { mutableStateOf("Занятие кинезиотерапией на МТБ") }
    var goal by remember { mutableStateOf(if (initialGoal.isNotBlank()) initialGoal else "Реабилитация поясницы и баланс мышц") }
    var durationMinutesText by remember { mutableStateOf("50") }
    var painBefore by remember { mutableFloatStateOf(4f) }
    var painAfter by remember { mutableFloatStateOf(1f) }
    var notes by remember { mutableStateOf("") }

    // Photos
    var availablePhotoBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var availablePhotoBase64 by remember { mutableStateOf<String?>(null) }

    var performedPhotoBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var performedPhotoBase64 by remember { mutableStateOf<String?>(null) }

    // Exercises list
    var exercises by remember {
        mutableStateOf(KinesitherapyExerciseItem.DEFAULT_PRESETS)
    }

    var showAddExerciseDialog by remember { mutableStateOf(false) }

    // Photo pickers
    val availablePhotoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val bmp = ImageUtils.uriToScaledBitmapAsync(context, uri)
                if (bmp != null) {
                    availablePhotoBitmap = bmp
                    val b64 = ImageUtils.bitmapToBase64Async(bmp)
                    availablePhotoBase64 = b64
                    // Auto trigger AI extraction
                    onExtractExercisesFromPhoto(b64) { recognized ->
                        if (recognized.isNotEmpty()) {
                            exercises = recognized
                        }
                    }
                }
            }
        }
    }

    val performedPhotoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val bmp = ImageUtils.uriToScaledBitmapAsync(context, uri)
                if (bmp != null) {
                    performedPhotoBitmap = bmp
                    performedPhotoBase64 = ImageUtils.bitmapToBase64Async(bmp)
                }
            }
        }
    }

    val currentTonnage = exercises.filter { it.isDone }.sumOf { (it.weightKg * it.sets * it.reps).toDouble() }.toFloat()
    val doneCount = exercises.count { it.isDone }

    Dialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.92f)
                .testTag("kinesitherapy_workout_dialog")
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF0F766E),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.SelfImprovement,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Кинезиотерапия и ЛФК",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Анализ фото упражнений, весов и нагрузки AI",
                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        enabled = !isLoading,
                        modifier = Modifier.testTag("button_close_kinesitherapy_dialog")
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Закрыть")
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                // Scrollable Form
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Goal & Title
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "🎯 Ваша цель реабилитации",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                OutlinedTextField(
                                    value = goal,
                                    onValueChange = { goal = it },
                                    label = { Text("Цель (поясница, шея, суставы, осанка)") },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("input_kinesitherapy_goal"),
                                    singleLine = true
                                )
                            }
                        }
                    }

                    // STEP 1: Photo of all available exercises / program sheet
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isDark) MaterialTheme.colorScheme.surfaceVariant else Color(0xFFF0FDFA)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "1. Фото программы всех упражнений",
                                            style = MaterialTheme.typography.titleSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFF0F766E)
                                            )
                                        )
                                        Text(
                                            text = "Карта назначений врача, стенд с тренажерами или лист",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFF134E4A)
                                            )
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                if (availablePhotoBitmap != null) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(130.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .border(1.dp, Color(0xFF0F766E), RoundedCornerShape(12.dp))
                                    ) {
                                        Image(
                                            bitmap = availablePhotoBitmap!!.asImageBitmap(),
                                            contentDescription = "Программа упражнений",
                                            modifier = Modifier.fillMaxSize()
                                        )
                                        IconButton(
                                            onClick = {
                                                availablePhotoBitmap = null
                                                availablePhotoBase64 = null
                                            },
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                        ) {
                                            Icon(imageVector = Icons.Default.Close, contentDescription = "Удалить", tint = Color.White)
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Button(
                                        onClick = {
                                            availablePhotoBase64?.let { b64 ->
                                                onExtractExercisesFromPhoto(b64) { recognized ->
                                                    if (recognized.isNotEmpty()) exercises = recognized
                                                }
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F766E)),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Распознать упражнения через AI", fontSize = 13.sp, maxLines = 1, softWrap = false)
                                    }
                                } else {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedButton(
                                            onClick = {
                                                availablePhotoPicker.launch(
                                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                                )
                                            },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(10.dp),
                                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                                        ) {
                                            Icon(imageVector = Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Загрузить фото", fontSize = 12.sp, maxLines = 1, softWrap = false)
                                        }

                                        Button(
                                            onClick = {
                                                scope.launch {
                                                    val sampleCard = ImageUtils.createSampleKinesitherapyProgramCard()
                                                    availablePhotoBitmap = sampleCard
                                                    val b64 = ImageUtils.bitmapToBase64Async(sampleCard)
                                                    availablePhotoBase64 = b64
                                                    onExtractExercisesFromPhoto(b64) { recognized ->
                                                        if (recognized.isNotEmpty()) exercises = recognized
                                                    }
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F766E)),
                                            modifier = Modifier.weight(1.1f),
                                            shape = RoundedCornerShape(10.dp),
                                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                                        ) {
                                            Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Демо-программа", fontSize = 12.sp, maxLines = 1, softWrap = false)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // STEP 2: Performed exercises and weights
                    item {
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
                                    text = "2. Выполненные упражнения и веса",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "Отметьте выполненные, укажите вес (кг) и повторы",
                                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                )
                            }
                            TextButton(onClick = { showAddExerciseDialog = true }) {
                                Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(2.dp))
                                Text("Добавить", fontSize = 12.sp, maxLines = 1, softWrap = false)
                            }
                        }
                    }

                    // Exercise items
                    itemsIndexed(exercises) { index, exercise ->
                        ExerciseRowItem(
                            item = exercise,
                            onToggleDone = { done ->
                                exercises = exercises.toMutableList().also {
                                    it[index] = exercise.copy(isDone = done)
                                }
                            },
                            onUpdateWeight = { delta ->
                                val newWeight = (exercise.weightKg + delta).coerceAtLeast(0f)
                                exercises = exercises.toMutableList().also {
                                    it[index] = exercise.copy(weightKg = newWeight)
                                }
                            },
                            onDelete = {
                                exercises = exercises.toMutableList().also { it.removeAt(index) }
                            }
                        )
                    }

                    // Live Tonnage Metric
                    item {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isDark) MaterialTheme.colorScheme.surfaceVariant else Color(0xFFEFF6FF),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.FitnessCenter,
                                        contentDescription = null,
                                        tint = if (isDark) MaterialTheme.colorScheme.primary else Color(0xFF1D4ED8),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Выполнено: $doneCount из ${exercises.size}",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            color = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFF1D4ED8),
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }
                                Text(
                                    text = "Тоннаж: ${currentTonnage.toInt()} кг (${(currentTonnage / 1000f * 10).toInt() / 10.0} т)",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        color = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFF1D4ED8),
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }
                    }

                    // STEP 3: Photo of workout process / weights stack
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "3. Фото выполненных упражнений / весового стека (опционально)",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "AI проанализирует положение троса, технику и правильность веса",
                                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                )
                                Spacer(modifier = Modifier.height(10.dp))

                                if (performedPhotoBitmap != null) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(110.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                    ) {
                                        Image(
                                            bitmap = performedPhotoBitmap!!.asImageBitmap(),
                                            contentDescription = "Фото тренировки",
                                            modifier = Modifier.fillMaxSize()
                                        )
                                        IconButton(
                                            onClick = {
                                                performedPhotoBitmap = null
                                                performedPhotoBase64 = null
                                            },
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                        ) {
                                            Icon(imageVector = Icons.Default.Close, contentDescription = "Удалить", tint = Color.White)
                                        }
                                    }
                                } else {
                                    OutlinedButton(
                                        onClick = {
                                            performedPhotoPicker.launch(
                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                            )
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(imageVector = Icons.Default.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Прикрепить фото тренировки / стека", fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }

                    // STEP 4: Pain & Well-being Scale (VAS)
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isDark) MaterialTheme.colorScheme.surfaceVariant else Color(0xFFFFFBEB)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "4. Оценка боли / дискомфорта (Шкала ВАШ)",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFF92400E)
                                    )
                                )
                                Text(
                                    text = "Кинезиотерапия должна снимать спазм и боль без осевой нагрузки",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFF78350F)
                                    )
                                )
                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        "Боль ДО тренировки:",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFF78350F)
                                        )
                                    )
                                    Text("${painBefore.toInt()} / 10", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = Color(0xFFDC2626)))
                                }
                                Slider(
                                    value = painBefore,
                                    onValueChange = { painBefore = it },
                                    valueRange = 0f..10f,
                                    steps = 9,
                                    colors = SliderDefaults.colors(thumbColor = Color(0xFFDC2626), activeTrackColor = Color(0xFFDC2626))
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        "Боль ПОСЛЕ тренировки:",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFF78350F)
                                        )
                                    )
                                    Text("${painAfter.toInt()} / 10", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = Color(0xFF16A34A)))
                                }
                                Slider(
                                    value = painAfter,
                                    onValueChange = { painAfter = it },
                                    valueRange = 0f..10f,
                                    steps = 9,
                                    colors = SliderDefaults.colors(thumbColor = Color(0xFF16A34A), activeTrackColor = Color(0xFF16A34A))
                                )
                            }
                        }
                    }

                    // Duration & Notes
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = durationMinutesText,
                                onValueChange = { durationMinutesText = it },
                                label = { Text("Длительность (мин)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = notes,
                                onValueChange = { notes = it },
                                label = { Text("Ощущения и заметки") },
                                modifier = Modifier.weight(1.5f),
                                singleLine = true
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Submit Button
                Button(
                    onClick = {
                        val dur = durationMinutesText.toIntOrNull() ?: 50
                        onSubmit(
                            title,
                            goal,
                            availablePhotoBase64,
                            performedPhotoBase64,
                            exercises,
                            painBefore.toInt(),
                            painAfter.toInt(),
                            dur,
                            notes
                        )
                    },
                    enabled = !isLoading,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F766E)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("button_submit_kinesitherapy_session")
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("AI проводит клинический анализ нагрузки...")
                    } else {
                        Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Оценить тренировку и рассчитать нагрузку AI", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
        }
    }

    if (showAddExerciseDialog) {
        AddCustomExerciseDialog(
            onDismiss = { showAddExerciseDialog = false },
            onAdd = { newExercise ->
                exercises = exercises + newExercise
                showAddExerciseDialog = false
            }
        )
    }
}

@Composable
private fun ExerciseRowItem(
    item: KinesitherapyExerciseItem,
    onToggleDone: (Boolean) -> Unit,
    onUpdateWeight: (Float) -> Unit,
    onDelete: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (item.isDone) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (item.isDone) 1.dp else 0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = item.isDone,
                    onCheckedChange = onToggleDone,
                    colors = CheckboxDefaults.colors(checkedColor = Color(0xFF0F766E))
                )
                Spacer(modifier = Modifier.width(6.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = if (item.isDone) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    Text(
                        text = "${item.category} • ${item.machineName}",
                        style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                    Icon(imageVector = Icons.Default.DeleteOutline, contentDescription = "Удалить", tint = Color.Gray, modifier = Modifier.size(16.dp))
                }
            }

            // Weight Stepper & Details
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 44.dp, top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Weight buttons
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { onUpdateWeight(-2.5f) },
                        modifier = Modifier
                            .size(26.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                    ) {
                        Icon(imageVector = Icons.Default.Remove, contentDescription = "-2.5 кг", modifier = Modifier.size(14.dp))
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${item.weightKg} кг",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) MaterialTheme.colorScheme.primary else Color(0xFF0F766E)
                        )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(
                        onClick = { onUpdateWeight(2.5f) },
                        modifier = Modifier
                            .size(26.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "+2.5 кг", modifier = Modifier.size(14.dp))
                    }
                }

                Text(
                    text = "${item.sets} x ${item.reps} повт.",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium)
                )

                Text(
                    text = "= ${item.totalVolumeKg.toInt()} кг",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) MaterialTheme.colorScheme.secondary else Color(0xFF1D4ED8)
                    )
                )
            }
        }
    }
}

@Composable
private fun AddCustomExerciseDialog(
    onDismiss: () -> Unit,
    onAdd: (KinesitherapyExerciseItem) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("ДЕКОМПРЕССИЯ СПИНЫ") }
    var weightText by remember { mutableStateOf("20") }
    var setsText by remember { mutableStateOf("3") }
    var repsText by remember { mutableStateOf("15") }
    var machineName by remember { mutableStateOf("МТБ-1") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "Добавить упражнение",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Название упражнения") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = machineName,
                    onValueChange = { machineName = it },
                    label = { Text("Тренажер (например МТБ нижний блок)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = weightText,
                        onValueChange = { weightText = it },
                        label = { Text("Вес (кг)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = setsText,
                        onValueChange = { setsText = it },
                        label = { Text("Подходы") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = repsText,
                        onValueChange = { repsText = it },
                        label = { Text("Повторы") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Отмена") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (name.isNotBlank()) {
                                onAdd(
                                    KinesitherapyExerciseItem(
                                        name = name.trim(),
                                        category = category,
                                        weightKg = weightText.toFloatOrNull() ?: 20f,
                                        sets = setsText.toIntOrNull() ?: 3,
                                        reps = repsText.toIntOrNull() ?: 15,
                                        machineName = machineName.trim(),
                                        isDone = true
                                    )
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F766E))
                    ) {
                        Text("Добавить")
                    }
                }
            }
        }
    }
}
