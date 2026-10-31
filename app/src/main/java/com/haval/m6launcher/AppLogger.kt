package com.haval.m6launcher

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object AppLogger {
    private const val FILE_NAME = "startup_log.txt"
    private const val MAX_LOG_SIZE = 100 * 1024 // 100 КБ (очистит, если больше)
    private val logScope = CoroutineScope(Dispatchers.IO)

    private fun getLogFile(context: Context): File {
        val directory = context.getExternalFilesDir(null) ?: context.filesDir
        return File(directory, FILE_NAME)
    }

    fun log(context: Context, message: String) {
        val appContext = context.applicationContext
        logScope.launch {
            try {
                val file = getLogFile(appContext)
                if (file.exists() && file.length() > MAX_LOG_SIZE) {
                    file.delete()
                }
                val time = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date())
                file.appendText("[$time] $message\n")
            } catch (e: Exception) { }
        }
    }

    fun readLogs(context: Context): String {
        return try {
            val file = getLogFile(context.applicationContext)
            if (file.exists()) file.readText() else "Лог пуст."
        } catch (e: Exception) {
            "Ошибка чтения лога: ${e.message}"
        }
    }
}