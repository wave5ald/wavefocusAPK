package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Mosque
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.firebase.GoogleAuthManager
import com.example.ui.components.WaveCard
import com.example.ui.components.WaveBrandmark
import com.example.ui.components.WaveListRow
import com.example.ui.components.WaveSectionLabel
import com.example.ui.theme.FlameOrange
import com.example.ui.theme.FocusIndigo
import com.example.ui.theme.SuccessGreen
import com.example.ui.viewmodel.RoutineViewModel
import com.example.util.RoutineDay
import com.example.ui.viewmodel.AppEdition
import com.example.util.BatteryOptimizationHelper

import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.ui.text.font.FontFamily
import com.example.data.sync.SyncState

enum class SettingsPage {
    MAIN,
    APPEARANCE,
    LANGUAGE,
    CLOUD_SYNC,
    FULL_BACKUP,
    TASKS_PACK
}

/**
 * Unified Settings & Backup Modal with clean local architecture,
 * theme options, battery optimization management, and JSON backup/restore.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupSettingsDialog(
    viewModel: RoutineViewModel,
    currentThemeMode: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val currentLanguage by viewModel.appLanguage.collectAsStateWithLifecycle()
    val currentEdition by viewModel.appEdition.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var currentPage by remember { mutableStateOf(SettingsPage.MAIN) }

    BackHandler(enabled = currentPage != SettingsPage.MAIN) {
        currentPage = SettingsPage.MAIN
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp,
        modifier = Modifier
            .fillMaxHeight(0.92f)
            .testTag("settings_unified_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {
            // Consistent Header on all pages
            SettingsPageHeader(
                currentPage = currentPage,
                currentLanguage = currentLanguage,
                onBack = { currentPage = SettingsPage.MAIN },
                onClose = onDismiss
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

            // Animated page switcher
            AnimatedContent(
                targetState = currentPage,
                transitionSpec = {
                    if (targetState == SettingsPage.MAIN) {
                        (slideInHorizontally { -it } + fadeIn()).togetherWith(slideOutHorizontally { it } + fadeOut())
                    } else {
                        (slideInHorizontally { it } + fadeIn()).togetherWith(slideOutHorizontally { -it } + fadeOut())
                    }
                },
                modifier = Modifier.weight(1f),
                label = "settings_page_transition"
            ) { page ->
                when (page) {
                    SettingsPage.MAIN -> {
                        SettingsMainPage(
                            viewModel = viewModel,
                            currentThemeMode = currentThemeMode,
                            currentLanguage = currentLanguage,
                            currentEdition = currentEdition,
                            onToggleEdition = { viewModel.toggleAppEdition() },
                            onNavigate = { currentPage = it }
                        )
                    }
                    SettingsPage.APPEARANCE -> {
                        AppearanceSettingsPage(
                            currentThemeMode = currentThemeMode,
                            onThemeSelected = { viewModel.setThemeMode(it) }
                        )
                    }
                    SettingsPage.LANGUAGE -> {
                        LanguageSettingsPage(
                            currentLanguage = currentLanguage,
                            onLanguageSelected = { viewModel.setAppLanguage(it) }
                        )
                    }
                    SettingsPage.CLOUD_SYNC -> {
                        CloudSyncSettingsPage(
                            viewModel = viewModel,
                            isArabic = currentLanguage == "ar"
                        )
                    }
                    SettingsPage.FULL_BACKUP -> {
                        FullBackupSettingsPage(
                            context = context,
                            onExport = { encrypt, callback ->
                                viewModel.exportBackup(encrypt, callback)
                            },
                            onRestore = { backupString, callback ->
                                viewModel.restoreBackup(backupString, callback)
                            }
                        )
                    }
                    SettingsPage.TASKS_PACK -> {
                        TasksPackSettingsPage(
                            context = context,
                            onExport = { callback ->
                                viewModel.exportTasksPack(callback)
                            },
                            onImport = { packString, merge, callback ->
                                viewModel.importTasksPack(packString, merge, callback)
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Top bar header with title, back button, and close icon.
 */
