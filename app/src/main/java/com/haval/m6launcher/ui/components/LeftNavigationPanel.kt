package com.haval.m6launcher.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.haval.m6launcher.ui.navigation.menuItems

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LeftNavigationPanel(
    modifier: Modifier = Modifier,
    currentRoute: String?,
    onItemClick: (String) -> Unit,
    onHomeDoubleTap: () -> Unit,  // Новый коллбэк
    onHomeLongPress: () -> Unit   // Новый коллбэк
) {
    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface)
            .padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.weight(1f))

        menuItems.forEach { item ->
            val isSelected = currentRoute == item.route

            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (isSelected) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f) else Color.Transparent)
                    .combinedClickable(
                        onClick = { onItemClick(item.route) },
                        onDoubleClick = {
                            if (item.route == "home") {
                                onHomeDoubleTap()
                            }
                        },
                        onLongClick = {
                            if (item.route == "home") {
                                onHomeLongPress()
                            }
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = item.label,
                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.size(32.dp)
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))
    }
}