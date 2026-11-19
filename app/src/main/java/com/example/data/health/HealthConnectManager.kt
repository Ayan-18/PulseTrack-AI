package com.example.data.health

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.OxygenSaturationRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.metadata.Device
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.health.connect.client.units.Energy
import androidx.health.connect.client.units.Percentage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

data class HealthConnectWorkout(
    val title: String,
    val workoutType: String,
    val durationMinutes: Int,
    val caloriesBurned: Int,
    val avgHeartRate: Int,
    val distanceMeters: Int,
    val startTimeMillis: Long
)

data class HealthConnectSnapshot(
    val steps: Int? = null,
    val avgHeartRate: Int? = null,
    val minHeartRate: Int? = null,
    val maxHeartRate: Int? = null,
    val sleepMinutes: Int? = null,
    val spo2Percent: Int? = null,
    val activeCalories: Int? = null,
    val workouts: List<HealthConnectWorkout> = emptyList(),
    val isFromHealthConnect: Boolean = false,
    val statusMessage: String = ""
)

sealed class HealthConnectAvailability {
    object Available : HealthConnectAvailability()
    object UpdateRequired : HealthConnectAvailability()
    object NotSupported : HealthConnectAvailability()
}

sealed class HealthSyncResult {
    data class Success(
        val message: String,
        val steps: Int?,
        val heartRate: Int?,
        val workoutsCount: Int,
        val isRealData: Boolean
    ) : HealthSyncResult()

    data class NeedsPermission(val permissions: Set<String>) : HealthSyncResult()
    data class UpdateRequired(val message: String) : HealthSyncResult()
    data class NotAvailable(val message: String) : HealthSyncResult()
    data class NoData(val message: String) : HealthSyncResult()
    data class Error(val message: String) : HealthSyncResult()
}

class HealthConnectManager(private val context: Context) {

