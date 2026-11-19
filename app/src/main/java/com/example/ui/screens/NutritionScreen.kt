package com.example.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.WaterDrop
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entities.FoodEntryEntity
import com.example.data.remote.FoodEstimate
import com.example.ui.NutritionTabState
import com.example.ui.PulseUiState
import com.example.ui.components.DateNavigatorBar
import com.example.util.ImageUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NutritionScreen(
    state: NutritionTabState,
    onAddWater: (Int) -> Unit,
    onEstimateFood: (description: String, onEstimated: (FoodEstimate) -> Unit) -> Unit,
    onEstimateFoodFromPhoto: (base64Image: String, mimeType: String, note: String, onEstimated: (FoodEstimate) -> Unit) -> Unit,
    onSaveFood: (mealType: String, desc: String, cals: Int, protein: Int, fats: Int, carbs: Int) -> Unit,
    onDeleteFood: (Long) -> Unit,
    onPreviousDay: () -> Unit = {},
    onNextDay: () -> Unit = {},
    onResetToday: () -> Unit = {},
    onSaveApiKey: (String) -> Unit = {},
    currentApiKey: String = ""
) {
    var showAiFoodDialog by remember { mutableStateOf(false) }
    var initialDialogMode by remember { mutableStateOf("PHOTO") }

    val totalFoodCals = state.totalFoodCalories
    val totalBurned = state.totalBurnedCalories
    val totalProtein = state.foodEntries.sumOf { it.proteinGrams }
    val totalFats = state.foodEntries.sumOf { it.fatsGrams }
    val totalCarbs = state.foodEntries.sumOf { it.carbsGrams }

    val waterGoal = if (state.userProfile.targetWaterMl > 0) state.userProfile.targetWaterMl else state.dailyMetric.waterGoalMl
    val waterProgress = (state.totalWaterMl.toFloat() / waterGoal.toFloat()).coerceIn(0f, 1f)
    val isDark = isSystemInDarkTheme()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
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

        // Hydration Widget
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDark) Color(0xFF0C4A6E).copy(alpha = 0.45f) else Color(0xFFE0F2FE)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFF0284C7),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.WaterDrop,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Водный баланс",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDark) Color(0xFF7DD3FC) else Color(0xFF0369A1)
                                    )
                                )
                                Text(
                                    text = "${state.totalWaterMl} / $waterGoal мл",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = if (isDark) Color(0xFFBAE6FD) else Color(0xFF075985)
                                    )
                                )
                            }
                        }

                        Text(
                            text = "${(waterProgress * 100).toInt()}%",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color(0xFF7DD3FC) else Color(0xFF0369A1)
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    LinearProgressIndicator(
                        progress = { waterProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = Color(0xFF0284C7),
                        trackColor = Color(0xFFBAE6FD)
                    )

                    if (waterProgress >= 1f) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFD1FAE5),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color(0xFF047857),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Норма воды выполнена! Привычка автоматически закрыта.",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF047857)
                                    )
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { onAddWater(250) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("button_water_250")
                        ) {
                            Icon(imageVector = Icons.Default.LocalDrink, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("+250 мл", fontSize = 12.sp)
                        }

                        Button(
                            onClick = { onAddWater(500) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0369A1)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("button_water_500")
                        ) {
                            Icon(imageVector = Icons.Default.WaterDrop, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("+500 мл", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Energy & Macro Balance
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
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
                                text = "Баланс калорий и энергии",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Съедено: $totalFoodCals ккал • Сожжено: $totalBurned ккал",
                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (totalFoodCals - totalBurned >= 0) Color(0xFFFFEDD5) else Color(0xFFCCFBF1)
                        ) {
                            Text(
                                text = if (totalFoodCals - totalBurned >= 0) "+${totalFoodCals - totalBurned} ккал" else "${totalFoodCals - totalBurned} ккал",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (totalFoodCals - totalBurned >= 0) Color(0xFFC2410C) else Color(0xFF0F766E)
                                ),
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Macros Breakdown Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MacroCard(title = "Белки", amount = "${totalProtein}г", color = Color(0xFF3B82F6), modifier = Modifier.weight(1f))
                        MacroCard(title = "Жиры", amount = "${totalFats}г", color = Color(0xFFF59E0B), modifier = Modifier.weight(1f))
                        MacroCard(title = "Углеводы", amount = "${totalCarbs}г", color = Color(0xFF10B981), modifier = Modifier.weight(1f))
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Dual AI Food Entry Action Buttons: Photo and Text
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                initialDialogMode = "PHOTO"
                                showAiFoodDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                            modifier = Modifier
                                .weight(1.15f)
                                .testTag("button_open_ai_photo_food")
                        ) {
                            Icon(imageVector = Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Фото еды (AI)",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                initialDialogMode = "TEXT"
                                showAiFoodDialog = true
                            },
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                            modifier = Modifier
                                .weight(0.85f)
                                .testTag("button_open_ai_text_food")
                        ) {
                            Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Текстом",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }
        }

        // Food Entries Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val diaryTitle = if (state.isSelectedDateToday) {
                    "Дневник приемов пищи (сегодня)"
                } else {
                    "Дневник приемов пищи (${state.selectedDate})"
                }
                Text(
                    text = diaryTitle,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
                )
                TextButton(
                    onClick = {
                        initialDialogMode = "PHOTO"
                        showAiFoodDialog = true
                    },
                    modifier = Modifier.testTag("button_header_add_food")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Добавить еду", maxLines = 1, softWrap = false)
                }
            }
        }

        // Food Items List
        if (state.foodEntries.isEmpty()) {
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
                            imageVector = Icons.Default.Restaurant,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Приемов пищи еще не записано",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Text(
                            text = "Сделайте фото вашей тарелки или напишите описание — Gemini AI автоматически распознает блюдо и посчитает КБЖУ",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        } else {
            items(state.foodEntries, key = { it.id }) { food ->
                FoodCard(food = food, onDelete = { onDeleteFood(food.id) })
            }
        }
    }

    if (showAiFoodDialog) {
        AiFoodDialog(
            initialMode = initialDialogMode,
            isLoading = state.isAiLoading,
            onDismiss = { showAiFoodDialog = false },
            onEstimateText = { desc, onEst ->
                onEstimateFood(desc, onEst)
            },
            onEstimatePhoto = { base64, mime, note, onEst ->
                onEstimateFoodFromPhoto(base64, mime, note, onEst)
            },
            onSave = { mealType, desc, cals, p, f, c ->
                onSaveFood(mealType, desc, cals, p, f, c)
                showAiFoodDialog = false
            },
            onSaveApiKey = onSaveApiKey,
            currentApiKey = currentApiKey
        )
    }
}

