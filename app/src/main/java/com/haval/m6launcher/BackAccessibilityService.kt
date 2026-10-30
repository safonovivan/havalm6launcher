package com.haval.m6launcher

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.PixelFormat
import android.view.GestureDetector
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import androidx.core.content.ContextCompat
import com.haval.m6launcher.data.SettingsRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlin.math.abs

class BackAccessibilityService : AccessibilityService() {

    companion object {
        var instance: BackAccessibilityService? = null
    }

    private lateinit var windowManager: WindowManager
    private var floatingButton: FloatingButtonView? = null
    private lateinit var params: WindowManager.LayoutParams
    private lateinit var settingsRepo: SettingsRepository

    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var isDragging = false

    private val showHideReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                "ACTION_SHOW_BUTTON" -> {
                    floatingButton?.alpha = 1f
                    floatingButton?.isClickable = true
                }
                "ACTION_HIDE_BUTTON" -> {
                    floatingButton?.alpha = 0f
                    floatingButton?.isClickable = false
                }
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        settingsRepo = SettingsRepository(this)

        val filter = IntentFilter().apply {
            addAction("ACTION_SHOW_BUTTON")
            addAction("ACTION_HIDE_BUTTON")
        }
        ContextCompat.registerReceiver(this, showHideReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        addFloatingButton()
    }

    override fun onDestroy() {
        instance = null
        try { unregisterReceiver(showHideReceiver) } catch (_: Exception) {}
        if (floatingButton != null) windowManager.removeView(floatingButton)
        super.onDestroy()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}

    // Метод для нажатия "Назад"
    fun performBackAction() {
        performGlobalAction(GLOBAL_ACTION_BACK)
    }

    private fun addFloatingButton() {
        floatingButton = FloatingButtonView(this)

        if (MainActivity.isLauncherVisible) {
            floatingButton?.alpha = 0f
            floatingButton?.isClickable = false
        }

        // ВАЖНО: Используем TYPE_ACCESSIBILITY_OVERLAY вместо TYPE_APPLICATION_OVERLAY.
        // Этот тип окна специально создан для AccessibilityService и не требует разрешений.
        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            width = (64 * resources.displayMetrics.density).toInt()
            height = (64 * resources.displayMetrics.density).toInt()
            x = 200
            y = 200
        }

        val gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                if (!isDragging && instance != null) {
                    instance?.performBackAction()
                }
                return true
            }

            override fun onDoubleTap(e: MotionEvent): Boolean {
                if (isDragging) return false
                val settings = runBlocking { settingsRepo.userSettings.first() }

                when (settings.floatingDoubleTapType) {
                    "home" -> {
                        val homeIntent = Intent(Intent.ACTION_MAIN)
                        homeIntent.addCategory(Intent.CATEGORY_HOME)
                        homeIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        startActivity(homeIntent)
                    }
                    "recents" -> {
                        performGlobalAction(GLOBAL_ACTION_RECENTS)
                    }
                    "app" -> {
                        if (settings.floatingDoubleTapValue.isNotBlank()) {
                            val launchIntent = packageManager.getLaunchIntentForPackage(settings.floatingDoubleTapValue)
                            if (launchIntent != null) {
                                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                startActivity(launchIntent)
                            }
                        }
                    }
                    "none" -> { }
                }
                return true
            }
        })

        floatingButton?.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)

            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isDragging = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - initialTouchX
                    val dy = event.rawY - initialTouchY

                    if (abs(dx) > 10 || abs(dy) > 10) {
                        isDragging = true
                        params.x = initialX + dx.toInt()
                        params.y = initialY + dy.toInt()
                        windowManager.updateViewLayout(floatingButton, params)
                    }
                    true
                }
                MotionEvent.ACTION_UP -> true
                else -> false
            }
        }

        windowManager.addView(floatingButton, params)
    }
}