@Composable
private fun SettingsPageHeader(
    currentPage: SettingsPage,
    currentLanguage: String,
    onBack: () -> Unit,
    onClose: () -> Unit
) {
    val isArabic = currentLanguage == "ar"
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (currentPage != SettingsPage.MAIN) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("settings_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = if (isArabic) "رجوع" else "Back",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            } else {
                WaveBrandmark(size = 36.dp)
            }

            Column {
                Text(
                    text = when (currentPage) {
                        SettingsPage.MAIN -> if (isArabic) "الإعدادات والأدوات" else "Settings & Tools"
                        SettingsPage.APPEARANCE -> if (isArabic) "المظهر والشاشة" else "Theme & Appearance"
                        SettingsPage.LANGUAGE -> if (isArabic) "لغة التطبيق" else "App Language"
                        SettingsPage.CLOUD_SYNC -> if (isArabic) "المزامنة السحابية عبر Google" else "Google Cloud Sync"
                        SettingsPage.FULL_BACKUP -> if (isArabic) "نسخ احتياطي واستعادة كاملة" else "App Backup & Restore"
                        SettingsPage.TASKS_PACK -> if (isArabic) "حزمة مهام الروتين" else "Routine Tasks Pack"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = when (currentPage) {
                        SettingsPage.MAIN -> if (isArabic) "تخصيص مظهر التطبيق واللغة وإدارة النسخ الاحتياطية" else "Customize app appearance, language & manage backups"
                        SettingsPage.APPEARANCE -> if (isArabic) "الوضع الداكن والفاتح وافتراضي النظام" else "Dark, light, and system themes"
                        SettingsPage.LANGUAGE -> if (isArabic) "العربية والإنجليزية مع دعم كامل للاتجاه RTL" else "English & Arabic with full RTL support"
                        SettingsPage.CLOUD_SYNC -> if (isArabic) "حساب Google • نفس مشروع PWA • مزامنة تلقائية" else "Google account • Same Firebase project as the PWA • Auto sync"
                        SettingsPage.FULL_BACKUP -> if (isArabic) "تصدير محلي مشفر بـ AES-256 للبيانات" else "AES-256 encrypted local data export"
                        SettingsPage.TASKS_PACK -> if (isArabic) "تصدير أو استيراد روتيناتك اليومية" else "Export or import daily routines"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )
            }
        }

        IconButton(
            onClick = onClose,
            modifier = Modifier.testTag("close_settings_sheet")
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = if (isArabic) "إغلاق" else "Close",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Main Settings Hub Page.
 */
@Composable
private fun SettingsMainPage(
    viewModel: RoutineViewModel,
    currentThemeMode: String,
    currentLanguage: String,
    currentEdition: AppEdition,
    onToggleEdition: () -> Unit,
    onNavigate: (SettingsPage) -> Unit
) {
    val context = LocalContext.current
    val isArabic = currentLanguage == "ar"
    val syncAccount by viewModel.syncAccount.collectAsStateWithLifecycle()
    val syncState by viewModel.syncState.collectAsStateWithLifecycle()
    val cloudConfigured = remember { viewModel.isCloudSyncConfigured }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section: Google Cloud Sync
        SettingsSectionHeader(title = if (isArabic) "المزامنة السحابية" else "GOOGLE CLOUD SYNC")
        SettingsNavigationRow(
            icon = when {
                syncAccount != null -> Icons.Default.CloudDone
                cloudConfigured -> Icons.Default.CloudSync
                else -> Icons.Default.CloudOff
            },
            iconTint = when {
                syncAccount != null -> SuccessGreen
                cloudConfigured -> FocusIndigo
                else -> FlameOrange
            },
            title = if (isArabic) "مزامنة Google (هاتف وكمبيوتر)" else "Google Cloud Sync (PC & Mobile)",
            subtitle = when {
                syncAccount != null -> if (isArabic) "متصل: ${syncAccount?.email} • (${syncState.name})" else "Connected: ${syncAccount?.email} • (${syncState.name})"
                !cloudConfigured -> if (isArabic) "إعداد Firebase مطلوب • اضغط للتفاصيل" else "Firebase setup needed • Tap for details"
                else -> if (isArabic) "غير متصل • سجّل الدخول بحساب Google للمزامنة" else "Not connected • Sign in with Google to sync"
            },
            onClick = { onNavigate(SettingsPage.CLOUD_SYNC) },
            testTag = "settings_nav_cloud_sync"
        )

        DayCalendarSection(viewModel = viewModel, isArabic = isArabic)

        // Section: Appearance
        SettingsSectionHeader(title = if (isArabic) "المظهر والشاشة" else "APPEARANCE")
        SettingsNavigationRow(
            icon = when (currentThemeMode) {
                "light" -> Icons.Default.LightMode
                "dark" -> Icons.Default.DarkMode
                else -> Icons.Default.Settings
            },
            iconTint = FocusIndigo,
            title = if (isArabic) "المظهر والعرض" else "Theme & Display",
            subtitle = when (currentThemeMode) {
                "light" -> if (isArabic) "الوضع الفاتح" else "Light Mode"
                "dark" -> if (isArabic) "الوضع الداكن" else "Dark Mode (Night Safe)"
                else -> if (isArabic) "افتراضي النظام" else "System Default"
            },
            onClick = { onNavigate(SettingsPage.APPEARANCE) },
            testTag = "settings_nav_theme"
        )

        // Section: Language & Edition
        SettingsSectionHeader(title = if (isArabic) "اللغة والنسخة والتقويم" else "LANGUAGE & EDITION")
        SettingsNavigationRow(
            icon = Icons.Default.Language,
            iconTint = FocusIndigo,
            title = if (isArabic) "لغة التطبيق" else "App Language",
            subtitle = if (isArabic) "العربية (Arabic • واجهة RTL)" else "English (الإنجليزية • LTR)",
            onClick = { onNavigate(SettingsPage.LANGUAGE) },
            testTag = "settings_nav_language"
        )

        SettingsActionRow(
            icon = Icons.Default.Mosque,
            iconTint = if (currentEdition == AppEdition.ISLAMIC) Color(0xFF0F766E) else FocusIndigo,
            title = if (isArabic) "نسخة التطبيق والتقويم" else "App Edition & Calendar",
            subtitle = if (currentEdition == AppEdition.ISLAMIC) {
                if (isArabic) "النسخة الإسلامية مفعّلة • التقويم الهجري • بيانات مخصصة" else "Islamic Edition Active • Hijri Calendar • Dedicated Data"
            } else {
                if (isArabic) "النسخة العامة (التقويم الميلادي) • اضغط للتحويل" else "Standard Edition (Gregorian Calendar) • Tap to switch"
            },
            onClick = onToggleEdition,
            testTag = "settings_action_toggle_edition"
        )

        // Section: Focus & Background Battery
        SettingsSectionHeader(title = if (isArabic) "التركيز والعمل في الخلفية" else "FOCUS & BACKGROUND EXECUTION")
        var isBatteryIgnored by remember {
            mutableStateOf(BatteryOptimizationHelper.isIgnoringBatteryOptimizations(context))
        }
        SettingsActionRow(
            icon = if (isBatteryIgnored) Icons.Default.BatteryChargingFull else Icons.Default.BatteryAlert,
            iconTint = if (isBatteryIgnored) SuccessGreen else FlameOrange,
            title = "Unrestricted Battery Permission",
            subtitle = if (isBatteryIgnored) "Unrestricted • Focus timer runs uninterrupted with screen off" else "Optimization active • Tap to allow uninterrupted background focus",
            onClick = {
                BatteryOptimizationHelper.requestIgnoreBatteryOptimizations(context)
                isBatteryIgnored = BatteryOptimizationHelper.isIgnoringBatteryOptimizations(context)
            },
            testTag = "settings_action_battery_optimization"
        )

        // Section: Backup & Restore
        SettingsSectionHeader(title = "DATA & BACKUP")
        SettingsNavigationRow(
            icon = Icons.Default.Storage,
            iconTint = MaterialTheme.colorScheme.primary,
            title = "Full App Backup & Restore",
            subtitle = "Export or restore all tasks, focus logs, notes & state",
            onClick = { onNavigate(SettingsPage.FULL_BACKUP) },
            testTag = "settings_nav_full_backup"
        )

        // Privacy guarantee card
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = SuccessGreen.copy(alpha = 0.08f),
            border = BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.25f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(Icons.Default.Security, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(20.dp))
                Column {
                    Text("100% Offline & Private", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = SuccessGreen)
                    Text("All tasks, focus logs, notes, and routines are stored locally on your device.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

/**
 * Sub-Page 1: Appearance & Theme
 */
@Composable
private fun AppearanceSettingsPage(
    currentThemeMode: String,
    onThemeSelected: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Choose your preferred visual theme for routines and focus sessions.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        listOf(
            Triple("system", "System Default", Icons.Default.Settings),
            Triple("light", "Light Mode", Icons.Default.LightMode),
            Triple("dark", "Dark Mode (Night Safe)", Icons.Default.DarkMode)
        ).forEach { (mode, label, icon) ->
            val isSelected = currentThemeMode == mode
            Surface(
                onClick = { onThemeSelected(mode) },
                shape = RoundedCornerShape(20.dp),
                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f) else MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth().testTag("theme_option_$mode")
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(icon, contentDescription = null, tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                    }
                    RadioButton(
                        selected = isSelected,
                        onClick = { onThemeSelected(mode) }
                    )
                }
            }
        }
    }
}

/**
 * Sub-Page: App Language (English & Arabic)
 */
@Composable
private fun LanguageSettingsPage(
    currentLanguage: String,
    onLanguageSelected: (String) -> Unit
) {
    val isArabic = currentLanguage == "ar"
    val languages = listOf(
        Triple("en", "English", "English (United States) • Left-to-Right interface"),
        Triple("ar", "العربية", "اللغة العربية • واجهة كاملة من اليمين إلى اليسار (RTL)")
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = if (isArabic) "اختر لغة واجهة التطبيق لتغيير اللغة فوراً مع الدعم الكامل للاتجاه من اليمين لليسار." else "Choose application language with instant dynamic switching and full RTL support for Arabic.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        languages.forEach { (code, label, desc) ->
            val isSelected = currentLanguage == code
            Surface(
                onClick = { onLanguageSelected(code) },
                shape = RoundedCornerShape(20.dp),
                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f) else MaterialTheme.colorScheme.surface,
                border = BorderStroke(
                    1.dp,
                    if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("language_option_$code")
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Language,
                                    contentDescription = null,
                                    tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        Column {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = desc,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        }
                    }

                    RadioButton(
                        selected = isSelected,
                        onClick = { onLanguageSelected(code) }
                    )
                }
            }
        }
    }
}

