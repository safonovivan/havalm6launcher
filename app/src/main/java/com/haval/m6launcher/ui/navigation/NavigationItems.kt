package com.haval.m6launcher.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

data class NavItem(
    val route: String,
    val icon: ImageVector,
    val label: String
)

val menuItems = listOf(
    NavItem("home", Icons.Default.Home, "Домой"),
    NavItem("nav", Icons.Default.Navigation, "Навигатор"),
    NavItem("music", Icons.Default.MusicNote, "Музыка"),
    NavItem("obd", Icons.Default.DirectionsCar, "Авто"),
    NavItem("apps", Icons.Default.Apps, "Приложения"),
    NavItem("settings", Icons.Default.Settings, "Настройки"),
    NavItem("voice", Icons.Default.Mic, "Ассистент")
)