    val healthConnectClient: HealthConnectClient? by lazy {
        try {
            if (HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE) {
                HealthConnectClient.getOrCreate(context)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("HealthConnect", "Failed to create HealthConnectClient: ${e.message}", e)
            null
        }
    }

    val readPermissions: Set<String> = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(HeartRateRecord::class),
        HealthPermission.getReadPermission(SleepSessionRecord::class),
        HealthPermission.getReadPermission(OxygenSaturationRecord::class),
        HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class),
        HealthPermission.getReadPermission(ExerciseSessionRecord::class)
    )

    val writePermissions: Set<String> = setOf(
        HealthPermission.getWritePermission(StepsRecord::class),
        HealthPermission.getWritePermission(HeartRateRecord::class),
        HealthPermission.getWritePermission(ActiveCaloriesBurnedRecord::class),
        HealthPermission.getWritePermission(ExerciseSessionRecord::class)
    )

    val allPermissions: Set<String> = readPermissions + writePermissions

    fun checkAvailability(): HealthConnectAvailability {
        return try {
            when (HealthConnectClient.getSdkStatus(context)) {
                HealthConnectClient.SDK_AVAILABLE -> HealthConnectAvailability.Available
                HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> HealthConnectAvailability.UpdateRequired
                else -> HealthConnectAvailability.NotSupported
            }
        } catch (e: Exception) {
            Log.e("HealthConnect", "Error checking Health Connect SDK status: ${e.message}", e)
            HealthConnectAvailability.NotSupported
        }
    }

    fun isAvailable(): Boolean {
        return checkAvailability() == HealthConnectAvailability.Available
    }

    suspend fun hasReadPermissions(): Boolean = withContext(Dispatchers.IO) {
        val client = healthConnectClient ?: return@withContext false
        try {
            val granted = client.permissionController.getGrantedPermissions()
            return@withContext granted.containsAll(readPermissions)
        } catch (e: Exception) {
            Log.w("HealthConnect", "Error checking permissions: ${e.message}")
            false
        }
    }

    suspend fun getMissingReadPermissions(): Set<String> = withContext(Dispatchers.IO) {
        val client = healthConnectClient ?: return@withContext readPermissions
        try {
            val granted = client.permissionController.getGrantedPermissions()
            return@withContext readPermissions - granted
        } catch (e: Exception) {
            Log.w("HealthConnect", "Error getting missing permissions: ${e.message}")
            readPermissions
        }
    }

    suspend fun hasAllPermissions(): Boolean = withContext(Dispatchers.IO) {
        val client = healthConnectClient ?: return@withContext false
        try {
            val granted = client.permissionController.getGrantedPermissions()
            return@withContext granted.containsAll(allPermissions)
        } catch (e: Exception) {
            Log.w("HealthConnect", "Error checking all permissions: ${e.message}")
            false
        }
    }

    suspend fun getMissingAllPermissions(): Set<String> = withContext(Dispatchers.IO) {
        val client = healthConnectClient ?: return@withContext allPermissions
        try {
            val granted = client.permissionController.getGrantedPermissions()
            return@withContext allPermissions - granted
        } catch (e: Exception) {
            Log.w("HealthConnect", "Error getting missing all permissions: ${e.message}")
            allPermissions
        }
    }

    suspend fun readTodaySnapshot(): HealthConnectSnapshot = withContext(Dispatchers.IO) {
        val client = healthConnectClient
        if (client == null) {
            return@withContext HealthConnectSnapshot(
                isFromHealthConnect = false,
                statusMessage = "Health Connect клиент недоступен на этом устройстве."
            )
        }

        try {
            val now = Instant.now()
            val zoneId = ZoneId.systemDefault()
            val startOfDay = LocalDate.now(zoneId).atStartOfDay(zoneId).toInstant()

            // 1. Read Steps (Aggregate first for precision, fallback to sum of records)
            var totalSteps: Int? = null
            try {
                val aggregateResponse = client.aggregate(
                    AggregateRequest(
                        metrics = setOf(StepsRecord.COUNT_TOTAL),
                        timeRangeFilter = TimeRangeFilter.between(startOfDay, now)
                    )
                )
                totalSteps = aggregateResponse[StepsRecord.COUNT_TOTAL]?.toInt()
            } catch (e: Exception) {
                Log.d("HealthConnect", "Steps aggregate fallback to records: ${e.message}")
            }

            if (totalSteps == null) {
                val stepsRequest = ReadRecordsRequest(
                    recordType = StepsRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(startOfDay, now)
                )
                val stepsRecords = client.readRecords(stepsRequest).records
                if (stepsRecords.isNotEmpty()) {
                    totalSteps = stepsRecords.sumOf { it.count.toInt() }
                }
            }

            // 2. Read Heart Rate
            val hrRequest = ReadRecordsRequest(
                recordType = HeartRateRecord::class,
                timeRangeFilter = TimeRangeFilter.between(startOfDay, now)
            )
            val hrRecords = client.readRecords(hrRequest).records
            val allHrSamples = hrRecords.flatMap { it.samples }.map { it.beatsPerMinute.toInt() }
            val avgHr = if (allHrSamples.isNotEmpty()) allHrSamples.average().toInt() else null
            val minHr = if (allHrSamples.isNotEmpty()) allHrSamples.minOrNull() else null
            val maxHr = if (allHrSamples.isNotEmpty()) allHrSamples.maxOrNull() else null

            // 3. Read Sleep (last 24 hours)
            val sleepRequest = ReadRecordsRequest(
                recordType = SleepSessionRecord::class,
                timeRangeFilter = TimeRangeFilter.between(now.minus(24, ChronoUnit.HOURS), now)
            )
            val sleepRecords = client.readRecords(sleepRequest).records
            val totalSleepMin = if (sleepRecords.isNotEmpty()) {
                sleepRecords.sumOf { ChronoUnit.MINUTES.between(it.startTime, it.endTime).toInt() }
            } else null

            // 4. Read SpO2 (Oxygen Saturation)
            val spo2Request = ReadRecordsRequest(
                recordType = OxygenSaturationRecord::class,
                timeRangeFilter = TimeRangeFilter.between(now.minus(24, ChronoUnit.HOURS), now)
            )
            val spo2Records = client.readRecords(spo2Request).records
            val latestSpo2 = spo2Records.lastOrNull()?.percentage?.value?.toInt()

            // 5. Read Active Calories
            var activeCalories: Int? = null
            try {
                val calAggregate = client.aggregate(
                    AggregateRequest(
                        metrics = setOf(ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL),
                        timeRangeFilter = TimeRangeFilter.between(startOfDay, now)
                    )
                )
                activeCalories = calAggregate[ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL]?.inKilocalories?.toInt()
            } catch (e: Exception) {
                Log.d("HealthConnect", "Calories aggregate fallback: ${e.message}")
            }

            if (activeCalories == null) {
                val calRequest = ReadRecordsRequest(
                    recordType = ActiveCaloriesBurnedRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(startOfDay, now)
                )
                val calRecords = client.readRecords(calRequest).records
                if (calRecords.isNotEmpty()) {
                    activeCalories = calRecords.sumOf { it.energy.inKilocalories.toInt() }
                }
            }

            // 6. Read Workouts (Exercise Sessions)
            val workoutsList = mutableListOf<HealthConnectWorkout>()
            try {
                val exerciseRequest = ReadRecordsRequest(
                    recordType = ExerciseSessionRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(startOfDay, now)
                )
                val exerciseRecords = client.readRecords(exerciseRequest).records
                for (rec in exerciseRecords) {
                    val durationMin = ChronoUnit.MINUTES.between(rec.startTime, rec.endTime).toInt().coerceAtLeast(1)
                    val typeName = mapExerciseTypeToName(rec.exerciseType, rec.title)
                    workoutsList.add(
                        HealthConnectWorkout(
                            title = rec.title ?: typeName,
                            workoutType = typeName,
                            durationMinutes = durationMin,
                            caloriesBurned = durationMin * 6, // Estimate if individual session burn not separate
                            avgHeartRate = avgHr ?: 130,
                            distanceMeters = 0,
                            startTimeMillis = rec.startTime.toEpochMilli()
                        )
                    )
                }
            } catch (e: Exception) {
                Log.w("HealthConnect", "Error reading exercise sessions: ${e.message}")
            }

            HealthConnectSnapshot(
                steps = totalSteps,
                avgHeartRate = avgHr,
                minHeartRate = minHr,
                maxHeartRate = maxHr,
                sleepMinutes = totalSleepMin,
                spo2Percent = latestSpo2,
                activeCalories = activeCalories,
                workouts = workoutsList,
                isFromHealthConnect = true,
                statusMessage = "Данные успешно получены из Health Connect."
            )
        } catch (e: Exception) {
            Log.e("HealthConnect", "Failed to query Health Connect data: ${e.message}", e)
            HealthConnectSnapshot(
                isFromHealthConnect = false,
                statusMessage = "Ошибка чтения Health Connect: ${e.message}"
            )
        }
    }

    suspend fun readHistoricalDaysMetrics(daysCount: Int = 7): Map<String, Pair<Int, Int>> = withContext(Dispatchers.IO) {
        val client = healthConnectClient ?: return@withContext emptyMap()
        val resultMap = mutableMapOf<String, Pair<Int, Int>>()
        val zoneId = ZoneId.systemDefault()
        val today = LocalDate.now(zoneId)

        for (offset in (daysCount - 1) downTo 0) {
            val targetDate = today.minusDays(offset.toLong())
            val dateString = targetDate.toString()
            val startOfDay = targetDate.atStartOfDay(zoneId).toInstant()
            val endOfDay = targetDate.plusDays(1).atStartOfDay(zoneId).toInstant()
            val now = Instant.now()
            val endFilter = if (targetDate == today && now.isBefore(endOfDay)) now else endOfDay

            var daySteps = 0
            var dayCalories = 0

            // 1. Read Steps
            try {
                val stepAgg = client.aggregate(
                    AggregateRequest(
                        metrics = setOf(StepsRecord.COUNT_TOTAL),
                        timeRangeFilter = TimeRangeFilter.between(startOfDay, endFilter)
                    )
                )
                val aggVal = stepAgg[StepsRecord.COUNT_TOTAL]?.toInt()
                if (aggVal != null) {
                    daySteps = aggVal
                } else {
                    val stepsReq = ReadRecordsRequest(
                        recordType = StepsRecord::class,
                        timeRangeFilter = TimeRangeFilter.between(startOfDay, endFilter)
                    )
                    daySteps = client.readRecords(stepsReq).records.sumOf { it.count.toInt() }
                }
            } catch (e: Exception) {
                Log.d("HealthConnect", "Historical steps aggregation for $dateString: ${e.message}")
            }

            // 2. Read Active Calories
            try {
                val calAgg = client.aggregate(
                    AggregateRequest(
                        metrics = setOf(ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL),
                        timeRangeFilter = TimeRangeFilter.between(startOfDay, endFilter)
                    )
                )
                val aggCal = calAgg[ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL]?.inKilocalories?.toInt()
                if (aggCal != null) {
                    dayCalories = aggCal
                } else {
                    val calReq = ReadRecordsRequest(
                        recordType = ActiveCaloriesBurnedRecord::class,
                        timeRangeFilter = TimeRangeFilter.between(startOfDay, endFilter)
                    )
                    dayCalories = client.readRecords(calReq).records.sumOf { it.energy.inKilocalories.toInt() }
                }
            } catch (e: Exception) {
                Log.d("HealthConnect", "Historical calories aggregation for $dateString: ${e.message}")
            }

            resultMap[dateString] = Pair(daySteps, dayCalories)
        }

        resultMap
    }

    suspend fun writeDemoHealthData(
        steps: Int = 3500,
        bpm: Long = 74,
        workoutTitle: String = "Быстрая ходьба с браслетом",
        durationMin: Long = 25
    ): Boolean = withContext(Dispatchers.IO) {
        val client = healthConnectClient ?: return@withContext false
        try {
            val now = Instant.now()
            val zoneOffset = ZoneOffset.systemDefault().rules.getOffset(now)
            val sessionStart = now.minus(durationMin, ChronoUnit.MINUTES)

            val stepsRecord = StepsRecord(
                count = steps.toLong(),
                startTime = sessionStart,
                endTime = now,
                startZoneOffset = zoneOffset,
                endZoneOffset = zoneOffset,
                metadata = Metadata()
            )

            val hrSample = HeartRateRecord.Sample(
                time = now.minus(5, ChronoUnit.MINUTES),
                beatsPerMinute = bpm
            )
            val hrRecord = HeartRateRecord(
                startTime = sessionStart,
                endTime = now,
                startZoneOffset = zoneOffset,
                endZoneOffset = zoneOffset,
                samples = listOf(hrSample),
                metadata = Metadata()
            )

            val calRecord = ActiveCaloriesBurnedRecord(
                startTime = sessionStart,
                endTime = now,
                startZoneOffset = zoneOffset,
                endZoneOffset = zoneOffset,
                energy = Energy.kilocalories((durationMin * 7.5)),
                metadata = Metadata()
            )

            val exerciseRecord = ExerciseSessionRecord(
                startTime = sessionStart,
                endTime = now,
                startZoneOffset = zoneOffset,
                endZoneOffset = zoneOffset,
                exerciseType = ExerciseSessionRecord.EXERCISE_TYPE_WALKING,
                title = workoutTitle,
                metadata = Metadata()
            )

            client.insertRecords(listOf(stepsRecord))
            client.insertRecords(listOf(hrRecord))
            client.insertRecords(listOf(calRecord))
            client.insertRecords(listOf(exerciseRecord))
            Log.i("HealthConnect", "Inserted demo records into Health Connect successfully")
            true
        } catch (e: Exception) {
            Log.e("HealthConnect", "Failed to write demo data: ${e.message}", e)
            false
        }
    }

    private fun mapExerciseTypeToName(exerciseType: Int, title: String?): String {
        if (!title.isNullOrBlank()) return title
        return when (exerciseType) {
            ExerciseSessionRecord.EXERCISE_TYPE_RUNNING -> "Бег на улице"
            ExerciseSessionRecord.EXERCISE_TYPE_WALKING -> "Быстрая ходьба"
            ExerciseSessionRecord.EXERCISE_TYPE_BIKING -> "Велосипед"
            ExerciseSessionRecord.EXERCISE_TYPE_STRENGTH_TRAINING -> "Силовая тренировка"
            ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_POOL,
            ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_OPEN_WATER -> "Плавание"
            ExerciseSessionRecord.EXERCISE_TYPE_YOGA -> "Йога"
            ExerciseSessionRecord.EXERCISE_TYPE_PILATES -> "Пилатес"
            ExerciseSessionRecord.EXERCISE_TYPE_HIGH_INTENSITY_INTERVAL_TRAINING -> "HIIT интервалы"
            else -> "Тренировка"
        }
    }

    companion object {
        const val PROVIDER_PACKAGE_NAME = "com.google.android.apps.healthdata"

        fun openHealthConnectPlayStore(ctx: Context) {
            try {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse("market://details?id=$PROVIDER_PACKAGE_NAME&url=healthconnect%3A%2F%2Fonboarding")
                    setPackage("com.android.vending")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                ctx.startActivity(intent)
            } catch (e: Exception) {
                try {
                    val webIntent = Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://play.google.com/store/apps/details?id=$PROVIDER_PACKAGE_NAME")
                    ).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    ctx.startActivity(webIntent)
                } catch (ex: Exception) {
                    Log.e("HealthConnect", "Failed to open Play Store: ${ex.message}", ex)
                }
            }
        }
    }
}
