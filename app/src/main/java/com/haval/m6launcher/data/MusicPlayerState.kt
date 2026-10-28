package com.haval.m6launcher.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.net.Uri
import android.provider.MediaStore
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateListOf
import androidx.documentfile.provider.DocumentFile
import java.nio.charset.Charset

data class Track(
    val uri: Uri,
    val title: String,
    val artist: String,
    val albumArt: Bitmap?
)

object MusicPlayerState {
    // НОВОЕ: Источник звука (для будущих Яндекс, Радио и т.д.)
    var currentSource by mutableStateOf("Музыка")

    var isPlaying by mutableStateOf(false)
    var currentTitle by mutableStateOf("Нажмите папку, чтобы выбрать MP3")
    var currentArtist by mutableStateOf("Неизвестный исполнитель")
    var currentAlbumArt by mutableStateOf<Bitmap?>(null)

    var currentPosition by mutableStateOf(0)
    var totalDuration by mutableStateOf(0)

    val playlist = mutableStateListOf<Track>()
    var currentIndex by mutableStateOf(-1)

    private var mediaPlayer: MediaPlayer? = null

    private fun fixEncoding(text: String?): String {
        if (text.isNullOrBlank()) return ""
        if (text.any { it in '\u0400'..'\u04FF' }) return text
        return try {
            val bytes = text.toByteArray(Charsets.ISO_8859_1)
            String(bytes, Charset.forName("windows-1251"))
        } catch (e: Exception) {
            text
        }
    }

    fun initPlayer(context: Context) {
        if (mediaPlayer == null) {
            try {
                val resId = context.resources.getIdentifier("sample", "raw", context.packageName)
                if (resId != 0) {
                    mediaPlayer = MediaPlayer.create(context, resId)
                    totalDuration = mediaPlayer?.duration ?: 0
                    mediaPlayer?.setOnCompletionListener { mp ->
                        if (playlist.isNotEmpty()) {
                            nextTrack(context)
                        } else {
                            MusicPlayerState.isPlaying = false
                            mp.seekTo(0)
                            MusicPlayerState.currentPosition = 0
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun loadDeviceMusic(context: Context) {
        val musicList = mutableListOf<Track>()
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST
        )
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val cursor = context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            null,
            "${MediaStore.Audio.Media.TITLE} ASC"
        )

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
                    if (artBytes != null) {
                        art = BitmapFactory.decodeByteArray(artBytes, 0, artBytes.size)
                    }
                } catch (e: Exception) {
                } finally {
                    retriever.release()
                }

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
                    val defaultTitle = Uri.decode(rawName)
                    var title = defaultTitle
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
                    } catch (e: Exception) {
                    } finally {
                        retriever.release()
                    }
                    tracks.add(Track(uri, title, artist, art))
                }
            }
        }
    }

    fun playTrack(context: Context, index: Int) {
        if (index < 0 || index >= playlist.size) return

        currentIndex = index
        val track = playlist[index]

        currentTitle = track.title
        currentArtist = track.artist
        currentAlbumArt = track.albumArt
        currentSource = "Музыка" // Сбрасываем на локальный источник

        mediaPlayer?.release()
        mediaPlayer = null
        MusicPlayerState.isPlaying = false
        MusicPlayerState.currentPosition = 0

        try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(context, track.uri)
                prepare()
                setOnCompletionListener { mp ->
                    if (playlist.isNotEmpty()) {
                        nextTrack(context)
                    } else {
                        MusicPlayerState.isPlaying = false
                        mp.seekTo(0)
                        MusicPlayerState.currentPosition = 0
                    }
                }
            }
            totalDuration = mediaPlayer?.duration ?: 0
            mediaPlayer?.start()
            MusicPlayerState.isPlaying = true
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun togglePlayPause() {
        mediaPlayer?.let { player ->
            if (MusicPlayerState.isPlaying) player.pause() else player.start()
            MusicPlayerState.isPlaying = !MusicPlayerState.isPlaying
        }
    }

    fun nextTrack(context: Context) {
        if (playlist.isNotEmpty()) {
            val nextIndex = (currentIndex + 1) % playlist.size
            playTrack(context, nextIndex)
        } else {
            mediaPlayer?.seekTo(0)
            MusicPlayerState.currentPosition = 0
            if (MusicPlayerState.isPlaying) mediaPlayer?.start()
        }
    }

    fun previousTrack(context: Context) {
        if (playlist.isNotEmpty()) {
            val prevIndex = if (currentIndex - 1 < 0) playlist.size - 1 else currentIndex - 1
            playTrack(context, prevIndex)
        } else {
            mediaPlayer?.seekTo(0)
            MusicPlayerState.currentPosition = 0
            if (MusicPlayerState.isPlaying) mediaPlayer?.start()
        }
    }

    fun updatePosition() {
        mediaPlayer?.let { player ->
            if (MusicPlayerState.isPlaying) MusicPlayerState.currentPosition = player.currentPosition
        }
    }

    fun seekTo(position: Int) {
        mediaPlayer?.seekTo(position)
        MusicPlayerState.currentPosition = position
    }
}