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
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.graphics.drawable.ShapeDrawable
import android.os.Build
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.annotation.IntDef
import androidx.core.content.ContextCompat
import ru.tinkoff.acquiring.sdk.R
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

internal class ResultNotificationView : View {

    @IntDef(value = [INVISIBLE, LOADING, SUCCESS, HIDING])
    @Retention(AnnotationRetention.SOURCE)
    annotation class Status

    companion object {
        const val INVISIBLE = 0
        const val LOADING = 1
        const val SUCCESS = 2
        const val HIDING = 3
    }

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(
            context,
            attrs,
            defStyle
    ) {
        progressDrawable = ProgressDrawable(context).apply {
            callback = this@ResultNotificationView
        }

        setLayerType(LAYER_TYPE_SOFTWARE, null)
        background = ShapeDrawable().apply { paint.color = Color.TRANSPARENT }
    }

    private val durationFadeAnimation = 200L
    private val durationOfDisplayViewWithText = 4000L
    private val durationOfDisplayViewWithoutText = 2000L
    private val maxViewWidth = 290.dpToPx()
    private val maxTextLength = 70

    private var progressAnimation: ValueAnimator? = null
    private var actionAnimation: ValueAnimator? = null
    private var hideAnimation: ValueAnimator? = null

    private var isAttached: Boolean = false

    @Status
    var status: Int = INVISIBLE
        private set

    var halfWidth = 0
    var halfHeight = 0
    var successAreaTop = 0

