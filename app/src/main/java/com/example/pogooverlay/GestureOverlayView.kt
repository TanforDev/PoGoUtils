package com.example.pogooverlay

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.View

class GestureOverlayView(context: Context) : View(context) {

    private val pathPaint = Paint().apply {
        color = Color.RED
        style = Paint.Style.STROKE
        strokeWidth = 10f
        isAntiAlias = true
        alpha = 150
    }

    private val progressPaint = Paint().apply {
        color = Color.GREEN
        style = Paint.Style.STROKE
        strokeWidth = 10f
        isAntiAlias = true
        alpha = 255
    }

    private val targetPaint = Paint().apply {
        color = Color.GREEN
        style = Paint.Style.FILL
        isAntiAlias = true
        alpha = 200
    }

    private var centerX: Float = 0f
    private var centerY: Float = 0f
    private var radiusX: Float = 0f
    private var radiusY: Float = 0f
    private var currentProgress: Float = 0f
    private var isVisible = false
    
    private var targetX: Float = 0f
    private var targetY: Float = 0f
    private var isTargetVisible = false

    fun showCircle(cx: Float, cy: Float, rx: Float, ry: Float) {
        centerX = cx
        centerY = cy
        radiusX = rx
        radiusY = ry
        currentProgress = 0f
        isVisible = true
        invalidate()
    }

    fun updateProgress(progress: Float) {
        currentProgress = progress
        invalidate()
    }

    fun hideCircle() {
        isVisible = false
        invalidate()
    }
    
    fun showTarget(x: Float, y: Float) {
        targetX = x
        targetY = y
        isTargetVisible = true
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
                    // Draw complete path in RED
            canvas.drawOval(
                centerX - radiusX, 
                centerY - radiusY, 
                centerX + radiusX, 
                centerY + radiusY, 
                pathPaint
            )
            
            // Draw progress in GREEN
            // Start from 260 degrees (Top-Left) and sweep CW
            val sweepAngle = 360f * currentProgress
            canvas.drawArc(
                centerX - radiusX, 
                centerY - radiusY, 
                centerX + radiusX, 
                centerY + radiusY, 
                260f, 
                sweepAngle, 
                false, 
                progressPaint
            )
        
        if (isTargetVisible) {
            canvas.drawCircle(targetX, targetY, 20f, targetPaint)
        }
    }
}
