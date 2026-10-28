package com.haval.m6launcher.ui.screens

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap

// Модель данных для приложения
data class AppInfo(
    val name: String,
    val packageName: String,
    val icon: Drawable,
    val isSystem: Boolean
)

@Composable
fun AppsScreen() {
    val context = LocalContext.current
    // ИЗМЕНЕНИЕ 1: Теперь по умолчанию true (только пользовательские)
    var showOnlyUserApps by remember { mutableStateOf(true) }
    var allApps by remember { mutableStateOf<List<AppInfo>>(emptyList()) }

    LaunchedEffect(Unit) {
        allApps = loadInstalledApps(context)
    }

    val displayedApps = if (showOnlyUserApps) {
        allApps.filter { !it.isSystem }
    } else {
        allApps
    }

    Column(modifier = Modifier.fillMaxSize()) {

        // --- ВЕРХНЯЯ ПАНЕЛЬ С ПЕРЕКЛЮЧАТЕЛЕМ ---
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Приложения", fontSize = 32.sp, color = MaterialTheme.colorScheme.onBackground)
            Spacer(modifier = Modifier.weight(1f))

            // ИЗМЕНЕНИЕ 2: Кнопки поменяны местами
            // Кнопка "Пользовательские"
            Button(
                onClick = { showOnlyUserApps = true },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (showOnlyUserApps) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                    contentColor = if (showOnlyUserApps) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                )
            ) {
                Text("Пользовательские")
            }

            // Кнопка "Все"
            Button(
                onClick = { showOnlyUserApps = false },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (!showOnlyUserApps) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                    contentColor = if (!showOnlyUserApps) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                )
            ) {
                Text("Все")
            }
        }

        // --- СЕТКА ПРИЛОЖЕНИЙ ---
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 120.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(displayedApps) { app ->
                AppIconItem(app = app, onClick = {
                    val intent = context.packageManager.getLaunchIntentForPackage(app.packageName)
                    if (intent != null) {
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(intent)
                    }
                })
            }
        }
    }
}

@Composable
fun AppIconItem(app: AppInfo, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            bitmap = app.icon.toBitmap(96, 96).asImageBitmap(),
            contentDescription = app.name,
            modifier = Modifier.size(64.dp).padding(bottom = 8.dp)
        )
        Text(
            text = app.name,
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

fun loadInstalledApps(context: Context): List<AppInfo> {
    val pm = context.packageManager
    val packages = pm.getInstalledApplications(0)
    val apps = mutableListOf<AppInfo>()

    for (pkg in packages) {
        val launchIntent = pm.getLaunchIntentForPackage(pkg.packageName)
        if (launchIntent != null) {
            apps.add(
                AppInfo(
                    name = pm.getApplicationLabel(pkg).toString(),
                    packageName = pkg.packageName,
                    icon = pm.getApplicationIcon(pkg.packageName),
                    isSystem = (pkg.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0
                )
            )
        }
    }
    return apps.sortedBy { it.name }
}