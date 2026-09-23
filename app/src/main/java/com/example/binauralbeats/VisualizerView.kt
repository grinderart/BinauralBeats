package com.example.binauralbeats

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import androidx.core.graphics.ColorUtils

class VisualizerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var waveform: ShortArray? = null

    // Pre-allocated Paint objects to avoid GC allocations during onDraw
    private val wavePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 6f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val idlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
        color = Color.parseColor("#33FFFFFF")
    }

    // Pre-allocated Path objects
    private val wavePath = Path()
    private val fillPath = Path()

    // Color management
    private var primaryColor: Int = Color.parseColor("#00E676")

    init {
        setWaveColor(primaryColor)
    }

    fun setWaveColor(color: Int) {
        this.primaryColor = color
        wavePaint.color = color
        updateGradient()
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        updateGradient()
    }

    private fun updateGradient() {
        val h = height.toFloat().coerceAtLeast(1f)
        val alphaColor = ColorUtils.setAlphaComponent(primaryColor, 70)
        val transparentColor = ColorUtils.setAlphaComponent(primaryColor, 0)

        fillPaint.shader = LinearGradient(
            0f, 0f, 0f, h,
            alphaColor,
            transparentColor,
            Shader.TileMode.CLAMP
        )
    }

    fun updateVisualizer(rawWaveform: ShortArray) {
        this.waveform = rawWaveform
        postInvalidateOnAnimation()
    }

    fun clearVisualizer() {
        this.waveform = null
        postInvalidateOnAnimation()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val w = width.toFloat()
        val h = height.toFloat()
        val centerY = h / 2f

        val wave = waveform
        if (wave == null || wave.isEmpty()) {
            // Draw subtle horizontal idle line
            canvas.drawLine(0f, centerY, w, centerY, idlePaint)
            return
        }

        wavePath.reset()
        fillPath.reset()

        val sampleStep = (wave.size / 64).coerceAtLeast(1) // Downsample to ~64 points for ultra-smooth Bezier curve
        val pointCount = wave.size / sampleStep
        if (pointCount < 2) return

        val dx = w / (pointCount - 1)

        var prevX = 0f
        var prevY = centerY + (wave[0] / Short.MAX_VALUE.toFloat()) * (h * 0.42f)

        wavePath.moveTo(prevX, prevY)
        fillPath.moveTo(0f, h)
        fillPath.lineTo(prevX, prevY)

        var pointIndex = 1
        var i = sampleStep
        while (i < wave.size && pointIndex < pointCount) {
            val x = pointIndex * dx
            val y = centerY + (wave[i] / Short.MAX_VALUE.toFloat()) * (h * 0.42f)

            // Cubic Bezier interpolation for smooth wave curvature
            val cx = (prevX + x) / 2f
            wavePath.quadTo(prevX, prevY, cx, (prevY + y) / 2f)
            fillPath.quadTo(prevX, prevY, cx, (prevY + y) / 2f)

            prevX = x
            prevY = y
            pointIndex++
            i += sampleStep
        }

        wavePath.lineTo(prevX, prevY)
        fillPath.lineTo(prevX, prevY)
        fillPath.lineTo(w, h)
        fillPath.close()

        // Render filled gradient under wave
        canvas.drawPath(fillPath, fillPaint)

        // Render smooth wave stroke
        canvas.drawPath(wavePath, wavePaint)
    }
}
