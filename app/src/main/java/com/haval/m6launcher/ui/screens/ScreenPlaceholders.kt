package com.haval.m6launcher.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.PopupProperties
import com.haval.m6launcher.data.MusicPlayerState
import com.haval.m6launcher.ui.components.FullPlayerContent
import com.haval.m6launcher.ui.components.HomeMusicWidget
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PlaceholderScreen(title: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = title, color = MaterialTheme.colorScheme.onBackground, fontSize = 32.sp)
    }
}

// ==========================================
// ГЛАВНЫЙ ЭКРАН (ДОМОЙ)
// ==========================================
@Composable
fun HomeScreen() {
    val context = LocalContext.current
    LaunchedEffect(Unit) { MusicPlayerState.initPlayer(context) }

    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(
            modifier = Modifier.weight(1f).fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ВЫЗЫВАЕМ НАШ НОВЫЙ КОМПОНЕНТ ИЗ PlayerComponents.kt
            HomeMusicWidget(modifier = Modifier.weight(1f))
        }

        Column(
            modifier = Modifier.weight(1f).fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            NavWidget(modifier = Modifier.weight(1f))
            InfoWidget(modifier = Modifier.height(100.dp))
        }
    }
}

@Composable
fun NavWidget(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text("Навигатор", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), fontSize = 18.sp)
            Spacer(modifier = Modifier.height(16.dp))
            Text("Мончегорская", fontSize = 32.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Text("42 мин", fontSize = 20.sp, color = Color(0xFF4CAF50))
        }
    }
}

