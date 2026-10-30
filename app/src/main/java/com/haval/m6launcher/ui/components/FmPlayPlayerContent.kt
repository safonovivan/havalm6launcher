package com.haval.m6launcher.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.haval.m6launcher.data.FmPlayApi
import com.haval.m6launcher.data.FmPlayStation
import com.haval.m6launcher.data.MusicPlayerState
import com.haval.m6launcher.data.UserSettings
import kotlinx.coroutines.launch

@Composable
fun FmPlayPlayerView(modifier: Modifier = Modifier, largeIcons: Boolean = false) {
    val context = LocalContext.current
    val station = MusicPlayerState.currentRadioStation
    val mainButtonSize = if (largeIcons) 100.dp else 80.dp
    val secondaryButtonSize = if (largeIcons) 64.dp else 56.dp

    Column(
        modifier = modifier.fillMaxSize(),
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
                    .clip(RoundedCornerShape(if (largeIcons) 32.dp else 16.dp))
                    .background(MaterialTheme.colorScheme.background)
                    // ИСПРАВЛЕНО: Добавили рамку для большой обложки
                    .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), RoundedCornerShape(if (largeIcons) 32.dp else 16.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (station != null) {
                    AsyncImage(
                        model = station.logoUrl,
                        contentDescription = station.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(Icons.Default.Radio, "Radio", modifier = Modifier.fillMaxSize(0.5f), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                }
            }
        }

        Text(
            text = station?.name ?: "Выберите станцию",
            fontSize = if (largeIcons) 32.sp else 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Text(
            text = "Интернет-радио",
            fontSize = if (largeIcons) 18.sp else 14.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
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

        if (MusicPlayerState.playbackError != null) {
            Text(
                text = MusicPlayerState.playbackError!!,
                color = MaterialTheme.colorScheme.error,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
fun FmPlayRadioList(
    modifier: Modifier = Modifier,
    settings: UserSettings,
    onSettingsChange: (UserSettings) -> Unit,
    onStationClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var stations by remember { mutableStateOf<List<FmPlayStation>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showOnlyFavorites by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val gridState = rememberLazyGridState()
    val listState = rememberLazyListState()

    fun loadStations(forceRefresh: Boolean = false) {
        isLoading = true
        errorMessage = null
        if (forceRefresh) FmPlayApi.clearCache()
        scope.launch {
            val (fetchedStations, error) = FmPlayApi.fetchStations(context)
            stations = fetchedStations
            errorMessage = error
            isLoading = false
        }
    }

    LaunchedEffect(Unit) { if (stations.isEmpty()) loadStations() }

    val favorites = stations.filter { settings.favoriteRadioIds.contains(it.id) }
    val displayedStations = if (showOnlyFavorites) favorites else stations

    val currentStationId = MusicPlayerState.currentRadioStation?.id
    LaunchedEffect(currentStationId, showOnlyFavorites, displayedStations) {
        if (currentStationId != null) {
            val index = displayedStations.indexOfFirst { it.id == currentStationId }
            if (index >= 0) {
                if (showOnlyFavorites) gridState.animateScrollToItem(index)
                else listState.animateScrollToItem(index)
            }
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f).background(MaterialTheme.colorScheme.background, RoundedCornerShape(8.dp)).padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Button(
                    onClick = { showOnlyFavorites = true },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (showOnlyFavorites) MaterialTheme.colorScheme.surface else Color.Transparent,
                        contentColor = if (showOnlyFavorites) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                ) { Text("Избранное") }

                Button(
                    onClick = { showOnlyFavorites = false },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (!showOnlyFavorites) MaterialTheme.colorScheme.surface else Color.Transparent,
                        contentColor = if (!showOnlyFavorites) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                ) { Text("Все станции") }
            }

            IconButton(onClick = { loadStations(forceRefresh = true) }, enabled = !isLoading) {
                if (isLoading) CircularProgressIndicator(modifier = Modifier.size(24.dp))
                else Icon(Icons.Default.Refresh, "Обновить", tint = MaterialTheme.colorScheme.primary)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else if (errorMessage != null) {
            Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Text("Ошибка загрузки!", color = MaterialTheme.colorScheme.error, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Text(errorMessage!!, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { loadStations(forceRefresh = true) }) { Text("Попробовать снова") }
            }
        } else if (displayedStations.isEmpty()) {
            Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Text(if (showOnlyFavorites) "Нет избранных станций" else "Список пуст", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                if (!showOnlyFavorites) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { loadStations(forceRefresh = true) }) { Text("Загрузить заново") }
                }
            }
        } else {
            if (showOnlyFavorites) {
                LazyVerticalGrid(
                    state = gridState,
                    columns = GridCells.Adaptive(140.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(displayedStations) { station ->
                        val isCurrent = MusicPlayerState.currentRadioStation?.id == station.id
                        Column(
                            modifier = Modifier.clickable {
                                MusicPlayerState.playRadioStream(context, station, displayedStations)
                                onStationClick()
                            },
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            AsyncImage(
                                model = station.logoUrl,
                                contentDescription = station.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(16.dp))
                                    // ИСПРАВЛЕНО: Фон изменили на surface (чтобы сочетался с рамкой) и добавили рамку
                                    .background(if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface)
                                    .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                station.name,
                                fontSize = 16.sp,
                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center,
                                color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(displayedStations) { station ->
                        val isFavorite = settings.favoriteRadioIds.contains(station.id)
                        val isCurrent = MusicPlayerState.currentRadioStation?.id == station.id
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable {
                                MusicPlayerState.playRadioStream(context, station, displayedStations)
                                onStationClick()
                            }.background(if (isCurrent) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else Color.Transparent).padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = station.logoUrl,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    // ИСПРАВЛЕНО: Добавили рамку для маленьких иконок в списке
                                    .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(station.name, modifier = Modifier.weight(1f), fontSize = 16.sp, color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)

                            IconButton(onClick = {
                                val newFavs = if (isFavorite) settings.favoriteRadioIds - station.id else settings.favoriteRadioIds + station.id
                                onSettingsChange(settings.copy(favoriteRadioIds = newFavs))
                            }) {
                                Icon(if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder, "Like", tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
                            }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f))
                    }
                }
            }
        }
    }
}