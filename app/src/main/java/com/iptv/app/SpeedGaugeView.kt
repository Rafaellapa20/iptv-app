package com.iptv.app

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

class SpeedGaugeView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var speedPercent = 0f // 0.0 to 1.0
    private val maxSpeed = 200f   // Max Mbps displayed

    private val arcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 14f
        strokeCap = Paint.Cap.ROUND
    }

    private val tickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#8A99AD")
        strokeWidth = 2f
        textSize = 24f
        textAlign = Paint.Align.CENTER
    }

    private val needlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#00E5FF")
        strokeWidth = 4f
        strokeCap = Paint.Cap.ROUND
        style = Paint.Style.STROKE
    }

    private val needleDotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#00E5FF")
        style = Paint.Style.FILL
    }

    private val bgArcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 14f
        strokeCap = Paint.Cap.ROUND
        color = Color.parseColor("#1A2A3D")
    }

    fun setSpeed(mbps: Float) {
        speedPercent = (mbps / maxSpeed).coerceIn(0f, 1f)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = width / 2f
        val cy = height / 2f
        val radius = min(cx, cy) - 30f

        val startAngle = 135f
        val sweepAngle = 270f

        val rect = RectF(cx - radius, cy - radius, cx + radius, cy + radius)

        // Background arc
        canvas.drawArc(rect, startAngle, sweepAngle, false, bgArcPaint)

        // Colored speed arc with gradient
        val colors = intArrayOf(
            Color.parseColor("#00FF88"),  // Green
            Color.parseColor("#FFD600"),  // Yellow
            Color.parseColor("#FF6600"),  // Orange
            Color.parseColor("#FF0040")   // Red
        )
        val gradient = SweepGradient(cx, cy, colors, null)
        val matrix = Matrix()
        matrix.setRotate(startAngle, cx, cy)
        gradient.setLocalMatrix(matrix)
        arcPaint.shader = gradient
        canvas.drawArc(rect, startAngle, sweepAngle * speedPercent, false, arcPaint)

        // Tick marks and labels
        val speeds = intArrayOf(0, 25, 50, 75, 100, 150, 200)
        for (speed in speeds) {
            val pct = speed / maxSpeed
            val angle = Math.toRadians((startAngle + sweepAngle * pct).toDouble())
            val innerR = radius - 22f
            val outerR = radius - 8f

            val x1 = cx + innerR * cos(angle).toFloat()
            val y1 = cy + innerR * sin(angle).toFloat()
            val x2 = cx + outerR * cos(angle).toFloat()
            val y2 = cy + outerR * sin(angle).toFloat()
            canvas.drawLine(x1, y1, x2, y2, tickPaint)

            val textR = radius - 38f
            val tx = cx + textR * cos(angle).toFloat()
            val ty = cy + textR * sin(angle).toFloat() + 8f
            canvas.drawText(speed.toString(), tx, ty, tickPaint)
        }

        // Needle
        val needleAngle = Math.toRadians((startAngle + sweepAngle * speedPercent).toDouble())
        val needleLength = radius - 50f
        val nx = cx + needleLength * cos(needleAngle).toFloat()
        val ny = cy + needleLength * sin(needleAngle).toFloat()
        canvas.drawLine(cx, cy, nx, ny, needlePaint)

        // Center dot
        canvas.drawCircle(cx, cy, 8f, needleDotPaint)
    }
}
