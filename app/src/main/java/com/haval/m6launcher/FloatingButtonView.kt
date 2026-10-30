package com.haval.m6launcher

import android.content.Context
import android.graphics.*
import android.view.View

class FloatingButtonView(context: Context) : View(context) {

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val trianglePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        // ИЗМЕНЕНО: Контур теперь оранжевый (AccentOrange #FF5722)
        color = Color.parseColor("#FF5722")
        strokeWidth = 2.5f * resources.displayMetrics.density
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        // Мягкое белое свечение на фоне
        bgPaint.shader = RadialGradient(
            w / 2f, h / 2f, w / 2f,
            intArrayOf(Color.parseColor("#66FFFFFF"), Color.TRANSPARENT),
            floatArrayOf(0.2f, 1f),
            Shader.TileMode.CLAMP
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = width / 2f
        val cy = height / 2f
        val r = width / 4f

        // Рисуем круг с растворением
        canvas.drawCircle(cx, cy, width / 2f, bgPaint)

        // Рисуем контурный треугольник
        val path = Path()
        path.moveTo(cx - r * 0.8f, cy)
        path.lineTo(cx + r * 0.6f, cy - r * 0.8f)
        path.lineTo(cx + r * 0.6f, cy + r * 0.8f)
        path.close()
        canvas.drawPath(path, trianglePaint)
    }
}