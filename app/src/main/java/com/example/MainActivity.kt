package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import com.example.worker.PulseWorkScheduler
import androidx.core.content.ContextCompat
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.TrackChanges
import androidx.compose.material.icons.outlined.Watch
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.health.connect.client.PermissionController
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.PulseTrackViewModel
import com.example.ui.components.OnboardingProfileDialog
import com.example.ui.components.PulseTopHeader
import com.example.ui.screens.HabitsScreen
import com.example.ui.screens.NutritionScreen
import com.example.ui.screens.StatsScreen
import com.example.ui.screens.TasksScreen
import com.example.ui.screens.WorkoutsScreen
import com.example.ui.theme.PulseTrackTheme

class MainActivity : ComponentActivity() {

    private val viewModel: PulseTrackViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            PulseWorkScheduler.schedulePeriodicTasks(applicationContext)
        } catch (e: Exception) {
            android.util.Log.w("MainActivity", "WorkManager schedule skipped: ${e.message}")
        }
        enableEdgeToEdge()
        setContent {
            PulseTrackTheme {
                PulseTrackApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun PulseTrackApp(viewModel: PulseTrackViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    BackHandler(enabled = uiState.currentTab != 0) {
        viewModel.setTab(0)
    }

    val healthConnectLauncher = rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract()
    ) { grantedPermissions ->
        viewModel.onHealthConnectPermissionsResult(grantedPermissions)
    }

    val onSyncHealthConnect = {
        viewModel.syncHealthConnect { permissions ->
            healthConnectLauncher.launch(permissions)
        }
    }

    val onWriteDemoHealthConnect = {
        viewModel.writeDemoHealthConnectDataAndSync { permissions ->
            healthConnectLauncher.launch(permissions)
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        modifier = Modifier.fillMaxSize(),
        topBar = {
            PulseTopHeader(
                uiState = uiState,
                onSyncBand = onSyncHealthConnect,
                onOpenProfile = { viewModel.openOnboarding() },
                onDismissNotification = { viewModel.dismissNotification() }
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier.testTag("pulse_bottom_navigation")
            ) {
                NavigationBarItem(
                    selected = uiState.currentTab == 0,
                    onClick = { viewModel.setTab(0) },
                    icon = {
                        Icon(
                            imageVector = if (uiState.currentTab == 0) Icons.Default.TrackChanges else Icons.Outlined.TrackChanges,
                            contentDescription = "Привычки"
                        )
                    },
                    label = {
                        Text(
                            text = "Привычки",
                            fontSize = 11.sp,
                            maxLines = 1,
                            softWrap = false,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    },
                    modifier = Modifier.testTag("nav_tab_habits")
                )

                NavigationBarItem(
                    selected = uiState.currentTab == 1,
                    onClick = { viewModel.setTab(1) },
                    icon = {
                        Icon(
                            imageVector = if (uiState.currentTab == 1) Icons.Default.Watch else Icons.Outlined.Watch,
                            contentDescription = "Браслет"
                        )
                    },
                    label = {
                        Text(
                            text = "Браслет",
                            fontSize = 11.sp,
                            maxLines = 1,
                            softWrap = false,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    },
                    modifier = Modifier.testTag("nav_tab_workouts")
                )

                NavigationBarItem(
                    selected = uiState.currentTab == 2,
                    onClick = { viewModel.setTab(2) },
                    icon = {
                        Icon(
                            imageVector = if (uiState.currentTab == 2) Icons.Default.Restaurant else Icons.Outlined.Restaurant,
                            contentDescription = "Питание"
                        )
                    },
                    label = {
                        Text(
                            text = "Питание",
                            fontSize = 11.sp,
                            maxLines = 1,
                            softWrap = false,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    },
                    modifier = Modifier.testTag("nav_tab_nutrition")
                )

                NavigationBarItem(
                    selected = uiState.currentTab == 3,
                    onClick = { viewModel.setTab(3) },
                    icon = {
                        Icon(
                            imageVector = if (uiState.currentTab == 3) Icons.Default.Checklist else Icons.Outlined.Checklist,
                            contentDescription = "Задачи"
                        )
                    },
                    label = {
                        Text(
                            text = "Задачи",
                            fontSize = 11.sp,
                            maxLines = 1,
                            softWrap = false,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    },
                    modifier = Modifier.testTag("nav_tab_tasks")
                )

                NavigationBarItem(
                    selected = uiState.currentTab == 4,
                    onClick = { viewModel.setTab(4) },
                    icon = {
                        Icon(
                            imageVector = if (uiState.currentTab == 4) Icons.Default.Insights else Icons.Outlined.Insights,
                            contentDescription = "AI Коуч"
                        )
                    },
                    label = {
                        Text(
                            text = "AI Коуч",
                            fontSize = 11.sp,
                            maxLines = 1,
                            softWrap = false,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    },
                    modifier = Modifier.testTag("nav_tab_stats")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (uiState.currentTab) {
                0 -> {
                    val habitsState by viewModel.habitsTabState.collectAsStateWithLifecycle()
                    HabitsScreen(
                        state = habitsState,
                        onToggleHabit = { id, done -> viewModel.toggleHabit(id, done) },
                        onAddHabit = { title, desc, cat, color, sync, threshold ->
                            viewModel.addHabit(title, desc, cat, color, sync, threshold)
                        },
                        onDeleteHabit = { id -> viewModel.deleteHabit(id) },
                        onGenerateAiPlan = { goal, onComplete ->
                            viewModel.generateHabitPlan(goal, onComplete)
                        },
                        onAcceptAiPlan = { habits -> viewModel.acceptHabitPlan(habits) },
                        onPreviousDay = { viewModel.selectPreviousDay() },
                        onNextDay = { viewModel.selectNextDay() },
                        onResetToday = { viewModel.resetToToday() }
                    )
                }

                1 -> {
                    val workoutsState by viewModel.workoutsTabState.collectAsStateWithLifecycle()
                    WorkoutsScreen(
                        state = workoutsState,
                        onSyncBand = onSyncHealthConnect,
                        onAddWorkout = { type, dur, cal, hr, dist ->
                            viewModel.addManualWorkout(type, dur, cal, hr, dist)
                        },
                        onDeleteWorkout = { id -> viewModel.deleteWorkout(id) },
                        onSetKinesitherapySubTab = { subTab -> viewModel.setKinesitherapySubTab(subTab) },
                        onExtractKinesitherapyFromPhoto = { base64, onResult ->
                            viewModel.extractKinesitherapyExercisesFromPhoto(base64, onResult)
                        },
                        onSaveKinesitherapySession = { title, goal, availablePhoto, performedPhoto, exercises, painBefore, painAfter, duration, notes ->
                            viewModel.analyzeAndSaveKinesitherapySession(
                                date = workoutsState.todayDate,
                                title = title,
                                primaryGoal = goal,
                                availableExercisesPhotoBase64 = availablePhoto,
                                performedExercisesPhotoBase64 = performedPhoto,
                                exercises = exercises,
                                painBefore = painBefore,
                                painAfter = painAfter,
                                durationMinutes = duration,
                                userNotes = notes,
                                onComplete = {}
                            )
                        },
                        onDeleteKinesitherapySession = { id -> viewModel.deleteKinesitherapySession(id) },
                        onWriteDemoHealthConnect = onWriteDemoHealthConnect,
                        onPurgeMockData = { viewModel.purgeMockData() }
                    )
                }

                2 -> {
                    val nutritionState by viewModel.nutritionTabState.collectAsStateWithLifecycle()
                    NutritionScreen(
                        state = nutritionState,
                        onAddWater = { amount -> viewModel.addWater(amount) },
                        onEstimateFood = { desc, onEst ->
                            viewModel.estimateAndAddFood(desc, onEst)
                        },
                        onEstimateFoodFromPhoto = { base64, mime, note, onEst ->
                            viewModel.estimateAndAddFoodFromPhoto(base64, mime, note, onEst)
                        },
                        onSaveFood = { mealType, desc, cals, p, f, c ->
                            viewModel.saveFoodEntry(mealType, desc, cals, p, f, c)
                        },
                        onDeleteFood = { id -> viewModel.deleteFoodEntry(id) },
                        onPreviousDay = { viewModel.selectPreviousDay() },
                        onNextDay = { viewModel.selectNextDay() },
                        onResetToday = { viewModel.resetToToday() },
                        onSaveApiKey = { key -> viewModel.setCustomApiKey(key) },
                        currentApiKey = viewModel.getEffectiveApiKey()
                    )
                }

                3 -> {
                    val tasksState by viewModel.tasksTabState.collectAsStateWithLifecycle()
                    TasksScreen(
                        state = tasksState,
                        onToggleTask = { task -> viewModel.toggleTask(task) },
                        onAddTask = { title, priority, cat, due ->
                            viewModel.addTask(title, priority, cat, due)
                        },
                        onDeleteTask = { id -> viewModel.deleteTask(id) },
                        onDecomposeTask = { task -> viewModel.decomposeTask(task) },
                        onAddNote = { title, content, tags, pin ->
                            viewModel.addNote(title, content, tags, pin)
                        },
                        onDeleteNote = { id -> viewModel.deleteNote(id) }
                    )
                }

                4 -> {
                    val statsState by viewModel.statsTabState.collectAsStateWithLifecycle()
                    StatsScreen(
                        state = statsState,
                        onRunAiAudit = { viewModel.runAiDailyAudit() },
                        onOpenProfile = { viewModel.openOnboarding() },
                        onRefreshAiAdvice = { viewModel.refreshAiAdvice() }
                    )
                }
            }
        }

        // First-run Onboarding & Profile Setup Dialog
        if (uiState.showOnboarding) {
            OnboardingProfileDialog(
                initialProfile = uiState.userProfile,
                canDismiss = uiState.userProfile.isOnboarded,
                onDismiss = { viewModel.closeOnboarding() },
                onSaveProfile = { age, weight, height, gender, activityLevel, goal ->
                    viewModel.saveUserProfile(age, weight, height, gender, activityLevel, goal)
                }
            )
        }
    }
}
