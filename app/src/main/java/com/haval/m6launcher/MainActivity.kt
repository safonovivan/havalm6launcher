package com.haval.m6launcher

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.haval.m6launcher.data.MusicPlayerState
import com.haval.m6launcher.data.SettingsRepository
import com.haval.m6launcher.data.UserSettings
import com.haval.m6launcher.ui.components.LeftNavigationPanel
import com.haval.m6launcher.ui.screens.*
import com.haval.m6launcher.ui.theme.HavalM6LauncherTheme
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.flow.map

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppLogger.log(applicationContext, "--- ЗАПУСК onCreate ---")
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val settingsRepo = SettingsRepository(applicationContext)

        setContent {
            // ЧИТАЕМ ТОЛЬКО ТЕМУ И ФУЛЛСКРИН В MAIN ACTIVITY (Чтобы не перерисовывать все при сохранении трека)
            val isDarkTheme by settingsRepo.userSettings.map { it.isDarkTheme }.collectAsState(initial = true)
            val isFullScreen by settingsRepo.userSettings.map { it.isFullScreen }.collectAsState(initial = true)
            val scope = rememberCoroutineScope()
            val context = LocalContext.current

            LaunchedEffect(Unit) {
                MusicPlayerState.restoreState(context, settingsRepo)
            }

            val restartApp = {
                val mStartActivity = Intent(context, MainActivity::class.java)
                mStartActivity.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                val mPendingIntentId = 123456
                val mPendingIntent = PendingIntent.getActivity(context, mPendingIntentId, mStartActivity, PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                val mgr = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 100, mPendingIntent)
                System.exit(0)
            }

            LaunchedEffect(isFullScreen) {
                val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
                if (isFullScreen) windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
                else windowInsetsController.show(WindowInsetsCompat.Type.systemBars())
                windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }

            HavalM6LauncherTheme(darkTheme = isDarkTheme) {
                MainLauncherContent(
                    settingsRepo = settingsRepo,
                    isFullScreen = isFullScreen,
                    onSettingsChange = { newSettings -> scope.launch { settingsRepo.updateSettings(newSettings) } },
                    executeAction = { type, value ->
                        when (type) {
                            "app" -> {
                                val intent = context.packageManager.getLaunchIntentForPackage(value)
                                if (intent != null) { intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); context.startActivity(intent) }
                                else Toast.makeText(context, "Приложение не найдено", Toast.LENGTH_SHORT).show()
                            }
                            "action" -> {
                                try {
                                    when (value) {
                                        "recents" -> {
                                            if (BackAccessibilityService.instance != null) {
                                                BackAccessibilityService.instance?.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_RECENTS)
                                            }
                                        }
                                        "android_settings" -> { val intent = Intent(android.provider.Settings.ACTION_SETTINGS); intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); context.startActivity(intent) }
                                        "browser" -> { val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://yandex.ru")); intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); context.startActivity(intent) }
                                        "dialer" -> { val intent = Intent(Intent.ACTION_DIAL); intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); context.startActivity(intent) }
                                    }
                                } catch (e: Exception) { }
                            }
                        }
                    },
                    onRestartApp = restartApp
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    override fun onStart() {
        super.onStart()
        MainActivity.isLauncherVisible = true
        sendBroadcast(Intent("ACTION_HIDE_BUTTON").setPackage(packageName))
    }

    override fun onStop() {
        super.onStop()
        MusicPlayerState.savePlaybackStateOnStop(this)
        MainActivity.isLauncherVisible = false
        sendBroadcast(Intent("ACTION_SHOW_BUTTON").setPackage(packageName))
    }

    companion object {
        var isLauncherVisible = false
    }
}

@Composable
fun MainLauncherContent(
    settingsRepo: SettingsRepository,
    isFullScreen: Boolean,
    onSettingsChange: (UserSettings) -> Unit,
    executeAction: (String, String) -> Unit,
    onRestartApp: () -> Unit
) {
    // ЧИТАЕМ ОСТАЛЬНЫЕ НАСТРОЙКИ ЗДЕСЬ (Перерисовывается только контент)
    val settings by settingsRepo.userSettings.collectAsState(initial = UserSettings())
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val context = LocalContext.current

    val folderPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            try { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (e: Exception) { }
            MusicPlayerState.loadTracksFromFolder(context, uri)
        }
    }

    val handlePickFolder = { folderPickerLauncher.launch(null) }

    val startVoiceInput = {
        var launched = false
        val recognizerIntent = Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL, android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE, "ru-RU")
            putExtra(android.speech.RecognizerIntent.EXTRA_PROMPT, "Слушаю...")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        if (recognizerIntent.resolveActivity(context.packageManager) != null) {
            try { context.startActivity(recognizerIntent); launched = true } catch (e: Exception) {}
        }
        if (!launched) {
            val voiceCommandIntent = Intent(Intent.ACTION_VOICE_COMMAND).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
            if (voiceCommandIntent.resolveActivity(context.packageManager) != null) {
                try { context.startActivity(voiceCommandIntent); launched = true } catch (e: Exception) {}
            }
        }
    }

    val paddingModifier = if (isFullScreen) Modifier else Modifier.systemBarsPadding()

    Row(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).then(paddingModifier)) {
        LeftNavigationPanel(
            modifier = Modifier.width(100.dp).fillMaxHeight(),
            currentRoute = currentRoute,
            isVoiceAssistantEnabled = settings.isVoiceAssistantEnabled,
            onItemClick = { route -> navController.navigate(route) { popUpTo(navController.graph.startDestinationId) { saveState = true }; launchSingleTop = true; restoreState = true } },
            onHomeDoubleTap = { executeAction(settings.homeDoubleTapType, settings.homeDoubleTapValue) },
            onHomeLongPress = { executeAction(settings.homeLongPressType, settings.homeLongPressValue) },
            onVoiceClick = startVoiceInput
        )

        NavHost(navController = navController, startDestination = "home", modifier = Modifier.fillMaxSize().padding(settings.contentPadding.dp)) {
            composable("home") { HomeScreen(settings = settings, onSettingsChange = onSettingsChange, onPickFolder = handlePickFolder, executeAction = executeAction) }
            composable("nav") {
                NavigatorScreen(
                    settingsRepo = settingsRepo,
                    mapTheme = settings.mapTheme,
                    onNavigateToSettings = { navController.navigate("settings") },
                    settings = settings,
                    onSettingsChange = onSettingsChange,
                    onPickFolder = handlePickFolder
                )
            }
            composable("music") { MusicScreen(settings = settings, onSettingsChange = onSettingsChange, onPickFolder = handlePickFolder) }
            composable("obd") { ObdScreen() }
            composable("apps") { AppsScreen() }
            composable("settings") { SettingsScreen(settings = settings, onSettingsChange = onSettingsChange, onRestartApp = onRestartApp) }
        }
    }
}