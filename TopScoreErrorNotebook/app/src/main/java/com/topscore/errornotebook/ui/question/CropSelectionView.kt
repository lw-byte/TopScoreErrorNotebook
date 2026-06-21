package com.topscore.errornotebook.ui.question

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.topscore.errornotebook.domain.model.Rect as DomainRect

/**
 * Custom view for selecting a question region from a captured image.
 * Displays the image with a draggable/resizable rectangle overlay.
 */
class CropSelectionView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // The image to display
    private var bitmap: Bitmap? = null

    // Destination rect for the bitmap (where it's drawn on screen)
    private val bitmapRect = RectF()

    // Crop rectangle in screen coordinates
    private val cropRect = RectF()

    // Touch handling
    private var touchMode = TouchMode.NONE
    private var lastTouchX = 0f
    private var lastTouchY = 0f

    // Handle size
    private val handleSize = 48f
    private val touchTolerance = 48f

    // Paint objects
    private val bitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val overlayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(160, 0, 0, 0)
        style = Paint.Style.FILL
    }

    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }

    private val handlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }

    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(100, 255, 255, 255)
        style = Paint.Style.STROKE
        strokeWidth = 1f
    }

    private val instructionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 42f
        textAlign = Paint.Align.CENTER
    }

    enum class TouchMode {
        NONE,
        MOVE,
        RESIZE_TOP_LEFT,
        RESIZE_TOP_RIGHT,
        RESIZE_BOTTOM_LEFT,
        RESIZE_BOTTOM_RIGHT,
        RESIZE_TOP,
        RESIZE_BOTTOM,
        RESIZE_LEFT,
        RESIZE_RIGHT
    }

    /**
     * Set the bitmap to display and initialize crop rectangle
     */
    fun setImageBitmap(bmp: Bitmap) {
        bitmap = bmp
        // Initialize crop rect after layout
        post {
            initCropRect()
            invalidate()
        }
    }

    /**
     * Get the bitmap currently displayed
     */
    fun getImageBitmap(): Bitmap? = bitmap

    private fun initCropRect() {
        if (width == 0 || height == 0 || bitmap == null) return

        // Calculate bitmap display rect
        calculateBitmapRect()

        // Default crop rect is 80% of the image area, centered
        val defaultRect = RectF(
            bitmapRect.left + bitmapRect.width() * 0.1f,
            bitmapRect.top + bitmapRect.height() * 0.1f,
            bitmapRect.right - bitmapRect.width() * 0.1f,
            bitmapRect.bottom - bitmapRect.height() * 0.1f
        )
        cropRect.set(defaultRect)
    }

    private fun calculateBitmapRect() {
        if (width == 0 || height == 0 || bitmap == null) return

        val bmpWidth = bitmap!!.width.toFloat()
        val bmpHeight = bitmap!!.height.toFloat()
        val viewWidth = width.toFloat()
        val viewHeight = height.toFloat()

        // Calculate scale to fit
        val scale = minOf(viewWidth / bmpWidth, viewHeight / bmpHeight)

        val scaledWidth = bmpWidth * scale
        val scaledHeight = bmpHeight * scale

        // Center in view
        val left = (viewWidth - scaledWidth) / 2f
        val top = (viewHeight - scaledHeight) / 2f

        bitmapRect.set(left, top, left + scaledWidth, top + scaledHeight)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (bitmap != null) {
            calculateBitmapRect()
            initCropRect()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Draw bitmap
        bitmap?.let { bmp ->
            canvas.drawBitmap(bmp, null, bitmapRect, bitmapPaint)
        }

        // Draw dark overlay outside crop area
        // Top
        canvas.drawRect(0f, 0f, width.toFloat(), cropRect.top, overlayPaint)
        // Bottom
        canvas.drawRect(0f, cropRect.bottom, width.toFloat(), height.toFloat(), overlayPaint)
        // Left
        canvas.drawRect(0f, cropRect.top, cropRect.left, cropRect.bottom, overlayPaint)
        // Right
        canvas.drawRect(cropRect.right, cropRect.top, width.toFloat(), cropRect.bottom, overlayPaint)

        // Draw grid lines (rule of thirds)
        val thirdWidth = cropRect.width() / 3f
        val thirdHeight = cropRect.height() / 3f

        // Vertical grid lines
        canvas.drawLine(cropRect.left + thirdWidth, cropRect.top, cropRect.left + thirdWidth, cropRect.bottom, gridPaint)
        canvas.drawLine(cropRect.left + thirdWidth * 2, cropRect.top, cropRect.left + thirdWidth * 2, cropRect.bottom, gridPaint)

        // Horizontal grid lines
        canvas.drawLine(cropRect.left, cropRect.top + thirdHeight, cropRect.right, cropRect.top + thirdHeight, gridPaint)
        canvas.drawLine(cropRect.left, cropRect.top + thirdHeight * 2, cropRect.right, cropRect.top + thirdHeight * 2, gridPaint)

        // Draw border
        canvas.drawRect(cropRect, borderPaint)

        // Draw corner handles
        drawHandle(canvas, cropRect.left, cropRect.top)
        drawHandle(canvas, cropRect.right, cropRect.top)
        drawHandle(canvas, cropRect.left, cropRect.bottom)
        drawHandle(canvas, cropRect.right, cropRect.bottom)

        // Draw edge handles
        drawEdgeHandle(canvas, cropRect.centerX(), cropRect.top, true)
        drawEdgeHandle(canvas, cropRect.centerX(), cropRect.bottom, true)
        drawEdgeHandle(canvas, cropRect.left, cropRect.centerY(), false)
        drawEdgeHandle(canvas, cropRect.right, cropRect.centerY(), false)
    }

    private fun drawHandle(canvas: Canvas, x: Float, y: Float) {
        canvas.drawCircle(x, y, handleSize / 2f, handlePaint)
    }

    private fun drawEdgeHandle(canvas: Canvas, x: Float, y: Float, horizontal: Boolean) {
        if (horizontal) {
            canvas.drawRect(x - handleSize / 3f, y - handleSize / 4f, x + handleSize / 3f, y + handleSize / 4f, handlePaint)
        } else {
            canvas.drawRect(x - handleSize / 4f, y - handleSize / 3f, x + handleSize / 4f, y + handleSize / 3f, handlePaint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                lastTouchX = event.x
                lastTouchY = event.y
                touchMode = getTouchMode(event.x, event.y)
                return touchMode != TouchMode.NONE
            }
            MotionEvent.ACTION_MOVE -> {
                if (touchMode != TouchMode.NONE) {
                    val dx = event.x - lastTouchX
                    val dy = event.y - lastTouchY

                    when (touchMode) {
                        TouchMode.MOVE -> {
                            cropRect.offset(dx, dy)
                        }
                        TouchMode.RESIZE_TOP_LEFT -> {
                            cropRect.left = clamp(cropRect.left + dx, bitmapRect.left, cropRect.right - 50f)
                            cropRect.top = clamp(cropRect.top + dy, bitmapRect.top, cropRect.bottom - 50f)
                        }
                        TouchMode.RESIZE_TOP_RIGHT -> {
                            cropRect.right = clamp(cropRect.right + dx, cropRect.left + 50f, bitmapRect.right)
                            cropRect.top = clamp(cropRect.top + dy, bitmapRect.top, cropRect.bottom - 50f)
                        }
                        TouchMode.RESIZE_BOTTOM_LEFT -> {
                            cropRect.left = clamp(cropRect.left + dx, bitmapRect.left, cropRect.right - 50f)
                            cropRect.bottom = clamp(cropRect.bottom + dy, cropRect.top + 50f, bitmapRect.bottom)
                        }
                        TouchMode.RESIZE_BOTTOM_RIGHT -> {
                            cropRect.right = clamp(cropRect.right + dx, cropRect.left + 50f, bitmapRect.right)
                            cropRect.bottom = clamp(cropRect.bottom + dy, cropRect.top + 50f, bitmapRect.bottom)
                        }
                        TouchMode.RESIZE_TOP -> {
                            cropRect.top = clamp(cropRect.top + dy, bitmapRect.top, cropRect.bottom - 50f)
                        }
                        TouchMode.RESIZE_BOTTOM -> {
                            cropRect.bottom = clamp(cropRect.bottom + dy, cropRect.top + 50f, bitmapRect.bottom)
                        }
                        TouchMode.RESIZE_LEFT -> {
                            cropRect.left = clamp(cropRect.left + dx, bitmapRect.left, cropRect.right - 50f)
                        }
                        TouchMode.RESIZE_RIGHT -> {
                            cropRect.right = clamp(cropRect.right + dx, cropRect.left + 50f, bitmapRect.right)
                        }
                        else -> {}
                    }

                    // Clamp to bitmap bounds
                    cropRect.intersect(bitmapRect)

                    lastTouchX = event.x
                    lastTouchY = event.y
                    invalidate()
                    return true
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                touchMode = TouchMode.NONE
            }
        }
        return super.onTouchEvent(event)
    }

    private fun getTouchMode(x: Float, y: Float): TouchMode {
        // Check corners first
        if (isNear(x, y, cropRect.left, cropRect.top)) return TouchMode.RESIZE_TOP_LEFT
        if (isNear(x, y, cropRect.right, cropRect.top)) return TouchMode.RESIZE_TOP_RIGHT
        if (isNear(x, y, cropRect.left, cropRect.bottom)) return TouchMode.RESIZE_BOTTOM_LEFT
        if (isNear(x, y, cropRect.right, cropRect.bottom)) return TouchMode.RESIZE_BOTTOM_RIGHT

        // Check edges
        if (isNearVertical(x, cropRect.left) && y in cropRect.top..cropRect.bottom) return TouchMode.RESIZE_LEFT
        if (isNearVertical(x, cropRect.right) && y in cropRect.top..cropRect.bottom) return TouchMode.RESIZE_RIGHT
        if (isNearHorizontal(y, cropRect.top) && x in cropRect.left..cropRect.right) return TouchMode.RESIZE_TOP
        if (isNearHorizontal(y, cropRect.bottom) && x in cropRect.left..cropRect.right) return TouchMode.RESIZE_BOTTOM

        // Check if inside rect for move
        if (cropRect.contains(x, y)) return TouchMode.MOVE

        return TouchMode.NONE
    }

    private fun isNear(x: Float, y: Float, targetX: Float, targetY: Float): Boolean {
        return kotlin.math.abs(x - targetX) <= touchTolerance && kotlin.math.abs(y - targetY) <= touchTolerance
    }

    private fun isNearVertical(x: Float, targetX: Float): Boolean {
        return kotlin.math.abs(x - targetX) <= touchTolerance
    }

    private fun isNearHorizontal(y: Float, targetY: Float): Boolean {
        return kotlin.math.abs(y - targetY) <= touchTolerance
    }

    private fun clamp(value: Float, min: Float, max: Float): Float {
        return value.coerceIn(min, max)
    }

    /**
     * Get the crop rectangle in normalized coordinates (0-1 range) relative to original bitmap
     */
    fun getCropRect(): DomainRect? {
        val bmp = bitmap ?: return null

        // Convert screen coordinates to bitmap coordinates
        val scaleX = bmp.width / bitmapRect.width()
        val scaleY = bmp.height / bitmapRect.height()

        val left = ((cropRect.left - bitmapRect.left) * scaleX).coerceIn(0f, bmp.width.toFloat())
        val top = ((cropRect.top - bitmapRect.top) * scaleY).coerceIn(0f, bmp.height.toFloat())
        val right = ((cropRect.right - bitmapRect.left) * scaleX).coerceIn(0f, bmp.width.toFloat())
        val bottom = ((cropRect.bottom - bitmapRect.top) * scaleY).coerceIn(0f, bmp.height.toFloat())

        return DomainRect(
            x = left,
            y = top,
            width = right - left,
            height = bottom - top
        )
    }

    /**
     * Crop the bitmap using the current selection
     */
    fun cropBitmap(): Bitmap? {
        val bmp = bitmap ?: return null
        val crop = getCropRect() ?: return null

        val x = crop.x.toInt().coerceIn(0, bmp.width - 1)
        val y = crop.y.toInt().coerceIn(0, bmp.height - 1)
        val w = crop.width.toInt().coerceIn(1, bmp.width - x)
        val h = crop.height.toInt().coerceIn(1, bmp.height - y)

        return Bitmap.createBitmap(bmp, x, y, w, h)
    }

    /**
     * Reset crop selection to default position
     */
    fun resetCrop() {
        initCropRect()
        invalidate()
    }
}
