package com.example.facialexpressionrecognition1

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

class OverlayView(context: Context, attrs: AttributeSet?) : View(context, attrs) {

    private val faceBounds = mutableListOf<Pair<RectF, String>>()
    private val paint = Paint().apply {
        color = Color.GREEN
        style = Paint.Style.STROKE
        strokeWidth = 8f
    }
    private val textPaint = Paint().apply {
        color = Color.RED
        textSize = 40f
        style = Paint.Style.FILL
    }

    fun setFaces(faceBounds: List<Pair<RectF, String>>) {
        this.faceBounds.clear()
        this.faceBounds.addAll(faceBounds)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        for ((bounds, emotion) in faceBounds) {
            canvas.drawRect(bounds, paint)
            canvas.drawText(emotion, bounds.left, bounds.top - 10, textPaint)
        }
    }
}