/**
 * Sub-Page 2: Full App Backup & Restore
 */
@Composable
private fun FullBackupSettingsPage(
    context: Context,
    onExport: (Boolean, (String) -> Unit) -> Unit,
    onRestore: (String, (Boolean) -> Unit) -> Unit
) {
    var encryptBackup by remember { mutableStateOf(true) }
    var exportedString by remember { mutableStateOf("") }
    var restoreInput by remember { mutableStateOf("") }
    var activeTab by remember { mutableStateOf(0) }
    val clipboard = remember { context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Tab switcher
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilledTonalButton(
                onClick = { activeTab = 0 },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = if (activeTab == 0) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Export Backup", fontWeight = FontWeight.Bold)
            }

            FilledTonalButton(
                onClick = { activeTab = 1 },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = if (activeTab == 1) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Restore Backup", fontWeight = FontWeight.Bold)
            }
        }

        if (activeTab == 0) {
            // EXPORT SECTION
            Text(
                text = "Export your complete app state: tasks, categories, completions, focus logs, notes, and calendar data into a secure backup string.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            SettingsToggleCard(
                title = "AES-256 Encryption",
                description = "Secure your backup string with on-device encryption",
                checked = encryptBackup,
                onCheckedChange = { encryptBackup = it },
                testTag = "toggle_encrypt_backup"
            )

            Button(
                onClick = {
                    onExport(encryptBackup) { code ->
                        exportedString = code
                    }
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("generate_backup_button")
            ) {
                Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Generate Backup Code", fontWeight = FontWeight.Bold)
            }

            if (exportedString.isNotBlank()) {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    border = BorderStroke(1.dp, FocusIndigo.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Backup Code Generated", fontWeight = FontWeight.Bold, color = SuccessGreen, fontSize = 12.sp)
                            Text("${exportedString.length} chars", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        OutlinedTextField(
                            value = exportedString,
                            onValueChange = {},
                            readOnly = true,
                            maxLines = 4,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = FocusIndigo,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                            )
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    val clip = ClipData.newPlainText("Wave Focus Backup", exportedString)
                                    clipboard.setPrimaryClip(clip)
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Copy")
                            }

                            Button(
                                onClick = {
                                    val sendIntent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(Intent.EXTRA_TEXT, exportedString)
                                        type = "text/plain"
                                    }
                                    context.startActivity(Intent.createChooser(sendIntent, "Share Backup"))
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Share")
                            }
                        }
                    }
                }
            }
        } else {
            // RESTORE SECTION
            Text(
                text = "Paste a previously exported backup string to restore all tasks, categories, focus sessions, notes, and history.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = {
                        val clip = clipboard.primaryClip
                        if (clip != null && clip.itemCount > 0) {
                            val text = clip.getItemAt(0).text?.toString() ?: ""
                            if (text.isNotBlank()) restoreInput = text
                        }
                    }
                ) {
                    Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Paste from Clipboard")
                }
            }

            OutlinedTextField(
                value = restoreInput,
                onValueChange = { restoreInput = it },
                label = { Text("Backup Data String") },
                placeholder = { Text("Paste backup code here...") },
                maxLines = 6,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("restore_input_field"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                )
            )

            Button(
                onClick = {
                    if (restoreInput.isNotBlank()) {
                        onRestore(restoreInput) { _ -> }
                    }
                },
                enabled = restoreInput.isNotBlank(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("confirm_restore_backup_button")
            ) {
                Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Restore Data Now", fontWeight = FontWeight.Bold)
            }
        }
    }
}

