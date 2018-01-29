package com.a21buttons.zoomimageview

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.support.v7.widget.AppCompatImageView
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import kotlin.math.PI
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

class CropImageView : AppCompatImageView {
    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    private val scaleGestureDetector = ScaleGestureDetector(context, MyOnScaleGestureListener())
    private val gestureDetector = GestureDetector(context, MyOnGestureListener())
    private var drawableWidth: Int? = null
    private var drawableHeight: Int? = null
    private var viewportWidth: Int? = null
    private var viewportHeight: Int? = null

    private val viewportPaint = Paint().apply {
        color = 0x80ff0000.toInt()
    }

    init {
        super.setScaleType(ScaleType.MATRIX)
    }

    var aspectRatio: ClosedFloatingPointRange<Float> = 0.8f.rangeTo(1f)
        set(value) {
            if (value.isEmpty()) {
                throw IllegalArgumentException("The range can't be empty")
            }
            if (field != value) {
                calculateViewportSize()
                field = value
                invalidate()
            }
        }

    override fun setScaleType(scaleType: ScaleType) {
        // no-op
    }

    override fun setImageDrawable(drawable: Drawable?) {
        super.setImageDrawable(drawable)
        drawableWidth = drawable?.intrinsicWidth
        drawableHeight = drawable?.intrinsicHeight
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val scaleGestureResult = scaleGestureDetector.onTouchEvent(event)
        val gestureResult = gestureDetector.onTouchEvent(event)
        val superResult = super.onTouchEvent(event)

        return scaleGestureResult || gestureResult || superResult
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val viewportWidth = viewportWidth!!
        val viewportHeight = viewportHeight!!
        val width: Float = width.toFloat()
        val height: Float = height.toFloat()

        val top: Float = (height - viewportHeight) / 2f
        val bottom: Float = height - top
        val left: Float = (width - viewportWidth) / 2f
        val right: Float = width - left

        canvas.drawRect(0f, top, left, bottom, viewportPaint) // left
        canvas.drawRect(0f, 0f, width, top, viewportPaint) // top
        canvas.drawRect(right, top, width, bottom, viewportPaint) // right
        canvas.drawRect(0f, bottom, width, height, viewportPaint) // bottom
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        calculateViewportSize()
    }

    fun rotate(degrees: Float) {
        val vWidth = width - paddingLeft - paddingRight
        val vHeight = height - paddingTop - paddingBottom
        applyMatrixTransformation {
            it.postRotate(degrees, vWidth / 2f, vHeight / 2f)
        }
    }

    private inner class MyOnScaleGestureListener : ScaleGestureDetector.OnScaleGestureListener {
        override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
            return true
        }

        override fun onScale(detector: ScaleGestureDetector): Boolean {
            applyMatrixTransformation {
                val scaleFactor = detector.scaleFactor
                it.postScale(scaleFactor, scaleFactor, detector.focusX, detector.focusY)
            }
            return true
        }

