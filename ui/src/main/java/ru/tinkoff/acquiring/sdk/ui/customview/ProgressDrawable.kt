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

package ru.tinkoff.acquiring.sdk.ui.customview

import android.animation.Animator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.RectF
import android.graphics.Rect
import android.graphics.Paint
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable
import android.util.TypedValue
import android.view.animation.LinearInterpolator
import androidx.annotation.FloatRange
import androidx.core.content.ContextCompat
import ru.tinkoff.acquiring.sdk.R

class ProgressDrawable(
        context: Context,
        color: Int = ContextCompat.getColor(context, R.color.acq_colorAccent)
) : Drawable() {

    private var startAngle = -90f
    private var endAngle = 0f
    private var rotateAngle = 0f

    private var animator: Animator? = null
    private var progressAnimator: Animator? = null

    private val rect = RectF()

    private val progressBarSmallStrokeWidth = 2f.dpToPx(context)
    private val progressBarStrokeWidth = 4f.dpToPx(context)
    private val progressBarMiddleSize = 40f.dpToPx(context)

    private val arcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeCap = Paint.Cap.ROUND
        style = Paint.Style.STROKE
        this.color = color
    }

    var isEverChanging: Boolean = true
        set(value) {
            if (field == value) return
            field = value
            if (value) {
                if (progressAnimator?.isRunning == true) {
                    progressAnimator?.cancel()
                    progressAnimator = null
                }
            }
            invalidateSelf()
        }

    var isRotate: Boolean = true
        set(value) {
            field = value
            invalidateSelf()
        }

    var progressColor: Int
        set(value) {
            arcPaint.color = value
            invalidateSelf()
        }
        get() = arcPaint.color

    override fun onBoundsChange(bounds: Rect) {
        super.onBoundsChange(bounds)
        val currentStrokeWidth = if (bounds.right - bounds.left >= progressBarMiddleSize) {
            progressBarStrokeWidth
        } else {
            progressBarSmallStrokeWidth
        }

        arcPaint.strokeWidth = currentStrokeWidth

        rect.set(
                bounds.left + currentStrokeWidth,
                bounds.top + currentStrokeWidth,
                bounds.left + bounds.width() - currentStrokeWidth,
                bounds.top + bounds.height() - currentStrokeWidth
        )
    }

    override fun draw(canvas: Canvas) {
        if (animator == null) {
            animator = initAnimator().apply {
                start()
            }
        }

        canvas.drawArc(rect, startAngle + rotateAngle, endAngle, false, arcPaint)
    }

    override fun setAlpha(alpha: Int) {
        arcPaint.alpha = alpha
    }

    override fun getOpacity() = PixelFormat.TRANSLUCENT

    override fun setColorFilter(colorFilter: ColorFilter?) {
        arcPaint.colorFilter = colorFilter
    }

    override fun setVisible(visible: Boolean, restart: Boolean): Boolean {
        if (visible) {
            animator?.start()
        } else {
            animator?.cancel()
        }
        return super.setVisible(visible, restart)
    }

    fun setProgress(@FloatRange(from = 0.0, to = 1.0) progress: Float) {
        val value = if (progress < 0) 0f else if (progress > 1) 1f else progress
        progressAnimator?.let {
            if (it.isRunning) it.cancel()
        }
        progressAnimator = initProgressAnimator(value).apply { start() }
        isEverChanging = false
    }

    private fun setStartAngle(float: Float) {
        startAngle = float
        invalidateSelf()
    }

    private fun setEndAngle(float: Float) {
        endAngle = float
        invalidateSelf()
    }

    private fun setRotateAngle(float: Float) {
        rotateAngle = float
        invalidateSelf()
    }

    private fun initAnimator(): ValueAnimator {
        return ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 2000L
            interpolator = LinearInterpolator()
            repeatCount = ValueAnimator.INFINITE
            addUpdateListener {
                val curValue = animatedValue as Float
                if (isEverChanging) {
                    val a: Float = if (curValue > 0.5f) 2 * curValue - 1 else 0f
                    val b: Float = if (curValue > 0.5) 1f else 2 * curValue
                    setStartAngle(360 * a - 90)
                    setEndAngle(360 * (b - a))
                }
                if (isRotate) {
                    setRotateAngle(360 * curValue - 180)
                }
            }
        }
    }

    private fun initProgressAnimator(newValue: Float): Animator {
        return ValueAnimator.ofFloat(endAngle, newValue * 360).apply {
            duration = 600L
            interpolator = LinearInterpolator()

            addUpdateListener {
                val curValue = animatedValue as Float
                setStartAngle(-90f)
                setEndAngle(curValue)
            }
        }
    }

    private fun Float.dpToPx(context: Context): Float {
        return TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                this,
                context.resources.displayMetrics
        )
    }
}