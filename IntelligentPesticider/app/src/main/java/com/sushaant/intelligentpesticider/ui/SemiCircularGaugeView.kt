//package com.sushaant.intelligentpesticider.ui
//
//import android.content.Context
//import android.graphics.*
//import android.util.AttributeSet
//import android.view.View
//import kotlin.math.min
//
//class SemiCircularGaugeView @JvmOverloads constructor(
//    context: Context,
//    attrs: AttributeSet? = null
//) : View(context, attrs) {
//
//    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
//    private val rect = RectF()
//
//    var value = 0f
//        set(v) {
//            field = v.coerceIn(0f, 100f)
//            invalidate()
//        }
//
//    override fun onDraw(canvas: Canvas) {
//        super.onDraw(canvas)
//
//        val size = min(width, height * 2)
//        rect.set(
//            (width - size) / 2f,
//            (height - size).toFloat(),
//            (width + size) / 2f,
//            height.toFloat()
//        )
//
//        // Background arc
//        paint.style = Paint.Style.STROKE
//        paint.strokeWidth = 18f
//        paint.color = Color.LTGRAY
//        canvas.drawArc(rect, 180f, 180f, false, paint)
//
//        // Color logic
//        paint.color = when {
//            value < 35 -> Color.parseColor("#2196F3") // Blue
//            value < 70 -> Color.parseColor("#FFC107") // Yellow
//            else -> Color.parseColor("#F44336")       // Red
//        }
//
//        canvas.drawArc(rect, 180f, 180f * (value / 100f), false, paint)
//
//        // Value text
//        paint.style = Paint.Style.FILL
//        paint.textAlign = Paint.Align.CENTER
//        paint.textSize = 42f
//        paint.color = Color.BLACK
//
//        canvas.drawText(
//            "${value.toInt()}",
//            width / 2f,
//            height - 20f,
//            paint
//        )
//    }
//}
//package com.sushaant.intelligentpesticider.ui
//
//import android.content.Context
//import android.graphics.*
//import android.util.AttributeSet
//import android.view.View
//import kotlin.math.min
//import androidx.core.graphics.toColorInt
//
//class SemiCircularGaugeView @JvmOverloads constructor(
//    context: Context,
//    attrs: AttributeSet? = null
//) : View(context, attrs) {
//
//    // Use two separate Paint objects for clarity and performance
//    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG)
//    private val foregroundPaint = Paint(Paint.ANTI_ALIAS_FLAG)
//    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
//    private val arcBounds = RectF()
//
//    var value = 0f
//        set(v) {
//            field = v.coerceIn(0f, 100f)
//            invalidate() // Redraw the view when the value changes
//        }
//
//    init {
//        // --- Setup paint for the background arc ---
//        backgroundPaint.style = Paint.Style.STROKE
//        backgroundPaint.strokeWidth = 35f // A bit thicker for better visuals
//        backgroundPaint.color = "#E0E0E0".toColorInt() // A light gray
//        backgroundPaint.strokeCap = Paint.Cap.ROUND // Makes the ends of the arc rounded
//
//        // --- Setup paint for the foreground (value) arc ---
//        foregroundPaint.style = Paint.Style.STROKE
//        foregroundPaint.strokeWidth = 35f
//        foregroundPaint.strokeCap = Paint.Cap.ROUND
//
//        // --- Setup paint for the text ---
//        textPaint.style = Paint.Style.FILL
//        textPaint.textAlign = Paint.Align.CENTER
//        textPaint.textSize = 50f // Slightly larger text
//        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
//        textPaint.color = Color.BLACK
//    }
//
//    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
//        super.onSizeChanged(w, h, oldw, oldh)
//
//        // --- Calculate the drawing area for the arc ---
//        val padding = 40f
//        // The arc should be drawn in a square box, use the smaller of width or height
//        val diameter = min(w.toFloat(), h.toFloat()) - padding
//        val left = (w - diameter) / 2f
//        val top = (h - diameter) / 2f
//        arcBounds.set(left, top, left + diameter, top + diameter)
//
//        // --- ✅ THE GRADIENT FIX: Create and apply the shader ---
//        // A SweepGradient sweeps around a center point (the center of our view)
//        val gradient = SweepGradient(
//            w / 2f,
//            h / 2f,
//            // Define the colors of the gradient
//            intArrayOf(
//                "#4CAF50".toColorInt(), // Green for low values
//                "#FFEB3B".toColorInt(), // Yellow for mid values
//                "#F44336".toColorInt()  // Red for high values
//            ),
//            // Define where each color stops. 0.0 is the start, 1.0 is the end.
//            // These positions correspond to the sweep angle (0 to 270 degrees)
//            floatArrayOf(0f, 0.5f, 1f)
//        )
//
//        // We need to rotate the canvas matrix for the gradient to align with our arc
//        val matrix = Matrix()
//        matrix.postRotate(135f, w / 2f, h / 2f) // Start the gradient at the same angle as the arc
//        gradient.setLocalMatrix(matrix)
//
//        // Apply the configured gradient to the foreground paint
//        foregroundPaint.shader = gradient
//    }
//
//    override fun onDraw(canvas: Canvas) {
//        super.onDraw(canvas)
//
//        // Define the geometry of the semicircle
//        val startAngle = 135f
//        val sweepAngle = 270f // A 270-degree arc looks better than a 180-degree one
//
//        // 1. Draw the gray background arc
//        canvas.drawArc(arcBounds, startAngle, sweepAngle, false, backgroundPaint)
//
//        // 2. Calculate the sweep angle for the current value
//        val valueSweepAngle = (value / 100f) * sweepAngle
//
//        // 3. Draw the colored foreground arc
//        canvas.drawArc(arcBounds, startAngle, valueSweepAngle, false, foregroundPaint)
//
//        // 4. Draw the value text in the center
//        // Adjust Y position to be roughly in the middle of the gauge
//        canvas.drawText(
//            "${value.toInt()}",
//            width / 2f,
//            height / 2f + textPaint.textSize / 3, // Center the text vertically
//            textPaint
//        )
//    }
//}
package com.sushaant.intelligentpesticider.ui

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import kotlin.math.min

class SemiCircularGaugeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val foregroundPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val arcBounds = RectF()

    // Default gradient colors if none are provided
    private var gradientColors: IntArray = intArrayOf(Color.GREEN, Color.RED)
    private var gradientPositions: FloatArray? = null

    var value = 0f
        set(v) {
            field = v.coerceIn(0f, 100f)
            invalidate() // Redraw the view when the value changes
        }

    init {
        // Setup paint for the background arc
        backgroundPaint.style = Paint.Style.STROKE
        backgroundPaint.strokeWidth = 35f
        backgroundPaint.color = Color.parseColor("#E0E0E0")
        backgroundPaint.strokeCap = Paint.Cap.ROUND

        // Setup paint for the foreground (value) arc
        foregroundPaint.style = Paint.Style.STROKE
        foregroundPaint.strokeWidth = 35f
        foregroundPaint.strokeCap = Paint.Cap.ROUND

        // Setup paint for the text
        textPaint.style = Paint.Style.FILL
        textPaint.textAlign = Paint.Align.CENTER
        textPaint.textSize = 50f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textPaint.color = Color.BLACK
    }

    /**
     * Sets the colors for the gradient. Call this before the view is displayed.
     * @param colors The array of colors for the gradient.
     * @param positions Optional array of relative positions (0.0 to 1.0) for each color.
     *                  If null, the colors are distributed evenly.
     */
    fun setGradient(colors: IntArray, positions: FloatArray? = null) {
        gradientColors = colors
        gradientPositions = positions
        updateGradient() // Update the gradient shader
    }

    private fun updateGradient() {
        if (width > 0 && height > 0) {
            val colorsForGradient = gradientColors.reversedArray()
            val positionsForGradient = gradientPositions?.let {
                it.reversed().map { pos -> 1.0f - pos }.toFloatArray()
            }

            val gradient = SweepGradient(
                width / 2f,
                height / 2f,
                gradientColors,
                gradientPositions
            )

            val matrix = Matrix()
            matrix.postRotate(125f, width / 2f, height / 2f)
            gradient.setLocalMatrix(matrix)

            foregroundPaint.shader = gradient
            invalidate() // Redraw with the new gradient
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        val padding = 35f
        val diameter = min(w.toFloat(), h.toFloat()) - padding
        val left = (w - diameter) / 2f
        val top = (h - diameter) / 2f
        arcBounds.set(left, top, left + diameter, top + diameter)

        // Apply the gradient when the size is known
        updateGradient()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val startAngle = 135f
        val sweepAngle = 270f

        // 1. Draw the gray background arc
        canvas.drawArc(arcBounds, startAngle, sweepAngle, false, backgroundPaint)

        // 2. Calculate the sweep angle for the current value
        val valueSweepAngle = (value / 100f) * sweepAngle

        // 3. Draw the colored foreground arc
        canvas.drawArc(arcBounds, startAngle, valueSweepAngle, false, foregroundPaint)

        // 4. Draw the value text in the center
        canvas.drawText(
            "${value.toInt()}",
            width / 2f,
            height / 2f + textPaint.textSize / 3,
            textPaint
        )
    }
}