/**
 * Sub-Page 3: Routine Tasks Pack Export / Import
 */
@Composable
private fun TasksPackSettingsPage(
    context: Context,
    onExport: ((String) -> Unit) -> Unit,
    onImport: (String, Boolean, (Boolean, Int) -> Unit) -> Unit
) {
    var activeTab by remember { mutableStateOf(0) }
    var exportedPack by remember { mutableStateOf("") }
    var importPackInput by remember { mutableStateOf("") }
    var mergeWithExisting by remember { mutableStateOf(true) }
    val clipboard = remember { context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilledTonalButton(
                onClick = { activeTab = 0 },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = if (activeTab == 0) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Export Pack", fontWeight = FontWeight.Bold)
            }

            FilledTonalButton(
                onClick = { activeTab = 1 },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = if (activeTab == 1) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Import Pack", fontWeight = FontWeight.Bold)
            }
        }

        if (activeTab == 0) {
            Text(
                text = "Export your task routines and categories to transfer to another device or save for later.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Button(
                onClick = {
                    onExport { pack -> exportedPack = pack }
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("generate_tasks_pack_button")
            ) {
                Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Generate Routine Pack", fontWeight = FontWeight.Bold)
            }

            if (exportedPack.isNotBlank()) {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    border = BorderStroke(1.dp, FocusIndigo.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Pack Ready", fontWeight = FontWeight.Bold, color = SuccessGreen, fontSize = 12.sp)
                            Text("${exportedPack.length} chars", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        OutlinedTextField(
                            value = exportedPack,
                            onValueChange = {},
                            readOnly = true,
                            maxLines = 4,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = FocusIndigo,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                            )
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    val clip = ClipData.newPlainText("Wave Focus Tasks Pack", exportedPack)
                                    clipboard.setPrimaryClip(clip)
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Copy Pack")
                            }

                            Button(
                                onClick = {
                                    val sendIntent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(Intent.EXTRA_TEXT, exportedPack)
                                        type = "text/plain"
                                    }
                                    context.startActivity(Intent.createChooser(sendIntent, "Share Tasks Pack"))
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Share")
                            }
                        }
                    }
                }
            }
        } else {
            Text(
                text = "Paste a routine tasks pack string to import habits and categories into your app.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = {
                        val clip = clipboard.primaryClip
                        if (clip != null && clip.itemCount > 0) {
                            val text = clip.getItemAt(0).text?.toString() ?: ""
                            if (text.isNotBlank()) importPackInput = text
                        }
                    }
                ) {
                    Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Paste Pack Code")
                }
            }

            OutlinedTextField(
                value = importPackInput,
                onValueChange = { importPackInput = it },
                label = { Text("Tasks Pack Code") },
                placeholder = { Text("Paste shared tasks pack code here...") },
                maxLines = 5,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("import_tasks_pack_input_field"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                )
            )

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { mergeWithExisting = true },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = mergeWithExisting, onClick = { mergeWithExisting = true })
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Merge with current tasks", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text("Keeps your current tasks and appends new ones", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { mergeWithExisting = false },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = !mergeWithExisting, onClick = { mergeWithExisting = false })
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Replace current tasks", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text("Replaces current daily routine with imported tasks", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            Button(
                onClick = {
                    if (importPackInput.isNotBlank()) {
                        onImport(importPackInput, mergeWithExisting) { _, _ -> }
                    }
                },
                enabled = importPackInput.isNotBlank(),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("confirm_import_tasks_pack_button")
            ) {
                Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Import Routine Tasks", fontWeight = FontWeight.Bold)
            }
        }
    }
}

