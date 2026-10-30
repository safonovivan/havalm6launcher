package com.haval.m6launcher.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateListOf
import androidx.documentfile.provider.DocumentFile
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.nio.charset.Charset

enum class PlayerType { LOCAL, YANDEX, RADIO, FMPLAY }

data class Track(
    val uri: Uri,
    val title: String,
    val artist: String,
    val albumArt: Bitmap?
)

object MusicPlayerState {
    var activePlayer by mutableStateOf(PlayerType.LOCAL)
    var isPlaying by mutableStateOf(false)

    var currentTitle by mutableStateOf("Нажмите папку, чтобы выбрать MP3")
    var currentArtist by mutableStateOf("Неизвестный исполнитель")
    var currentAlbumArt by mutableStateOf<Bitmap?>(null)
    var currentPosition by mutableStateOf(0)
    var totalDuration by mutableStateOf(0)

    var currentRadioStation by mutableStateOf<FmPlayStation?>(null)
    var currentRadioQueue = mutableStateListOf<FmPlayStation>()
    var currentRadioIndex by mutableStateOf(-1)
    var playbackError by mutableStateOf<String?>(null)

    val playlist = mutableStateListOf<Track>()
    var currentIndex by mutableStateOf(-1)

    private var exoPlayer: ExoPlayer? = null

