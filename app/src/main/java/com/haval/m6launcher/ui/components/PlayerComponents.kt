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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.haval.m6launcher.data.MusicPlayerState
import com.haval.m6launcher.data.Track
import kotlinx.coroutines.delay
import java.util.Locale

fun formatTime(ms: Int): String {
    if (ms <= 0) return "0:00"
    val seconds = (ms / 1000) % 60
    val minutes = (ms / (1000 * 60)) % 60
    return String.format(Locale.getDefault(), "%d:%02d", minutes, seconds)
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
        IconButton(onClick = { }, modifier = Modifier.size(secondaryButtonSize)) {
            Icon(Icons.Default.FavoriteBorder, "Like", tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f), modifier = Modifier.size(secondaryButtonSize * 0.8f))
        }

        IconButton(onClick = { MusicPlayerState.previousTrack(context) }, modifier = Modifier.size(secondaryButtonSize)) {
            Icon(Icons.Default.SkipPrevious, "Previous", tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f), modifier = Modifier.size(secondaryButtonSize * 0.8f))
        }

        IconButton(onClick = { MusicPlayerState.togglePlayPause() }, modifier = Modifier.size(mainButtonSize)) {
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
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .background(if (isCurrent) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else androidx.compose.ui.graphics.Color.Transparent)
            .padding(12.dp),
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
        if (isCurrent && isPlaying) {
            Icon(Icons.Default.PlayArrow, contentDescription = "Playing", tint = MaterialTheme.colorScheme.primary)
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f))
}