/**
 * Helper row for navigating to sub-pages.
 */
@Composable
private fun SettingsNavigationRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: androidx.compose.ui.graphics.Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    testTag: String
) {
    WaveCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag),
        contentPadding = PaddingValues(0.dp)
    ) {
        WaveListRow(
            icon = icon,
            title = title,
            subtitle = subtitle,
            iconTint = iconTint,
            iconContainer = iconTint.copy(alpha = 0.14f),
            onClick = onClick,
            trailing = {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(18.dp)
                )
            }
        )
    }
}

/**
 * Helper row for direct action items.
 */
@Composable
private fun SettingsActionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: androidx.compose.ui.graphics.Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    testTag: String
) {
    WaveCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag),
        contentPadding = PaddingValues(0.dp)
    ) {
        WaveListRow(
            icon = icon,
            title = title,
            subtitle = subtitle,
            iconTint = iconTint,
            iconContainer = iconTint.copy(alpha = 0.14f),
            onClick = onClick
        )
    }
}

/**
 * Helper toggle card for switches.
 */
@Composable
private fun SettingsToggleCard(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    WaveCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.5.sp)
            }
            Spacer(modifier = Modifier.width(10.dp))
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                modifier = Modifier.testTag(testTag)
            )
        }
    }
}

/**
 * Section Header in Settings.
 */
