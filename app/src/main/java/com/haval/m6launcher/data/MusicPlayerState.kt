package com.haval.m6launcher.data

import android.media.MediaPlayer
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object MusicPlayerState {
    var isPlaying by mutableStateOf(false)
    var currentTitle by mutableStateOf("Purple Eyes")
    var currentArtist by mutableStateOf("Elephant")

    // Новые состояния для прогресс-бара
    var currentPosition by mutableStateOf(0)
    var totalDuration by mutableStateOf(0)

    private var mediaPlayer: MediaPlayer? = null

    fun initPlayer(context: android.content.Context) {
        if (mediaPlayer == null) {
            try {
                val resId = context.resources.getIdentifier("sample", "raw", context.packageName)
                if (resId != 0) {
                    mediaPlayer = MediaPlayer.create(context, resId)
                    totalDuration = mediaPlayer?.duration ?: 0

                    mediaPlayer?.setOnCompletionListener {
                        isPlaying = false
                        it.seekTo(0)
                        currentPosition = 0
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun togglePlayPause() {
        mediaPlayer?.let { player ->
            if (isPlaying) {
                player.pause()
            } else {
                player.start()
            }
            isPlaying = !isPlaying
        } ?: run {
            isPlaying = !isPlaying
        }
    }

    fun nextTrack() {
        mediaPlayer?.let { player ->
            player.seekTo(0)
            currentPosition = 0
            if (isPlaying) player.start()
        }
        currentTitle = "Next Track (Demo)"
    }

    // Функция для обновления позиции (будет вызываться из UI)
    fun updatePosition() {
        mediaPlayer?.let { player ->
            if (isPlaying) {
                currentPosition = player.currentPosition
            }
        }
    }

    // Функция для перемотки трека
    fun seekTo(position: Int) {
        mediaPlayer?.seekTo(position)
        currentPosition = position
    }
}