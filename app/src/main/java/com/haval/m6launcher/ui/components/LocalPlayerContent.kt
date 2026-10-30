package com.haval.m6launcher.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign // <-- ДОБАВЛЕН ИМПОРТ
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.haval.m6launcher.data.MusicPlayerState
import kotlinx.coroutines.delay

@Composable
fun LocalPlayerView(
    modifier: Modifier = Modifier,
    largeIcons: Boolean = false,
    showSlider: Boolean = true
) {
    LaunchedEffect(Unit) {
        while (true) {
            MusicPlayerState.updatePosition()
            delay(1000L)
        }
    }

    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        // Обложка
        Box(
            modifier = Modifier.weight(1f).fillMaxWidth().padding(vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .aspectRatio(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(if (largeIcons) 32.dp else 16.dp))
                    .background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center
            ) {
                if (MusicPlayerState.currentAlbumArt != null) {
                    Image(
                        bitmap = MusicPlayerState.currentAlbumArt!!.asImageBitmap(),
                        contentDescription = "Cover",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        Icons.Default.MusicNote,
                        "Cover",
                        modifier = Modifier.fillMaxSize(0.5f),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    )
                }
            }
        }

        // Текст (Обязательно растягиваем Box и внутренние Text на всю ширину)
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(), // Занимаем всю ширину
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = MusicPlayerState.currentTitle,
                    modifier = Modifier.fillMaxWidth(), // Принудительно на всю ширину
                    textAlign = TextAlign.Center,       // Центрируем
                    fontSize = if (largeIcons) 36.sp else 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = MusicPlayerState.currentArtist,
                    modifier = Modifier.fillMaxWidth(), // Принудительно на всю ширину
                    textAlign = TextAlign.Center,       // Центрируем
                    fontSize = if (largeIcons) 20.sp else 16.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    maxLines = 1
                )
            }
        }

        // Ползунок
        if (showSlider) {
            Column(modifier = Modifier.fillMaxWidth()) {
                val maxDuration = if (MusicPlayerState.totalDuration > 0) MusicPlayerState.totalDuration.toFloat() else 1f
                val safePosition = MusicPlayerState.currentPosition.toFloat().coerceIn(0f, maxDuration)

                Slider(
                    value = safePosition,
                    onValueChange = { MusicPlayerState.seekTo(it.toInt()) },
                    valueRange = 0f..maxDuration,
                    modifier = Modifier.height(32.dp)
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(formatTime(MusicPlayerState.currentPosition), fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    Text(formatTime(MusicPlayerState.totalDuration), fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Кнопки
        PlayerControls(modifier = Modifier.fillMaxWidth(), largeIcons = largeIcons)
    }
}