@Composable
private fun SettingsSectionHeader(title: String) {
    WaveSectionLabel(text = title)
}

/**
 * Multi-User Cloud Sync & Account Page:
 * - Frictionless account connection (Sync Key or Username + PIN)
 * - Strict multi-user account isolation (Cloud sync vault per key)
 * - Local-first auto sync with smart merge conflict resolution
 * - Manual JSON backup export / import fallback
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CloudSyncSettingsPage(
    viewModel: RoutineViewModel,
    isArabic: Boolean
) {
    val context = LocalContext.current
    val syncKey by viewModel.syncKey.collectAsStateWithLifecycle()
    val syncUsername by viewModel.syncUsername.collectAsStateWithLifecycle()
    val syncState by viewModel.syncState.collectAsStateWithLifecycle()
    val syncMessage by viewModel.syncMessage.collectAsStateWithLifecycle()
    val lastSyncTimestamp by viewModel.lastSyncTimestamp.collectAsStateWithLifecycle()

    var showExportDialog by remember { mutableStateOf(false) }
    var exportedJsonText by remember { mutableStateOf("") }
    var showImportDialog by remember { mutableStateOf(false) }
    var importJsonText by remember { mutableStateOf("") }

    val activity = remember(context) { GoogleAuthManager.findActivity(context) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (!syncKey.isNullOrBlank()) {
            // === 1. CONNECTED — Google account linked to the PWA document ===
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = SuccessGreen.copy(alpha = 0.08f),
                border = BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(SuccessGreen.copy(alpha = 0.18f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudDone,
                                contentDescription = null,
                                tint = SuccessGreen,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isArabic) "متصل بحساب Google" else "Connected with Google",
                                style = MaterialTheme.typography.titleSmall,
                                color = SuccessGreen
                            )
                            Text(
                                text = syncUsername ?: (if (isArabic) "حساب متزامن" else "Synced account"),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                        Surface(
                            shape = CircleShape,
                            color = when (syncState) {
                                SyncState.SYNCING -> FlameOrange.copy(alpha = 0.18f)
                                SyncState.SYNCED -> SuccessGreen.copy(alpha = 0.18f)
                                SyncState.ERROR -> MaterialTheme.colorScheme.errorContainer
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            }
                        ) {
                            Text(
                                text = when (syncState) {
                                    SyncState.SYNCING -> if (isArabic) "يزامن…" else "Syncing…"
                                    SyncState.SYNCED -> if (isArabic) "متزامن ✓" else "Synced ✓"
                                    SyncState.ERROR -> if (isArabic) "خطأ" else "Error"
                                    SyncState.OFFLINE -> if (isArabic) "دون اتصال" else "Offline"
                                    SyncState.IDLE -> if (isArabic) "جاهز" else "Ready"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }

                    Text(
                        text = if (isArabic) {
                            "تستخدم نفس مشروع Firebase ومستند PWA: users/{uid}/apps/wavefocus"
                        } else {
                            "Same Firebase project and document as the PWA: users/{uid}/apps/wavefocus"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (syncMessage.isNotBlank()) {
                        Text(
                            text = syncMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (lastSyncTimestamp > 0L) {
                        val dateStr = java.text.SimpleDateFormat("MMM d, HH:mm", java.util.Locale.getDefault())
                            .format(java.util.Date(lastSyncTimestamp))
                        Text(
                            text = if (isArabic) "آخر مزامنة: $dateStr" else "Last synced: $dateStr",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { viewModel.triggerCloudSync() },
                            enabled = syncState != SyncState.SYNCING,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("sync_now_button")
                        ) {
                            if (syncState == SyncState.SYNCING) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(if (isArabic) "يزامن…" else "Syncing…")
                            } else {
                                Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (isArabic) "زامن الآن" else "Sync now")
                            }
                        }

                        OutlinedButton(
                            onClick = { viewModel.disconnectCloudSync() },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("disconnect_sync_button")
                        ) {
                            Icon(Icons.Default.CloudOff, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isArabic) "تسجيل الخروج" else "Sign out")
                        }
                    }
                }
            }
        } else {
            // === 2. SIGNED OUT — Google Sign-In ===
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudSync,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Text(
                        text = if (isArabic) "مزامنة سحابية بحساب Google" else "Cloud sync with Google",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (isArabic) {
                            "سجّل الدخول بنفس حساب Google المستخدم في تطبيق الويب. تبقى بياناتك محلية وتُرفع إلى نفس المستند في Firestore."
                        } else {
                            "Sign in with the same Google account you use on the web app. Your data stays local-first and mirrors to the same Firestore document."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Button(
                        onClick = { activity?.let { viewModel.signInToCloud(it) } },
                        enabled = activity != null && syncState != SyncState.SYNCING,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("google_sign_in_button")
                    ) {
                        if (syncState == SyncState.SYNCING) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (isArabic) "جارٍ تسجيل الدخول…" else "Signing in…")
                        } else {
                            Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (isArabic) "تسجيل الدخول بحساب Google" else "Continue with Google")
                        }
                    }

                    if (!viewModel.isCloudSyncConfigured) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = if (isArabic) {
                                    "إعداد مطلوب: أضف معرّف عميل الويب (wave_web_client_id) وتأكد من تسجيل بصمة SHA-1 لتطبيقك في مشروع Firebase."
                                } else {
                                    "Setup needed: add the Web client ID (wave_web_client_id) and register this app's SHA-1 in the Firebase project."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }

                    if (syncMessage.isNotBlank()) {
                        Text(
                            text = syncMessage,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        // === 3. MANUAL BACKUP FALLBACK (JSON EXPORT / IMPORT) ===
        SettingsSectionHeader(title = if (isArabic) "النسخ الاحتياطي اليدوي (JSON)" else "MANUAL BACKUP FALLBACK (JSON)")

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Storage,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = if (isArabic) "تصدير واستيراد يدوي (JSON)" else "Manual JSON Backup / Restore",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (isArabic)
                                "احفظ نسخة احتياطية من مهامك وروتينك وجلساتك كملف JSON أو استوردها يدوياً."
                            else
                                "Export all tasks, routines, quests & sessions as a JSON file or restore manually.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FilledTonalButton(
                        onClick = {
                            viewModel.exportCloudBackupJson { json ->
                                exportedJsonText = json
                                showExportDialog = true
                            }
                        },
                        modifier = Modifier.weight(1f).testTag("export_json_button")
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isArabic) "تصدير JSON" else "Export JSON")
                    }

                    OutlinedButton(
                        onClick = { showImportDialog = true },
                        modifier = Modifier.weight(1f).testTag("import_json_button")
                    ) {
                        Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isArabic) "استيراد JSON" else "Import JSON")
                    }
                }
            }
        }
    }

    // Export Dialog / Sheet
    if (showExportDialog) {
        ModalBottomSheet(
            onDismissRequest = { showExportDialog = false },
            modifier = Modifier.testTag("export_json_bottom_sheet")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = if (isArabic) "ملف النسخ الاحتياطي (JSON)" else "Exported JSON Backup",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (isArabic) "تم إنشاء ملف النسخ بنجاح. يمكنك نسخه أو مشاركته:" else "Backup generated. You can copy or share this JSON file:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth().height(160.dp)
                ) {
                    Text(
                        text = exportedJsonText,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(10.dp).verticalScroll(rememberScrollState())
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("WaveFocus Backup", exportedJsonText))
                        },
                        modifier = Modifier.weight(1f).testTag("copy_json_button")
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isArabic) "نسخ إلى الحافظة" else "Copy JSON")
                    }

                    FilledTonalButton(
                        onClick = {
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, exportedJsonText)
                                type = "application/json"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Share JSON Backup"))
                        },
                        modifier = Modifier.weight(1f).testTag("share_json_button")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isArabic) "مشاركة الملف" else "Share File")
                    }
                }
            }
        }
    }

    // Import Dialog / Sheet
    if (showImportDialog) {
        ModalBottomSheet(
            onDismissRequest = { showImportDialog = false },
            modifier = Modifier.testTag("import_json_bottom_sheet")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = if (isArabic) "استيراد نسخة احتياطية (JSON)" else "Import JSON Backup",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (isArabic) "الصق محتوى ملف JSON هنا لاستعادة المهام والجلسات والروتين:" else "Paste your JSON backup data below to restore your routines and sessions:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = importJsonText,
                    onValueChange = { importJsonText = it },
                    placeholder = { Text("{\n  \"app\": \"WaveFocus\", ...\n}") },
                    modifier = Modifier.fillMaxWidth().height(160.dp).testTag("import_json_input"),
                    textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FilledTonalButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clipText = clipboard.primaryClip?.getItemAt(0)?.text?.toString() ?: ""
                            if (clipText.isNotBlank()) importJsonText = clipText
                        },
                        modifier = Modifier.weight(1f).testTag("paste_from_clipboard_button")
                    ) {
                        Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isArabic) "لصق من الحافظة" else "Paste")
                    }

                    Button(
                        onClick = {
                            if (importJsonText.isNotBlank()) {
                                viewModel.importCloudBackupJson(importJsonText) { success, count ->
                                    if (success) {
                                        showImportDialog = false
                                        importJsonText = ""
                                    }
                                }
                            }
                        },
                        enabled = importJsonText.isNotBlank(),
                        modifier = Modifier.weight(1f).testTag("confirm_import_button")
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isArabic) "استيراد ودمج" else "Import & Merge")
                    }
                }
            }
        }
    }
}

/**
 * Day & Calendar settings: daily task reset time + Saudi Hijri verification.
 */
