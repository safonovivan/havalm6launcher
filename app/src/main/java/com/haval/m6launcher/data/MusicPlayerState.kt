package com.haval.m6launcher.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.MediaStore
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateListOf
import androidx.documentfile.provider.DocumentFile
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
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

    private fun initExoPlayer(context: Context) {
        if (exoPlayer == null) {
            exoPlayer = ExoPlayer.Builder(context).build().apply {
                addListener(object : Player.Listener {
                    override fun onIsPlayingChanged(isPlayingNow: Boolean) {
                        MusicPlayerState.isPlaying = isPlayingNow
                    }
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        if (playbackState == Player.STATE_ENDED) {
                            nextTrack(context)
                        }
                    }
                    override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                        MusicPlayerState.playbackError = "Ошибка ExoPlayer: ${error.errorCodeName}"
                        MusicPlayerState.isPlaying = false
                    }
                })
            }
        }
    }

    fun playRadioStream(context: Context, station: FmPlayStation, queue: List<FmPlayStation>) {
        initExoPlayer(context)
        playbackError = null
        activePlayer = PlayerType.FMPLAY

        // ИСПРАВЛЕНИЕ БАГА: Делаем копию списка перед очисткой
        val safeQueue = queue.toList()
        currentRadioQueue.clear()
        currentRadioQueue.addAll(safeQueue)

        currentRadioIndex = safeQueue.indexOfFirst { it.id == station.id }

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
            playWhenReady = true
        }
    }

    fun loadDeviceMusic(context: Context) {
        val musicList = mutableListOf<Track>()
        val projection = arrayOf(MediaStore.Audio.Media._ID, MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media.ARTIST)
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val cursor = context.contentResolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, projection, selection, null, "${MediaStore.Audio.Media.TITLE} ASC")

        cursor?.use {
            val idCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)

            while (it.moveToNext()) {
                val id = it.getLong(idCol)
                val title = fixEncoding(it.getString(titleCol) ?: "Без названия")
                val artist = fixEncoding(it.getString(artistCol) ?: "Неизвестен")
                val uri = Uri.withAppendedPath(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id.toString())

                var art: Bitmap? = null
                val retriever = MediaMetadataRetriever()
                try {
                    retriever.setDataSource(context, uri)
                    val artBytes = retriever.embeddedPicture
                    if (artBytes != null) art = BitmapFactory.decodeByteArray(artBytes, 0, artBytes.size)
                } catch (e: Exception) {} finally { retriever.release() }

                musicList.add(Track(uri, title, artist, art))
            }
        }
        playlist.clear()
        playlist.addAll(musicList)
        if (playlist.isNotEmpty()) playTrack(context, 0)
    }

    fun loadTracksFromFolder(context: Context, treeUri: Uri) {
        val documentFile = DocumentFile.fromTreeUri(context, treeUri)
        if (documentFile != null && documentFile.isDirectory) {
            val newTracks = mutableListOf<Track>()
            scanDirectory(context, documentFile, newTracks)
            if (newTracks.isNotEmpty()) {
                playlist.clear()
                playlist.addAll(newTracks)
                playTrack(context, 0)
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
                    var title = Uri.decode(rawName)
                    var artist = "Неизвестный исполнитель"
                    var art: Bitmap? = null
                    try {
                        retriever.setDataSource(context, uri)
                        val metaTitle = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                        val metaArtist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                        if (!metaTitle.isNullOrBlank()) title = fixEncoding(metaTitle)
                        if (!metaArtist.isNullOrBlank()) artist = fixEncoding(metaArtist)
                        val artBytes = retriever.embeddedPicture
                        if (artBytes != null) art = BitmapFactory.decodeByteArray(artBytes, 0, artBytes.size)
                    } catch (e: Exception) {} finally { retriever.release() }
                    tracks.add(Track(uri, title, artist, art))
                }
            }
        }
    }

    fun playTrack(context: Context, index: Int) {
        if (index < 0 || index >= playlist.size) return
        initExoPlayer(context)
        currentIndex = index
        val track = playlist[index]
        currentTitle = track.title
        currentArtist = track.artist
        currentAlbumArt = track.albumArt
        activePlayer = PlayerType.LOCAL

        exoPlayer?.apply {
            stop()
            clearMediaItems()
            setMediaItem(MediaItem.fromUri(track.uri))
            prepare()
            playWhenReady = true
        }
    }

    fun togglePlayPause() {
        exoPlayer?.let { player ->
            if (player.isPlaying) player.pause() else player.play()
        }
    }

    fun nextTrack(context: Context) {
        if (activePlayer == PlayerType.FMPLAY) {
            if (currentRadioQueue.isNotEmpty() && currentRadioIndex != -1) {
                val nextIdx = (currentRadioIndex + 1) % currentRadioQueue.size
                playRadioStream(context, currentRadioQueue[nextIdx], currentRadioQueue)
            }
            return
        }

        if (playlist.isNotEmpty()) {
            val nextIndex = (currentIndex + 1) % playlist.size
            playTrack(context, nextIndex)
        }
    }

    fun previousTrack(context: Context) {
        if (activePlayer == PlayerType.FMPLAY) {
            if (currentRadioQueue.isNotEmpty() && currentRadioIndex != -1) {
                val prevIdx = if (currentRadioIndex - 1 < 0) currentRadioQueue.size - 1 else currentRadioIndex - 1
                playRadioStream(context, currentRadioQueue[prevIdx], currentRadioQueue)
            }
            return
        }

        if (playlist.isNotEmpty()) {
            val prevIndex = if (currentIndex - 1 < 0) playlist.size - 1 else currentIndex - 1
            playTrack(context, prevIndex)
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