@Composable
fun PlaylistToggleButton(showPlaylist: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
            contentColor = MaterialTheme.colorScheme.primary
        ),
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Icon(
            imageVector = if (showPlaylist) Icons.AutoMirrored.Filled.ArrowBack else Icons.Default.PlaylistPlay,
            contentDescription = "Playlist Toggle",
            modifier = Modifier.size(28.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = if (showPlaylist) "Назад" else "Плейлист",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun PlaylistActionButtons(onScanMedia: () -> Unit, onPickFolder: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth().padding(bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(onClick = onScanMedia, modifier = Modifier.weight(1f), contentPadding = PaddingValues(4.dp)) {
            Icon(Icons.Default.LibraryMusic, null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Скан", fontSize = 12.sp)
        }
        Button(onClick = onPickFolder, modifier = Modifier.weight(1f), contentPadding = PaddingValues(4.dp)) {
            Icon(Icons.Default.Folder, null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Папка", fontSize = 12.sp)
        }
    }
}

@Composable
fun HomeMusicWidget(
    modifier: Modifier = Modifier,
    onScanMedia: () -> Unit,
    onPickFolder: () -> Unit
) {
    val context = LocalContext.current
    var showPlaylist by remember { mutableStateOf(false) }

    // НОВОЕ: Состояние списка для автоскролла
    val listState = rememberLazyListState()

    // НОВОЕ: Следим за открытием списка и сменой трека
    LaunchedEffect(showPlaylist, MusicPlayerState.currentIndex) {
        if (showPlaylist && MusicPlayerState.currentIndex >= 0) {
            listState.animateScrollToItem(MusicPlayerState.currentIndex)
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            MusicPlayerState.updatePosition()
            delay(1000L)
        }
    }

    Card(modifier = modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(24.dp)) {
        Column(
            modifier = Modifier.padding(16.dp).fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(MusicPlayerState.currentSource, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), fontSize = 18.sp)
                PlaylistToggleButton(showPlaylist = showPlaylist, onClick = { showPlaylist = !showPlaylist })
            }

            if (!showPlaylist) {
                Column(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceEvenly
                ) {
                    Box(
                        modifier = Modifier.weight(1f).fillMaxWidth().padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .aspectRatio(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.background),
                            contentAlignment = Alignment.Center
                        ) {
                            if (MusicPlayerState.currentAlbumArt != null) {
                                Image(bitmap = MusicPlayerState.currentAlbumArt!!.asImageBitmap(), contentDescription = "Cover", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                            } else {
                                Icon(Icons.Default.MusicNote, "Cover", modifier = Modifier.fillMaxSize(0.5f), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                            }
                        }
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(MusicPlayerState.currentTitle, fontSize = 26.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
                        Text(MusicPlayerState.currentArtist, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f), maxLines = 1)
                    }

                    Column(modifier = Modifier.fillMaxWidth()) {
                        Slider(
                            value = MusicPlayerState.currentPosition.toFloat(),
                            onValueChange = { MusicPlayerState.seekTo(it.toInt()) },
                            valueRange = 0f..(if (MusicPlayerState.totalDuration > 0) MusicPlayerState.totalDuration.toFloat() else 1f),
                            modifier = Modifier.height(32.dp)
                        )
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(formatTime(MusicPlayerState.currentPosition), fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                            Text(formatTime(MusicPlayerState.totalDuration), fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    PlayerControls(modifier = Modifier.fillMaxWidth())
                }
            } else {
                Column(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    PlaylistActionButtons(onScanMedia = onScanMedia, onPickFolder = onPickFolder)

                    if (MusicPlayerState.playlist.isEmpty()) {
                        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text("Плейлист пуст", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        }
                    } else {
                        // НОВОЕ: Привязали listState
                        LazyColumn(state = listState, modifier = Modifier.weight(1f).fillMaxWidth()) {
                            itemsIndexed(MusicPlayerState.playlist) { index, track ->
                                TrackItem(
                                    track = track,
                                    isCurrent = index == MusicPlayerState.currentIndex,
                                    isPlaying = MusicPlayerState.isPlaying,
                                    onClick = {
                                        MusicPlayerState.playTrack(context, index)
                                        showPlaylist = false
                                    }
                                )
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
    onScanMedia: () -> Unit,
    onPickFolder: () -> Unit
) {
    val context = LocalContext.current
    var showPlaylist by remember { mutableStateOf(false) }

    // НОВОЕ: Состояние списка для автоскролла
    val listState = rememberLazyListState()

    // НОВОЕ: Следим за открытием списка и сменой трека
    LaunchedEffect(showPlaylist, MusicPlayerState.currentIndex) {
        if (showPlaylist && MusicPlayerState.currentIndex >= 0) {
            listState.animateScrollToItem(MusicPlayerState.currentIndex)
        }
    }

    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(
            modifier = Modifier.padding(16.dp).fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(MusicPlayerState.currentSource, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), fontSize = 16.sp)
                PlaylistToggleButton(showPlaylist = showPlaylist, onClick = { showPlaylist = !showPlaylist })
            }

            if (!showPlaylist) {
                Column(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceEvenly
                ) {
                    Box(
                        modifier = Modifier.weight(1f).fillMaxWidth().padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .aspectRatio(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.background),
                            contentAlignment = Alignment.Center
                        ) {
                            if (MusicPlayerState.currentAlbumArt != null) {
                                Image(bitmap = MusicPlayerState.currentAlbumArt!!.asImageBitmap(), contentDescription = "Album Art", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                            } else {
                                Icon(Icons.Default.MusicNote, "Album Art", modifier = Modifier.fillMaxSize(0.5f), tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f))
                            }
                        }
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(MusicPlayerState.currentTitle, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
                        Text(MusicPlayerState.currentArtist, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), maxLines = 1)
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    PlayerControls(modifier = Modifier.fillMaxWidth())
                }
            } else {
                Column(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    PlaylistActionButtons(onScanMedia = onScanMedia, onPickFolder = onPickFolder)

                    if (MusicPlayerState.playlist.isEmpty()) {
                        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text("Плейлист пуст", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        }
                    } else {
                        // НОВОЕ: Привязали listState
                        LazyColumn(state = listState, modifier = Modifier.weight(1f).fillMaxWidth()) {
                            itemsIndexed(MusicPlayerState.playlist) { index, track ->
                                TrackItem(
                                    track = track,
                                    isCurrent = index == MusicPlayerState.currentIndex,
                                    isPlaying = MusicPlayerState.isPlaying,
                                    onClick = {
                                        MusicPlayerState.playTrack(context, index)
                                        showPlaylist = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FullPlayerContent(onScanMedia: () -> Unit, onPickFolder: () -> Unit) {
    val context = LocalContext.current
    var activeTab by remember { mutableStateOf(0) }

    val listState = rememberLazyListState()

    LaunchedEffect(MusicPlayerState.currentIndex) {
        if (MusicPlayerState.currentIndex >= 0) {
            listState.animateScrollToItem(MusicPlayerState.currentIndex)
        }
    }

    Row(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Column(
            modifier = Modifier.weight(1f).fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                Text(MusicPlayerState.currentSource, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f), fontSize = 20.sp)
            }

            Box(
                modifier = Modifier.weight(1f).fillMaxWidth().padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(32.dp))
                        .background(MaterialTheme.colorScheme.surface),
                    contentAlignment = Alignment.Center
                ) {
                    if (MusicPlayerState.currentAlbumArt != null) {
                        Image(bitmap = MusicPlayerState.currentAlbumArt!!.asImageBitmap(), contentDescription = "Cover", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    } else {
                        Icon(Icons.Default.MusicNote, "Cover", modifier = Modifier.fillMaxSize(0.5f), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                    }
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(MusicPlayerState.currentTitle, fontSize = 36.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(MusicPlayerState.currentArtist, fontSize = 20.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f), maxLines = 1)
            }

            Column(modifier = Modifier.fillMaxWidth()) {
                Slider(
                    value = MusicPlayerState.currentPosition.toFloat(),
                    onValueChange = { MusicPlayerState.seekTo(it.toInt()) },
                    valueRange = 0f..(if (MusicPlayerState.totalDuration > 0) MusicPlayerState.totalDuration.toFloat() else 1f)
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(formatTime(MusicPlayerState.currentPosition), fontSize = 16.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
                    Text(formatTime(MusicPlayerState.totalDuration), fontSize = 16.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
                }
            }

            PlayerControls(modifier = Modifier.fillMaxWidth(), largeIcons = true)
        }

        Card(
            modifier = Modifier.weight(1f).fillMaxHeight(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {

                Row(
                    modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background, RoundedCornerShape(8.dp)).padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Button(
                        onClick = { activeTab = 0 },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (activeTab == 0) MaterialTheme.colorScheme.surface else androidx.compose.ui.graphics.Color.Transparent,
                            contentColor = if (activeTab == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        ),
                        elevation = if (activeTab == 0) ButtonDefaults.buttonElevation(defaultElevation = 2.dp) else null
                    ) {
                        Text("Медиатека")
                    }
                    Button(
                        onClick = { activeTab = 1 },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (activeTab == 1) MaterialTheme.colorScheme.surface else androidx.compose.ui.graphics.Color.Transparent,
                            contentColor = if (activeTab == 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        ),
                        elevation = if (activeTab == 1) ButtonDefaults.buttonElevation(defaultElevation = 2.dp) else null
                    ) {
                        Text("Папка")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (activeTab == 0) {
                    Button(onClick = onScanMedia, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.LibraryMusic, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Сканировать память")
                    }
                } else {
                    Button(onClick = onPickFolder, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Folder, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Выбрать папку")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text("Плейлист (${MusicPlayerState.playlist.size})", fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                Spacer(modifier = Modifier.height(8.dp))

                if (MusicPlayerState.playlist.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Список пуст.\nВыберите действие выше.", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    }
                } else {
                    LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                        itemsIndexed(MusicPlayerState.playlist) { index, track ->
                            TrackItem(
                                track = track,
                                isCurrent = index == MusicPlayerState.currentIndex,
                                isPlaying = MusicPlayerState.isPlaying,
                                onClick = { MusicPlayerState.playTrack(context, index) }
                            )
                        }
                    }
                }
            }
        }
    }
}