    private var textWidth = 0
    private val sp14 =
            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 14f, context.resources.displayMetrics)
    private val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.acq_colorText)
        textSize = sp14
        alpha = 0
    }
    private val textSideMargin = 24.dpToPx()
    private val textTopMargin = 16.dpToPx()
    private var staticLayout: StaticLayout? = null

    private val iconSize = context.resources.getDimensionPixelOffset(R.dimen.acq_notification_icon_size)
    private val iconMargin = 32.dpToPx()
    private var icon: Drawable? = ContextCompat.getDrawable(context, R.drawable.acq_icon_done)

    private var progressDrawable: ProgressDrawable? = null
    private val progressBarSize =
            context.resources.getDimensionPixelSize(R.dimen.acq_notification_progressbar_size)
    private var progressAreaWidth = 56.dpToPx()

    private val areaRadius = 4.dpToPx()
    private var areaWidth = 0
    private val minAreaWidthWithText = 138.dpToPx()
    private var areaHeight = 0
    private val areaRect = RectF()
    private val shadowRadius = 20.dpToPx()
    private val shadowDy = 5.dpToPx()
    private val areaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.acq_colorMain)
        setShadowLayer(shadowRadius.toFloat(), 0f, shadowDy.toFloat(), 0X19000000)
    }

    private var progressFactor: Float = 1f
    private var actionFactor: Float = 1f

    private val action = Runnable { hide() }

    private val listeners = mutableListOf<ResultNotificationViewListener>()

    fun addListener(observer: ResultNotificationViewListener) {
        listeners.add(observer)
    }

    fun removeListener(observer: ResultNotificationViewListener) {
        listeners.remove(observer)
    }

    fun stopAllAnimation() {
        progressAnimation?.cancel()
        actionAnimation?.cancel()
        hideAnimation?.cancel()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        listeners.forEach { it.onClick(this.status, event) }
        return true
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        halfWidth = width / 2
        halfHeight = height / 2
        layoutAreaRect()
        layoutProgress(progressFactor)
        layoutAction(actionFactor)
    }

    private fun layoutAreaRect() {
        val successAreaLeft: Int
        val successAreaRight: Int
        val successAreaBottom: Int

        if (width > areaWidth) {
            successAreaLeft = halfWidth - areaWidth / 2
            successAreaRight = successAreaLeft + areaWidth
        } else {
            successAreaLeft = left
            successAreaRight = right
        }

        if (height > areaHeight) {
            successAreaTop = halfHeight - areaHeight / 2
            successAreaBottom = successAreaTop + areaHeight
        } else {
            successAreaTop = top
            successAreaBottom = bottom
        }
        areaRect.set(
                successAreaLeft.toFloat(),
                successAreaTop.toFloat(),
                successAreaRight.toFloat(),
                successAreaBottom.toFloat()
        )
    }

    private fun layoutProgress(factor: Float) {
        val halfProgressSize = (progressBarSize * factor).toInt() / 2
        progressDrawable?.setBounds(
                halfWidth - halfProgressSize,
                halfHeight - halfProgressSize,
                halfWidth + halfProgressSize,
                halfHeight + halfProgressSize
        )
    }

    private fun layoutAction(factor: Float) {
        val halfIconSize =
                ((progressBarSize + (this.iconSize - progressBarSize) * factor) / 2).toInt()
        val newIconCenterY = if (staticLayout == null) {
            halfHeight
        } else {
            (halfHeight + (successAreaTop + iconMargin + halfIconSize - halfHeight) * factor).toInt()
        }

        icon?.setBounds(
                (halfWidth - halfIconSize),
                newIconCenterY - halfIconSize,
                halfWidth + halfIconSize,
                newIconCenterY + halfIconSize
        )

        icon?.alpha = (255 * (0.3 + 0.7 * factor)).toInt()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawRoundRect(areaRect, areaRadius.toFloat(), areaRadius.toFloat(), areaPaint)

        when (status) {
            LOADING -> progressDrawable?.draw(canvas)
            SUCCESS, HIDING -> {
                staticLayout?.let {
                    canvas.save()
                    canvas.translate(
                            ((width - it.width) / 2).toFloat(),
                            areaRect.top + iconMargin + iconSize + textTopMargin
                    )
                    it.draw(canvas)
                    canvas.restore()
                }
                icon?.draw(canvas)
            }
        }
    }

    fun showProgress() {
        status = LOADING
        listeners.forEach { it.onProgress() }

        var factor: Float

        progressAnimation = ValueAnimator.ofFloat(0f, 1f).apply {
            addUpdateListener {
                factor = animatedValue as Float
                areaWidth = (progressAreaWidth * factor).toInt()
                areaHeight = (progressAreaWidth * factor).toInt()
                layoutAreaRect()
                progressFactor = factor
                layoutProgress(factor)
                invalidate()
            }
            start()
        }
    }

    fun showAction(
            text: String? = null,
            icon: Drawable? = ContextCompat.getDrawable(context, R.drawable.acq_icon_done)
    ) {
        this.icon = icon
        status = SUCCESS

        val formattedText = if (text.isNullOrBlank()) {
            null
        } else {
            val modifiedText =
                    if (text.length > maxTextLength) text.substring(0, maxTextLength) else text
            textWidth =
                    min(textPaint.measureText(modifiedText).toInt(), maxViewWidth - 2 * textSideMargin)
            modifiedText
        }

        val spacingMultiplier = 1f
        val spacingAddition = 0f
        val includePadding = false

        @Suppress("DEPRECATION")
        staticLayout = when {
            formattedText.isNullOrBlank() -> null
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ->
                StaticLayout.Builder.obtain(
                        formattedText,
                        0,
                        formattedText.length,
                        textPaint,
                        textWidth
                )
                        .setAlignment(Layout.Alignment.ALIGN_CENTER)
                        .setLineSpacing(spacingAddition, spacingMultiplier)
                        .setIncludePad(includePadding)
                        .build()
            else -> StaticLayout(
                    formattedText,
                    textPaint,
                    textWidth,
                    Layout.Alignment.ALIGN_CENTER,
                    spacingMultiplier,
                    spacingAddition,
                    includePadding
            )
        }
        staticLayout?.let {
            var maxStrokeWidth = 0
            for (i in 0 until it.lineCount) {
                maxStrokeWidth = max(maxStrokeWidth, it.getLineWidth(i).roundToInt())
            }
            textWidth = min(textWidth, maxStrokeWidth)
        }

        val newWidth = if (textWidth == 0) {
            2 * iconMargin + iconSize
        } else {
            max(textWidth + 2 * textSideMargin, minAreaWidthWithText)
        }
        val startWidthSize = areaRect.width().toInt()

        val newHeight = iconSize + 2 * iconMargin +
                (staticLayout?.let { textTopMargin + it.height } ?: 0)

        val startHeightSize = areaRect.height().toInt()

        listeners.forEach { it.onAction() }

        var factor: Float

        actionAnimation = ValueAnimator.ofFloat(0f, 1f).apply {
            interpolator = DecelerateInterpolator(2.5f)
            addUpdateListener {
                factor = animatedValue as Float
                areaWidth = startWidthSize + ((newWidth - startWidthSize) * factor).toInt()
                areaHeight = startHeightSize + ((newHeight - startHeightSize) * factor).toInt()
                textPaint.apply {
                    if (factor > 0.7) alpha = (255 * (-0.6 + 1.6 * factor)).toInt()
                    textSize = sp14 * factor
                }
                actionFactor = factor
                layoutAreaRect()
                layoutAction(factor)
                invalidate()
            }
            addListener(object : Animator.AnimatorListener {
                override fun onAnimationRepeat(animation: Animator) {}

                override fun onAnimationEnd(animation: Animator) {
                    val delay = if (formattedText.isNullOrBlank()) {
                        durationOfDisplayViewWithoutText
                    } else {
                        durationOfDisplayViewWithText
                    }
                    this@ResultNotificationView.postDelayed(action, delay)
                }

                override fun onAnimationCancel(animation: Animator) {}

                override fun onAnimationStart(animation: Animator) {}
            })
            start()
        }
    }

    fun showSuccess(text: String? = null) {
        showAction(text, ContextCompat.getDrawable(context, R.drawable.acq_icon_done))
    }

    fun showError(text: String? = null) {
        showAction(text, ContextCompat.getDrawable(context, R.drawable.acq_icon_error))
    }

    fun showWarning(text: String? = null) {
        showAction(text, ContextCompat.getDrawable(context, R.drawable.acq_icon_warning))
    }

    fun hide() {
        status = HIDING
        removeCallbacks(action)

        hideAnimation = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = durationFadeAnimation
            addUpdateListener {
                this@ResultNotificationView.alpha = (1 - animatedValue as Float)
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    this@ResultNotificationView.listeners.forEach {
                        if (context is Activity && !(context as Activity).isFinishing && !(context as Activity).isDestroyed) {
                            status = INVISIBLE
                            it.onHide()
                        }
                    }
                }
            })
            start()
        }
    }

    override fun onSetAlpha(alpha: Int): Boolean {
        areaPaint.alpha = alpha
        textPaint.alpha = alpha
        icon?.alpha = alpha
        return super.onSetAlpha(alpha)
    }

    override fun verifyDrawable(who: Drawable): Boolean {
        return if (who === progressDrawable || who === icon) true else super.verifyDrawable(who)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        isAttached = true
        updateDrawableVisibility()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        isAttached = false
        updateDrawableVisibility()
    }

    override fun onVisibilityAggregated(isVisible: Boolean) {
        super.onVisibilityAggregated(isVisible)
        updateDrawableVisibility()
    }

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            updateDrawableVisibility()
        }
    }

    private fun updateDrawableVisibility() {
        progressDrawable?.setVisible(
                visible = status == LOADING && isAttached && windowVisibility == VISIBLE && isShown,
                restart = false
        )
    }

    private fun Int.dpToPx(): Int {
        return TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                this.toFloat(),
                context.resources.displayMetrics
        ).toInt()
    }

    interface ResultNotificationViewListener {
        fun onClick(@Status status: Int, event: MotionEvent) = Unit
        fun onProgress() = Unit
        fun onAction() = Unit
        fun onHide() = Unit
    }
}