@Composable
private fun DayCalendarSection(
    viewModel: RoutineViewModel,
    isArabic: Boolean
) {
    val resetMinutes by viewModel.dayResetMinutes.collectAsStateWithLifecycle()
    val hijriOffset by viewModel.hijriOffsetDays.collectAsStateWithLifecycle()
    val verifiedAt by viewModel.hijriVerifiedAt.collectAsStateWithLifecycle()
    val hijriState by viewModel.hijriSyncState.collectAsStateWithLifecycle()

    val verifiedLabel = if (verifiedAt > 0L) {
        val fmt = java.text.SimpleDateFormat("dd MMM, HH:mm", java.util.Locale.getDefault())
        (if (isArabic) "آخر تحقق: " else "Verified: ") + fmt.format(java.util.Date(verifiedAt))
    } else {
        if (isArabic) "لم يتم التحقق بعد" else "Not verified yet"
    }
    val statusLabel = when (hijriState) {
        "syncing" -> if (isArabic) "جارٍ التحقق..." else "Checking..."
        "error" -> if (isArabic) "تعذر الاتصال — يُستخدم آخر تحقق" else "Offline — using last verification"
        else -> verifiedLabel
    }

    SettingsSectionHeader(title = if (isArabic) "المهام والتقويم" else "DAY & CALENDAR")
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = if (isArabic) "وقت تجديد المهام" else "Tasks reset at",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = RoutineDay.format12(resetMinutes),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                TextButton(onClick = { viewModel.setDayResetMinutes(resetMinutes - 60) }) { Text("-1h") }
                TextButton(onClick = { viewModel.setDayResetMinutes(resetMinutes - 30) }) { Text("-30m") }
                TextButton(onClick = { viewModel.setDayResetMinutes(resetMinutes + 30) }) { Text("+30m") }
                TextButton(onClick = { viewModel.setDayResetMinutes(resetMinutes + 60) }) { Text("+1h") }
            }
            Text(
                text = if (isArabic) {
                    "يبدأ يوم المهام الجديد في هذا الوقت، وتتبعه المهام والسلاسل وجلسات التركيز. يتجدد تلقائيًا كل يوم."
                } else {
                    "A new routine day starts at this time. Tasks, streaks and focus sessions follow it and reset automatically every day."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = if (isArabic) "التقويم الهجري (أم القرى)" else "Hijri calendar (Umm al-Qura)",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = statusLabel + "  ·  " + (if (isArabic) "الفرق " else "offset ") + hijriOffset + "d",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = if (isArabic) {
                    "يتحقق التطبيق تلقائيًا من تقويم أم القرى السعودي عند الاتصال بالإنترنت لتطابق أطوال الأشهر."
                } else {
                    "The app verifies the Saudi Umm al-Qura calendar automatically when online so month lengths stay correct."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(
                onClick = { viewModel.verifyHijriCalendar(force = true) },
                enabled = hijriState != "syncing"
            ) {
                Text(if (isArabic) "تحقق الآن" else "Verify now")
            }
        }
    }
}
