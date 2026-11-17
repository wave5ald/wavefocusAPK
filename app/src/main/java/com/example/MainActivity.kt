package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.MorePagesSheet
import com.example.ui.components.WaveBackground
import com.example.ui.components.AppUpdateBanner
import com.example.ui.components.WaveBottomBar
import com.example.ui.components.WaveTopBar
import com.example.ui.screens.AnalyticsScreen
import com.example.ui.screens.BackupSettingsDialog
import com.example.ui.screens.CustomPageScreen
import com.example.ui.screens.FocusTimerScreen
import com.example.ui.screens.NotesAndBoardsScreen
import com.example.ui.screens.RoutineScreen
import com.example.ui.screens.ScheduleScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.AppEdition
import com.example.ui.viewmodel.NavTab
import com.example.ui.viewmodel.RoutineViewModel
import com.example.util.LocalAppLanguage
import com.example.util.appStrings

class MainActivity : ComponentActivity() {
    private var routineViewModel: RoutineViewModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: RoutineViewModel = viewModel()
            routineViewModel = viewModel

            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
            val appLanguage by viewModel.appLanguage.collectAsStateWithLifecycle()
            val layoutDirection = if (appLanguage == "ar") LayoutDirection.Rtl else LayoutDirection.Ltr

            CompositionLocalProvider(
                LocalLayoutDirection provides layoutDirection,
                LocalAppLanguage provides appLanguage
            ) {
                MyApplicationTheme(themeMode = themeMode) {
                    DailyRoutineApp(viewModel = viewModel, themeMode = themeMode)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        routineViewModel?.syncTimerWithBackgroundService()
    }
}

@Composable
fun DailyRoutineApp(
    viewModel: RoutineViewModel,
    themeMode: String
) {
    val strings = appStrings()
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
    val tasks by viewModel.tasksForSelectedDate.collectAsStateWithLifecycle()
    val activeTasks by viewModel.allActiveTasks.collectAsStateWithLifecycle()

    val timerMode by viewModel.timerMode.collectAsStateWithLifecycle()
    val totalSeconds by viewModel.timerTotalSeconds.collectAsStateWithLifecycle()
    val remainingSeconds by viewModel.timerRemainingSeconds.collectAsStateWithLifecycle()
    val isTimerRunning by viewModel.isTimerRunning.collectAsStateWithLifecycle()
    val linkedTask by viewModel.linkedRoutineTask.collectAsStateWithLifecycle()
    val todayFocusSessions by viewModel.todayFocusSessions.collectAsStateWithLifecycle()

    val calendarMonth by viewModel.calendarMonth.collectAsStateWithLifecycle()
    val scriptsForDate by viewModel.scriptsForSelectedDate.collectAsStateWithLifecycle()
    val allScripts by viewModel.allScripts.collectAsStateWithLifecycle()

    val analyticsData by viewModel.analyticsData.collectAsStateWithLifecycle()
    val userMessage by viewModel.userMessage.collectAsStateWithLifecycle()
    val activeCustomPage by viewModel.activeCustomPage.collectAsStateWithLifecycle()
    val appEdition by viewModel.appEdition.collectAsStateWithLifecycle()
    val currentLang by viewModel.appLanguage.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showMorePagesSheet by remember { mutableStateOf(false) }

    LaunchedEffect(userMessage) {
        userMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearUserMessage()
        }
    }

    val title = when (currentTab) {
        NavTab.ROUTINE -> strings.routine
        NavTab.FOCUS -> strings.focus
        NavTab.SCRIPTS -> strings.schedule
        NavTab.NOTES -> strings.notes
        NavTab.ANALYTICS -> strings.progress
        NavTab.CUSTOM_PAGE -> activeCustomPage?.title?.take(16) ?: strings.workspace
    }

    val isArabic = currentLang == "ar"
    val editionLabel = if (appEdition == AppEdition.ISLAMIC) {
        if (isArabic) "ميلادي" else "Gregorian"
    } else {
        if (isArabic) "هجري" else "Hijri"
    }
    val languageLabel = if (isArabic) "English" else "العربية"

    WaveBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
        ) {
            WaveTopBar(
                title = title,
                editionLabel = editionLabel,
                editionActive = appEdition == AppEdition.ISLAMIC,
                languageLabel = languageLabel,
                onToggleEdition = { viewModel.toggleAppEdition() },
                onToggleLanguage = { viewModel.toggleLanguage() },
                onMenu = { showMorePagesSheet = true }
            )

            AppUpdateBanner(viewModel = viewModel)

            Box(modifier = Modifier.weight(1f)) {
                Crossfade(targetState = currentTab, label = "tabCrossfade") { tab ->
                    when (tab) {
                        NavTab.ROUTINE -> RoutineScreen(
                            viewModel = viewModel,
                            tasks = tasks,
                            selectedDate = selectedDate
                        )
                        NavTab.FOCUS -> FocusTimerScreen(
                            viewModel = viewModel,
                            timerMode = timerMode,
                            totalSeconds = totalSeconds,
                            remainingSeconds = remainingSeconds,
                            isRunning = isTimerRunning,
                            linkedTask = linkedTask,
                            activeTasks = activeTasks,
                            todaySessions = todayFocusSessions
                        )
                        NavTab.SCRIPTS -> ScheduleScreen(
                            viewModel = viewModel,
                            selectedDate = selectedDate,
                            calendarMonth = calendarMonth,
                            scriptsForDate = scriptsForDate,
                            allScripts = allScripts,
                            tasks = tasks,
                            allActiveTasks = activeTasks
                        )
                        NavTab.NOTES -> NotesAndBoardsScreen(
                            viewModel = viewModel
                        )
                        NavTab.ANALYTICS -> AnalyticsScreen(
                            viewModel = viewModel,
                            analyticsData = analyticsData
                        )
                        NavTab.CUSTOM_PAGE -> CustomPageScreen(
                            viewModel = viewModel
                        )
                    }
                }

                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            WaveBottomBar(
                currentTab = currentTab,
                labels = mapOf(
                    NavTab.ROUTINE to strings.routine,
                    NavTab.FOCUS to strings.focus,
                    NavTab.SCRIPTS to strings.schedule,
                    NavTab.NOTES to strings.notes
                ),
                icons = mapOf(
                    NavTab.ROUTINE to (if (currentTab == NavTab.ROUTINE) Icons.Filled.CheckCircle else Icons.Outlined.CheckCircle),
                    NavTab.FOCUS to (if (currentTab == NavTab.FOCUS) Icons.Filled.Timer else Icons.Outlined.Timer),
                    NavTab.SCRIPTS to (if (currentTab == NavTab.SCRIPTS) Icons.Filled.CalendarMonth else Icons.Outlined.CalendarMonth),
                    NavTab.NOTES to (if (currentTab == NavTab.NOTES) Icons.Filled.Description else Icons.Outlined.Description)
                ),
                onSelect = { viewModel.setTab(it) }
            )
        }
    }

    if (showMorePagesSheet) {
        MorePagesSheet(
            viewModel = viewModel,
            currentTab = currentTab,
            onOpenSettings = { showSettingsDialog = true },
            onDismiss = { showMorePagesSheet = false }
        )
    }

    if (showSettingsDialog) {
        BackupSettingsDialog(
            viewModel = viewModel,
            currentThemeMode = themeMode,
            onDismiss = { showSettingsDialog = false }
        )
    }
}
