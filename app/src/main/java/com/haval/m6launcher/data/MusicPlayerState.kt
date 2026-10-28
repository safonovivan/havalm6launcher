package com.haval.m6launcher.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object MusicPlayerState {
    var isPlaying by mutableStateOf(false)
    var currentTitle by mutableStateOf("Нажмите папку, чтобы выбрать MP3")
    var currentArtist by mutableStateOf("Неизвестный исполнитель")
    var currentAlbumArt by mutableStateOf<Bitmap?>(null)

    var currentPosition by mutableStateOf(0)
    var totalDuration by mutableStateOf(0)

    private var mediaPlayer: MediaPlayer? = null

    fun initPlayer(context: Context) {
        if (mediaPlayer == null) {
            try {
                val resId = context.resources.getIdentifier("sample", "raw", context.packageName)
                if (resId != 0) {
                    mediaPlayer = MediaPlayer.create(context, resId)
                    totalDuration = mediaPlayer?.duration ?: 0
                    mediaPlayer?.setOnCompletionListener { mp ->
                        MusicPlayerState.isPlaying = false
                        mp.seekTo(0)
                        MusicPlayerState.currentPosition = 0
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun loadTrackFromUri(context: Context, uri: Uri) {
        mediaPlayer?.release()
        mediaPlayer = null
        MusicPlayerState.isPlaying = false
        MusicPlayerState.currentPosition = 0

        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)

            val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
            val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)

            currentTitle = if (!title.isNullOrBlank()) title else uri.lastPathSegment ?: "Неизвестный трек"
            currentArtist = if (!artist.isNullOrBlank()) artist else "Неизвестный исполнитель"

            val artBytes = retriever.embeddedPicture
            currentAlbumArt = if (artBytes != null) {
                BitmapFactory.decodeByteArray(artBytes, 0, artBytes.size)
            } else {
                null
            }

            mediaPlayer = MediaPlayer().apply {
                setDataSource(context, uri)
                prepare()
                setOnCompletionListener { mp ->
                    MusicPlayerState.isPlaying = false
                    mp.seekTo(0)
                    MusicPlayerState.currentPosition = 0
                }
            }
            totalDuration = mediaPlayer?.duration ?: 0

        } catch (e: Exception) {
            e.printStackTrace()
            currentTitle = "Ошибка чтения файла"
            currentArtist = ""
            currentAlbumArt = null
        } finally {
            retriever.release()
        }
    }

    fun togglePlayPause() {
        mediaPlayer?.let { player ->
            if (MusicPlayerState.isPlaying) {
                player.pause()
            } else {
                player.start()
            }
            MusicPlayerState.isPlaying = !MusicPlayerState.isPlaying
        }
    }

    fun nextTrack() {
        mediaPlayer?.let { player ->
            player.seekTo(0)
            MusicPlayerState.currentPosition = 0
            if (MusicPlayerState.isPlaying) player.start()
        }
    }

    fun updatePosition() {
        mediaPlayer?.let { player ->
            if (MusicPlayerState.isPlaying) {
                MusicPlayerState.currentPosition = player.currentPosition
            }
        }
    }

    fun seekTo(position: Int) {
        mediaPlayer?.seekTo(position)
        MusicPlayerState.currentPosition = position
    }
}