package com.creator.unnitv.live

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.os.Handler
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import com.ksyun.media.streamer.capture.camera.ICameraHintView
import java.util.*

/**
 * View to show camera focus rect and zoom value.
 */
class CameraHintView : View, ICameraHintView {
    private var mShowRect = false
    private var mRect: Rect? = null
    private var mFocusPaint: Paint? = null
    private var mHandler: Handler? = null
    private var mHideRect: Runnable? = null
    private var mZoomPaint: Paint? = null
    private var mShowZoomRatio = false
    private var mZoomRatio = 0f
    private var mHideZoom: Runnable? = null

    constructor(context: Context?) : super(context) {
        init()
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        init()
    }

    private fun init() {
        mHandler = Handler()
        mHideRect = Runnable {
            mShowRect = false
            invalidate()
        }
        mHideZoom = Runnable {
            mShowZoomRatio = false
            invalidate()
        }
        mFocusPaint = Paint()
        mFocusPaint!!.style = Paint.Style.STROKE
        mFocusPaint!!.strokeWidth = dpToPx(1f)
        mZoomPaint = Paint()
        mZoomPaint!!.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        mZoomPaint!!.style = Paint.Style.FILL_AND_STROKE
        mZoomPaint!!.strokeWidth = 1f
        mZoomPaint!!.color = COLOR_ZOOM_TEXT
        mZoomPaint!!.textAlign = Paint.Align.CENTER
        mZoomPaint!!.textSize = dpToPx(16f)
        mZoomPaint!!.isAntiAlias = true
        mZoomPaint!!.isFilterBitmap = true
    }

    fun hideAll() {
        mHandler!!.removeCallbacks(mHideRect!!)
        mHandler!!.removeCallbacks(mHideZoom!!)
        mHandler!!.post(mHideRect!!)
        mHandler!!.post(mHideZoom!!)
    }

    override fun updateZoomRatio(`val`: Float) {
        mShowZoomRatio = true
        mZoomRatio = `val`
        mHandler!!.removeCallbacks(mHideZoom!!)
        if (`val` == 1.0f) {
            mHandler!!.postDelayed(mHideZoom!!, 1000)
        }
        invalidate()
    }

    override fun startFocus(rect: Rect) {
        mShowRect = true
        mRect = rect
        mFocusPaint!!.color = COLOR_FOCUSING
        mHandler!!.removeCallbacks(mHideRect!!)
        mHandler!!.postDelayed(mHideRect!!, 3000)
        invalidate()
    }

    override fun setFocused(success: Boolean) {
        mShowRect = true
        mFocusPaint!!.color =
            if (success) COLOR_FOCUSED else COLOR_UNFOCUSED
        mHandler!!.removeCallbacks(mHideRect!!)
        mHandler!!.postDelayed(mHideRect!!, 400)
        invalidate()
    }

    public override fun onDraw(canvas: Canvas) {
        if (mShowRect) {
            mFocusPaint!!.strokeWidth = 3f
            mFocusPaint!!.style = Paint.Style.STROKE
            canvas.drawRect(
                mRect!!.left.toFloat(),
                mRect!!.top.toFloat(),
                mRect!!.right.toFloat(),
                mRect!!.bottom.toFloat(),
                mFocusPaint!!
            )
        }
        if (mShowZoomRatio) {
            val text = String.format(Locale.getDefault(), "%.1f", mZoomRatio) + "x"
            val x = (width * 0.5f).toInt()
            val y = dpToPx(48f).toInt()
            canvas.drawText(text, x.toFloat(), y.toFloat(), mZoomPaint!!)
        }
    }

    private fun dpToPx(dp: Float): Float {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, dp,
            resources.displayMetrics
        )
    }

    companion object {
        private const val TAG = "CameraHintView"
        private const val COLOR_FOCUSING = -0x11282829
        private const val COLOR_FOCUSED = -0x11ff0100
        private const val COLOR_UNFOCUSED = -0x11010000
        private const val COLOR_ZOOM_TEXT = -0x1
    }
}