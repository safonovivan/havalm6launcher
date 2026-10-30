package com.haval.m6launcher.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay // <-- ДОБАВЛЕН ИМПОРТ
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.haval.m6launcher.data.MusicPlayerState
import com.haval.m6launcher.data.PlayerType
import com.haval.m6launcher.data.Track
import com.haval.m6launcher.data.UserSettings
import java.util.Locale

fun formatTime(ms: Int): String {
    if (ms <= 0) return "0:00"
    val seconds = (ms / 1000) % 60
    val minutes = (ms / (1000 * 60)) % 60
    return String.format(Locale.getDefault(), "%d:%02d", minutes, seconds)
}

@Composable
fun PlayerSwitcherButtons(
    modifier: Modifier = Modifier,
    onPlayerSwitch: () -> Unit = {}
) {
    val context = LocalContext.current

    Column(
        modifier = modifier.fillMaxHeight(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val buttonSize = 56.dp
        val buttons = listOf(PlayerType.LOCAL, PlayerType.YANDEX, PlayerType.RADIO, PlayerType.FMPLAY)

        buttons.forEach { type ->
            IconButton(
                onClick = {
                    MusicPlayerState.switchPlayer(context, type)
                    onPlayerSwitch()
                },
                modifier = Modifier.size(buttonSize).clip(RoundedCornerShape(12.dp)).background(if (MusicPlayerState.activePlayer == type) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color.Transparent)
            ) {
                if (type == PlayerType.YANDEX) {
                    Text("Я", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = if (MusicPlayerState.activePlayer == type) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                } else {
                    val icon = when(type) {
                        PlayerType.LOCAL -> Icons.Default.Folder
                        PlayerType.RADIO -> Icons.Default.Radio
                        PlayerType.FMPLAY -> Icons.Default.Language
                        else -> Icons.Default.MusicNote
                    }
                    Icon(icon, contentDescription = type.name, tint = if (MusicPlayerState.activePlayer == type) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                }
            }
        }
    }
}

@Composable
fun PlayerControls(modifier: Modifier = Modifier, largeIcons: Boolean = false) {
    val context = LocalContext.current
    val mainButtonSize = if (largeIcons) 100.dp else 80.dp
    val secondaryButtonSize = if (largeIcons) 64.dp else 56.dp

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = { MusicPlayerState.previousTrack(context) }, modifier = Modifier.size(secondaryButtonSize)) {
            Icon(Icons.Default.SkipPrevious, "Previous", tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f), modifier = Modifier.size(secondaryButtonSize * 0.8f))
        }
        IconButton(onClick = { MusicPlayerState.togglePlayPause(context) }, modifier = Modifier.size(mainButtonSize)) {
            Icon(
                imageVector = if (MusicPlayerState.isPlaying) Icons.Default.PauseCircleFilled else Icons.Default.PlayCircleFilled,
                contentDescription = "Play/Pause",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(mainButtonSize)
            )
        }
        IconButton(onClick = { MusicPlayerState.nextTrack(context) }, modifier = Modifier.size(secondaryButtonSize)) {
            Icon(Icons.Default.SkipNext, "Next", tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f), modifier = Modifier.size(secondaryButtonSize * 0.8f))
        }
    }
}

