package com.example.pogooverlay

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Path
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.ImageButton
import android.animation.ValueAnimator
import android.view.animation.LinearInterpolator
import android.util.Log
import android.widget.Toast
import kotlin.math.cos
import kotlin.math.sin

class OverlayAccessibilityService : AccessibilityService() {

    private val TAG = "OverlayService"

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private var statusTextView: android.widget.TextView? = null
    private var yValueTextView: android.widget.TextView? = null
    private var ySlider: android.widget.SeekBar? = null
    private var gestureOverlayView: GestureOverlayView? = null

    private var currentGestureX: Float = 0f
    private var currentGestureY: Float = 0f
    private val handler = Handler(Looper.getMainLooper())

    // Screen dimensions
    private var screenWidth = 0
    private var screenHeight = 0

//delay
    // Circle dimensions
    private var circleCenterX = 0f
    private var circleCenterY = 0f
    private var swipeTargetY = 0f
    private var circleSpeed = 500L // Default 1 second
    private var radiusX = 500f
    private var radiusY = 300f

    override fun onServiceConnected() {
        super.onServiceConnected()

        showStatus("Service Connected")

        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        val metrics = DisplayMetrics()
        windowManager?.defaultDisplay?.getRealMetrics(metrics)
        screenWidth = metrics.widthPixels
        screenHeight = metrics.heightPixels

        setupOverlay()
        setupGestureOverlay()
        setupCircleRunnable()

        isRunning = true
        val filter = android.content.IntentFilter(ACTION_DISABLE_SERVICE)
        registerReceiver(disableServiceReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupOverlay() {
        overlayView = LayoutInflater.from(this).inflate(R.layout.overlay_layout, null)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )

        params.gravity = Gravity.BOTTOM or Gravity.START
        params.x = 50
        params.y = 100

        val button = overlayView?.findViewById<ImageButton>(R.id.overlay_button)
        statusTextView = overlayView?.findViewById(R.id.overlay_status_text)

        button?.setOnClickListener {
            showStatus("Spin & Throw!")
            // Ensure metrics are up to date
            updateScreenMetrics()
            performSpinAndThrow()
        }

        yValueTextView = overlayView?.findViewById(R.id.y_value_text)
        ySlider = overlayView?.findViewById(R.id.y_slider)

        // Initialize slider
        ySlider?.max = screenHeight
        val defaultTargetY = (screenHeight * 0.2f).toInt() // Default target near top
        // Invert init: Slider Up (High Progress) = Target Up (Low Y)
        // Progress = ScreenHeight - TargetY
        ySlider?.progress = screenHeight - defaultTargetY
        swipeTargetY = defaultTargetY.toFloat()
        yValueTextView?.text = "Target Y: $defaultTargetY"

        ySlider?.setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: android.widget.SeekBar?, progress: Int, fromUser: Boolean) {
                // Invert mapping: Progress 0 (Bottom) -> Target Height (Bottom)
                // Progress Max (Top) -> Target 0 (Top)
                swipeTargetY = (screenHeight - progress).toFloat()
                yValueTextView?.text = "Target Y: ${swipeTargetY.toInt()}"
                gestureOverlayView?.showTarget(screenWidth * 0.5f, swipeTargetY)
            }

            override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) {}
        })

        windowManager?.addView(overlayView, params)
        
        // Initialize metrics and show circle immediately
        updateScreenMetrics()
        gestureOverlayView?.showCircle(circleCenterX, circleCenterY, radiusX, radiusY)
        // Also show target initially
        gestureOverlayView?.showTarget(screenWidth * 0.5f, swipeTargetY)
    }

    private fun updateScreenMetrics() {
        val metrics = DisplayMetrics()
        windowManager?.defaultDisplay?.getRealMetrics(metrics)
        screenWidth = metrics.widthPixels
        screenHeight = metrics.heightPixels
        
        circleCenterX = (screenWidth * 0.5f)
        circleCenterY = screenHeight * 0.75f
    }

    private fun setupGestureOverlay() {
        gestureOverlayView = GestureOverlayView(this)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )

        windowManager?.addView(gestureOverlayView, params)
    }

    public override fun onInterrupt() {
    }

    private fun startCircleGesture() {
        // Ensure metrics are up to date
        updateScreenMetrics()

        showStatus("Holding Center...")
        gestureOverlayView?.showCircle(circleCenterX, circleCenterY, radiusX, radiusY)

        // Dispatch a long hold gesture at the center
        val path = Path()
        path.moveTo(circleCenterX, circleCenterY)
        // Small wiggle to ensure it's treated as a touch
        path.lineTo(circleCenterX, circleCenterY + 1f) 

        val gestureBuilder = GestureDescription.Builder()
        // Hold for 60 seconds (effectively indefinite until stopped)
        val strokeDescription = GestureDescription.StrokeDescription(path, 0, 60000)
        gestureBuilder.addStroke(strokeDescription)

        dispatchGesture(gestureBuilder.build(), null, null)
    }

    private lateinit var circleRunnable: Runnable

    // circleRunnable is no longer used
    private fun setupCircleRunnable() {
    }

    private fun performSpinAndThrow() {
        showStatus("Spinning...")
        val path = Path()
        
        // Spin from 260 degrees (Top-Left) to 180 degrees (Left)
        // 260 degrees is at (centerX + radius, centerY)
        // Sweep angle is 280 degrees (Clockwise: 260 -> 360/0 -> 90 -> 180)
        val rect = android.graphics.RectF(
            circleCenterX - radiusX,
            circleCenterY - radiusY,
            circleCenterX + radiusX,
            circleCenterY + radiusY
        )
        
        // startAngle 260 is Top-Left. Sweep 280 deg CW.
        path.arcTo(rect, 260f, 280f)

        // Throw to target
        // End of spin is at 180 degrees (Left)
        // We are already at the end of the arc, so just lineTo the target
        val endX = screenWidth / 3f
        val endY = swipeTargetY
        path.lineTo(endX, endY)

        val gestureBuilder = GestureDescription.Builder()
        // Spin duration: proportional to speed
        val spinDuration = (circleSpeed * (280f / 360f)).toLong()
        // Add some time for the throw (e.g. 100ms)
        val totalDuration = spinDuration + 100L
        
        val strokeDescription = GestureDescription.StrokeDescription(path, 0, totalDuration)
        gestureBuilder.addStroke(strokeDescription)

        dispatchGesture(gestureBuilder.build(), null, null)
    }




    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Not used
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        unregisterReceiver(disableServiceReceiver)
        if (overlayView != null) {
            windowManager?.removeView(overlayView)
            overlayView = null
        }
        if (gestureOverlayView != null) {
            windowManager?.removeView(gestureOverlayView)
            gestureOverlayView = null
        }
    }

    companion object {
        var isRunning = false
        const val ACTION_DISABLE_SERVICE = "com.example.pogooverlay.ACTION_DISABLE_SERVICE"
    }

    private val disableServiceReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: android.content.Intent?) {
            if (intent?.action == ACTION_DISABLE_SERVICE) {
                disableSelf()
            }
        }
    }

    private fun showStatus(message: String) {
        handler.post {
            statusTextView?.text = message
            statusTextView?.visibility = View.VISIBLE
            
            // Auto-hide after 2 seconds 
            handler.removeCallbacks(hideStatusRunnable)
            handler.postDelayed(hideStatusRunnable, 2000)
        }
    }

    private val hideStatusRunnable = Runnable {
        statusTextView?.visibility = View.GONE
    }


}
