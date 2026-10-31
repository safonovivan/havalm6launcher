package com.haval.m6launcher.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.net.wifi.WifiManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.PopupProperties
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.haval.m6launcher.AppLogger
import com.haval.m6launcher.data.LocationHelper
import com.haval.m6launcher.data.UserSettings
import com.haval.m6launcher.ui.components.AppPickerDialog
import com.haval.m6launcher.ui.components.FullPlayerContent
import com.haval.m6launcher.ui.components.HomeMusicWidget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PlaceholderScreen(title: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = title, color = MaterialTheme.colorScheme.onBackground, fontSize = 32.sp)
    }
}

@Composable
fun HomeScreen(
    settings: UserSettings,
    onSettingsChange: (UserSettings) -> Unit,
    onPickFolder: () -> Unit,
    executeAction: (String, String) -> Unit
) {
    Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Column(modifier = Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            HomeMusicWidget(
                modifier = Modifier.weight(1f),
                settings = settings,
                onSettingsChange = onSettingsChange,
                onPickFolder = onPickFolder
            )
        }
        Column(modifier = Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            QuickAccessWidget(
                modifier = Modifier.weight(1f),
                settings = settings,
                onSettingsChange = onSettingsChange,
                executeAction = executeAction
            )
            InfoWidget(modifier = Modifier.height(100.dp))
        }
    }
}

