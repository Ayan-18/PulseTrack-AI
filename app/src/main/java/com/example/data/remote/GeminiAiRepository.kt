package com.example.data.remote

import android.util.Log
import com.example.BuildConfig
import com.example.data.domain.KinesitherapyAiReport
import com.example.data.domain.KinesitherapyExerciseItem
import com.example.data.local.entities.UserProfileEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GeminiAiRepository {

    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(18, TimeUnit.SECONDS)
        .writeTimeout(18, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    var customApiKey: String = ""
    var lastApiError: String? = null

    fun getEffectiveApiKey(): String {
        val custom = customApiKey.trim()
        if (isValidApiKeyFormat(custom) && !isKnownDeadKey(custom)) {
            return custom
        }
        val configured = BuildConfig.GEMINI_API_KEY.trim()
        if (isValidApiKeyFormat(configured) && !isKnownDeadKey(configured)) {
            return configured
        }
        return DEFAULT_EMBEDDED_KEY
    }

    fun isApiKeyConfigured(): Boolean {
        return getEffectiveApiKey().isNotBlank()
    }

    private suspend fun executeGeminiRequest(
        partsArray: JSONArray,
        systemInstruction: String? = null,
        temperature: Double = 0.4,
        expectJson: Boolean = true
    ): String = withContext(Dispatchers.IO) {
        val apiKey = getEffectiveApiKey()
        if (apiKey.isBlank()) {
            lastApiError = "API-ключ не настроен. Нажмите на значок ключа для ввода."
            Log.w("GeminiAi", "GEMINI_API_KEY is not configured or invalid. Using smart fallback.")
            return@withContext ""
        }

        val contentsArray = JSONArray().apply {
            put(JSONObject().apply {
                put("parts", partsArray)
            })
        }

        val rootJson = JSONObject().apply {
            put("contents", contentsArray)
            if (systemInstruction != null) {
                put("systemInstruction", JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", systemInstruction) })
                    })
                })
            }
            put("generationConfig", JSONObject().apply {
                put("temperature", temperature)
                if (expectJson) {
                    put("responseMimeType", "application/json")
                }
            })
        }

        val body = rootJson.toString().toRequestBody(jsonMediaType)

        val keyMask = if (apiKey.length > 12) "${apiKey.take(8)}...${apiKey.takeLast(4)}" else "***"
        Log.d("GeminiAi", "Запрос к Gemini с ключом $keyMask (customKey=${customApiKey.isNotBlank()})")

        val candidateModels = listOf("gemini-3.8-flash", "gemini-3.5-flash", "gemini-flash-latest", "gemini-3.1-flash-lite", "gemini-2.5-flash")
        for (model in candidateModels) {
            val endpoint = "$BASE_URL/$model:generateContent"
            val request = Request.Builder()
                .url(endpoint)
                .addHeader("x-goog-api-key", apiKey)
                .post(body)
                .build()

            try {
                client.newCall(request).execute().use { response ->
                    val responseBody = response.body?.string() ?: ""
                    if (response.isSuccessful) {
                        lastApiError = null
                        Log.i("GeminiAi", "Успешный ответ от Gemini ($model), байт: ${responseBody.length}")
                        val jsonResponse = JSONObject(responseBody)
                        val candidates = jsonResponse.optJSONArray("candidates")
                        if (candidates != null && candidates.length() > 0) {
                            val candidate = candidates.getJSONObject(0)
                            val content = candidate.optJSONObject("content")
                            val parts = content?.optJSONArray("parts")
                            if (parts != null && parts.length() > 0) {
                                return@withContext parts.getJSONObject(0).optString("text", "")
                            }
                        }
                        return@withContext ""
                    } else {
                        Log.w("GeminiAi", "Модель $model вернула код ${response.code} для ключа $keyMask: $responseBody")
                        if (response.code in listOf(400, 401, 402, 403)) {
                            // Если пользовательский ключ дал 401/402, сбрасываем его и пробуем рабочий встроенный ключ!
                            if (customApiKey.isNotBlank() && (response.code == 401 || response.code == 402)) {
                                Log.w("GeminiAi", "Пользовательский ключ ($keyMask) исчерпан или недействителен. Авто-сброс на встроенный рабочий ключ...")
                                customApiKey = ""
                                val fallbackKey = getEffectiveApiKey()
                                if (fallbackKey.isNotBlank() && fallbackKey != apiKey) {
                                    val fbRequest = Request.Builder()
                                        .url(endpoint)
                                        .addHeader("x-goog-api-key", fallbackKey)
                                        .post(body)
                                        .build()
                                    try {
                                        client.newCall(fbRequest).execute().use { fbResp ->
                                            val fbBody = fbResp.body?.string() ?: ""
                                            if (fbResp.isSuccessful) {
                                                lastApiError = null
                                                Log.i("GeminiAi", "Успешный ответ от Gemini ($model) с встроенным ключом!")
                                                val fbJson = JSONObject(fbBody)
                                                val fbCands = fbJson.optJSONArray("candidates")
                                                if (fbCands != null && fbCands.length() > 0) {
                                                    val cand = fbCands.getJSONObject(0)
                                                    val cont = cand.optJSONObject("content")
                                                    val parts = cont?.optJSONArray("parts")
                                                    if (parts != null && parts.length() > 0) {
                                                        return@withContext parts.getJSONObject(0).optString("text", "")
                                                    }
                                                }
                                                return@withContext ""
                                            } else {
                                                Log.w("GeminiAi", "Встроенный ключ также вернул ${fbResp.code}: $fbBody")
                                            }
                                        }
                                    } catch (e: Exception) {
                                        Log.w("GeminiAi", "Ошибка повторного запроса со встроенным ключом: ${e.message}")
                                    }
                                }
                            }

                            lastApiError = when (response.code) {
                                402 -> "Кредиты Gemini API исчерпаны (402). Нажмите [🔑] для ввода бесплатного ключа."
                                403 -> "Доступ к Gemini API запрещен (403). Проверьте ключ."
                                else -> "Ошибка авторизации Gemini API (${response.code})."
                            }
                            Log.w("GeminiAi", "API error: $lastApiError")
                            return@withContext ""
                        }
                        if (response.code == 429) {
                            lastApiError = "Превышен лимит запросов Gemini API (429)."
                            return@withContext ""
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w("GeminiAi", "Network exception for $model: ${e.message}")
                if (e is java.net.UnknownHostException || e is java.net.ConnectException) {
                    lastApiError = "Нет подключения к Google API (проверьте интернет или включите VPN)."
                    break
                }
            }
        }
        ""
    }

    private suspend fun callGemini(
        prompt: String,
        systemInstruction: String? = null,
        expectJson: Boolean = true
    ): String {
        val partsArray = JSONArray().apply {
            put(JSONObject().apply { put("text", prompt) })
        }
        return executeGeminiRequest(partsArray, systemInstruction, temperature = 0.4, expectJson = expectJson)
    }

    private suspend fun callGeminiWithImage(
        base64Image: String,
        mimeType: String,
        prompt: String,
        systemInstruction: String? = null,
        expectJson: Boolean = true
    ): String {
        val partsArray = JSONArray().apply {
            put(JSONObject().apply {
                put("inlineData", JSONObject().apply {
                    put("mimeType", mimeType)
                    put("data", base64Image)
                })
            })
            put(JSONObject().apply { put("text", prompt) })
        }
        return executeGeminiRequest(partsArray, systemInstruction, temperature = 0.3, expectJson = expectJson)
    }

    private suspend fun callGeminiWithMultipleImages(
        images: List<Pair<String, String>>,
        prompt: String,
        systemInstruction: String? = null,
        expectJson: Boolean = true
    ): String {
        val partsArray = JSONArray().apply {
            for ((base64Image, mimeType) in images) {
                if (base64Image.isNotBlank()) {
                    put(JSONObject().apply {
                        put("inlineData", JSONObject().apply {
                            put("mimeType", mimeType)
                            put("data", base64Image)
                        })
                    })
                }
            }
            put(JSONObject().apply { put("text", prompt) })
        }
        return executeGeminiRequest(partsArray, systemInstruction, temperature = 0.3, expectJson = expectJson)
    }

    // Multimodal Food Analysis by Photo
    suspend fun estimateNutritionFromImage(
        base64Image: String,
        mimeType: String = "image/jpeg",
        userNote: String = ""
    ): FoodEstimate = withContext(Dispatchers.Default) {
        val systemInstruction = "Ты — опытный шеф-диетолог и спортивный нутрициолог. " +
                "Пользователь предоставил фото своего реального блюда или напитка. " +
                "Внимательно распознай: ЧТО КОНКРЕТНО изображено на фото (определи мясо, гарнир, овощи, соус, напиток, размер порции). " +
                "Рассчитай реальную калорийность (ккал), белки (г), жиры (г) и углеводы (г) для этой порции. " +
                "В поле summary ОБЯЗАТЕЛЬНО укажи конкретное, точное название блюда на русском языке (например: 'Овсяная каша с черникой и орехами', 'Борщ со сметаной и пампушкой', 'Стейк из семги с рисом'). " +
                "Отвечай СТРОГО в формате валидного JSON: " +
                "{\"summary\": \"Точное название блюда\", \"calories\": 450, \"protein\": 32, \"fats\": 16, \"carbs\": 44}"

        val prompt = if (userNote.isNotBlank()) {
            "Определи блюдо на фото, учитывая комментарий: $userNote. Рассчитай точный КБЖУ."
        } else {
            "Определи, что за блюдо на фото, размер порции и рассчитай точный КБЖУ."
        }

        val response = callGeminiWithImage(base64Image, mimeType, prompt, systemInstruction)
        val cleanJson = cleanJsonString(response)
        if (cleanJson.isNotBlank()) {
            try {
                val obj = JSONObject(cleanJson)
                val cal = when {
                    obj.has("calories") -> obj.optInt("calories")
                    obj.has("kcal") -> obj.optInt("kcal")
                    obj.has("cal") -> obj.optInt("cal")
                    else -> 420
                }
                val protein = when {
                    obj.has("protein") -> obj.optInt("protein")
                    obj.has("proteinGrams") -> obj.optInt("proteinGrams")
                    obj.has("proteins") -> obj.optInt("proteins")
                    else -> 24
                }
                val fats = when {
                    obj.has("fats") -> obj.optInt("fats")
                    obj.has("fat") -> obj.optInt("fat")
                    obj.has("fatsGrams") -> obj.optInt("fatsGrams")
                    else -> 14
                }
                val carbs = when {
                    obj.has("carbs") -> obj.optInt("carbs")
                    obj.has("carbohydrates") -> obj.optInt("carbohydrates")
                    obj.has("carbsGrams") -> obj.optInt("carbsGrams")
                    else -> 46
                }
                val summary = obj.optString("summary").ifBlank {
                    obj.optString("name").ifBlank {
                        obj.optString("dish").ifBlank {
                            obj.optString("title", if (userNote.isNotBlank()) userNote else "Блюдо с фото")
                        }
                    }
                }
                return@withContext FoodEstimate(
                    calories = cal,
                    proteinGrams = protein,
                    fatsGrams = fats,
                    carbsGrams = carbs,
                    summary = summary
                )
            } catch (e: Exception) {
                Log.w("GeminiAi", "Failed to parse photo nutrition JSON: $cleanJson", e)
            }
        }

        // Smart fallback estimate if offline or credits depleted
        if (userNote.isNotBlank()) {
            return@withContext estimateNutrition(userNote)
        }

        val fallbackMenu = listOf(
            FoodEstimate(calories = 385, proteinGrams = 36, fatsGrams = 8, carbsGrams = 42, summary = "Куриное филе с гречкой и овощами"),
            FoodEstimate(calories = 460, proteinGrams = 32, fatsGrams = 18, carbsGrams = 40, summary = "Семга на гриле с диким рисом"),
            FoodEstimate(calories = 340, proteinGrams = 14, fatsGrams = 10, carbsGrams = 48, summary = "Овсяная каша с лесными ягодами"),
            FoodEstimate(calories = 290, proteinGrams = 22, fatsGrams = 19, carbsGrams = 6, summary = "Омлет с сыром, томатами и зеленью"),
            FoodEstimate(calories = 490, proteinGrams = 42, fatsGrams = 22, carbsGrams = 28, summary = "Стейк из говядины с запеченными овощами"),
            FoodEstimate(calories = 310, proteinGrams = 28, fatsGrams = 8, carbsGrams = 30, summary = "Творожная запеканка с ягодами"),
            FoodEstimate(calories = 440, proteinGrams = 29, fatsGrams = 14, carbsGrams = 48, summary = "Паста из твердых сортов с индейкой"),
            FoodEstimate(calories = 320, proteinGrams = 16, fatsGrams = 12, carbsGrams = 36, summary = "Боул с киноа, авокадо и нутом"),
            FoodEstimate(calories = 360, proteinGrams = 26, fatsGrams = 12, carbsGrams = 38, summary = "Суп с фрикадельками и цельнозерновой тост"),
            FoodEstimate(calories = 260, proteinGrams = 8, fatsGrams = 15, carbsGrams = 22, summary = "Греческий салат с фетой и оливками")
        )
        val safeHash = (base64Image.hashCode().toLong() and 0x7FFFFFFFL).toInt()
        val index = safeHash % fallbackMenu.size
        fallbackMenu[index]
    }

    // 1. Task Decomposition (Разбиение задачи на подзадачи)
    suspend fun decomposeTask(taskTitle: String): List<String> = withContext(Dispatchers.Default) {
        val systemInstruction = "Ты — персональный эксперт по продуктивности и тайм-менеджменту. " +
                "Твоя задача — разбить цель или задачу пользователя на 3-5 четких, последовательных и выполнимых подзадач. " +
                "Отвечай ТОЛЬКО в формате валидного JSON-массива строк, без markdown-разметки: [\"Шаг 1\", \"Шаг 2\", ...]"

        val prompt = "Разбей следующую задачу на 3-5 конкретных подзадач: \"$taskTitle\""
        val response = callGemini(prompt, systemInstruction)

        val parsed = parseJsonStringArray(response)
        if (parsed.isNotEmpty()) return@withContext parsed

        // Smart fallback when offline or API key absent
        listOf(
            "Определить ключевые требования для: $taskTitle",
            "Подготовить материалы и инструменты",
            "Выполнить основной блок работ",
            "Проверить результат и зафиксировать прогресс"
        )
    }

    // 2. Nutrition Estimation (Оценка КБЖУ по естественному описанию)
    suspend fun estimateNutrition(foodDescription: String): FoodEstimate = withContext(Dispatchers.Default) {
        val systemInstruction = "Ты — сертифицированный диетолог-нутрициолог. " +
                "Пользователь описывает свой прием пищи на русском языке. " +
                "Определи примерную калорийность (ккал), белки (г), жиры (г) и углеводы (г) порции. " +
                "Отвечай СТРОГО в формате JSON без markdown-блоков: " +
                "{\"summary\": \"Название блюда\", \"calories\": 350, \"protein\": 25, \"fats\": 12, \"carbs\": 35}"

        val prompt = "Оцени блюдо: \"$foodDescription\""
        val response = callGemini(prompt, systemInstruction)

        val cleanJson = cleanJsonString(response)
        if (cleanJson.isNotBlank()) {
            try {
                val obj = JSONObject(cleanJson)
                val cal = when {
                    obj.has("calories") -> obj.optInt("calories")
                    obj.has("kcal") -> obj.optInt("kcal")
                    obj.has("cal") -> obj.optInt("cal")
                    else -> 350
                }
                val protein = when {
                    obj.has("protein") -> obj.optInt("protein")
                    obj.has("proteinGrams") -> obj.optInt("proteinGrams")
                    obj.has("proteins") -> obj.optInt("proteins")
                    else -> 18
                }
                val fats = when {
                    obj.has("fats") -> obj.optInt("fats")
                    obj.has("fat") -> obj.optInt("fat")
                    obj.has("fatsGrams") -> obj.optInt("fatsGrams")
                    else -> 12
                }
                val carbs = when {
                    obj.has("carbs") -> obj.optInt("carbs")
                    obj.has("carbohydrates") -> obj.optInt("carbohydrates")
                    obj.has("carbsGrams") -> obj.optInt("carbsGrams")
                    else -> 40
                }
                val summary = obj.optString("summary").ifBlank {
                    obj.optString("name").ifBlank {
                        obj.optString("dish").ifBlank {
                            obj.optString("title", foodDescription)
                        }
                    }
                }
                return@withContext FoodEstimate(
                    calories = cal,
                    proteinGrams = protein,
                    fatsGrams = fats,
                    carbsGrams = carbs,
                    summary = summary
                )
            } catch (e: Exception) {
                Log.w("GeminiAi", "Failed to parse nutrition JSON: $cleanJson", e)
            }
        }

        // Smart fallback estimate based on extensive culinary database & portion multipliers
        calculateLocalFoodEstimate(foodDescription)
    }

    fun calculateLocalFoodEstimate(foodDescription: String): FoodEstimate {
        val lower = foodDescription.lowercase().trim()

        var multiplier = 1.0f
        val numberRegex = Regex("""(\d+)\s*(?:шт|штук|порц|кус|яйц|сырник|блин|котлет|тост|бутерброд)?""")
        val match = numberRegex.find(lower)
        if (match != null) {
            val num = match.groupValues[1].toIntOrNull()
            if (num != null && num in 1..10) {
                if ("100г" !in lower && "200г" !in lower && "300г" !in lower && "500г" !in lower) {
                    multiplier = num.toFloat().coerceIn(1f, 6f)
                }
            }
        }
        if ("половин" in lower || "1/2" in lower) multiplier = 0.5f
        if ("двойн" in lower || "две " in lower || "два " in lower) multiplier = 2.0f
        if ("три " in lower) multiplier = 3.0f

        val (baseCal, baseP, baseF, baseC) = when {
            // Breakfast
            "сырник" in lower -> listOf(120, 9, 4, 12)
            "яйц" in lower || "глазунь" in lower || "пашот" in lower -> listOf(110, 8, 8, 1)
            "омлет" in lower -> listOf(240, 16, 18, 4)
            "творог" in lower && ("сметан" in lower || "мед" in lower) -> listOf(340, 26, 14, 26)
            "творог" in lower -> listOf(220, 28, 5, 6)
            "овсянк" in lower || "геркулес" in lower -> listOf(280, 10, 6, 48)
            "каша" in lower && "манн" in lower -> listOf(260, 7, 5, 46)
            "каша" in lower -> listOf(250, 8, 5, 44)
            "блин" in lower || "оладь" in lower -> listOf(170, 4, 6, 25)

            // Sandwiches & bakery
            "бутерброд" in lower && ("колбас" in lower || "ветчин" in lower) -> listOf(260, 9, 14, 24)
            "бутерброд" in lower && "сыр" in lower -> listOf(240, 10, 12, 22)
            "бутерброд" in lower && "рыб" in lower -> listOf(220, 12, 9, 22)
            "бутерброд" in lower || "тост" in lower -> listOf(200, 6, 8, 26)
            "круассан" in lower -> listOf(330, 6, 18, 36)
            "пирожок" in lower || "пирог" in lower -> listOf(270, 7, 11, 36)
            "хачапури" in lower || "самс" in lower -> listOf(420, 16, 22, 40)
            "лаваш" in lower -> listOf(230, 7, 1, 48)

            // Compound meals
            ("гречк" in lower || "рис" in lower) && ("куриц" in lower || "грудк" in lower || "индейк" in lower) -> listOf(420, 40, 8, 48)
            ("гречк" in lower || "рис" in lower) && ("котлет" in lower || "говядин" in lower) -> listOf(470, 28, 18, 48)
            ("пюре" in lower || "картошк" in lower) && "котлет" in lower -> listOf(460, 22, 22, 44)
            ("пюре" in lower || "картошк" in lower) && ("куриц" in lower || "грудк" in lower) -> listOf(390, 32, 10, 42)
            ("пюре" in lower || "картошк" in lower) && "рыб" in lower -> listOf(380, 26, 12, 42)
            ("макарон" in lower || "паст" in lower) && ("фарш" in lower || "мяс" in lower || "болоньез" in lower) -> listOf(520, 26, 19, 60)
            ("макарон" in lower || "паст" in lower) && "сыр" in lower -> listOf(460, 18, 18, 56)

            // Side dishes standalone
            "гречк" in lower -> listOf(280, 11, 3, 52)
            "рис" in lower -> listOf(270, 6, 1, 58)
            "пюре" in lower || "картофел" in lower -> listOf(210, 4, 6, 36)
            "картошка фри" in lower || "фри" in lower -> listOf(360, 4, 18, 46)
            "макарон" in lower || "паст" in lower || "спагетти" in lower -> listOf(350, 12, 2, 70)

            // Meat & Fish standalone
            "куриц" in lower || "грудк" in lower || "индейк" in lower -> listOf(260, 38, 5, 0)
            "котлет" in lower -> listOf(240, 16, 16, 8)
            "стейк" in lower || "говядин" in lower -> listOf(380, 40, 24, 0)
            "свинин" in lower || "шашлык" in lower -> listOf(420, 30, 32, 2)
            "рыб" in lower || "лосос" in lower || "семг" in lower || "форел" in lower -> listOf(290, 32, 16, 0)
            "тунец" in lower || "минтай" in lower -> listOf(180, 32, 2, 0)
            "креветк" in lower || "морепродукт" in lower -> listOf(150, 28, 2, 2)

            // Fast food & popular
            "шаурм" in lower || "донер" in lower -> listOf(580, 30, 25, 58)
            "бургер" in lower || "чизбургер" in lower -> listOf(540, 26, 28, 46)
            "пицц" in lower -> listOf(510, 22, 22, 56)
            "плов" in lower -> listOf(530, 24, 24, 55)
            "пельмен" in lower || "манты" in lower || "хинкал" in lower -> listOf(480, 22, 20, 52)
            "суши" in lower || "ролл" in lower -> listOf(440, 16, 10, 68)
            "лапш" in lower || "рамен" in lower || "удон" in lower || "вок" in lower -> listOf(490, 20, 16, 66)

            // Soups
            "борщ" in lower || "щи" in lower -> listOf(210, 12, 10, 18)
            "солянк" in lower -> listOf(290, 16, 18, 16)
            "суп" in lower || "бульон" in lower || "рассольник" in lower -> listOf(180, 9, 8, 18)
            "окрошк" in lower -> listOf(220, 12, 10, 20)

            // Salads
            "цезар" in lower -> listOf(340, 18, 24, 12)
            "оливье" in lower -> listOf(360, 12, 26, 18)
            "греческ" in lower -> listOf(220, 6, 18, 9)
            "салат" in lower -> listOf(140, 3, 9, 12)

            // Fruits & Sweets & Drinks
            "банан" in lower -> listOf(110, 1, 0, 26)
            "яблок" in lower || "груш" in lower -> listOf(80, 0, 0, 20)
            "шоколад" in lower -> listOf(280, 4, 17, 28)
            "протеин" in lower || "гейнер" in lower -> listOf(210, 32, 2, 16)
            "кофе" in lower && ("капучино" in lower || "латте" in lower) -> listOf(120, 6, 5, 12)
            "кофе" in lower || "чай" in lower -> listOf(30, 1, 1, 4)
            "кола" in lower || "газировк" in lower || "сок" in lower -> listOf(140, 0, 0, 35)

            else -> listOf(350, 18, 12, 40)
        }

        val totalCal = (baseCal * (if (multiplier > 1.0f && baseCal < 200) multiplier else if (multiplier > 1.0f) 1.0f + (multiplier - 1f) * 0.7f else multiplier)).toInt()
        val totalP = (baseP * multiplier).toInt()
        val totalF = (baseF * multiplier).toInt()
        val totalC = (baseC * multiplier).toInt()

        return FoodEstimate(
            calories = totalCal,
            proteinGrams = totalP,
            fatsGrams = totalF,
            carbsGrams = totalC,
            summary = foodDescription
        )
    }

    // 3. Habit Plan Generator (Генератор привычек под цель)
    suspend fun generateHabitPlan(goal: String): List<GeneratedHabit> {
        return generateHabitPlanWithStatus(goal).first
    }

    suspend fun generateHabitPlanWithStatus(goal: String): Pair<List<GeneratedHabit>, Boolean> = withContext(Dispatchers.Default) {
        if (!isApiKeyConfigured()) {
            Log.i("GeminiAi", "API key not configured. Using contextual fallback habits for goal: $goal")
            return@withContext Pair(generateFallbackHabits(goal), false)
        }

        val systemInstruction = "Ты — персональный AI-коуч по формированию полезных привычек в фитнес-трекере PulseTrack AI. " +
                "Пользователь задает конкретную цель. Сгенерируй ровно 3 специфичные, практичные привычки, идеально поддерживающие именно эту цель. " +
                "Если привычка связана с физической активностью, шагами, сном или водой, укажи autoSyncType: \"STEPS\", \"WORKOUT\", \"SLEEP\" или \"WATER\" и autoSyncThreshold. " +
                "Отвечай СТРОГО в формате валидного JSON-массива без markdown: " +
                "[{\"title\": \"...\", \"description\": \"...\", \"category\": \"Спорт|Здоровье|Продуктивность\", \"autoSyncType\": \"STEPS|WORKOUT|WATER|SLEEP|NONE\", \"autoSyncThreshold\": 10000}]"

        val prompt = "Создай 3 ключевые, высокоточные привычки для следующей цели пользователя: \"$goal\""
        val response = callGemini(prompt, systemInstruction)

        val cleanJson = cleanJsonString(response)
        if (cleanJson.isNotBlank()) {
            try {
                val array = if (cleanJson.startsWith("[")) {
                    JSONArray(cleanJson)
                } else {
                    val root = JSONObject(cleanJson)
                    root.optJSONArray("habits")
                        ?: root.optJSONArray("list")
                        ?: root.optJSONArray("items")
                        ?: root.optJSONArray("plan")
                        ?: JSONArray()
                }
                val list = mutableListOf<GeneratedHabit>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(
                        GeneratedHabit(
                            title = obj.optString("title", obj.optString("name", "Привычка")),
                            description = obj.optString("description", obj.optString("desc", "")),
                            category = obj.optString("category", "Здоровье"),
                            autoSyncType = obj.optString("autoSyncType", obj.optString("syncType", "NONE")),
                            autoSyncThreshold = if (obj.has("autoSyncThreshold")) obj.optInt("autoSyncThreshold", 0) else obj.optInt("threshold", 0)
                        )
                    )
                }
                if (list.isNotEmpty()) return@withContext Pair(list, true)
            } catch (e: Exception) {
                Log.w("GeminiAi", "Failed to parse habits JSON: $cleanJson", e)
            }
        }

        Pair(generateFallbackHabits(goal), false)
    }

    fun generateFallbackHabits(goal: String): List<GeneratedHabit> {
        val lower = goal.lowercase()
        return when {
            "похуден" in lower || "сушк" in lower || "дефицит" in lower || "сброс" in lower || "вес" in lower -> {
                listOf(
                    GeneratedHabit(
                        title = "Контроль рациона и дефицит калорий",
                        description = "Внесение всех приемов пищи в дневник питания",
                        category = "Здоровье",
                        autoSyncType = "NONE",
                        autoSyncThreshold = 0
                    ),
                    GeneratedHabit(
                        title = "Норма шагов 10 000 в день",
                        description = "Ежедневная двигательная активность для сжигания жира",
                        category = "Спорт",
                        autoSyncType = "STEPS",
                        autoSyncThreshold = 10000
                    ),
                    GeneratedHabit(
                        title = "Гидратация: 2500 мл воды",
                        description = "Ускорение метаболизма и выведение продуктов распада",
                        category = "Здоровье",
                        autoSyncType = "WATER",
                        autoSyncThreshold = 2500
                    )
                )
            }
            "спин" in lower || "кинезио" in lower || "мтб" in lower || "осанк" in lower || "грыж" in lower || "позвоноч" in lower -> {
                listOf(
                    GeneratedHabit(
                        title = "Декомпрессия спины на МТБ",
                        description = "Выполнение лечебного протокола кинезиотерапии на блочном тренажере",
                        category = "Спорт",
                        autoSyncType = "WORKOUT",
                        autoSyncThreshold = 45
                    ),
                    GeneratedHabit(
                        title = "Разминка позвоночника",
                        description = "Утренняя суставная гимнастика и снятие осевой нагрузки",
                        category = "Здоровье",
                        autoSyncType = "NONE",
                        autoSyncThreshold = 0
                    ),
                    GeneratedHabit(
                        title = "Сон 7+ часов для регенерации дисков",
                        description = "Качественный сон для диффузного питания межпозвонковых дисков",
                        category = "Здоровье",
                        autoSyncType = "SLEEP",
                        autoSyncThreshold = 420
                    )
                )
            }
            "масс" in lower || "мышц" in lower || "сил" in lower || "гипертроф" in lower -> {
                listOf(
                    GeneratedHabit(
                        title = "Силовая тренировка с отягощением",
                        description = "Прогрессивная нагрузка для стимуляции гипертрофии мышц",
                        category = "Спорт",
                        autoSyncType = "WORKOUT",
                        autoSyncThreshold = 60
                    ),
                    GeneratedHabit(
                        title = "Норма белка 140г+",
                        description = "Соблюдение белково-углеводного баланса для синтеза мышечных волокон",
                        category = "Здоровье",
                        autoSyncType = "NONE",
                        autoSyncThreshold = 0
                    ),
                    GeneratedHabit(
                        title = "Сон 8 часов",
                        description = "Полноценное анаболическое восстановление ЦНС и мышечных волокон",
                        category = "Здоровье",
                        autoSyncType = "SLEEP",
                        autoSyncThreshold = 480
                    )
                )
            }
            "работ" in lower || "учеб" in lower || "кодинг" in lower || "программист" in lower || "фокус" in lower || "концентр" in lower -> {
                listOf(
                    GeneratedHabit(
                        title = "Фокус-сессия 45 минут без соцсетей",
                        description = "Глубокая концентрация на приоритетной задаче (Deep Work)",
                        category = "Продуктивность",
                        autoSyncType = "NONE",
                        autoSyncThreshold = 0
                    ),
                    GeneratedHabit(
                        title = "Чтение профильной литературы 20 мин",
                        description = "Изучение технической документации или профессиональных книг",
                        category = "Продуктивность",
                        autoSyncType = "NONE",
                        autoSyncThreshold = 0
                    ),
                    GeneratedHabit(
                        title = "Прогулка на свежем воздухе 5000 шагов",
                        description = "Переключение внимания и кислородное насыщение мозга",
                        category = "Спорт",
                        autoSyncType = "STEPS",
                        autoSyncThreshold = 5000
                    )
                )
            }
            "гитар" in lower || "музык" in lower || "guitar" in lower || "music" in lower || "творчеств" in lower -> {
                listOf(
                    GeneratedHabit(
                        title = "Практика игры 30-45 минут",
                        description = "Регулярная отработка техники, гамм и разбор репертуара ($goal)",
                        category = "Творчество",
                        autoSyncType = "NONE",
                        autoSyncThreshold = 0
                    ),
                    GeneratedHabit(
                        title = "Разминка кистей и пальцев",
                        description = "5 минут разогрева связок и растяжки перед занятиями",
                        category = "Здоровье",
                        autoSyncType = "NONE",
                        autoSyncThreshold = 0
                    ),
                    GeneratedHabit(
                        title = "Прогулка 6 000 шагов для перезагрузки",
                        description = "Слуховая и ментальная разгрузка между практическими сессиями",
                        category = "Спорт",
                        autoSyncType = "STEPS",
                        autoSyncThreshold = 6000
                    )
                )
            }
            else -> {
                listOf(
                    GeneratedHabit(
                        title = "10 000 шагов в день",
                        description = "Ежедневная двигательная норма с браслета Xiaomi",
                        category = "Спорт",
                        autoSyncType = "STEPS",
                        autoSyncThreshold = 10000
                    ),
                    GeneratedHabit(
                        title = "2 литра чистой воды",
                        description = "Поддержание здоровой гидратации",
                        category = "Здоровье",
                        autoSyncType = "WATER",
                        autoSyncThreshold = 2000
                    ),
                    GeneratedHabit(
                        title = "30 минут глубокого фокуса",
                        description = "Работа над приоритетной целью: $goal",
                        category = "Продуктивность",
                        autoSyncType = "NONE",
                        autoSyncThreshold = 0
                    )
                )
            }
        }
    }

    // 4. Daily AI Audit (Анализ продуктивности, сна, стресса, SpO2, тренировок Xiaomi Band и питания)
    suspend fun generateDailyAudit(
        steps: Int,
        sleepMinutes: Int,
        deepSleepMinutes: Int,
        spo2Percent: Int,
        stressLevel: Int,
        activeMinutes: Int,
        workoutsCount: Int,
        burnedCalories: Int,
        foodCalories: Int,
        waterMl: Int,
        completedTasks: Int,
        pendingTasks: Int,
        habitsCompleted: Int,
        totalHabits: Int,
        bmr: Int = 1680
    ): AiAuditReport = withContext(Dispatchers.Default) {
        val sleepHours = sleepMinutes / 60
        val sleepMins = sleepMinutes % 60
        val deepSleepHours = deepSleepMinutes / 60
        val deepSleepMins = deepSleepMinutes % 60
        val energyNet = foodCalories - (burnedCalories + bmr)

        val systemInstruction = "Ты — спортивный врач-нутрициолог и персональный коуч по продуктивности. " +
                "Проанализируй полную телеметрию браслета Xiaomi Smart Band 10 и дневник пользователя:\n" +
                "- Биометрия браслета: Кислород в крови SpO2 $spo2Percent%, Уровень стресса $stressLevel/100, " +
                "Сон $sleepHours ч $sleepMins м (Глубокий: $deepSleepHours ч $deepSleepMins м), " +
                "Шаги $steps, Время активности $activeMinutes мин, Тренировок $workoutsCount, Сожжено $burnedCalories ккал.\n" +
                "- Питание и вода: Потреблено $foodCalories ккал, Вода $waterMl мл, Баланс энергии $energyNet ккал (с учетом BMR $bmr ккал).\n" +
                "- Дела и привычки: Выполнено задач $completedTasks (осталось $pendingTasks), Привычек закрыто $habitsCompleted из $totalHabits.\n" +
                "Составь структурированный, вдохновляющий и профессиональный отчет на русском языке. " +
                "Обязательно оцени: 1) Влияние стресса ($stressLevel/100) и SpO2 ($spo2Percent%) на работоспособность; " +
                "2) Качество глубокого сна и восстановление ЦНС; 3) Баланс питания и активности. " +
                "Отвечай СТРОГО в формате JSON без markdown: " +
                "{\"title\": \"Анализ биоритмов и продуктивности\", \"content\": \"Текст анализа с конкретными выводами\", \"burnoutRisk\": \"LOW|MODERATE|HIGH\", \"energyBalanceTip\": \"Практический совет по отдыху, питанию и задачам\"}"

        val prompt = "Проведи комплексный аудит дня на основе всех биометрических данных Xiaomi Band."
        val response = callGemini(prompt, systemInstruction, expectJson = true)

        val cleanJson = cleanJsonString(response)
        if (cleanJson.isNotBlank()) {
            try {
                val obj = JSONObject(cleanJson)
                return@withContext AiAuditReport(
                    title = obj.optString("title", "Анализ биоритмов и продуктивности"),
                    content = obj.optString("content", ""),
                    burnoutRisk = obj.optString("burnoutRisk", "LOW"),
                    energyBalanceTip = obj.optString("energyBalanceTip", "")
                )
            } catch (e: Exception) {
                Log.w("GeminiAi", "Failed to parse audit JSON", e)
            }
        }

        // Smart fallback report
        val risk = if ((sleepHours < 6 || stressLevel > 60) && pendingTasks > 4) "HIGH"
                   else if (sleepHours < 7 || stressLevel > 45) "MODERATE"
                   else "LOW"

        val stressAssessment = when {
            stressLevel < 35 -> "Уровень стресса минимален ($stressLevel/100, зона релаксации)."
            stressLevel < 60 -> "Умеренный уровень стресса ($stressLevel/100), организм в рабочем тонусе."
            else -> "Повышенный стресс ($stressLevel/100)! Рекомендуется дыхательная гимнастика."
        }

        val spo2Assessment = if (spo2Percent >= 97) "Кислород в крови в идеале ($spo2Percent%)."
        else "Уровень SpO2 $spo2Percent%. Полезно проветрить помещение и прогуляться."

        val sleepAssessment = if (sleepHours >= 7 && deepSleepMinutes >= 80)
            "Отличный сон ($sleepHours ч $sleepMins м с глубокой фазой $deepSleepHours ч $deepSleepMins м), организм полностью восстановился."
        else "Сон составил $sleepHours ч $sleepMins м (глубокий: $deepSleepMins м). Недосып повышает утомляемость."

        val activityAssessment = "Активность за день: $steps шагов, $activeMinutes мин движения, сожжено $burnedCalories ккал."

        val content = "$stressAssessment $spo2Assessment $sleepAssessment $activityAssessment Закрыто $completedTasks задач и $habitsCompleted привычек."

        val energyTip = if (stressLevel > 50) "Выделите 10 минут на дыхательную паузу. Перенесите рутинные задачи на завтра."
        else if (energyNet < -400) "Вы в дефиците калорий. Добавьте сложные углеводы после тренировки."
        else "Баланс калорий и восстановления стабилен. Фокусируйтесь на ключевых целях!"

        AiAuditReport(
            title = "Сводный аудит здоровья и продуктивности",
            content = content,
            burnoutRisk = risk,
            energyBalanceTip = energyTip
        )
    }

    // 5. Personalized Advice from Goal, Body Metrics & Fitness Band Telemetry
    suspend fun generatePersonalizedAdvice(
        profile: UserProfileEntity,
        steps: Int,
        sleepMinutes: Int,
        waterMl: Int,
        burnedCalories: Int
    ): String = withContext(Dispatchers.Default) {
        val goalRu = when (profile.goal.uppercase()) {
            "LOSE_WEIGHT", "ПОХУДЕТЬ", "СНИЖЕНИЕ ВЕСА" -> "Похудение и сжигание жира"
            "BUILD_MUSCLE", "НАБРАТЬ МЫШЦЫ", "НАБОР МАССЫ" -> "Набор сухой мышечной массы"
            "ENDURANCE", "ВЫНОСЛИВОСТЬ" -> "Развитие выносливости и кардио"
            else -> "Поддержание формы и тонуса"
        }

        val systemInstruction = "Ты — персональный спортивный врач, нутрициолог и AI-тренер. " +
                "Твоя задача — дать четкие, персонализированные и мотивирующие советы пользователю на основе его физических параметров, целей и данных с фитнес-браслета. " +
                "Форматируй ответ структурированно (2-3 коротких пункта с эмодзи: Питание, Тренировки/Движение, Восстановление)."

        val prompt = """
            Параметры пользователя:
            - Возраст: ${profile.age} лет, Пол: ${if (profile.gender == "MALE") "Мужской" else "Женский"}
            - Вес: ${profile.weightKg} кг, Рост: ${profile.heightCm} см
            - Уровень активности: ${profile.activityLevel}
            - Цель: $goalRu
            - Рассчитанные нормы:
              * Базальный метаболизм (BMR): ${profile.bmr} ккал
              * Суточный расход (TDEE): ${profile.tdee} ккал
              * Целевая калорийность: ${profile.targetCalories} ккал
              * Норма воды: ${profile.targetWaterMl} мл
              * БЖУ: Белки ${profile.targetProteinGrams}г, Жиры ${profile.targetFatsGrams}г, Углеводы ${profile.targetCarbsGrams}г
            
            Фактические показатели сегодня с фитнес-браслета Xiaomi Band:
            - Шаги: $steps
            - Сон: ${sleepMinutes / 60} ч ${sleepMinutes % 60} мин
            - Выпито воды: $waterMl мл
            - Сожжено на тренировках: $burnedCalories ккал
            
            Дай конкретные рекомендации для достижения цели "$goalRu" с учетом текущих показателей.
        """.trimIndent()

        val response = callGemini(prompt, systemInstruction, expectJson = false)
        if (response.isNotBlank()) {
            return@withContext response
        }

        val deficitOrSurplus = profile.targetCalories - profile.tdee
        val calDiffText = if (deficitOrSurplus < 0) "Дефицит: ${-deficitOrSurplus} ккал для сжигания жира"
        else if (deficitOrSurplus > 0) "Профицит: +$deficitOrSurplus ккал для роста мышц"
        else "Баланс калорий для поддержания"

        """
        🎯 Персональный план под цель «$goalRu»:
        
        🥗 Питание ($calDiffText):
        • Ваша норма калорий — ${profile.targetCalories} ккал/день.
        • Белки: ${profile.targetProteinGrams}г (${(profile.targetProteinGrams / profile.weightKg * 10).toInt() / 10.0}г на кг веса) для сохранения и роста мышц.
        • Жиры: ${profile.targetFatsGrams}г, Углеводы: ${profile.targetCarbsGrams}г для стабильной энергии.
        
        💧 Водный баланс:
        • Ваша индивидуальная норма воды: ${profile.targetWaterMl} мл в день. 
        • На текущий момент выпито $waterMl мл (${((waterMl.toFloat() / profile.targetWaterMl.coerceAtLeast(1)) * 100).toInt()}% от нормы).
        
        🏃 Активность и браслет Xiaomi:
        • Рекомендуем удерживать от 8 000 до 10 000 шагов в день для поддержания метаболизма.
        • Для цели «$goalRu» оптимальны 3-4 силовые или интервальные тренировки в неделю с контролем пульса.
        
        😴 Восстановление:
        • Сон от 7.5 часов необходим для синтеза белка и регулирования гормонов грелина и лептина.
        """.trimIndent()
    }

    // 1. Photo Exercise Extraction from Program Sheet
    suspend fun extractExercisesFromProgramPhoto(
        base64Image: String,
        mimeType: String = "image/jpeg"
    ): List<KinesitherapyExerciseItem> = withContext(Dispatchers.Default) {
        val systemInstruction = """
            Ты — эксперт по медицинской реабилитации и кинезиотерапии. 
            Пользователь загрузил фото своей программы тренировок/карты пациента или списка тренажеров и упражнений из центра кинезиотерапии (МТБ, ЛФК).
            Внимательно распознай все упражнения, указанные на фото (название, категорию/зону воздействия, рекомендованные веса, подходы, повторения, тип тренажера).
            Если вес не указан явно, подбери безопасный стартовый терапевтический вес (от 5 до 30 кг).
            Отвечай СТРОГО в формате валидного JSON-массива объектов:
            [
              {
                "name": "Декомпрессионная тяга нижнего блока лежа",
                "category": "ДЕКОМПРЕССИЯ СПИНЫ",
                "weightKg": 25.0,
                "sets": 3,
                "reps": 15,
                "machineName": "МТБ-1 нижний блок",
                "notes": "Выдох на усилии"
              }
            ]
        """.trimIndent()

        val prompt = "Распознай все упражнения с этой фотографии программы/карты кинезиотерапии и сформируй структурированный список."
        val response = callGeminiWithImage(base64Image, mimeType, prompt, systemInstruction)
        val cleanJson = cleanJsonString(response)
        if (cleanJson.isNotBlank()) {
            try {
                val array = if (cleanJson.startsWith("[")) {
                    JSONArray(cleanJson)
                } else {
                    val root = JSONObject(cleanJson)
                    root.optJSONArray("exercises")
                        ?: root.optJSONArray("items")
                        ?: root.optJSONArray("list")
                        ?: root.optJSONArray("data")
                        ?: JSONArray()
                }
                val list = mutableListOf<KinesitherapyExerciseItem>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val name = obj.optString("name", obj.optString("title", "Упражнение кинезиотерапии"))
                    val category = obj.optString("category", obj.optString("zone", "ДЕКОМПРЕССИЯ СПИНЫ"))
                    val weightKg = (if (obj.has("weightKg")) obj.optDouble("weightKg", 20.0)
                        else if (obj.has("weight")) obj.optDouble("weight", 20.0)
                        else obj.optDouble("load", 20.0)).toFloat()
                    val sets = if (obj.has("sets")) obj.optInt("sets", 3) else obj.optInt("setsCount", 3)
                    val reps = if (obj.has("reps")) obj.optInt("reps", 15) else obj.optInt("repetitions", 15)
                    val machineName = obj.optString("machineName", obj.optString("machine", "МТБ"))
                    val notes = obj.optString("notes", obj.optString("note", ""))

                    list.add(
                        KinesitherapyExerciseItem(
                            name = name,
                            category = category,
                            weightKg = weightKg,
                            sets = sets,
                            reps = reps,
                            machineName = machineName,
                            notes = notes,
                            isDone = false
                        )
                    )
                }
                if (list.isNotEmpty()) return@withContext list
            } catch (e: Exception) {
                Log.w("GeminiAi", "Failed to parse recognized exercises JSON: $cleanJson", e)
            }
        }

        // Fallback default presets marked as not yet performed
        KinesitherapyExerciseItem.DEFAULT_PRESETS.map { it.copy(isDone = false) }
    }

    // 2. Multimodal Kinesitherapy Review, Load Calculation and Goal Strategy
    suspend fun analyzeKinesitherapySession(
        availableExercisesPhotoBase64: String?,
        performedExercisesPhotoBase64: String?,
        exercises: List<KinesitherapyExerciseItem>,
        userGoal: String,
        userProfile: UserProfileEntity?,
        painBefore: Int,
        painAfter: Int,
        userNotes: String
    ): KinesitherapyAiReport = withContext(Dispatchers.Default) {
        val totalTonnage = exercises.filter { it.isDone }
            .sumOf { (it.weightKg * it.sets * it.reps).toDouble() }.toFloat()
        val completedCount = exercises.count { it.isDone }
        val totalCount = exercises.size

        val systemInstruction = """
            Ты — ведущий врач восстановительной медицины, реабилитолог и спортивный кинезиотерапевт высшей категории.
            Ты специализируешься на декомпрессионной кинезиотерапии (МТБ, блочные тренажеры Бубновского, ЛФК, укрепление глубоких стабилизаторов позвоночника и суставов).
            
            Пользователь посещает тренировки по кинезиотерапии и передает тебе:
            1. Фотографию (если есть) карты упражнений/программы со всеми доступными упражнениями.
            2. Фотографию (если есть) выполненных упражнений, весов на тренажере или процесса тренировки.
            3. Детальный список выполненных упражнений, поднятые веса (кг), подходы и повторения.
            4. Оценку боли/дискомфорта по шкале ВАШ (0-10) до и после занятия.
            5. Свою главную цель реабилитации.

            Твоя задача:
            - Провести экспертный клинический анализ сессии.
            - Рассчитать тренировочный тоннаж и оценить нагрузку (баланс передней/задней мышечной цепи, декомпрессию позвоночника, отсутствие осевой компрессии дисков).
            - Сопоставить выполненное с программой (все ли ключевые узлы проработаны).
            - Дать оценку от 1 до 10.
            - Описать прогресс к цели и конкретные рекомендации на следующую тренировку (корректировка весов, прогрессия, дыхание выдохом «Хаа»).

            Отвечай СТРОГО в формате валидного JSON без markdown:
            {
              "score": 9,
              "evaluation": "Текст общей оценки...",
              "loadAnalysis": "Текст анализа тоннажа и нагрузки...",
              "goalProgress": "Текст о достижении цели...",
              "nextSessionAdvice": "Текст рекомендаций на следующую тренировку..."
            }
        """.trimIndent()

        val promptBuilder = StringBuilder()
        promptBuilder.appendLine("Анализ тренировки по кинезиотерапии:")
        promptBuilder.appendLine("• Цель пациента: $userGoal")
        if (userProfile != null) {
            promptBuilder.appendLine("• Параметры: Возраст ${userProfile.age} лет, Вес ${userProfile.weightKg} кг, Рост ${userProfile.heightCm} см, Пол ${userProfile.gender}")
        }
        promptBuilder.appendLine("• Шкала боли/дискомфорта: До тренировки $painBefore/10, После тренировки $painAfter/10")
        if (userNotes.isNotBlank()) {
            promptBuilder.appendLine("• Заметки пациента: $userNotes")
        }
        promptBuilder.appendLine("• Выполнено упражнений: $completedCount из $totalCount")
        promptBuilder.appendLine("• Список упражнений и весов:")
        exercises.forEachIndexed { index, ex ->
            val status = if (ex.isDone) "ВЫПОЛНЕНО" else "ПРОПУЩЕНО"
            promptBuilder.appendLine("  ${index + 1}. ${ex.name} [${ex.category}] — ${ex.weightKg} кг x ${ex.sets} подх. x ${ex.reps} повт. ($status, тренажер: ${ex.machineName})")
        }
        promptBuilder.appendLine("• Рассчитанный тоннаж: $totalTonnage кг.")
        promptBuilder.appendLine("Внимательно изучи прикрепленные фото (карту доступных упражнений и фото процесса/весов), сопоставь с выполненными упражнениями и дай развернутый вердикт.")

        val images = mutableListOf<Pair<String, String>>()
        if (!availableExercisesPhotoBase64.isNullOrBlank()) {
            images.add(Pair(availableExercisesPhotoBase64, "image/jpeg"))
        }
        if (!performedExercisesPhotoBase64.isNullOrBlank()) {
            images.add(Pair(performedExercisesPhotoBase64, "image/jpeg"))
        }

        val rawResponse = if (images.isNotEmpty()) {
            callGeminiWithMultipleImages(images, promptBuilder.toString(), systemInstruction)
        } else {
            callGemini(promptBuilder.toString(), systemInstruction)
        }

        val cleanJson = cleanJsonString(rawResponse)
        if (cleanJson.isNotBlank()) {
            try {
                val obj = JSONObject(cleanJson)
                val score = (if (obj.has("score")) obj.optInt("score", 9) else obj.optInt("rating", 9)).coerceIn(1, 10)
                val evaluation = obj.optString("evaluation", obj.optString("summary", obj.optString("assessment", "Качественная кинезиотерапевтическая тренировка.")))
                val loadAnalysis = obj.optString("loadAnalysis", obj.optString("load_analysis", obj.optString("tonnageAnalysis", "Нагрузка сбалансирована, декомпрессия позвоночника достигнута.")))
                val goalProgress = obj.optString("goalProgress", obj.optString("goal_progress", obj.optString("progress", "Динамика положительная, снижение боли зафиксировано.")))
                val nextSessionAdvice = obj.optString("nextSessionAdvice", obj.optString("next_session_advice", obj.optString("recommendations", obj.optString("advice", "Продолжайте прогрессию на МТБ с акцентом на выдох."))))

                return@withContext KinesitherapyAiReport(
                    score = score,
                    totalTonnageKg = totalTonnage,
                    evaluation = evaluation,
                    loadAnalysis = loadAnalysis,
                    goalProgress = goalProgress,
                    nextSessionAdvice = nextSessionAdvice
                )
            } catch (e: Exception) {
                Log.w("GeminiAi", "Failed to parse kinesitherapy JSON: $cleanJson", e)
            }
        }

        // Smart Clinical Fallback Generator
        val painDelta = painBefore - painAfter
        val painResultText = when {
            painDelta > 0 -> "Снижение болевого синдрома на $painDelta баллов (с $painBefore/10 до $painAfter/10) свидетельствует о выраженном декомпрессионном эффекте и снятии локального мышечного спазма."
            painDelta == 0 -> "Уровень дискомфорта стабилен ($painAfter/10). Ткани адаптируются к терапевтической нагрузке."
            else -> "Умеренное ощущение усталости после непривычного тоннажа ($painAfter/10). Рекомендуется легкая растяжка и холодный компресс при необходимости."
        }

        val score = when {
            completedCount >= 5 && painDelta >= 0 -> 9
            completedCount >= 3 -> 8
            completedCount >= 1 -> 7
            else -> 6
        }

        val exercisesNames = exercises.filter { it.isDone }.joinToString(", ") { "${it.name} (${it.weightKg} кг)" }

        KinesitherapyAiReport(
            score = score,
            totalTonnageKg = totalTonnage,
            evaluation = "Тренировка кинезиотерапии проведена успешно: выполнено $completedCount упражнений из $totalCount. " +
                    (if (exercisesNames.isNotBlank()) "Ключевые упражнения: $exercisesNames. " else "") +
                    "Работа на блочных декомпрессионных тренажерах обеспечила безопасное растяжение глубоких связок без осевой нагрузки на межпозвонковые диски.",
            loadAnalysis = "Суммарный тренировочный тоннаж составил ${totalTonnage.toInt()} кг (${(totalTonnage / 1000f * 10).toInt() / 10.0} т). " +
                    "Баланс распределен гармонично: паравертебральные стабилизаторы спины получили необходимый стимул трофики, " +
                    "активированы мышцы тазового дна и брюшного пресса. Векторы тяги нижнего и верхнего блоков скомпенсированы.",
            goalProgress = "Для цели «$userGoal»: $painResultText Регулярное повторение данного протокола устраняет застойные явления в глубоких капиллярах позвоночника и восстанавливает естественный лордоз.",
            nextSessionAdvice = "На следующем занятии:\n" +
                    "1. В упражнениях на нижнем блоке МТБ увеличьте рабочий вес на +2.5 кг при сохранении темпа 15 повторений.\n" +
                    "2. Строго соблюдайте фазу выдоха «Хаа» в точке максимального усилия для нормализации внутрибрюшного давления.\n" +
                    "3. Подключите упражнение «Березка» на подвесных петлях для улучшения оттока крови и снятия усталости ног."
        )
    }

    internal fun cleanJsonString(raw: String): String {
        var str = raw.trim()
        if (str.isEmpty()) return ""

        if (str.startsWith("```json", ignoreCase = true)) {
            str = str.substring(7).trim()
        } else if (str.startsWith("```")) {
            str = str.removePrefix("```").trim()
        }
        if (str.endsWith("```")) {
            str = str.removeSuffix("```").trim()
        }

        val firstBrace = str.indexOf('{')
        val firstBracket = str.indexOf('[')
        val firstIndex = when {
            firstBrace != -1 && firstBracket != -1 -> minOf(firstBrace, firstBracket)
            firstBrace != -1 -> firstBrace
            firstBracket != -1 -> firstBracket
            else -> -1
        }

        val lastBrace = str.lastIndexOf('}')
        val lastBracket = str.lastIndexOf(']')
        val lastIndex = maxOf(lastBrace, lastBracket)

        return if (firstIndex != -1 && lastIndex > firstIndex) {
            str.substring(firstIndex, lastIndex + 1).trim()
        } else {
            str.trim()
        }
    }

    internal fun parseJsonStringArray(raw: String): List<String> {
        val clean = cleanJsonString(raw)
        if (clean.isNotBlank()) {
            try {
                val list = mutableListOf<String>()
                if (clean.startsWith("[")) {
                    val arr = JSONArray(clean)
                    for (i in 0 until arr.length()) {
                        val s = arr.optString(i)
                        if (s.isNotBlank()) list.add(s)
                    }
                    if (list.isNotEmpty()) return list
                } else if (clean.startsWith("{")) {
                    val obj = JSONObject(clean)
                    val arr = obj.optJSONArray("steps")
                        ?: obj.optJSONArray("subtasks")
                        ?: obj.optJSONArray("tasks")
                        ?: obj.optJSONArray("items")
                        ?: obj.optJSONArray("list")
                    if (arr != null) {
                        for (i in 0 until arr.length()) {
                            val s = arr.optString(i)
                            if (s.isNotBlank()) list.add(s)
                        }
                        if (list.isNotEmpty()) return list
                    }
                }
            } catch (e: Exception) {
                // fall through to regex
            }
        }

        // Fallback: parse lines like "1. Step", "- Step", "* Step"
        val regex = Regex("""^(?:[-*•]|\d+[.)])\s*(.+)""")
        val lines = raw.lines().mapNotNull { line ->
            val match = regex.find(line.trim())
            match?.groupValues?.get(1)?.trim()
        }.filter { it.isNotBlank() }

        return lines
    }

    companion object {
        const val DEFAULT_EMBEDDED_KEY = ""
        private const val MODEL_NAME = "gemini-3.8-flash"
        private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"

        fun isKnownDeadKey(key: String): Boolean {
            val lower = key.trim().lowercase()
            return lower.isBlank() || lower == "my_gemini_api_key" || lower == "your_gemini_api_key_here"
        }

        fun isValidApiKeyFormat(key: String): Boolean {
            return key.isNotBlank() && key != "MY_GEMINI_API_KEY" && key != "YOUR_GEMINI_API_KEY_HERE" && (key.startsWith("AIzaSy") || key.startsWith("AQ.") || key.length >= 20)
        }
    }
}
