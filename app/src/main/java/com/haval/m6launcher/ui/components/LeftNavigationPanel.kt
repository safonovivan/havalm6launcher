package com.haval.m6launcher.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.haval.m6launcher.ui.navigation.menuItems
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LeftNavigationPanel(
    modifier: Modifier = Modifier,
    currentRoute: String?,
    isVoiceAssistantEnabled: Boolean,
    onItemClick: (String) -> Unit,
    onHomeDoubleTap: () -> Unit,
    onHomeLongPress: () -> Unit,
    onVoiceClick: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var isVoicePulsing by remember { mutableStateOf(false) }

    val filteredMenuItems = menuItems.filter {
        it.route != "voice" || isVoiceAssistantEnabled
    }

    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface)
            .padding(vertical = 12.dp), // Чуть уменьшили отступы по краям
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly // КЛЮЧЕВОЙ ФИКС: равномерно распределяем всё по высоте
    ) {
        filteredMenuItems.forEach { item ->
            val isSelected = currentRoute == item.route

            if (item.route == "voice") {
                // Анимация микрофона (без изменений)
                Box(
                    modifier = Modifier
                        .size(56.dp) // Чуть уменьшили размер
                        .clip(RoundedCornerShape(16.dp))
                        .combinedClickable(
                            onClick = {
                                if (!isVoicePulsing) {
                                    isVoicePulsing = true
                                    scope.launch {
                                        delay(1200)
                                        onVoiceClick()
                                        isVoicePulsing = false
                                    }
                                }
                            },
                            onDoubleClick = {},
                            onLongClick = {}
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isVoicePulsing) {
                        val infiniteTransition = rememberInfiniteTransition(label = "voicePulse")
                        val scale by infiniteTransition.animateFloat(
                            initialValue = 1f, targetValue = 1.6f,
                            animationSpec = infiniteRepeatable(animation = tween(600, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Restart),
                            label = "scale"
                        )
                        val alpha by infiniteTransition.animateFloat(
                            initialValue = 0.7f, targetValue = 0f,
                            animationSpec = infiniteRepeatable(animation = tween(600, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Restart),
                            label = "alpha"
                        )
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .scale(scale)
                                .alpha(alpha)
                                .background(MaterialTheme.colorScheme.primary, CircleShape)
                        )
                    }
                    val iconColor by animateColorAsState(
                        targetValue = if (isVoicePulsing) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        label = "voiceIconColor"
                    )
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label,
                        tint = iconColor,
                        modifier = Modifier.size(28.dp) // Чуть уменьшили
                    )
                }
            } else {
                // Обычные кнопки
                Box(
                    modifier = Modifier
                        .size(56.dp) // Чуть уменьшили
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isSelected) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f) else Color.Transparent)
                        .combinedClickable(
                            onClick = { onItemClick(item.route) },
                            onDoubleClick = { if (item.route == "home") onHomeDoubleTap() },
                            onLongClick = { if (item.route == "home") onHomeLongPress() }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label,
                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.size(28.dp) // Чуть уменьшили
                    )
                }
            }
        }
    }
}