@Composable
fun InfoWidget(modifier: Modifier = Modifier) {
    var currentTime by remember { mutableStateOf("--:--") }
    LaunchedEffect(Unit) {
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        while (true) { currentTime = timeFormat.format(Date()); delay(1000L) }
    }
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(currentTime, fontSize = 32.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Text("+18°", fontSize = 32.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

// ==========================================
// ЭКРАН МУЗЫКИ (ПОЛНОЭКРАННЫЙ)
// ==========================================
@Composable
fun MusicScreen() {
    val context = LocalContext.current
    LaunchedEffect(Unit) { MusicPlayerState.initPlayer(context) }

    LaunchedEffect(Unit) {
        while (true) {
            MusicPlayerState.updatePosition()
            delay(1000L)
        }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            MusicPlayerState.loadTrackFromUri(context, uri)
        }
    }

    // ВЫЗЫВАЕМ НАШ НОВЫЙ КОМПОНЕНТ, ПЕРЕДАВАЯ ЛОГИКУ ВЫБОРА ФАЙЛА
    FullPlayerContent(onPickFile = { filePickerLauncher.launch("audio/*") })
}

// ==========================================
// НАСТРОЙКИ (БЕЗ ИЗМЕНЕНИЙ, ОСТАВЛЯЕМ КАК БЫЛО)
// ==========================================
@Composable
fun SettingsScreen(
    settings: com.haval.m6launcher.data.UserSettings,
    onSettingsChange: (com.haval.m6launcher.data.UserSettings) -> Unit,
    onRestartApp: () -> Unit
) {
    val context = LocalContext.current
    val allApps = remember { loadInstalledApps(context).filter { !it.isSystem } }

    var isKeyVisible by remember { mutableStateOf(false) }
    var doubleTapExpanded by remember { mutableStateOf(false) }
    var longPressExpanded by remember { mutableStateOf(false) }
    var doubleTapActionExpanded by remember { mutableStateOf(false) }
    var longPressActionExpanded by remember { mutableStateOf(false) }
    var doubleTapAppExpanded by remember { mutableStateOf(false) }
    var longPressAppExpanded by remember { mutableStateOf(false) }
    var mapThemeExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp).imePadding().verticalScroll(androidx.compose.foundation.rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text("Настройки", fontSize = 32.sp, color = MaterialTheme.colorScheme.onBackground)
        HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Темная тема", fontSize = 24.sp, color = MaterialTheme.colorScheme.onBackground)
            Switch(checked = settings.isDarkTheme, onCheckedChange = { onSettingsChange(settings.copy(isDarkTheme = it)) })
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Полноэкранный режим", fontSize = 24.sp, color = MaterialTheme.colorScheme.onBackground)
            Switch(checked = settings.isFullScreen, onCheckedChange = { onSettingsChange(settings.copy(isFullScreen = it)) })
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Голосовой помощник", fontSize = 24.sp, color = MaterialTheme.colorScheme.onBackground)
            Switch(checked = settings.isVoiceAssistantEnabled, onCheckedChange = { onSettingsChange(settings.copy(isVoiceAssistantEnabled = it)) })
        }
        Column {
            Text("Отступы контента (${settings.contentPadding} dp)", fontSize = 24.sp, color = MaterialTheme.colorScheme.onBackground)
            Slider(value = settings.contentPadding.toFloat(), onValueChange = { onSettingsChange(settings.copy(contentPadding = it.toInt())) }, valueRange = 0f..48f)
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f))

        Text("Яндекс.Карты", fontSize = 20.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))

        Column {
            Text("Ключ разработчика", fontSize = 20.sp, color = MaterialTheme.colorScheme.onBackground)
            Spacer(modifier = Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = settings.yandexMapApiKey,
                    onValueChange = { onSettingsChange(settings.copy(yandexMapApiKey = it)) },
                    placeholder = { Text("Введите API ключ...") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    visualTransformation = if (isKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        val image = if (isKeyVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                        IconButton(onClick = { isKeyVisible = !isKeyVisible }) { Icon(imageVector = image, contentDescription = "Show/Hide", tint = MaterialTheme.colorScheme.onSurface) }
                    }
                )
                Spacer(modifier = Modifier.width(16.dp))

                Button(
                    onClick = onRestartApp,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.height(56.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Restart")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Применить")
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            if (settings.yandexMapApiKey.isNotBlank()) {
                Text("✓ Ключ введен. Нажмите «Применить» для перезапуска.", fontSize = 14.sp, color = Color(0xFF4CAF50))
            } else {
                Text("⚠ Ключ не задан. Карта работать не будет.", fontSize = 14.sp, color = Color.Red)
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Оформление карты", fontSize = 20.sp, color = MaterialTheme.colorScheme.onBackground)
            Box {
                Button(onClick = { mapThemeExpanded = true }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Text(text = when (settings.mapTheme) { "light" -> "Светлая"; "dark" -> "Тёмная"; else -> "Авто" }, color = MaterialTheme.colorScheme.primary)
                }
                DropdownMenu(expanded = mapThemeExpanded, onDismissRequest = { mapThemeExpanded = false }, properties = PopupProperties(focusable = false)) {
                    DropdownMenuItem(text = { Text("Светлая") }, onClick = { onSettingsChange(settings.copy(mapTheme = "light")); mapThemeExpanded = false })
                    DropdownMenuItem(text = { Text("Тёмная") }, onClick = { onSettingsChange(settings.copy(mapTheme = "dark")); mapThemeExpanded = false })
                    DropdownMenuItem(text = { Text("Авто") }, onClick = { onSettingsChange(settings.copy(mapTheme = "auto")); mapThemeExpanded = false })
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f))

        Text("Кнопка «Домой»", fontSize = 20.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))

        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Двойной клик", fontSize = 20.sp, color = MaterialTheme.colorScheme.onBackground)
                Box {
                    Button(onClick = { doubleTapExpanded = true }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Text(getActionTypeName(settings.homeDoubleTapType), color = MaterialTheme.colorScheme.primary)
                    }
                    DropdownMenu(expanded = doubleTapExpanded, onDismissRequest = { doubleTapExpanded = false }, properties = PopupProperties(focusable = false)) {
                        DropdownMenuItem(text = { Text("Ничего") }, onClick = { onSettingsChange(settings.copy(homeDoubleTapType = "none", homeDoubleTapValue = "")); doubleTapExpanded = false })
                        DropdownMenuItem(text = { Text("Действие") }, onClick = { onSettingsChange(settings.copy(homeDoubleTapType = "action", homeDoubleTapValue = "recents")); doubleTapExpanded = false })
                        DropdownMenuItem(text = { Text("Приложение") }, onClick = { onSettingsChange(settings.copy(homeDoubleTapType = "app", homeDoubleTapValue = "")); doubleTapExpanded = false })
                    }
                }
            }

            if (settings.homeDoubleTapType == "action") {
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth().padding(start = 16.dp), horizontalArrangement = Arrangement.Start, verticalAlignment = Alignment.CenterVertically) {
                    Text("Конкретное действие: ", fontSize = 16.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f))
                    Box {
                        TextButton(onClick = { doubleTapActionExpanded = true }) { Text(getActionValueName(settings.homeDoubleTapValue), fontSize = 16.sp, color = MaterialTheme.colorScheme.primary) }
                        DropdownMenu(expanded = doubleTapActionExpanded, onDismissRequest = { doubleTapActionExpanded = false }, properties = PopupProperties(focusable = false)) {
                            DropdownMenuItem(text = { Text("Недавние приложения") }, onClick = { onSettingsChange(settings.copy(homeDoubleTapValue = "recents")); doubleTapActionExpanded = false })
                            DropdownMenuItem(text = { Text("Настройки Android") }, onClick = { onSettingsChange(settings.copy(homeDoubleTapValue = "android_settings")); doubleTapActionExpanded = false })
                            DropdownMenuItem(text = { Text("Открыть Браузер") }, onClick = { onSettingsChange(settings.copy(homeDoubleTapValue = "browser")); doubleTapActionExpanded = false })
                            DropdownMenuItem(text = { Text("Набор номера") }, onClick = { onSettingsChange(settings.copy(homeDoubleTapValue = "dialer")); doubleTapActionExpanded = false })
                        }
                    }
                }
            } else if (settings.homeDoubleTapType == "app") {
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth().padding(start = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Приложение: ", fontSize = 16.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f))
                    Box {
                        TextButton(onClick = { doubleTapAppExpanded = true }) { Text(getAppNameByPackage(settings.homeDoubleTapValue), fontSize = 16.sp, color = MaterialTheme.colorScheme.primary) }
                        DropdownMenu(expanded = doubleTapAppExpanded, onDismissRequest = { doubleTapAppExpanded = false }, properties = PopupProperties(focusable = false), modifier = Modifier.heightIn(max = 300.dp)) {
                            allApps.forEach { app -> DropdownMenuItem(text = { Text(app.name) }, onClick = { onSettingsChange(settings.copy(homeDoubleTapValue = app.packageName)); doubleTapAppExpanded = false }) }
                        }
                    }
                }
            }
        }

        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Долгое нажатие", fontSize = 20.sp, color = MaterialTheme.colorScheme.onBackground)
                Box {
                    Button(onClick = { longPressExpanded = true }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Text(getActionTypeName(settings.homeLongPressType), color = MaterialTheme.colorScheme.primary)
                    }
                    DropdownMenu(expanded = longPressExpanded, onDismissRequest = { longPressExpanded = false }, properties = PopupProperties(focusable = false)) {
                        DropdownMenuItem(text = { Text("Ничего") }, onClick = { onSettingsChange(settings.copy(homeLongPressType = "none", homeLongPressValue = "")); longPressExpanded = false })
                        DropdownMenuItem(text = { Text("Действие") }, onClick = { onSettingsChange(settings.copy(homeLongPressType = "action", homeLongPressValue = "recents")); longPressExpanded = false })
                        DropdownMenuItem(text = { Text("Приложение") }, onClick = { onSettingsChange(settings.copy(homeLongPressType = "app", homeLongPressValue = "")); longPressExpanded = false })
                    }
                }
            }

            if (settings.homeLongPressType == "action") {
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth().padding(start = 16.dp), horizontalArrangement = Arrangement.Start, verticalAlignment = Alignment.CenterVertically) {
                    Text("Конкретное действие: ", fontSize = 16.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f))
                    Box {
                        TextButton(onClick = { longPressActionExpanded = true }) { Text(getActionValueName(settings.homeLongPressValue), fontSize = 16.sp, color = MaterialTheme.colorScheme.primary) }
                        DropdownMenu(expanded = longPressActionExpanded, onDismissRequest = { longPressActionExpanded = false }, properties = PopupProperties(focusable = false)) {
                            DropdownMenuItem(text = { Text("Недавние приложения") }, onClick = { onSettingsChange(settings.copy(homeLongPressValue = "recents")); longPressActionExpanded = false })
                            DropdownMenuItem(text = { Text("Настройки Android") }, onClick = { onSettingsChange(settings.copy(homeLongPressValue = "android_settings")); longPressActionExpanded = false })
                            DropdownMenuItem(text = { Text("Открыть Браузер") }, onClick = { onSettingsChange(settings.copy(homeLongPressValue = "browser")); longPressActionExpanded = false })
                            DropdownMenuItem(text = { Text("Набор номера") }, onClick = { onSettingsChange(settings.copy(homeLongPressValue = "dialer")); longPressActionExpanded = false })
                        }
                    }
                }
            } else if (settings.homeLongPressType == "app") {
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth().padding(start = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Приложение: ", fontSize = 16.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f))
                    Box {
                        TextButton(onClick = { longPressAppExpanded = true }) { Text(getAppNameByPackage(settings.homeLongPressValue), fontSize = 16.sp, color = MaterialTheme.colorScheme.primary) }
                        DropdownMenu(expanded = longPressAppExpanded, onDismissRequest = { longPressAppExpanded = false }, properties = PopupProperties(focusable = false), modifier = Modifier.heightIn(max = 300.dp)) {
                            allApps.forEach { app -> DropdownMenuItem(text = { Text(app.name) }, onClick = { onSettingsChange(settings.copy(homeLongPressValue = app.packageName)); longPressAppExpanded = false }) }
                        }
                    }
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f))
        Spacer(modifier = Modifier.height(32.dp))
    }
}

fun getActionTypeName(type: String): String {
    return when (type) { "none" -> "Ничего"; "action" -> "Действие"; "app" -> "Приложение"; else -> "Не задано" }
}

fun getActionValueName(value: String): String {
    return when (value) { "recents" -> "Недавние приложения"; "android_settings" -> "Настройки Android"; "browser" -> "Открыть Браузер"; "dialer" -> "Набор номера"; else -> value }
}

@Composable
fun getAppNameByPackage(packageName: String): String {
    if (packageName.isBlank()) return "Не выбрано"
    val context = LocalContext.current
    return try {
        val pm = context.packageManager
        val appInfo = pm.getApplicationInfo(packageName, 0)
        pm.getApplicationLabel(appInfo).toString()
    } catch (e: Exception) { "Неизвестное приложение" }
}

@Composable fun ObdScreen() = PlaceholderScreen("Параметры авто (OBD)")