@Composable
fun MacroCard(title: String, amount: String, color: Color, modifier: Modifier = Modifier) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(color, CircleShape)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = amount,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}

@Composable
fun FoodCard(food: FoodEntryEntity, onDelete: () -> Unit) {
    val mealTitle = when (food.mealType) {
        "BREAKFAST" -> "Завтрак"
        "LUNCH" -> "Обед"
        "DINNER" -> "Ужин"
        else -> "Перекус"
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
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Restaurant,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = mealTitle,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                    Text(
                        text = "${food.calories} ккал",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFEA580C)
                        )
                    )
                }

                Text(
                    text = food.description,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Б: ${food.proteinGrams}г",
                        style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF3B82F6), fontWeight = FontWeight.SemiBold)
                    )
                    Text(
                        text = "Ж: ${food.fatsGrams}г",
                        style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFFF59E0B), fontWeight = FontWeight.SemiBold)
                    )
                    Text(
                        text = "У: ${food.carbsGrams}г",
                        style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF10B981), fontWeight = FontWeight.SemiBold)
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
fun AiFoodDialog(
    initialMode: String = "PHOTO",
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onEstimateText: (description: String, onEstimated: (FoodEstimate) -> Unit) -> Unit,
    onEstimatePhoto: (base64Image: String, mimeType: String, note: String, onEstimated: (FoodEstimate) -> Unit) -> Unit,
    onSave: (mealType: String, desc: String, cals: Int, protein: Int, fats: Int, carbs: Int) -> Unit,
    onSaveApiKey: (String) -> Unit = {},
    currentApiKey: String = ""
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var inputMode by remember { mutableStateOf(initialMode) } // "PHOTO" or "TEXT"

    var mealType by remember { mutableStateOf("LUNCH") }
    var descriptionText by remember { mutableStateOf("") }
    var photoNoteText by remember { mutableStateOf("") }

    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var base64Image by remember { mutableStateOf<String?>(null) }
    var estimatedResult by remember { mutableStateOf<FoodEstimate?>(null) }

    var calText by remember { mutableStateOf("") }
    var proteinText by remember { mutableStateOf("") }
    var fatsText by remember { mutableStateOf("") }
    var carbsText by remember { mutableStateOf("") }

    var showApiKeyDialog by remember { mutableStateOf(false) }
    var apiKeyInput by remember { mutableStateOf(currentApiKey.ifBlank { com.example.data.remote.GeminiAiRepository.DEFAULT_EMBEDDED_KEY }) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            scope.launch {
                val scaled = ImageUtils.scaleBitmap(bitmap)
                previewBitmap = scaled
                base64Image = ImageUtils.bitmapToBase64Async(scaled)
                estimatedResult = null
            }
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val scaled = ImageUtils.uriToScaledBitmapAsync(context, uri)
                if (scaled != null) {
                    previewBitmap = scaled
                    base64Image = ImageUtils.bitmapToBase64Async(scaled)
                    estimatedResult = null
                }
            }
        }
    }

    val mealOptions = listOf(
        "BREAKFAST" to "Завтрак",
        "LUNCH" to "Обед",
        "DINNER" to "Ужин",
        "SNACK" to "Перекус"
    )
    var mealExpanded by remember { mutableStateOf(false) }

    if (showApiKeyDialog) {
        AlertDialog(
            onDismissRequest = { showApiKeyDialog = false },
            title = {
                Text("Ключ Gemini API", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Если у вас есть собственный ключ Google AI Studio (начинается с AIzaSy...), введите его сюда для работы с Gemini 3.8 Flash:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    OutlinedTextField(
                        value = apiKeyInput,
                        onValueChange = { apiKeyInput = it },
                        label = { Text("API Key") },
                        placeholder = { Text("AIzaSy...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        "Создать бесплатный ключ: aistudio.google.com/apikey",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = {
                        onSaveApiKey("")
                        apiKeyInput = ""
                        showApiKeyDialog = false
                    }) {
                        Text("Сбросить")
                    }
                    Button(onClick = {
                        onSaveApiKey(apiKeyInput.trim())
                        showApiKeyDialog = false
                    }) {
                        Text("Сохранить")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showApiKeyDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (inputMode == "PHOTO") "AI-Сканер еды" else "AI-Оценка питания",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
                IconButton(onClick = {
                    apiKeyInput = currentApiKey.ifBlank { com.example.data.remote.GeminiAiRepository.DEFAULT_EMBEDDED_KEY }
                    showApiKeyDialog = true
                }) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Ключ Gemini API",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Mode selector chips: Photo vs Text
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = inputMode == "PHOTO",
                        onClick = { inputMode = "PHOTO" },
                        label = { Text("📸 По фото") },
                        leadingIcon = if (inputMode == "PHOTO") {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null,
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = inputMode == "TEXT",
                        onClick = { inputMode = "TEXT" },
                        label = { Text("✍️ Описанием") },
                        leadingIcon = if (inputMode == "TEXT") {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Meal type dropdown
                ExposedDropdownMenuBox(
                    expanded = mealExpanded,
                    onExpandedChange = { mealExpanded = it }
                ) {
                    val label = mealOptions.firstOrNull { it.first == mealType }?.second ?: "Обед"
                    OutlinedTextField(
                        value = label,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Прием пищи") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = mealExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(androidx.compose.material3.MenuAnchorType.PrimaryNotEditable, true)
                    )
                    ExposedDropdownMenu(
                        expanded = mealExpanded,
                        onDismissRequest = { mealExpanded = false }
                    ) {
                        mealOptions.forEach { opt ->
                            DropdownMenuItem(
                                text = { Text(opt.second) },
                                onClick = {
                                    mealType = opt.first
                                    mealExpanded = false
                                }
                            )
                        }
                    }
                }

                if (inputMode == "PHOTO") {
                    if (previewBitmap == null) {
                        // Empty photo picker state
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = null,
                                    modifier = Modifier.size(36.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Добавьте фото блюда для AI-анализа",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                                )
                                Text(
                                    text = "Gemini 3.5 Flash определит размер порции, ингредиенты и КБЖУ",
                                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = { cameraLauncher.launch(null) },
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Камера", fontSize = 12.sp)
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            galleryLauncher.launch(
                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                            )
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Галерея", fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    } else {
                        // Image chosen and previewed
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(140.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Image(
                                    bitmap = previewBitmap!!.asImageBitmap(),
                                    contentDescription = "Фото блюда",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color.Black.copy(alpha = 0.6f),
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(8.dp)
                                        .clickable {
                                            galleryLauncher.launch(
                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                            )
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Заменить", color = Color.White, fontSize = 11.sp)
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = photoNoteText,
                                onValueChange = { photoNoteText = it },
                                label = { Text("Уточнение (необязательно)") },
                                placeholder = { Text("Напр.: половина порции, соус песто") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                } else {
                    // TEXT mode
                    OutlinedTextField(
                        value = descriptionText,
                        onValueChange = { descriptionText = it },
                        label = { Text("Что вы съели?") },
                        placeholder = { Text("Например: 2 яйца всмятку, тост с авокадо и капучино") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_food_description")
                    )
                }

                if (isLoading) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (inputMode == "PHOTO") "Gemini анализирует фото блюда..." else "Gemini рассчитывает калории...",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                if (estimatedResult != null) {
                    Text(
                        text = "Результат расчета КБЖУ:",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )

                    OutlinedTextField(
                        value = descriptionText,
                        onValueChange = { descriptionText = it },
                        label = { Text("Название блюда") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedButton(
                        onClick = {
                            if (descriptionText.isNotBlank()) {
                                onEstimateText(descriptionText) { est ->
                                    estimatedResult = est
                                    calText = est.calories.toString()
                                    proteinText = est.proteinGrams.toString()
                                    fatsText = est.fatsGrams.toString()
                                    carbsText = est.carbsGrams.toString()
                                }
                            }
                        },
                        enabled = !isLoading && descriptionText.isNotBlank(),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .testTag("btn_recalculate_food_ai")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "✨ Пересчитать КБЖУ по новому названию",
                            fontSize = 12.sp,
                            maxLines = 1,
                            softWrap = false
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedTextField(
                            value = calText,
                            onValueChange = { calText = it.filter { ch -> ch.isDigit() } },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            label = { Text("Ккал") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = proteinText,
                            onValueChange = { proteinText = it.filter { ch -> ch.isDigit() } },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            label = { Text("Белки") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = fatsText,
                            onValueChange = { fatsText = it.filter { ch -> ch.isDigit() } },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            label = { Text("Жиры") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = carbsText,
                            onValueChange = { carbsText = it.filter { ch -> ch.isDigit() } },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            label = { Text("Углев") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        },
        confirmButton = {
            if (estimatedResult == null) {
                Button(
                    onClick = {
                        if (inputMode == "PHOTO") {
                            val base64 = base64Image
                            if (base64 != null) {
                                onEstimatePhoto(base64, "image/jpeg", photoNoteText) { est ->
                                    estimatedResult = est
                                    descriptionText = est.summary
                                    calText = est.calories.toString()
                                    proteinText = est.proteinGrams.toString()
                                    fatsText = est.fatsGrams.toString()
                                    carbsText = est.carbsGrams.toString()
                                }
                            }
                        } else {
                            if (descriptionText.isNotBlank()) {
                                onEstimateText(descriptionText) { est ->
                                    estimatedResult = est
                                    calText = est.calories.toString()
                                    proteinText = est.proteinGrams.toString()
                                    fatsText = est.fatsGrams.toString()
                                    carbsText = est.carbsGrams.toString()
                                }
                            }
                        }
                    },
                    enabled = (if (inputMode == "PHOTO") base64Image != null else descriptionText.isNotBlank()) && !isLoading,
                    modifier = Modifier.testTag("estimate_food_button")
                ) {
                    Text(if (inputMode == "PHOTO") "Распознать по фото" else "Рассчитать КБЖУ")
                }
            } else {
                Button(
                    onClick = {
                        val cals = calText.toIntOrNull() ?: 300
                        val p = proteinText.toIntOrNull() ?: 15
                        val f = fatsText.toIntOrNull() ?: 10
                        val c = carbsText.toIntOrNull() ?: 30
                        val desc = if (descriptionText.isNotBlank()) descriptionText else (estimatedResult?.summary ?: "Прием пищи")
                        onSave(mealType, desc, cals, p, f, c)
                    },
                    modifier = Modifier.testTag("save_food_button")
                ) {
                    Text("Сохранить в дневник")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        }
    )
}

@Composable
fun NutritionScreen(
    uiState: PulseUiState,
    onAddWater: (Int) -> Unit,
    onEstimateFood: (description: String, onEstimated: (FoodEstimate) -> Unit) -> Unit,
    onEstimateFoodFromPhoto: (base64Image: String, mimeType: String, note: String, onEstimated: (FoodEstimate) -> Unit) -> Unit,
    onSaveFood: (mealType: String, desc: String, cals: Int, protein: Int, fats: Int, carbs: Int) -> Unit,
    onDeleteFood: (Long) -> Unit,
    onPreviousDay: () -> Unit = {},
    onNextDay: () -> Unit = {},
    onResetToday: () -> Unit = {},
    onSaveApiKey: (String) -> Unit = {},
    currentApiKey: String = ""
) {
    NutritionScreen(
        state = NutritionTabState(
            foodEntries = uiState.foodEntries,
            allFoodEntries = uiState.allFoodEntries,
            waterLogs = uiState.waterLogs,
            totalFoodCalories = uiState.totalFoodCalories,
            totalWaterMl = uiState.totalWaterMl,
            userProfile = uiState.userProfile,
            selectedDate = uiState.selectedDate,
            isSelectedDateToday = uiState.isSelectedDateToday,
            totalBurnedCalories = uiState.totalBurnedCalories,
            todayDate = uiState.todayDate,
            isAiLoading = uiState.isAiLoading,
            dailyMetric = uiState.dailyMetric
        ),
        onAddWater = onAddWater,
        onEstimateFood = onEstimateFood,
        onEstimateFoodFromPhoto = onEstimateFoodFromPhoto,
        onSaveFood = onSaveFood,
        onDeleteFood = onDeleteFood,
        onPreviousDay = onPreviousDay,
        onNextDay = onNextDay,
        onResetToday = onResetToday,
        onSaveApiKey = onSaveApiKey,
        currentApiKey = currentApiKey
    )
}