        override fun onScaleEnd(detector: ScaleGestureDetector) {
        }
    }

    private inner class MyOnGestureListener : GestureDetector.OnGestureListener {
        override fun onDown(e: MotionEvent): Boolean {
            return false
        }

        override fun onShowPress(e: MotionEvent) {
        }

        override fun onSingleTapUp(e: MotionEvent): Boolean {
            return false
        }

        override fun onLongPress(e: MotionEvent) {
        }

        override fun onFling(e1: MotionEvent, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
            return false
        }

        override fun onScroll(e1: MotionEvent, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
            applyMatrixTransformation { it.postTranslate(-distanceX, -distanceY) }
            return true
        }
    }

    private val _matrix = Matrix()
    private val _values: FloatArray = FloatArray(9)
    private fun applyMatrixTransformation(action: (Matrix) -> Unit) {
        val dWidth = drawableWidth?.toFloat()
        val dHeight = drawableHeight?.toFloat()
        val viewportWidth = viewportWidth?.toFloat()
        val viewportHeight = viewportHeight?.toFloat()
        val vWidth = (width - paddingLeft - paddingRight).toFloat()
        val vHeight = (height - paddingTop - paddingBottom).toFloat()
        if (dWidth == null || dHeight == null || viewportWidth == null || viewportHeight == null) {
            return
        }
        _matrix.set(imageMatrix)

        action(_matrix)

        _matrix.getValues(_values)
        var scale = sqrt(_values[0] * _values[0] + _values[3] * _values[3])
        val acosTheta = acos(_values[0] / scale)
        val theta = if (asin(_values[3] / scale) >= -0f) acosTheta else 2 * PI.toFloat() - acosTheta
        val degrees = Math.toDegrees(theta.toDouble()).toFloat()
        val px = vWidth / 2f
        val py = vHeight / 2f
        _matrix.postRotate(-degrees, px, py)

        _matrix.getValues(_values)
        var tx = _values[2]
        var ty = _values[5]

        scale = max(scale, max(aspectRatio.start * viewportHeight / dWidth, viewportWidth / (aspectRatio.endInclusive * dHeight)))
        scale = max(scale, min(viewportWidth / dWidth.toFloat(), viewportHeight / dHeight.toFloat()))

        val dScaledWidth = dWidth * scale
        val dScaledHeight = dHeight * scale

        tx = calculateTranslation(tx, dScaledWidth, vWidth, viewportWidth)
        ty = calculateTranslation(ty, dScaledHeight, vHeight, viewportHeight)

        _matrix.setScale(scale, scale)
        _matrix.postTranslate(tx, ty)
        _matrix.postRotate(degrees, px, py)

        imageMatrix = _matrix
    }

    private fun calculateViewportSize() {
        val vWidth = width - paddingLeft - paddingRight
        val vHeight = height - paddingTop - paddingBottom

        val vAspectRatio = vWidth / vHeight.toFloat()
        val viewportWidth: Int
        val viewportHeight: Int
        if (vAspectRatio !in aspectRatio) {
            val viewportAspectRatio = if (aspectRatio.endInclusive < 1f) {
                aspectRatio.endInclusive
            } else if (aspectRatio.start > 1f) {
                aspectRatio.start
            } else {
                1f
            }

            if (viewportAspectRatio > vAspectRatio) {
                viewportWidth = vWidth
                viewportHeight = (viewportWidth * viewportAspectRatio).roundToInt()
            } else {
                viewportHeight = vHeight
                viewportWidth = (viewportHeight / viewportAspectRatio).roundToInt()
            }
        } else {
            viewportWidth = vWidth
            viewportHeight = vHeight
        }
        this.viewportWidth = viewportWidth
        this.viewportHeight = viewportHeight
    }

    private inline fun calculateTranslation(translation: Float, dScaledSize: Float, vSize: Float, viewportSize: Float): Float {
        return if (dScaledSize < viewportSize) {
            (vSize - dScaledSize) / 2f
        } else {
            val a = (vSize - viewportSize) / 2f
            max(min(a, translation), vSize - dScaledSize - a)
        }
    }

    fun getCoppedBitmap(): () -> Bitmap? {
        val rect = croppedRect()
        return if (rect == null) {
            { null }
        } else {
            val d = drawable
            {
                when (d) {
                    is BitmapDrawable -> d.bitmap.crop(rect)
                    else -> throw RuntimeException("Not supported drawable")
                }
            }
        }
    }

    private fun croppedRect(): Rect? {
        val dWidth = drawableWidth?.toFloat()
        val dHeight = drawableHeight?.toFloat()
        val viewportWidth = viewportWidth?.toFloat()
        val viewportHeight = viewportHeight?.toFloat()
        if (dWidth == null || dHeight == null || viewportWidth == null || viewportHeight == null) {
            return null
        }
        imageMatrix.getValues(_values)
        val scale = sqrt(_values[0] * _values[0] + _values[3] * _values[3])

        val left = -min(0f, (_values[2] - (width - viewportWidth) / 2) / scale)
        val top = -min(0f, (_values[5] - (height - viewportHeight) / 2) / scale)
        val right = left + min(dWidth, viewportWidth / scale)
        val bottom = top + min(dHeight, viewportHeight / scale)

        val x = max(0f, left).roundToInt()
        val y = max(0f, top).roundToInt()
        val x2 = min(dWidth, right).roundToInt()
        val y2 = min(dHeight, bottom).roundToInt()

        return Rect(x, y, x2, y2)
    }
}

private fun Bitmap.crop(rect: Rect): Bitmap {
    return Bitmap.createBitmap(this, rect.left, rect.top, rect.right - rect.left, rect.bottom - rect.top)
}
