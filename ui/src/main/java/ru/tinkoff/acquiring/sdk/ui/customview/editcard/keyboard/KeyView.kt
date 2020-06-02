/*
 * Copyright © 2020 Tinkoff Bank
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package ru.tinkoff.acquiring.sdk.ui.customview.editcard.keyboard

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import ru.tinkoff.acquiring.sdk.R
import ru.tinkoff.acquiring.sdk.ui.customview.editcard.dpToPx
import kotlin.math.max

/**
 * @author Mariya Chernyadieva
 */
internal class KeyView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var keyCode = 0
        set(value) {
            field = value
            contentText = value.toString()
        }
    var contentImage: Bitmap? = null
    var textColor: Int = Color.WHITE
        set(value) {
            field = value
            contentPaint.color = value
            contentPaint.colorFilter = PorterDuffColorFilter(value, PorterDuff.Mode.SRC_IN)
            invalidate()
        }
    var keyColor: Int = 0
        set(value) {
            field = value
            setBackgroundColor(value)
            circlePaint.color = value
            invalidate()
        }

    private var contentText: String? = null
    private var drawingPressAnimation = false
    private var textWidth = 0f
    private var contentPaint: Paint = Paint()
    private var circleAnimator: ValueAnimator? = null
    private var circleCenter: PointF = PointF()
    private var circleRadius = 0f
    private var circlePaint: Paint = Paint()

    init {
        isClickable = true
        with(contentPaint) {
            textSize = if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
                KEY_DEFAULT_TEXT_SIZE_DP.dpToPx(context)
            } else {
                KEY_LANDSCAPE_TEXT_SIZE_DP.dpToPx(context)
            }
            isAntiAlias = true
            color = ContextCompat.getColor(context, R.color.acq_colorKeyText)
            typeface = Typeface.create(DEFAULT_FONT_FAMILY, Typeface.NORMAL)
        }
        val colorKeyCircle = ContextCompat.getColor(context, R.color.acq_colorKeyCircle)
        circlePaint.colorFilter = PorterDuffColorFilter(colorKeyCircle, PorterDuff.Mode.OVERLAY)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action != MotionEvent.ACTION_DOWN) {
            return super.onTouchEvent(event)
        }
        if (drawingPressAnimation) {
            circleAnimator!!.cancel()
        }
        drawingPressAnimation = true
        circleCenter = PointF(event.x, event.y)
        circleAnimator = createCircleAnimator()
        circleAnimator!!.start()
        return super.onTouchEvent(event)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val width = MeasureSpec.getSize(widthMeasureSpec) * KEY_SCALE_X
        val height = MeasureSpec.getSize(heightMeasureSpec) * KEY_SCALE_Y
        setMeasuredDimension(width.toInt(), height.toInt())
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (contentText != null && keyCode != KEYCODE_DEL) {
            textWidth = contentPaint.measureText(contentText)
            val x = width / 2 - textWidth / 2
            val y = height / 2 - (contentPaint.descent() + contentPaint.ascent()) / 2
            canvas.drawText(contentText!!, x, y, contentPaint)
        }
        if (contentImage != null) {
            canvas.drawBitmap(contentImage!!, width / 2 - contentImage!!.width / 2.toFloat(),
                    height / 2 - contentImage!!.height / 2.toFloat(), contentPaint)
        }
        if (drawingPressAnimation) {
            canvas.drawCircle(circleCenter.x, circleCenter.y, circleRadius, circlePaint)
        }
    }

    private fun createCircleAnimator(): ValueAnimator {
        val maxDimen = max(width, height) * 0.8f
        return ValueAnimator.ofFloat(0f, maxDimen).apply {
            duration = CIRCLE_ANIMATION_DURATION_MILLIS.toLong()
            addUpdateListener { animation ->
                circleRadius = animation.animatedValue as Float
                invalidate()
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    drawingPressAnimation = false
                    invalidate()
                }
            })
        }
    }

    companion object {

        private const val KEYCODE_DEL = 10
        private const val KEY_SCALE_X = 0.333f
        private const val KEY_SCALE_Y = 0.25f
        private const val KEY_DEFAULT_TEXT_SIZE_DP = 34f
        private const val KEY_LANDSCAPE_TEXT_SIZE_DP = 24f
        private const val CIRCLE_ANIMATION_DURATION_MILLIS = 200
        private const val DEFAULT_FONT_FAMILY = "sans-serif-light"
    }
}