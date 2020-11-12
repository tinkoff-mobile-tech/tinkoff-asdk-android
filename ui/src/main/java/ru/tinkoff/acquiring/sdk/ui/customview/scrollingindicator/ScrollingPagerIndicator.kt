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

package ru.tinkoff.acquiring.sdk.ui.customview.scrollingindicator

import android.animation.ArgbEvaluator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.util.SparseArray
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import androidx.annotation.ColorInt
import androidx.viewpager.widget.ViewPager
import ru.tinkoff.acquiring.sdk.R
import kotlin.math.abs
import kotlin.math.min

/**
 * @author Mariya Chernyadieva
 */
internal class ScrollingPagerIndicator @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var dotCount: Int
        get() {
            return if (looped && itemCount > this.visibleDotCount) {
                infiniteDotCount
            } else {
                itemCount - 1
            }
        }
        set(count) = initDots(count)

    @ColorInt
    private var dotColor: Int = 0
    @ColorInt
    private var selectedDotColor: Int = 0

    private val dotNormalSize: Int
    private val dotSelectedSize: Int
    private val spaceBetweenDotCenters: Int

    private var itemCount: Int = 0
    private var visibleDotThreshold: Int = 0
    private var infiniteDotCount: Int = 0
    private var visibleDotCount: Int = 0
        set(visibleDotCount) {
            require(visibleDotCount % 2 != 0) { "visibleDotCount must be odd" }
            field = visibleDotCount
            this.infiniteDotCount = visibleDotCount + 2

            if (attachRunnable != null) {
                reattach()
            } else {
                requestLayout()
            }
        }

    private var visibleFramePosition: Float = 0.toFloat()
    private var visibleFrameWidth: Float = 0.toFloat()
    private var firstDotOffset: Float = 0.toFloat()
    private var plusHeight: Int = 0
    private var plusWidth: Int = 0
    private var cardListHeight: Int = 0
    private var cardListWidth: Int = 0
    private var clickAreaExtend: Int = 0

    private var dotCountInitialized: Boolean = false
    private var looped: Boolean = false

    private val paint: Paint
    private var lastPlusDrawable: Drawable
    private var cardListDrawable: Drawable
    private val colorEvaluator = ArgbEvaluator()
    private val plusRect = Rect()
    private val cardListRect = Rect()

    private var dotScale: SparseArray<Float>? = null
    private var attachRunnable: Runnable? = null
    private var currentAttacher: PagerAttacher<*>? = null
    private var plusClickListener: OnPlusIndicatorClickListener? = null
    private var listClickListener: OnListIndicatorClickListener? = null
    private var pageChangeListener: OnPageChangeListener? = null

    init {
        val attributes = context.obtainStyledAttributes(
                attrs, R.styleable.ScrollingPagerIndicator, defStyleAttr, R.style.AcquiringScrollingPagerIndicator)
        with(attributes) {
            dotColor = getColor(R.styleable.ScrollingPagerIndicator_spi_dotColor, 0)
            selectedDotColor = getColor(R.styleable.ScrollingPagerIndicator_spi_dotSelectedColor, dotColor)
            dotNormalSize = getDimensionPixelSize(R.styleable.ScrollingPagerIndicator_spi_dotSize, 0)
            dotSelectedSize = getDimensionPixelSize(R.styleable.ScrollingPagerIndicator_spi_dotSelectedSize, 0)
            spaceBetweenDotCenters = getDimensionPixelSize(R.styleable.ScrollingPagerIndicator_spi_dotSpacing, 0) + dotNormalSize
            looped = getBoolean(R.styleable.ScrollingPagerIndicator_spi_looped, false)
            visibleDotCount = getInt(R.styleable.ScrollingPagerIndicator_spi_visibleDotCount, 0)
            visibleDotThreshold = getInt(R.styleable.ScrollingPagerIndicator_spi_visibleDotThreshold, 2)
            lastPlusDrawable = androidx.core.content.ContextCompat.getDrawable(context, R.drawable.acq_indicator_plus)!!
            cardListDrawable = androidx.core.content.ContextCompat.getDrawable(context, R.drawable.acq_icon_list)!!
            recycle()
        }

        plusHeight = lastPlusDrawable.intrinsicHeight
        plusWidth = lastPlusDrawable.intrinsicWidth

        cardListHeight = cardListDrawable.intrinsicHeight
        cardListWidth = cardListDrawable.intrinsicWidth

        cardListDrawable.colorFilter = PorterDuffColorFilter(dotColor, PorterDuff.Mode.SRC_ATOP)

        clickAreaExtend = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, CLICK_AREA_EXTEND, resources.displayMetrics).toInt()

        paint = Paint()
        paint.isAntiAlias = true

        if (isInEditMode) {
            dotCount = visibleDotCount
            onPageScrolled(visibleDotCount / 2, 0f)
        }
    }

    fun setOnPlusClickListener(listener: OnPlusIndicatorClickListener) {
        this.plusClickListener = listener
    }

    fun setOnListClickListener(listener: OnListIndicatorClickListener) {
        this.listClickListener = listener
    }

    fun setOnPageChangeListener(listener: OnPageChangeListener) {
        this.pageChangeListener = listener
    }

    fun attachToPager(pager: ViewPager) {
        detachFromPager()
        val attacher = ViewPagerAttacher()
        attacher.attachToPager(this, pager)
        attacher.setCustomPageChangeListener(pageChangeListener)
        currentAttacher = attacher

        attachRunnable = Runnable {
            itemCount = -1
            attachToPager(pager)
        }
    }

    fun detachFromPager() {
        if (currentAttacher != null) {
            currentAttacher!!.detachFromPager()
            currentAttacher = null
            attachRunnable = null
        }
        dotCountInitialized = false
    }

    fun reattach() {
        if (attachRunnable != null) {
            attachRunnable!!.run()
            invalidate()
        }
    }

    fun onPageScrolled(page: Int, offset: Float) {
        require(!(offset < 0 || offset > 1)) { "Offset must be [0, 1]" }
        if (page < 0 || page != 0 && page >= itemCount) {
            throw IndexOutOfBoundsException("page must be [0, adapter.getItemCount())")
        }

        if (!looped || itemCount <= this.visibleDotCount && itemCount > 1) {
            dotScale!!.clear()

            scaleDotByOffset(page, offset)
            setPlusSelected(false)

            when {
                page < itemCount - 1 -> scaleDotByOffset(page + 1, 1 - offset)
                page == itemCount - 1 -> {
                    setPlusSelected(true)
                    scaleDotByOffset(0, 1 - offset)
                }
                itemCount > 1 -> scaleDotByOffset(0, 1 - offset)
            }

            invalidate()
        }
        adjustFramePosition(offset, page)
        invalidate()
    }

    fun setCurrentPosition(position: Int) {
        if (position != 0 && (position < 0 || position >= itemCount)) {
            throw IndexOutOfBoundsException("Position must be [0, adapter.getItemCount()]")
        }
        setPlusSelected(false)
        if (itemCount == 0) {
            return
        }
        if (position == itemCount - 1) {
            setPlusSelected(true)
        }
        adjustFramePosition(0f, position)
        updateScaleInIdleState(position)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val measuredWidth: Int = if (isInEditMode) {
            (this.visibleDotCount - 1) * spaceBetweenDotCenters + dotSelectedSize + plusWidth +
                    cardListWidth + spaceBetweenDotCenters / 2
        } else {
            if (itemCount >= this.visibleDotCount)
                visibleFrameWidth.toInt() + plusWidth + cardListWidth + spaceBetweenDotCenters * 2
            else
                (itemCount - 1) * spaceBetweenDotCenters + dotSelectedSize + plusWidth +
                        cardListWidth + spaceBetweenDotCenters * 2
        }

        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)

        val desiredHeight = dotSelectedSize + plusHeight / 2
        val measuredHeight: Int

        measuredHeight = when (heightMode) {
            MeasureSpec.EXACTLY -> heightSize
            MeasureSpec.AT_MOST -> min(desiredHeight, heightSize)
            MeasureSpec.UNSPECIFIED -> desiredHeight
            else -> desiredHeight
        }

        setMeasuredDimension(measuredWidth, measuredHeight)
    }

    override fun onDraw(canvas: Canvas) {
        if (dotCount == 0) {
            return
        }

        val scaleDistance = (spaceBetweenDotCenters + (dotSelectedSize - dotNormalSize) / 2) * 0.7f
        val smallScaleDistance = (dotSelectedSize / 2).toFloat()
        val centerScaleDistance = 6f / 7f * spaceBetweenDotCenters

        val firstVisibleDotPos = (visibleFramePosition - firstDotOffset).toInt() / spaceBetweenDotCenters
        var lastVisibleDotPos = firstVisibleDotPos + (visibleFramePosition + visibleFrameWidth -
                getDotOffsetAt(firstVisibleDotPos)).toInt() / spaceBetweenDotCenters

        if (firstVisibleDotPos == 0 && lastVisibleDotPos + 1 > dotCount) {
            lastVisibleDotPos = dotCount - 1
        }

        val listTop = measuredHeight / 2 - cardListHeight / 2
        val listBottom = listTop + cardListHeight
        val listLeft = 0
        val listRight = listLeft + cardListWidth
        cardListDrawable.setBounds(listLeft, listTop, listRight, listBottom)
        cardListDrawable.draw(canvas)

        cardListRect.apply {
            this.top = listTop
            this.bottom = listBottom
            this.left = listLeft
            this.right = listRight
        }


        for (i in firstVisibleDotPos..lastVisibleDotPos) {
            val dot = getDotOffsetAt(i)
            if (dot >= visibleFramePosition && dot < visibleFramePosition + visibleFrameWidth) {
                var diameter: Float
                val scale = if (looped && itemCount > this.visibleDotCount) {
                    val frameCenter = visibleFramePosition + visibleFrameWidth / 2
                    if (dot >= frameCenter - centerScaleDistance && dot <= frameCenter) {
                        (dot - frameCenter + centerScaleDistance) / centerScaleDistance
                    } else if (dot > frameCenter && dot < frameCenter + centerScaleDistance) {
                        1 - (dot - frameCenter) / centerScaleDistance
                    } else {
                        0f
                    }
                } else {
                    getDotScaleAt(i)
                }
                diameter = dotNormalSize + (dotSelectedSize - dotNormalSize) * scale

                if (itemCount > this.visibleDotCount) {
                    val currentScaleDistance: Float = if (!looped && (i == 0 || i == dotCount - 1)) {
                        smallScaleDistance
                    } else {
                        scaleDistance
                    }

                    if (dot - visibleFramePosition < currentScaleDistance) {
                        val calculatedDiameter = diameter * (dot - visibleFramePosition) / currentScaleDistance
                        if (calculatedDiameter < diameter) {
                            diameter = calculatedDiameter
                        }
                    } else if (dot - visibleFramePosition > width - currentScaleDistance) {
                        val calculatedDiameter = diameter * (-dot + visibleFramePosition + width.toFloat()) / currentScaleDistance
                        if (calculatedDiameter < diameter) {
                            diameter = calculatedDiameter
                        }
                    }
                }

                paint.color = calculateDotColor(scale)

                var cx = dot - visibleFramePosition
                if (dotCount < visibleDotThreshold) {
                    cx += spaceBetweenDotCenters / 2
                }

                canvas.drawCircle(cx + cardListWidth + spaceBetweenDotCenters,
                        (measuredHeight / 2).toFloat(),
                        diameter / 2,
                        paint)
            }
        }

        val plusTop = measuredHeight / 2 - plusHeight / 2 + 2
        val plusBottom = plusTop + plusHeight
        val plusLeft = if (dotCount <= visibleDotThreshold) {
            width / 2 - plusWidth / 2 + spaceBetweenDotCenters + cardListWidth
        } else {
            width / 2 - plusWidth / 2 + spaceBetweenDotCenters / 2 * visibleDotCount + cardListWidth + spaceBetweenDotCenters / 3
        }
        val plusRight = plusLeft + plusWidth
        lastPlusDrawable.setBounds(plusLeft, plusTop, plusRight, plusBottom)
        lastPlusDrawable.draw(canvas)

        plusRect.apply {
            this.top = plusTop
            this.bottom = plusBottom
            this.left = plusLeft
            this.right = plusRight
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                if (isPlusClicked(event)) {
                    invalidate()
                    plusClickListener?.onClick()
                }
                if (isListClicked(event)) {
                    invalidate()
                    listClickListener?.onClick()
                }
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    @ColorInt
    private fun calculateDotColor(dotScale: Float): Int {
        return colorEvaluator.evaluate(dotScale, dotColor, selectedDotColor) as Int
    }

    private fun setPlusSelected(selected: Boolean) {
        val color = if (selected) selectedDotColor else dotColor
        lastPlusDrawable.colorFilter = PorterDuffColorFilter(color, PorterDuff.Mode.SRC_ATOP)
    }

    private fun updateScaleInIdleState(currentPos: Int) {
        if (!looped || itemCount < this.visibleDotCount) {
            dotScale!!.clear()
            dotScale!!.put(currentPos, 1f)
            invalidate()
        }
    }

    private fun initDots(itemCount: Int) {
        if (this.itemCount == itemCount && dotCountInitialized) {
            return
        }
        this.itemCount = itemCount
        dotCountInitialized = true
        dotScale = SparseArray()

        if (itemCount < visibleDotThreshold) {
            requestLayout()
            invalidate()
            return
        }

        firstDotOffset = (if (looped && this.itemCount > this.visibleDotCount) 0 else dotSelectedSize / 2).toFloat()
        visibleFrameWidth = ((this.visibleDotCount - 1) * spaceBetweenDotCenters + dotSelectedSize).toFloat()

        requestLayout()
        invalidate()
    }

    private fun adjustFramePosition(offset: Float, pos: Int) {
        if (itemCount <= this.visibleDotCount) {
            visibleFramePosition = 0f
        } else if (!looped && itemCount > this.visibleDotCount) {
            val center = getDotOffsetAt(pos) + spaceBetweenDotCenters * offset
            visibleFramePosition = center - visibleFrameWidth / 2

            val firstCenteredDotIndex = this.visibleDotCount / 2
            val lastCenteredDot = getDotOffsetAt(dotCount - 1 - firstCenteredDotIndex)
            if (visibleFramePosition + visibleFrameWidth / 2 < getDotOffsetAt(firstCenteredDotIndex)) {
                visibleFramePosition = getDotOffsetAt(firstCenteredDotIndex) - visibleFrameWidth / 2
            } else if (visibleFramePosition + visibleFrameWidth / 2 > lastCenteredDot) {
                visibleFramePosition = lastCenteredDot - visibleFrameWidth / 2
            }
        } else {
            val center = getDotOffsetAt(infiniteDotCount / 2) + spaceBetweenDotCenters * offset
            visibleFramePosition = center - visibleFrameWidth / 2
        }
    }

    private fun scaleDotByOffset(position: Int, offset: Float) {
        if (dotScale == null || dotCount == 0) {
            return
        }
        setDotScaleAt(position, 1 - abs(offset))
    }

    private fun getDotOffsetAt(index: Int): Float {
        return firstDotOffset + index * spaceBetweenDotCenters
    }

    private fun getDotScaleAt(index: Int): Float {
        return dotScale!!.get(index) ?: 0f
    }

    private fun setDotScaleAt(index: Int, scale: Float) {
        if (scale == 0f) {
            dotScale!!.remove(index)
        } else {
            dotScale!!.put(index, scale)
        }
    }

    private fun isPlusClicked(event: MotionEvent): Boolean {
        return event.x >= plusRect.left - clickAreaExtend &&
                event.x <= plusRect.right + clickAreaExtend &&
                event.y >= plusRect.top - clickAreaExtend &&
                event.y <= plusRect.bottom + clickAreaExtend
    }

    private fun isListClicked(event: MotionEvent): Boolean {
        return event.x >= cardListRect.left - clickAreaExtend &&
                event.x <= cardListRect.right + clickAreaExtend &&
                event.y >= cardListRect.top - clickAreaExtend &&
                event.y <= cardListRect.bottom + clickAreaExtend
    }

    companion object {
        private const val CLICK_AREA_EXTEND = 10f
    }

    interface OnPlusIndicatorClickListener {

        fun onClick()
    }

    interface OnListIndicatorClickListener {

        fun onClick()
    }

    interface OnPageChangeListener {

        fun onChange(currentItem: Int)
    }
}