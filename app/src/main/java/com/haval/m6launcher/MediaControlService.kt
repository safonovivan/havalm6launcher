package com.haval.m6launcher

import android.content.ComponentName
import android.content.Context
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.service.notification.NotificationListenerService
import android.util.Log

class MediaControlService : NotificationListenerService() {

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d("MediaControl", "Служба подключена, можно управлять плеерами")
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        Log.d("MediaControl", "Служба отключена")
    }

    // Функция для получения контроллера конкретного приложения (FMPlay)
    fun getControllerForPackage(context: Context, targetPackage: String): MediaController? {
        val mediaSessionManager = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager

        // Передаем компонент нашей службы
        val componentName = ComponentName(context, MediaControlService::class.java)

        try {
            val activeSessions = mediaSessionManager.getActiveSessions(componentName)
            for (controller in activeSessions) {
                if (controller.packageName == targetPackage) {
                    return controller
                }
            }
        } catch (e: SecurityException) {
            Log.e("MediaControl", "Нет доступа к медиасессиям. Нужно разрешение.")
        }
        return null
    }
}