    fun savePlaybackStateOnStop(context: Context) {
        if (activePlayer == PlayerType.LOCAL && currentIndex != -1) {
            val pos = exoPlayer?.currentPosition?.toInt() ?: 0
            val repo = SettingsRepository(context.applicationContext)
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val settings = repo.userSettings.first()
                    repo.updateSettings(settings.copy(lastTrackIndex = currentIndex, lastTrackPosition = pos))
                } catch (e: Exception) { }
            }
        }
    }

    suspend fun restoreState(context: Context, settingsRepo: SettingsRepository) {
        val settings = settingsRepo.userSettings.first()

        // 1. ЗАГРУЖАЕМ ЛОКАЛЬНЫЙ ПЛЕЙЛИСТ
        if (settings.lastFolderUri.isNotBlank()) {
            val documentFile = DocumentFile.fromTreeUri(context, Uri.parse(settings.lastFolderUri))
            if (documentFile != null && documentFile.isDirectory) {
                val newTracks = mutableListOf<Track>()
                scanDirectory(context, documentFile, newTracks)
                if (newTracks.isNotEmpty()) {
                    playlist.clear()
                    playlist.addAll(newTracks)

                    // ВАЖНОЕ ИСПРАВЛЕНИЕ: Устанавливаем индекс трека из настроек сразу при загрузке!
                    val safeIndex = settings.lastTrackIndex.coerceIn(0, playlist.size - 1)
                    currentIndex = safeIndex
                    currentPosition = settings.lastTrackPosition // Восстанавливаем и секунду

                    // Обновляем данные для UI
                    val track = playlist[safeIndex]
                    currentTitle = track.title
                    currentArtist = track.artist
                    currentAlbumArt = track.albumArt
                }
            }
        }

        // 2. ЗАГРУЖАЕМ РАДИО
        val (stations, _) = FmPlayApi.fetchStations(context)
        if (stations.isNotEmpty()) {
            val favorites = stations.filter { settings.favoriteRadioIds.contains(it.id) }
            val savedStation = stations.find { it.id == settings.lastRadioStationId }
            val stationToPlay = if (favorites.any { it.id == savedStation?.id }) savedStation else favorites.firstOrNull() ?: stations.first()

            currentRadioStation = stationToPlay
            currentRadioQueue.clear()
            currentRadioQueue.addAll(stations)
            currentRadioIndex = stations.indexOf(stationToPlay)
        }

        // 3. ВОССТАНАВЛИВАЕМ АКТИВНЫЙ ПЛЕЕР
        activePlayer = try { PlayerType.valueOf(settings.lastActivePlayer) } catch (e: Exception) { PlayerType.LOCAL }

        if (activePlayer == PlayerType.LOCAL && playlist.isNotEmpty()) {
            if (settings.isAutoPlayEnabled) {
                playTrack(context, currentIndex, currentPosition, autoPlay = true)
            }
        } else if (activePlayer == PlayerType.FMPLAY && currentRadioStation != null) {
            if (settings.isAutoPlayEnabled) {
                playRadioStream(context, currentRadioStation!!, currentRadioQueue.toList(), autoPlay = true)
            }
        }
    }

    fun switchPlayer(context: Context, newPlayer: PlayerType) {
        if (activePlayer == newPlayer) return

        val repo = SettingsRepository(context.applicationContext)

        if (activePlayer == PlayerType.LOCAL && currentIndex != -1) {
            val pos = exoPlayer?.currentPosition?.toInt() ?: 0
            currentPosition = pos
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val settings = repo.userSettings.first()
                    repo.updateSettings(settings.copy(lastTrackIndex = currentIndex, lastTrackPosition = pos))
                } catch (e: Exception) { }
            }
        }

        if (exoPlayer?.isPlaying == true) exoPlayer?.pause()
        activePlayer = newPlayer

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val settings = repo.userSettings.first()
                repo.updateSettings(settings.copy(
                    lastActivePlayer = newPlayer.name,
                    lastRadioStationId = if (newPlayer == PlayerType.FMPLAY) currentRadioStation?.id ?: "" else settings.lastRadioStationId
                ))
            } catch (e: Exception) { }
        }

        when (newPlayer) {
            PlayerType.LOCAL -> {
                if (playlist.isNotEmpty()) {
                    // Используем сохраненный currentIndex (который мы восстановили при старте)
                    val safeIndex = if (currentIndex in 0 until playlist.size) currentIndex else 0
                    currentIndex = safeIndex
                    val track = playlist[safeIndex]
                    currentTitle = track.title
                    currentArtist = track.artist
                    currentAlbumArt = track.albumArt
                    // Воспроизводим с сохраненной позиции
                    playTrack(context, safeIndex, currentPosition, autoPlay = true)
                } else {
                    currentTitle = "Нажмите папку, чтобы выбрать MP3"
                    currentArtist = "Неизвестный исполнитель"
                    currentAlbumArt = null
                }
            }
            PlayerType.FMPLAY -> {
                if (currentRadioStation != null) {
                    currentTitle = currentRadioStation!!.name
                    currentArtist = "Интернет-радио"
                    currentAlbumArt = null
                    val queue = if (currentRadioQueue.isNotEmpty()) currentRadioQueue.toList() else listOf(currentRadioStation!!)
                    playRadioStream(context, currentRadioStation!!, queue, autoPlay = true)
                } else {
                    currentTitle = "Выберите станцию"
                    currentArtist = "Интернет-радио"
                    currentAlbumArt = null
                }
            }
            else -> {}
        }
    }

    fun togglePlayPause(context: Context) {
        if (isPlaying) {
            val currentPos = exoPlayer?.currentPosition?.toInt() ?: 0
            exoPlayer?.pause()

            if (activePlayer == PlayerType.LOCAL && currentIndex != -1) {
                val repo = SettingsRepository(context.applicationContext)
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val settings = repo.userSettings.first()
                        repo.updateSettings(settings.copy(lastTrackIndex = currentIndex, lastTrackPosition = currentPos))
                    } catch (e: Exception) { }
                }
            }
        } else {
            if (activePlayer == PlayerType.FMPLAY) {
                if (currentRadioStation != null) {
                    val queue = if (currentRadioQueue.isNotEmpty()) currentRadioQueue.toList() else listOf(currentRadioStation!!)
                    playRadioStream(context, currentRadioStation!!, queue, autoPlay = true)
                }
            } else if (activePlayer == PlayerType.LOCAL) {
                if (playlist.isNotEmpty()) {
                    val safeIndex = if (currentIndex in 0 until playlist.size) currentIndex else 0
                    playTrack(context, safeIndex, currentPosition, autoPlay = true)
                }
            }
        }
    }

    private fun initExoPlayer(context: Context) {
        if (exoPlayer == null) {
            exoPlayer = ExoPlayer.Builder(context).build().apply {
                addListener(object : Player.Listener {
                    override fun onIsPlayingChanged(isPlayingNow: Boolean) { MusicPlayerState.isPlaying = isPlayingNow }
                    override fun onPlaybackStateChanged(playbackState: Int) { if (playbackState == Player.STATE_ENDED) nextTrack(context) }
                    override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                        MusicPlayerState.playbackError = "Ошибка ExoPlayer: ${error.errorCodeName}"
                        MusicPlayerState.isPlaying = false
                    }
                })
            }
        }
    }

    fun playRadioStream(context: Context, station: FmPlayStation, queue: List<FmPlayStation>, autoPlay: Boolean = true) {
        initExoPlayer(context)
        playbackError = null
        activePlayer = PlayerType.FMPLAY

        val repo = SettingsRepository(context.applicationContext)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val settings = repo.userSettings.first()
                repo.updateSettings(settings.copy(lastActivePlayer = "FMPLAY", lastRadioStationId = station.id))
            } catch (e: Exception) { }
        }

        val safeQueue = queue.toList()
        if (safeQueue.isNotEmpty()) {
            currentRadioQueue.clear()
            currentRadioQueue.addAll(safeQueue)
            currentRadioIndex = safeQueue.indexOfFirst { it.id == station.id }.coerceAtLeast(0)
        } else {
            currentRadioQueue.clear()
            currentRadioQueue.add(station)
            currentRadioIndex = 0
        }

        currentRadioStation = station
        currentTitle = station.name
        currentArtist = "Интернет-радио"
        currentAlbumArt = null
        currentPosition = 0
        totalDuration = 0

        exoPlayer?.apply {
            stop()
            clearMediaItems()
            setMediaItem(MediaItem.fromUri(station.streamUrl))
            prepare()
            playWhenReady = autoPlay
        }
    }

    fun loadTracksFromFolder(context: Context, treeUri: Uri, autoPlay: Boolean = true) {
        val documentFile = DocumentFile.fromTreeUri(context, treeUri)
        if (documentFile != null && documentFile.isDirectory) {
            val newTracks = mutableListOf<Track>()
            scanDirectory(context, documentFile, newTracks)
            if (newTracks.isNotEmpty()) {
                playlist.clear()
                playlist.addAll(newTracks)
                currentIndex = 0
                currentPosition = 0

                val repo = SettingsRepository(context.applicationContext)
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val settings = repo.userSettings.first()
                        repo.updateSettings(settings.copy(lastActivePlayer = "LOCAL", lastFolderUri = treeUri.toString(), lastTrackIndex = 0, lastTrackPosition = 0))
                    } catch (e: Exception) { }
                }

                if (autoPlay) {
                    playTrack(context, 0, 0, autoPlay = true)
                } else {
                    val track = playlist[0]
                    currentTitle = track.title
                    currentArtist = track.artist
                    currentAlbumArt = track.albumArt
                }
            }
        }
    }

    private fun scanDirectory(context: Context, dir: DocumentFile, tracks: MutableList<Track>) {
        val files = dir.listFiles()
        for (file in files) {
            if (file.isDirectory) {
                scanDirectory(context, file, tracks)
            } else if (file.isFile) {
                val rawName = file.name ?: ""
                val mime = file.type ?: ""
                if (mime.startsWith("audio/") || rawName.endsWith(".mp3", true) || rawName.endsWith(".flac", true) || rawName.endsWith(".wav", true)) {
                    val uri = file.uri
                    val retriever = MediaMetadataRetriever()

                    var title = ""
                    var artist = ""
                    var art: Bitmap? = null

                    try {
                        retriever.setDataSource(context, uri)
                        // ИСПРАВЛЕНИЕ: Обрезаем невидимые пробелы с помощью .trim()
                        title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)?.trim() ?: ""
                        artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)?.trim() ?: ""
                        val artBytes = retriever.embeddedPicture
                        if (artBytes != null) art = BitmapFactory.decodeByteArray(artBytes, 0, artBytes.size)
                    } catch (e: Exception) {} finally { retriever.release() }

                    // УМНЫЙ ПАРСЕР
                    if (title.isBlank()) {
                        val nameWithoutExt = Uri.decode(rawName).substringBeforeLast(".").trim()
                        if (nameWithoutExt.contains(" - ")) {
                            val parts = nameWithoutExt.split(" - ", limit = 2)
                            artist = parts[0].trim()
                            title = parts[1].trim()
                        } else {
                            title = nameWithoutExt
                        }
                    }

                    if (artist.isBlank() || artist.equals("<unknown>", ignoreCase = true)) {
                        artist = "Неизвестный исполнитель"
                    }

                    title = fixEncoding(title).trim()
                    artist = fixEncoding(artist).trim()

                    tracks.add(Track(uri, title, artist, art))
                }
            }
        }
    }

    fun playTrack(context: Context, index: Int, startPosition: Int = 0, autoPlay: Boolean = true) {
        if (index < 0 || index >= playlist.size) return
        initExoPlayer(context)
        currentIndex = index
        val track = playlist[index]
        currentTitle = track.title
        currentArtist = track.artist
        currentAlbumArt = track.albumArt
        activePlayer = PlayerType.LOCAL

        currentPosition = startPosition

        val repo = SettingsRepository(context.applicationContext)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val settings = repo.userSettings.first()
                repo.updateSettings(settings.copy(lastTrackIndex = index, lastTrackPosition = startPosition))
            } catch (e: Exception) { }
        }

        exoPlayer?.apply {
            stop()
            clearMediaItems()
            setMediaItem(MediaItem.fromUri(track.uri))
            prepare()
            if (startPosition > 0) seekTo(startPosition.toLong())
            playWhenReady = autoPlay
        }
    }

    fun nextTrack(context: Context) {
        if (activePlayer == PlayerType.FMPLAY) {
            if (currentRadioQueue.isNotEmpty() && currentRadioIndex != -1) {
                val nextIdx = (currentRadioIndex + 1) % currentRadioQueue.size
                playRadioStream(context, currentRadioQueue[nextIdx], currentRadioQueue.toList())
            }
            return
        }
        if (playlist.isNotEmpty()) {
            val nextIndex = (currentIndex + 1) % playlist.size
            playTrack(context, nextIndex, 0)
        }
    }

    fun previousTrack(context: Context) {
        if (activePlayer == PlayerType.FMPLAY) {
            if (currentRadioQueue.isNotEmpty() && currentRadioIndex != -1) {
                val prevIdx = if (currentRadioIndex - 1 < 0) currentRadioQueue.size - 1 else currentRadioIndex - 1
                playRadioStream(context, currentRadioQueue[prevIdx], currentRadioQueue.toList())
            }
            return
        }
        if (playlist.isNotEmpty()) {
            val prevIndex = if (currentIndex - 1 < 0) playlist.size - 1 else currentIndex - 1
            playTrack(context, prevIndex, 0)
        }
    }

    fun updatePosition() {
        exoPlayer?.let { player ->
            if (player.isPlaying && activePlayer != PlayerType.FMPLAY) {
                currentPosition = player.currentPosition.toInt()
                totalDuration = player.duration.toInt()
            }
        }
    }

    fun seekTo(position: Int) {
        if (activePlayer == PlayerType.FMPLAY) return
        exoPlayer?.seekTo(position.toLong())
        currentPosition = position
    }

    private fun fixEncoding(text: String?): String {
        if (text.isNullOrBlank()) return ""
        if (text.any { it in '\u0400'..'\u04FF' }) return text
        return try {
            val bytes = text.toByteArray(Charsets.ISO_8859_1)
            String(bytes, Charset.forName("windows-1251"))
        } catch (e: Exception) { text }
    }
}