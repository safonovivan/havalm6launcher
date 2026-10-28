package com.haval.m6launcher.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.haval.m6launcher.data.MusicPlayerState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Вспомогательная функция для времени
fun formatTime(ms: Int): String {
    if (ms <= 0) return "0:00"
    val seconds = (ms / 1000) % 60
    val minutes = (ms / (1000 * 60)) % 60
    return String.format(Locale.getDefault(), "%d:%02d", minutes, seconds)
}

// 1. Кнопки управления (Лайк, Плей, Следующий)
@Composable
fun PlayerControls(modifier: Modifier = Modifier, largeIcons: Boolean = false) {
    val mainButtonSize = if (largeIcons) 120.dp else 100.dp
    val secondaryButtonSize = if (largeIcons) 72.dp else 64.dp

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = { }, modifier = Modifier.size(secondaryButtonSize)) {
            Icon(Icons.Default.FavoriteBorder, "Like", tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f), modifier = Modifier.size(secondaryButtonSize * 0.8f))
        }

        IconButton(onClick = { MusicPlayerState.togglePlayPause() }, modifier = Modifier.size(mainButtonSize)) {
            Icon(
                imageVector = if (MusicPlayerState.isPlaying) Icons.Default.PauseCircleFilled else Icons.Default.PlayCircleFilled,
                contentDescription = "Play/Pause",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(mainButtonSize)
            )
        }

        IconButton(onClick = { MusicPlayerState.nextTrack() }, modifier = Modifier.size(secondaryButtonSize)) {
            Icon(Icons.Default.SkipNext, "Next", tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f), modifier = Modifier.size(secondaryButtonSize * 0.8f))
        }
    }
}

// 2. Виджет для Главного экрана (Домой)
@Composable
fun HomeMusicWidget(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp).fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Музыка", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), fontSize = 18.sp)

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(90.dp).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.background),
                    contentAlignment = Alignment.Center
                ) {
                    if (MusicPlayerState.currentAlbumArt != null) {
                        Image(bitmap = MusicPlayerState.currentAlbumArt!!.asImageBitmap(), contentDescription = "Cover", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    } else {
                        Icon(Icons.Default.MusicNote, "Cover", modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(MusicPlayerState.currentTitle, fontSize = 32.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
                    Text(MusicPlayerState.currentArtist, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f), maxLines = 1)
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Slider(
                value = MusicPlayerState.currentPosition.toFloat(),
                onValueChange = { MusicPlayerState.seekTo(it.toInt()) },
                valueRange = 0f..(if (MusicPlayerState.totalDuration > 0) MusicPlayerState.totalDuration.toFloat() else 1f),
                modifier = Modifier.height(40.dp),
                colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.primary, inactiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(formatTime(MusicPlayerState.currentPosition), fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                Text(formatTime(MusicPlayerState.totalDuration), fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            }

            Spacer(modifier = Modifier.height(16.dp))
            PlayerControls(modifier = Modifier.fillMaxWidth())
        }
    }
}

// 3. Панель для экрана Навигатора
@Composable
fun NavPlayerPanel(modifier: Modifier = Modifier) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center
            ) {
                if (MusicPlayerState.currentAlbumArt != null) {
                    Image(bitmap = MusicPlayerState.currentAlbumArt!!.asImageBitmap(), contentDescription = "Album Art", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                } else {
                    Icon(Icons.Default.MusicNote, "Album Art", modifier = Modifier.size(80.dp), tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f))
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(MusicPlayerState.currentTitle, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
            Text(MusicPlayerState.currentArtist, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), maxLines = 1)
            Spacer(modifier = Modifier.height(16.dp))
            PlayerControls(modifier = Modifier.fillMaxWidth())
        }
    }
}

// 4. Полноэкранный плеер (для вкладки Музыки)
@Composable
fun FullPlayerContent(onPickFile: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val dateFormat = SimpleDateFormat("EEEE, d MMMM", Locale.getDefault())
            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            val now = Date()
            Text(dateFormat.format(now).replaceFirstChar { it.uppercase() }, fontSize = 24.sp, color = MaterialTheme.colorScheme.onBackground)

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(timeFormat.format(now), fontSize = 24.sp, color = MaterialTheme.colorScheme.onBackground)
                Spacer(modifier = Modifier.width(16.dp))
                IconButton(onClick = onPickFile) {
                    Icon(Icons.Default.FolderOpen, contentDescription = "Выбрать файл", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }

        Box(
            modifier = Modifier.size(300.dp).clip(RoundedCornerShape(32.dp)).background(MaterialTheme.colorScheme.surface),
            contentAlignment = Alignment.Center
        ) {
            if (MusicPlayerState.currentAlbumArt != null) {
                Image(bitmap = MusicPlayerState.currentAlbumArt!!.asImageBitmap(), contentDescription = "Cover", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            } else {
                Icon(Icons.Default.MusicNote, "Cover", modifier = Modifier.size(120.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(MusicPlayerState.currentTitle, fontSize = 48.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground, maxLines = 1)
            Text(MusicPlayerState.currentArtist, fontSize = 24.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f), maxLines = 1)
        }

        Column(modifier = Modifier.fillMaxWidth(0.8f)) {
            Slider(
                value = MusicPlayerState.currentPosition.toFloat(),
                onValueChange = { MusicPlayerState.seekTo(it.toInt()) },
                valueRange = 0f..(if (MusicPlayerState.totalDuration > 0) MusicPlayerState.totalDuration.toFloat() else 1f),
                modifier = Modifier.height(40.dp),
                colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.primary, inactiveTrackColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f))
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(formatTime(MusicPlayerState.currentPosition), fontSize = 20.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
                Text(formatTime(MusicPlayerState.totalDuration), fontSize = 20.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
            }
        }

        PlayerControls(modifier = Modifier.fillMaxWidth(0.6f), largeIcons = true)
    }
}