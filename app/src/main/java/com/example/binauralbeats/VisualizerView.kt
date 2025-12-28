package com.example.binauralbeats

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

class VisualizerView(context: Context, attrs: AttributeSet?) : View(context, attrs) {

    // CORRECCIÓN: Ahora trabajamos con un array de Shorts, que son los datos crudos.
    private var waveform: ShortArray? = null
    private val paint = Paint()

    init {
        paint.strokeWidth = 2f
        paint.isAntiAlias = true
        paint.color = Color.parseColor("#4CAF50")
    }

    // CORRECCIÓN: La función ahora acepta un ShortArray.
    fun updateVisualizer(waveform: ShortArray) {
        this.waveform = waveform
        invalidate() // Redibujar la vista
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (waveform == null) {
            return
        }

        val width = width.toFloat()
        val height = height.toFloat()
        val centerY = height / 2f

        val points = FloatArray(waveform!!.size * 4)

        // CORRECCIÓN: La lógica de dibujo se adapta a los valores de Short.
        for (i in 0 until waveform!!.size - 1) {
            val x1 = i * (width / waveform!!.size)
            val y1 = centerY + (waveform!![i] / Short.MAX_VALUE.toFloat()) * (height / 2f)

            val x2 = (i + 1) * (width / waveform!!.size)
            val y2 = centerY + (waveform!![i + 1] / Short.MAX_VALUE.toFloat()) * (height / 2f)

            points[i * 4] = x1
            points[i * 4 + 1] = y1
            points[i * 4 + 2] = x2
            points[i * 4 + 3] = y2
        }

        canvas.drawLines(points, paint)
    }
}