@Composable
fun TrackItem(track: Track, isCurrent: Boolean, isPlaying: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.background(if (isCurrent) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else Color.Transparent).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.Center) {
            if (track.albumArt != null) Image(bitmap = track.albumArt.asImageBitmap(), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            else Icon(Icons.Default.MusicNote, null, tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(track.title, fontSize = 16.sp, fontWeight = if(isCurrent) FontWeight.Bold else FontWeight.Normal, color = if(isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(track.artist, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (isCurrent && isPlaying) Icon(Icons.Default.PlayArrow, contentDescription = "Playing", tint = MaterialTheme.colorScheme.primary)
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f))
}

@Composable
fun PlaylistToggleButton(showPlaylist: Boolean, text: String = "Плейлист", onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), contentColor = MaterialTheme.colorScheme.primary),
        shape = RoundedCornerShape(12.dp)
    ) {
        Icon(if (showPlaylist) Icons.AutoMirrored.Filled.ArrowBack else Icons.AutoMirrored.Filled.PlaylistPlay, "Toggle", modifier = Modifier.size(28.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(if (showPlaylist) "Назад" else text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun PlaylistActionButtons(onPickFolder: () -> Unit, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = onPickFolder, modifier = Modifier.weight(1f)) {
            Icon(Icons.Default.Folder, null, Modifier.size(18.dp))
            Spacer(Modifier.width(4.dp))
            Text("Выбрать папку", fontSize = 14.sp)
        }
    }
}

@Composable
fun HomeMusicWidget(
    modifier: Modifier = Modifier,
    settings: UserSettings,
    onSettingsChange: (UserSettings) -> Unit,
    onPickFolder: () -> Unit
) {
    val context = LocalContext.current
    var showPlaylist by remember { mutableStateOf(false) }
    var showRadioList by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    LaunchedEffect(showPlaylist, MusicPlayerState.currentIndex) {
        if (showPlaylist && MusicPlayerState.currentIndex >= 0) listState.animateScrollToItem(MusicPlayerState.currentIndex)
    }

    Card(modifier = modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(24.dp)) {
        Row(modifier = Modifier.padding(16.dp).fillMaxSize()) {
            PlayerSwitcherButtons(
                modifier = Modifier.padding(end = 16.dp),
                onPlayerSwitch = { showPlaylist = false; showRadioList = false }
            )
            Column(modifier = Modifier.weight(1f).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.SpaceBetween) {

                Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                    if (MusicPlayerState.activePlayer == PlayerType.LOCAL) {
                        PlaylistToggleButton(showPlaylist = showPlaylist, text = "Плейлист", onClick = { showPlaylist = !showPlaylist })
                    }
                    if (MusicPlayerState.activePlayer == PlayerType.FMPLAY) {
                        PlaylistToggleButton(showPlaylist = showRadioList, text = "Радиостанции", onClick = { showRadioList = !showRadioList })
                    }
                }

                when(MusicPlayerState.activePlayer) {
                    PlayerType.FMPLAY -> {
                        if (!showRadioList) FmPlayPlayerView(modifier = Modifier.weight(1f))
                        else FmPlayRadioList(modifier = Modifier.weight(1f), settings = settings, onSettingsChange = onSettingsChange, onStationClick = { showRadioList = false })
                    }
                    PlayerType.YANDEX -> YandexPlayerContent(modifier = Modifier.weight(1f))
                    PlayerType.RADIO -> RadioPlayerContent(modifier = Modifier.weight(1f))
                    PlayerType.LOCAL -> {
                        if (!showPlaylist) {
                            LocalPlayerView(modifier = Modifier.weight(1f).fillMaxWidth())
                        } else {
                            Column(modifier = Modifier.weight(1f).fillMaxWidth()) {
                                PlaylistActionButtons(onPickFolder = onPickFolder)
                                if (MusicPlayerState.playlist.isEmpty()) {
                                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { Text("Плейлист пуст", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)) }
                                } else {
                                    LazyColumn(state = listState, modifier = Modifier.weight(1f).fillMaxWidth()) {
                                        itemsIndexed(MusicPlayerState.playlist) { index, track ->
                                            TrackItem(track = track, isCurrent = index == MusicPlayerState.currentIndex, isPlaying = MusicPlayerState.isPlaying, onClick = { MusicPlayerState.playTrack(context, index); showPlaylist = false })
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NavPlayerPanel(
    modifier: Modifier = Modifier,
    settings: UserSettings,
    onSettingsChange: (UserSettings) -> Unit,
    onPickFolder: () -> Unit
) {
    val context = LocalContext.current
    var showPlaylist by remember { mutableStateOf(false) }
    var showRadioList by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    LaunchedEffect(showPlaylist, MusicPlayerState.currentIndex) {
        if (showPlaylist && MusicPlayerState.currentIndex >= 0) listState.animateScrollToItem(MusicPlayerState.currentIndex)
    }

    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(modifier = Modifier.padding(16.dp).fillMaxSize()) {
            PlayerSwitcherButtons(
                modifier = Modifier.padding(end = 12.dp),
                onPlayerSwitch = { showPlaylist = false; showRadioList = false }
            )
            Column(modifier = Modifier.weight(1f).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.SpaceBetween) {

                Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                    if (MusicPlayerState.activePlayer == PlayerType.LOCAL) {
                        PlaylistToggleButton(showPlaylist = showPlaylist, text = "Плейлист", onClick = { showPlaylist = !showPlaylist })
                    }
                    if (MusicPlayerState.activePlayer == PlayerType.FMPLAY) {
                        PlaylistToggleButton(showPlaylist = showRadioList, text = "Радиостанции", onClick = { showRadioList = !showRadioList })
                    }
                }

                when(MusicPlayerState.activePlayer) {
                    PlayerType.FMPLAY -> {
                        if (!showRadioList) FmPlayPlayerView(modifier = Modifier.weight(1f))
                        else FmPlayRadioList(modifier = Modifier.weight(1f), settings = settings, onSettingsChange = onSettingsChange, onStationClick = { showRadioList = false })
                    }
                    PlayerType.YANDEX -> YandexPlayerContent(modifier = Modifier.weight(1f))
                    PlayerType.RADIO -> RadioPlayerContent(modifier = Modifier.weight(1f))
                    PlayerType.LOCAL -> {
                        if (!showPlaylist) {
                            LocalPlayerView(modifier = Modifier.weight(1f).fillMaxWidth())
                        } else {
                            Column(modifier = Modifier.weight(1f).fillMaxWidth()) {
                                PlaylistActionButtons(onPickFolder = onPickFolder)
                                if (MusicPlayerState.playlist.isEmpty()) {
                                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { Text("Плейлист пуст", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)) }
                                } else {
                                    LazyColumn(state = listState, modifier = Modifier.weight(1f).fillMaxWidth()) {
                                        itemsIndexed(MusicPlayerState.playlist) { index, track ->
                                            TrackItem(track = track, isCurrent = index == MusicPlayerState.currentIndex, isPlaying = MusicPlayerState.isPlaying, onClick = { MusicPlayerState.playTrack(context, index); showPlaylist = false })
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FullPlayerContent(
    settings: UserSettings,
    onSettingsChange: (UserSettings) -> Unit,
    onPickFolder: () -> Unit
) {
    val context = LocalContext.current
    val listState = rememberLazyListState()

    LaunchedEffect(MusicPlayerState.currentIndex) {
        if (MusicPlayerState.currentIndex >= 0) listState.animateScrollToItem(MusicPlayerState.currentIndex)
    }

    Row(modifier = Modifier.fillMaxSize().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
        Card(modifier = Modifier.weight(1f).fillMaxHeight(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(24.dp)) {
            Row(modifier = Modifier.padding(16.dp).fillMaxSize()) {
                PlayerSwitcherButtons(modifier = Modifier.padding(end = 16.dp))
                Column(modifier = Modifier.weight(1f).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    when(MusicPlayerState.activePlayer) {
                        PlayerType.FMPLAY -> FmPlayPlayerView(modifier = Modifier.weight(1f), largeIcons = true)
                        PlayerType.YANDEX -> YandexPlayerContent(modifier = Modifier.weight(1f))
                        PlayerType.RADIO -> RadioPlayerContent(modifier = Modifier.weight(1f))
                        PlayerType.LOCAL -> LocalPlayerView(modifier = Modifier.weight(1f).fillMaxWidth(), largeIcons = true)
                    }
                }
            }
        }

        Card(modifier = Modifier.weight(1f).fillMaxHeight(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(24.dp)) {
            when(MusicPlayerState.activePlayer) {
                PlayerType.FMPLAY -> FmPlayRadioList(modifier = Modifier.padding(16.dp), settings = settings, onSettingsChange = onSettingsChange)
                PlayerType.LOCAL -> {
                    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                        Button(onClick = onPickFolder, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Default.Folder, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Выбрать папку с музыкой")
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Плейлист (${MusicPlayerState.playlist.size})", fontSize = 18.sp)
                        Spacer(modifier = Modifier.height(8.dp))

                        if (MusicPlayerState.playlist.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Список пуст.", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)) }
                        } else {
                            LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                                itemsIndexed(MusicPlayerState.playlist) { index, track ->
                                    TrackItem(track = track, isCurrent = index == MusicPlayerState.currentIndex, isPlaying = MusicPlayerState.isPlaying, onClick = { MusicPlayerState.playTrack(context, index) })
                                }
                            }
                        }
                    }
                }
                else -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Список в разработке", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)) }
                }
            }
        }
    }
}