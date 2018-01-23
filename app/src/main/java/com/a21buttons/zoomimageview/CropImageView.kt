package com.a21buttons.zoomimageview

import android.content.Context
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.drawable.Drawable
import android.support.v7.widget.AppCompatImageView
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

class CropImageView : AppCompatImageView {
    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    private val scaleGestureDetector = ScaleGestureDetector(context, MyOnScaleGestureListener())
    private val gestureDetector = GestureDetector(context, MyOnGestureListener())
    private var drawableWith: Int? = null
    private var drawableHeight: Int? = null

    init {
        super.setScaleType(ScaleType.MATRIX)
    }

    override fun setScaleType(scaleType: ScaleType) {
        // no-op
    }

    override fun setImageDrawable(drawable: Drawable?) {
        super.setImageDrawable(drawable)
        drawableWith = drawable?.intrinsicWidth
        drawableHeight = drawable?.intrinsicHeight
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val scaleGestureResult = scaleGestureDetector.onTouchEvent(event)
        val gestureResult = gestureDetector.onTouchEvent(event)
        val superResult = super.onTouchEvent(event)

        return scaleGestureResult || gestureResult || superResult
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
        val dWidth = drawableWith
        val dHeight = drawableHeight
        if (dWidth == null || dHeight == null) {
            return
        }
        _matrix.set(imageMatrix)

        action(_matrix)

        _matrix.getValues(_values)
        var tx = _values[2]
        var ty = _values[5]
        var scale = sqrt(_values[0] * _values[0] + _values[3] * _values[3])

        val vWidth = width - paddingLeft - paddingRight
        val vHeight = height - paddingTop - paddingBottom

        scale = max(min(vWidth / dWidth.toFloat(), vHeight / dHeight.toFloat()), scale)

        tx = calculateTranslation(tx, scale, dWidth, vWidth)
        ty = calculateTranslation(ty, scale, dHeight, vHeight)

        // TODO get the visible rectangle

        _values[0] = scale
        _values[1] = 0f
        _values[2] = tx
        _values[3] = 0f
        _values[4] = scale
        _values[5] = ty

        _matrix.setValues(_values)

        imageMatrix = _matrix
    }

    private inline fun calculateTranslation(translation: Float, scale: Float, dSize: Int, vSize: Int): Float {
        val dScaledSize = dSize * scale
        return if (dScaledSize < vSize) {
            (vSize - dScaledSize) / 2f
        } else {
            max(min(0f, translation), vSize - dScaledSize)
        }
    }
}