@Composable
fun QuickAccessWidget(
    modifier: Modifier = Modifier,
    settings: UserSettings,
    onSettingsChange: (UserSettings) -> Unit,
    executeAction: (String, String) -> Unit
) {
    var pickerIndex by remember { mutableStateOf(-1) }

    Card(modifier = modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(24.dp)) {
        Column(modifier = Modifier.padding(16.dp).fillMaxSize()) {
            Text("Быстрый доступ", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))

            Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                for (row in 0 until 3) {
                    Row(modifier = Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        for (col in 0 until 3) {
                            val index = row * 3 + col
                            val appPackage = settings.quickAccessApps[index]

                            AppSlot(
                                packageName = appPackage,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    if (appPackage.isBlank()) pickerIndex = index
                                    else executeAction("app", appPackage)
                                },
                                onLongClick = {
                                    if (appPackage.isNotBlank()) {
                                        val newList = settings.quickAccessApps.toMutableList()
                                        newList[index] = ""
                                        onSettingsChange(settings.copy(quickAccessApps = newList))
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (pickerIndex != -1) {
        AppPickerDialog(
            onDismiss = { pickerIndex = -1 },
            onAppSelected = { packageName, _ ->
                val newList = settings.quickAccessApps.toMutableList()
                newList[pickerIndex] = packageName
                onSettingsChange(settings.copy(quickAccessApps = newList))
                pickerIndex = -1
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppSlot(packageName: String, modifier: Modifier = Modifier, onClick: () -> Unit, onLongClick: () -> Unit) {
    val context = LocalContext.current
    val shape = RoundedCornerShape(16.dp)

    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(shape)
            .background(MaterialTheme.colorScheme.background)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        contentAlignment = Alignment.Center
    ) {
        if (packageName.isBlank()) {
            Icon(Icons.Default.Add, contentDescription = "Добавить", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp))
        } else {
            val icon = remember(packageName) {
                try { context.packageManager.getApplicationIcon(packageName) } catch (e: Exception) { null }
            }
            val name = remember(packageName) {
                try { context.packageManager.getApplicationLabel(context.packageManager.getApplicationInfo(packageName, 0)).toString() } catch (e: Exception) { "" }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceEvenly
            ) {
                if (icon != null) {
                    Image(
                        bitmap = icon.toBitmap(144, 144).asImageBitmap(),
                        contentDescription = name,
                        modifier = Modifier.size(60.dp)
                    )
                }

                Text(
                    text = name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
        }
    }
}

@SuppressLint("MissingPermission")
@Composable
fun InfoWidget(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var currentTime by remember { mutableStateOf("--:--") }
    var currentDate by remember { mutableStateOf("") }

    val wifiManager = remember { context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager }
    val bluetoothAdapter = remember { BluetoothAdapter.getDefaultAdapter() }

    var isWifiOn by remember { mutableStateOf(wifiManager.isWifiEnabled) }
    var isBtOn by remember { mutableStateOf(bluetoothAdapter?.isEnabled == true) }

    var temperature by remember { mutableStateOf("--°") }
    var currentCity by remember { mutableStateOf("Поиск...") }
    var weatherIcon by remember { mutableStateOf(Icons.Default.Cloud) }

    // БЕРЕМ КООРДИНАТЫ ИЗ ГЛОБАЛЬНОГО ХЕЛПЕРА
    val currentLoc = LocationHelper.currentLocation
    var hasPermission by remember { mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted -> hasPermission = isGranted }

    LaunchedEffect(Unit) { if (!hasPermission) permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION) }

    // Часы
    LaunchedEffect(Unit) {
        while (true) {
            val now = Date()
            currentTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(now)
            val day = SimpleDateFormat("EEEE", Locale.getDefault()).format(now)
            val dayCap = day.substring(0, 1).uppercase() + day.substring(1)
            currentDate = "$dayCap, ${SimpleDateFormat("d MMM", Locale.getDefault()).format(now)}"
            delay(1000L)
        }
    }

    // Погода (обновляется автоматически при появлении/смене координат)
    LaunchedEffect(currentLoc) {
        if (currentLoc == null) return@LaunchedEffect
        try {
            withContext(Dispatchers.IO) {
                var cityName = "Неизвестно"
                try {
                    val geocoder = Geocoder(context, Locale.getDefault())
                    val addresses = geocoder.getFromLocation(currentLoc.latitude, currentLoc.longitude, 1)
                    if (!addresses.isNullOrEmpty()) {
                        cityName = addresses[0].locality ?: addresses[0].subAdminArea ?: addresses[0].adminArea ?: "Неизвестно"
                    }
                } catch (e: Exception) { }

                AppLogger.log(context, "Погода API: Запрос для $cityName")

                val url = URL("https://wttr.in/${currentLoc.latitude},${currentLoc.longitude}?format=%t")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 10000
                connection.readTimeout = 10000
                connection.setRequestProperty("User-Agent", "curl/7.81.0")

                if (connection.responseCode == 200) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }.trim()
                    withContext(Dispatchers.Main) {
                        if (!response.startsWith("<") && response.isNotBlank()) {
                            currentCity = cityName
                            temperature = response.replace("C", "").replace("+", "+").trim()
                            AppLogger.log(context, "Погода API: Успешно получено $temperature")
                        }
                    }
                } else {
                    AppLogger.log(context, "Погода API: Ошибка HTTP ${connection.responseCode}")
                }
            }
        } catch (e: Exception) {
            AppLogger.log(context, "Погода API: Ошибка сети ${e.message}")
        }
    }

    Card(modifier = modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(24.dp)) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // ЛЕВАЯ ЧАСТЬ: Часы
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                Column(verticalArrangement = Arrangement.Center) {
                    Text(currentTime, fontSize = 32.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Text(currentDate, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), maxLines = 1)
                }
            }

            // ЦЕНТР: Кнопки
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    isWifiOn = !isWifiOn
                    wifiManager.isWifiEnabled = isWifiOn
                }) {
                    Icon(
                        imageVector = Icons.Default.Wifi,
                        contentDescription = "Wi-Fi",
                        tint = if (isWifiOn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                        modifier = Modifier.size(56.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                IconButton(onClick = {
                    isBtOn = !isBtOn
                    if (isBtOn) bluetoothAdapter?.enable() else bluetoothAdapter?.disable()
                }) {
                    Icon(
                        imageVector = Icons.Default.Bluetooth,
                        contentDescription = "Bluetooth",
                        tint = if (isBtOn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                        modifier = Modifier.size(56.dp)
                    )
                }
            }

            // ПРАВАЯ ЧАСТЬ: Погода
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
                Column(horizontalAlignment = Alignment.End) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = weatherIcon,
                            contentDescription = "Погода",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = temperature,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = currentCity,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun MusicScreen(
    settings: UserSettings,
    onSettingsChange: (UserSettings) -> Unit,
    onPickFolder: () -> Unit
) {
    FullPlayerContent(
        settings = settings,
        onSettingsChange = onSettingsChange,
        onPickFolder = onPickFolder
    )
}

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
    var floatingDoubleTapExpanded by remember { mutableStateOf(false) }
    var floatingAppExpanded by remember { mutableStateOf(false) }
    val clipboardManager = LocalClipboardManager.current

    // СОСТОЯНИЕ ДЛЯ ЛОГОВ
    var showLogs by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp).imePadding().verticalScroll(rememberScrollState()),
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

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("Автовоспроизведение", fontSize = 24.sp, color = MaterialTheme.colorScheme.onBackground)
                Text("Запускать плеер при старте ГУ", fontSize = 14.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
            }
            Switch(checked = settings.isAutoPlayEnabled, onCheckedChange = { onSettingsChange(settings.copy(isAutoPlayEnabled = it)) })
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
                Button(onClick = onRestartApp, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary), modifier = Modifier.height(56.dp)) {
                    Icon(Icons.Default.Refresh, contentDescription = "Restart")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Применить")
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            if (settings.yandexMapApiKey.isNotBlank()) Text("✓ Ключ введен. Нажмите «Применить» для перезапуска.", fontSize = 14.sp, color = Color(0xFF4CAF50))
            else Text("⚠ Ключ не задан. Карта работать не будет.", fontSize = 14.sp, color = Color.Red)
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
        Text("Плавающая кнопка", fontSize = 20.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))

        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Двойной клик по кнопке", fontSize = 20.sp, color = MaterialTheme.colorScheme.onBackground)
                Box {
                    Button(onClick = { floatingDoubleTapExpanded = true }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Text(getFloatingActionName(settings.floatingDoubleTapType), color = MaterialTheme.colorScheme.primary)
                    }
                    DropdownMenu(expanded = floatingDoubleTapExpanded, onDismissRequest = { floatingDoubleTapExpanded = false }, properties = PopupProperties(focusable = false)) {
                        DropdownMenuItem(text = { Text("Домой (Рабочий стол)") }, onClick = { onSettingsChange(settings.copy(floatingDoubleTapType = "home")); floatingDoubleTapExpanded = false })
                        DropdownMenuItem(text = { Text("Недавние приложения") }, onClick = { onSettingsChange(settings.copy(floatingDoubleTapType = "recents")); floatingDoubleTapExpanded = false })
                        DropdownMenuItem(text = { Text("Открыть приложение") }, onClick = { onSettingsChange(settings.copy(floatingDoubleTapType = "app", floatingDoubleTapValue = "")); floatingDoubleTapExpanded = false })
                        DropdownMenuItem(text = { Text("Ничего") }, onClick = { onSettingsChange(settings.copy(floatingDoubleTapType = "none")); floatingDoubleTapExpanded = false })
                    }
                }
            }

            if (settings.floatingDoubleTapType == "app") {
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth().padding(start = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Приложение: ", fontSize = 16.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f))
                    Box {
                        TextButton(onClick = { floatingAppExpanded = true }) { Text(getAppNameByPackage(settings.floatingDoubleTapValue), fontSize = 16.sp, color = MaterialTheme.colorScheme.primary) }
                        DropdownMenu(expanded = floatingAppExpanded, onDismissRequest = { floatingAppExpanded = false }, properties = PopupProperties(focusable = false), modifier = Modifier.heightIn(max = 300.dp)) {
                            allApps.forEach { app -> DropdownMenuItem(text = { Text(app.name) }, onClick = { onSettingsChange(settings.copy(floatingDoubleTapValue = app.packageName)); floatingAppExpanded = false }) }
                        }
                    }
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f))

        // КНОПКА ПРОСМОТРА ЛОГОВ
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = { showLogs = true },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Посмотреть лог запуска (для отладки)", color = MaterialTheme.colorScheme.onSecondary)
        }

        if (showLogs) {
            val logText = AppLogger.readLogs(context)
            AlertDialog(
                onDismissRequest = { showLogs = false },
                confirmButton = {
                    TextButton(onClick = { showLogs = false }) { Text("Закрыть") }
                },
                dismissButton = {
                    TextButton(onClick = {
                        clipboardManager.setText(AnnotatedString(logText))
                        android.widget.Toast.makeText(context, "Лог скопирован в буфер", android.widget.Toast.LENGTH_SHORT).show()
                        showLogs = false
                    }) {
                        Text("Скопировать")
                    }
                },
                title = { Text("Лог запуска") },
                text = {
                    Text(
                        text = logText,
                        fontSize = 10.sp,
                        modifier = Modifier.verticalScroll(rememberScrollState()).height(300.dp)
                    )
                }
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

fun getActionTypeName(type: String): String {
    return when (type) { "none" -> "Ничего"; "action" -> "Действие"; "app" -> "Приложение"; else -> "Не задано" }
}

fun getActionValueName(value: String): String {
    return when (value) { "recents" -> "Недавние приложения"; "android_settings" -> "Настройки Android"; "browser" -> "Открыть Браузер"; "dialer" -> "Набор номера"; else -> value }
}

fun getFloatingActionName(type: String): String {
    return when (type) {
        "home" -> "Домой"
        "recents" -> "Недавние"
        "app" -> "Приложение"
        "none" -> "Ничего"
        else -> "Домой"
    }
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