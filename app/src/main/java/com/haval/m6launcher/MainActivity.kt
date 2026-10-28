package com.haval.m6launcher

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.speech.RecognizerIntent // НОВЫЙ ИМПОРТ
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import com.haval.m6launcher.data.SettingsRepository
import com.haval.m6launcher.data.UserSettings
import com.haval.m6launcher.ui.components.LeftNavigationPanel
import com.haval.m6launcher.ui.screens.*
import com.haval.m6launcher.ui.theme.HavalM6LauncherTheme
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import androidx.compose.runtime.rememberCoroutineScope
import com.yandex.mapkit.MapKitFactory

class MainActivity : ComponentActivity() {

    private var isFullScreenSetting = true
    private var apiKey = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val settingsRepo = SettingsRepository(applicationContext)
        val savedSettings = runBlocking { settingsRepo.userSettings.first() }
        apiKey = savedSettings.yandexMapApiKey

        if (apiKey.isNotBlank()) {
            try {
                MapKitFactory.setApiKey(apiKey)
                MapKitFactory.initialize(this)
            } catch (e: Throwable) {
                Toast.makeText(this, "Ошибка инициализации карт: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }

        setContent {
            val settings by settingsRepo.userSettings.collectAsState(initial = UserSettings())
            val scope = rememberCoroutineScope()
            val context = LocalContext.current

            val restartApp = {
                val mStartActivity = Intent(context, MainActivity::class.java)
                mStartActivity.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                val mPendingIntentId = 123456
                val mPendingIntent = PendingIntent.getActivity(
                    context,
                    mPendingIntentId,
                    mStartActivity,
                    PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                val mgr = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 100, mPendingIntent)
                System.exit(0)
            }

            LaunchedEffect(settings.isFullScreen) {
                isFullScreenSetting = settings.isFullScreen
                val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
                if (settings.isFullScreen) {
                    windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
                } else {
                    windowInsetsController.show(WindowInsetsCompat.Type.systemBars())
                }
                windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }

            HavalM6LauncherTheme(darkTheme = settings.isDarkTheme) {
                MainLauncherContent(
                    settings = settings,
                    onSettingsChange = { newSettings -> scope.launch { settingsRepo.updateSettings(newSettings) } },
                    executeAction = { type, value ->
                        when (type) {
                            "app" -> {
                                val intent = context.packageManager.getLaunchIntentForPackage(value)
                                if (intent != null) {
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    context.startActivity(intent)
                                } else {
                                    Toast.makeText(context, "Приложение не найдено", Toast.LENGTH_SHORT).show()
                                }
                            }
                            "action" -> {
                                try {
                                    when (value) {
                                        "recents" -> {
                                            val intent = Intent("com.android.systemui.recent.action.SHOW_RECENT_APPS")
                                            context.startActivity(intent)
                                        }
                                        "android_settings" -> {
                                            val intent = Intent(android.provider.Settings.ACTION_SETTINGS)
                                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                            context.startActivity(intent)
                                        }
                                        "browser" -> {
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://yandex.ru"))
                                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                            context.startActivity(intent)
                                        }
                                        "dialer" -> {
                                            val intent = Intent(Intent.ACTION_DIAL)
                                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                            context.startActivity(intent)
                                        }
                                    }
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Действие не поддерживается", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    },
                    onRestartApp = restartApp
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        if (apiKey.isNotBlank()) MapKitFactory.getInstance().onStart()
    }

    override fun onStop() {
        super.onStop()
        if (apiKey.isNotBlank()) MapKitFactory.getInstance().onStop()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus && isFullScreenSetting) {
            val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
            windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
        }
    }
}

@Composable
fun MainLauncherContent(
    settings: UserSettings,
    onSettingsChange: (UserSettings) -> Unit,
    executeAction: (String, String) -> Unit,
    onRestartApp: () -> Unit
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val context = LocalContext.current

    // ФУНКЦИЯ ЗАПУСКА ГОЛОСОВОГО ПОМОЩНИКА (АДАПТИРОВАНА ПОД ANDROID 9 / ГУ БЕЗ GOOGLE, ПРИОРИТЕТ НА РУССКИЙ ЯЗЫК)

    val startVoiceInput = {
        var launched = false

        // --- Попытка 1: Стандартный распознаватель речи (Работает везде, знает русский) ---
        val recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ru-RU") // Жестко задаем русский язык
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Слушаю...")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        if (recognizerIntent.resolveActivity(context.packageManager) != null) {
            try {
                context.startActivity(recognizerIntent)
                launched = true
            } catch (e: Exception) {
                // Если что-то пошло не так, идем дальше
            }
        }

        // --- Попытка 2: Системный голосовой ассистент (Google, Алиса и т.д.) ---
        if (!launched) {
            val voiceCommandIntent = Intent(Intent.ACTION_VOICE_COMMAND).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (voiceCommandIntent.resolveActivity(context.packageManager) != null) {
                try {
                    context.startActivity(voiceCommandIntent)
                    launched = true
                } catch (e: Exception) {
                    // Не сработало
                }
            }
        }

        // --- Если вообще ничего не нашли ---
        if (!launched) {
            Toast.makeText(context, "Голосовой ввод не найден на этом устройстве", Toast.LENGTH_SHORT).show()
        }
    }

    Row(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
    ) {
        LeftNavigationPanel(
            modifier = Modifier.width(100.dp).fillMaxHeight(),
            currentRoute = currentRoute,
            isVoiceAssistantEnabled = settings.isVoiceAssistantEnabled,
            onItemClick = { route -> navController.navigate(route) { popUpTo(navController.graph.startDestinationId) { saveState = true }; launchSingleTop = true; restoreState = true } },
            onHomeDoubleTap = { executeAction(settings.homeDoubleTapType, settings.homeDoubleTapValue) },
            onHomeLongPress = { executeAction(settings.homeLongPressType, settings.homeLongPressValue) },
            onVoiceClick = startVoiceInput
        )

        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.fillMaxSize().padding(settings.contentPadding.dp)
        ) {
            composable("home") { HomeScreen() }
            composable("nav") {
                NavigatorScreen(
                    apiKey = settings.yandexMapApiKey,
                    mapTheme = settings.mapTheme,
                    onNavigateToSettings = { navController.navigate("settings") }
                )
            }
            composable("music") { MusicScreen() }
            composable("obd") { ObdScreen() }
            composable("apps") { AppsScreen() }
            composable("settings") {
                SettingsScreen(
                    settings = settings,
                    onSettingsChange = onSettingsChange,
                    onRestartApp = onRestartApp
                )
            }
            // composable("voice") удалён — экран больше не нужен
        }
    }
}