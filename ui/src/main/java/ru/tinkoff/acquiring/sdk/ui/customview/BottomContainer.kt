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
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.TimeInterpolator
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.graphics.Point
import android.graphics.Rect
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.WindowManager
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import ru.tinkoff.acquiring.sdk.R
import ru.tinkoff.acquiring.sdk.ui.customview.editcard.keyboard.SecureKeyboard
import kotlin.math.absoluteValue

/**
 * @author Mariya Chernyadieva
 */
internal class BottomContainer @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    var showInitAnimation = false
    var containerState = STATE_HIDDEN
    var isShowed = false
        private set

    private val systemKeyboardManager = SystemKeyboardManager(context as Activity)
    private val viewConfiguration = ViewConfiguration.get(context)
    private val scrollDistance = viewConfiguration.scaledTouchSlop
    private var containerStateListener: ContainerStateListener? = null
    private var layoutListener: ViewTreeObserver.OnGlobalLayoutListener? = null
    private var velocityTracker: VelocityTracker? = null
    private var initAnimation: AnimatorSet? = null
    private var expandAnimation: Animator? = null
    private var scrollableView: View? = null
    private var background: FrameLayout? = null
    private var isFullScreenOpened = false
    private var isScrollDisabled = true
    private var isMovingEnabled = false
    private var isExpanded = false
    private var initialPositionY = 0
    private var expandedPositionY = 0f
    private var pixelPerSecond = 1000
    private var keyboardHeight = 0
    private var topPositionY = 0
    private var screenHeight = 0
    private var centerScreen = 0
    private var deltaY = 0f

    private var statusBarHeight = 0

    private var touchPoint1: Float = 0f
    private var touchPoint2: Float = 0f
    private var distance: Float = 0f

    private var isNestedScrollOccurs = false

    private var isSystemKeyboardOpened = false
    private var isCustomKeyboardOpened = false

    override fun onFinishInflate() {
        super.onFinishInflate()

        val display = (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay
        val point = Point()
        display.getSize(point)
        screenHeight = point.y
        centerScreen = screenHeight / 2

        setToPosition(screenHeight.toFloat())
        statusBarHeight = getStatusBarHeight()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        setBackgroundResource(R.drawable.acq_top_rounded_background)

        background = (this.parent as View).findViewById(R.id.acq_activity_background_layout)
        background?.apply {
            visibility = View.GONE
            setOnClickListener {
                hide()
            }
        }

        layoutListener = ViewTreeObserver.OnGlobalLayoutListener {
            val customKeyboard = (context as Activity).findViewById<SecureKeyboard>(R.id.edit_card_secure_keyboard)

            when {
                customKeyboard != null && customKeyboard.isShowing() -> {
                    isCustomKeyboardOpened = true
                    if (keyboardHeight != customKeyboard.height) {
                        isScrollDisabled = true
                    }
                    keyboardHeight = customKeyboard.height
                    handleKeyboard()
                }
                customKeyboard != null && !customKeyboard.isShowing() && !isSystemKeyboardOpened -> {
                    isCustomKeyboardOpened = false
                    keyboardHeight = 0
                }
                else -> isCustomKeyboardOpened = false
            }

            handleKeyboard()
        }

        systemKeyboardManager.init().heightListener = object : SystemKeyboardManager.KeyboardHeightListener {
            override fun onHeightChanged(height: Int) {
                when {
                    height != 0 -> {
                        isSystemKeyboardOpened = true
                        if (keyboardHeight != height) {
                            isScrollDisabled = true
                        }
                        keyboardHeight = height
                    }
                    height == 0 && !isCustomKeyboardOpened -> {
                        isSystemKeyboardOpened = false
                        keyboardHeight = 0
                    }
                    else -> isSystemKeyboardOpened = false
                }

                handleKeyboard()
            }
        }

        this.viewTreeObserver.addOnGlobalLayoutListener(layoutListener)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        systemKeyboardManager.detach()
        this.viewTreeObserver.removeOnGlobalLayoutListener(layoutListener)
    }

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        background?.isEnabled = enabled
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        val prevInitialPositionY = initialPositionY
        initialPositionY = height - getChildHeight()
        if (prevInitialPositionY == 0 || prevInitialPositionY != initialPositionY) {
            if (showInitAnimation) {
                if (initialPositionY <= topPositionY) {
                    initialPositionY = statusBarHeight
                    show()
                } else {
                    show()
                }
            } else {
                if (isExpanded) {
                    if (expandAnimation?.isRunning != true) {
                        setToPosition(expandedPositionY)
                    }
                } else {
                    if (initialPositionY <= topPositionY || containerState == STATE_FULLSCREEN) {
                        openFullScreen()
                    } else {
                        if (containerState == STATE_SHOWED) {
                            if (initAnimation?.isRunning != true) {
                                setToPosition(initialPositionY.toFloat())
                            }
                        } else {
                            setToPosition(screenHeight.toFloat())
                        }
                    }
                }
            }
        }
    }

    override fun onInterceptTouchEvent(motionEvent: MotionEvent): Boolean {
        return when (motionEvent.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                isNestedScrollOccurs = false
                val canScroll = scrollableView?.canScrollVertically(1) == true ||
                        scrollableView?.canScrollVertically(-1) == true

                if (isScrollableViewTouch(motionEvent.x, motionEvent.y) && canScroll) {
                    isNestedScrollOccurs = true
                    false
                } else {
                    touchPoint1 = motionEvent.y
                    deltaY = this.y - motionEvent.rawY
                    velocityTracker?.clear()
                    velocityTracker = VelocityTracker.obtain()
                    velocityTracker?.addMovement(motionEvent)
                    super.onInterceptTouchEvent(motionEvent)
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (isNestedScrollOccurs) {
                    false
                } else {
                    touchPoint2 = motionEvent.y
                    distance = touchPoint1 - touchPoint2
                    if (distance.absoluteValue >= scrollDistance && isMovingEnabled) {
                        true
                    } else {
                        super.onInterceptTouchEvent(motionEvent)
                    }
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isNestedScrollOccurs = false
                super.onInterceptTouchEvent(motionEvent)
            }
            else -> super.onInterceptTouchEvent(motionEvent)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (isMovingEnabled) {
            when (event.action) {

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    val velocity = (velocityTracker?.yVelocity) ?: 0f
                    val velocityThreshold = 1000
                    val isMoving = velocity.absoluteValue > velocityThreshold
                    val isUpDirection = distance > 0

                    val duration = calculateDuration(velocity)
                    when {
                        (isMoving && !isUpDirection) || this.y > centerScreen * 1.3 -> hide(duration)
                        isExpanded -> moveToPosition(expandedPositionY)
                        else -> moveToPosition(initialPositionY.toFloat())
                    }
                }

                MotionEvent.ACTION_MOVE -> {
                    velocityTracker?.apply {
                        addMovement(event)
                        computeCurrentVelocity(pixelPerSecond)
                    }

                    val targetPosition = event.rawY + this.deltaY
                    if ((!isExpanded && targetPosition >= initialPositionY) ||
                            (isExpanded && targetPosition >= expandedPositionY)) {
                        this.y = targetPosition
                    }
                }
            }
        }
        return true
    }

    fun setContainerStateListener(listener: ContainerStateListener) {
        this.containerStateListener = listener
    }

    fun show() {
        val translationAnimator = getTranslationYAnimator(this, initialPositionY.toFloat(),
                INIT_ANIMATION_DURATION, DecelerateInterpolator())
        val fadeInAnimation = getFadeInAnimator(background!!)
        val startAnimations = listOf(fadeInAnimation, translationAnimator)

        initAnimation = AnimatorSet().apply {
            playTogether(startAnimations)
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationStart(animation: Animator) {
                    background?.visibility = View.VISIBLE
                    containerState = STATE_SHOWED
                    containerStateListener?.onShowed()
                }
            })
        }
        initAnimation!!.start()

        isMovingEnabled = true
        showInitAnimation = false
        isShowed = true
    }

    fun hide(duration: Long = HIDE_ANIMATION_DURATION) {
        isEnabled = false
        isShowed = false

        val translationAnimator = getTranslationYAnimator(this, screenHeight.toFloat(),
                duration, LinearInterpolator())

        val finishAnimations = mutableListOf(translationAnimator)
        if (background != null) {
            finishAnimations.add(getFadeOutAnimator(background!!))
        }
        AnimatorSet().apply {
            playTogether(finishAnimations.toList())
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    containerState = STATE_HIDDEN
                    containerStateListener?.onHidden()
                    background?.visibility = View.GONE
                }
            })
        }.start()
    }

    fun openFullScreen() {
        isFullScreenOpened = true
        isMovingEnabled = false

        val destinyPositionY = if (containerState == STATE_FULLSCREEN) topPositionY else statusBarHeight
        getTranslationYAnimator(this, destinyPositionY.toFloat(), MOVING_ANIMATION_DURATION,
                DecelerateInterpolator())
                .apply {
                    addListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            this@BottomContainer.y = destinyPositionY.toFloat()
                            setBackgroundColor(ContextCompat.getColor(context, R.color.acq_colorMain))
                            containerState = STATE_FULLSCREEN
                            resizeScrollContainer()
                            containerStateListener?.onFullscreenOpened()
                        }
                    })
                }.start()
    }

    fun expand() {
        isExpanded = true
        expandedPositionY = (initialPositionY - keyboardHeight).toFloat()
        if (expandedPositionY <= statusBarHeight.toFloat()) {
            expandedPositionY = statusBarHeight.toFloat()
        } else if (expandedPositionY == initialPositionY.toFloat()) {
            isExpanded = false
            return
        }

        expandAnimation = getTranslationYAnimator(this, expandedPositionY, MOVING_ANIMATION_DURATION,
                DecelerateInterpolator())
                .apply {
                    addListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            this@BottomContainer.y = expandedPositionY
                            resizeScrollContainer()
                        }
                    })
                    start()
                }
    }

    fun collapse() {
        isExpanded = false
        isMovingEnabled = true
        if (isFullScreenOpened) {
            isFullScreenOpened = false
            setBackgroundResource(R.drawable.acq_top_rounded_background)
        }
        containerState = STATE_SHOWED
        moveToPosition(initialPositionY.toFloat())
    }

    fun containsRect(x: Int, y: Int): Boolean {
        val containerRect = Rect()
        getHitRect(containerRect)
        return containerRect.contains(x, y)
    }

    private fun getChildHeight(): Int {
        var childHeight = 0
        for (index in 0 until this.childCount) {
            val child = this.getChildAt(index)
            childHeight += child.height
        }

        return childHeight
    }

    private fun getPositionInParent(child: View?): IntArray {
        if (child == null) {
            return intArrayOf(0, 0)
        }

        val relativePosition = intArrayOf(child.left, child.top)
        var currentParent = child.parent as ViewGroup?

        while (currentParent != null && currentParent !== this) {
            relativePosition[0] += currentParent.left
            relativePosition[1] += currentParent.top
            currentParent = currentParent.parent as ViewGroup?
        }
        return relativePosition
    }

    private fun isScrollableViewTouch(x: Float, y: Float): Boolean {
        val scrollViewPosition = getPositionInParent(scrollableView)

        val viewX = scrollViewPosition[0]
        val viewY = scrollViewPosition[1]

        val boundRight = viewX + (scrollableView?.width ?: 0)
        val boundBottom = viewY + (scrollableView?.height ?: 0)

        return x > viewX && x < boundRight && y > viewY && y < boundBottom
    }

    private fun resizeScrollContainer() {
        val scrollContainer = getChildAt(0)
        if (isSystemKeyboardOpened || isCustomKeyboardOpened) {
            val offset = if (containerState == STATE_FULLSCREEN) 0 else statusBarHeight
            val heightParam = this.height - keyboardHeight - offset
            scrollContainer.layoutParams.height = heightParam
            isScrollDisabled = false
        } else {
            scrollContainer.layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
            isScrollDisabled = true
        }
        scrollContainer.requestLayout()
    }

    private fun handleKeyboard() {
        if (isSystemKeyboardOpened || isCustomKeyboardOpened) {
            isMovingEnabled = false
            if (!isExpanded && containerState == STATE_SHOWED) {
                if (initAnimation != null && initAnimation!!.isRunning) {
                    initAnimation!!.apply {
                        addListener(object : AnimatorListenerAdapter() {
                            override fun onAnimationEnd(animation: Animator) {
                                expand()
                            }
                        })
                    }
                } else {
                    expand()
                }
            } else {
                if (isScrollDisabled) {
                    resizeScrollContainer()
                }
            }
        } else {
            if (!isScrollDisabled) {
                resizeScrollContainer()
            }
            if (isExpanded && !isFullScreenOpened && containerState == STATE_SHOWED) {
                postDelayed({
                    if (isExpanded && (!isSystemKeyboardOpened && !isCustomKeyboardOpened)) {
                        collapse()
                    }
                }, KEYBOARD_SWITCHING_DELAY)
            }
        }
    }

    private fun calculateDuration(velocity: Float): Long {
        val pxPerSec = (velocity / pixelPerSecond).absoluteValue
        val isUpDirection = distance > 0
        val duration = (10 / pxPerSec.absoluteValue * 100).toLong()

        return when {
            duration < 250 && isUpDirection -> 100
            duration < 250 && !isUpDirection -> 200
            duration > 400 -> 250
            else -> duration
        }
    }

    private fun getStatusBarHeight(): Int {
        val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
        return if (resourceId > 0) resources.getDimensionPixelSize(resourceId) else 0
    }

    private fun setToPosition(positionY: Float) {
        this.y = positionY
        if (positionY != screenHeight.toFloat()) {
            isMovingEnabled = true
            background?.visibility = View.VISIBLE
        }
    }

    private fun moveToPosition(destinyPositionY: Float) {
        getTranslationYAnimator(this, destinyPositionY, MOVING_ANIMATION_DURATION, LinearInterpolator()).start()
    }

    private fun getTranslationYAnimator(view: View, destinyPositionY: Float, duration: Long, interpolator: TimeInterpolator): ObjectAnimator {
        return ObjectAnimator.ofFloat(view, View.TRANSLATION_Y, destinyPositionY).apply {
            this.duration = duration
            this.interpolator = interpolator
        }
    }

    private fun getFadeInAnimator(view: View): ObjectAnimator {
        return ObjectAnimator.ofFloat(view, View.ALPHA, 0f, 1f).apply {
            duration = INIT_ANIMATION_DURATION
            interpolator = AccelerateInterpolator()
        }
    }

    private fun getFadeOutAnimator(view: View): ObjectAnimator {
        return ObjectAnimator.ofFloat(view, View.ALPHA, 1f, 0f).apply {
            duration = HIDE_ANIMATION_DURATION
            interpolator = DecelerateInterpolator()
        }
    }

    interface ContainerStateListener {

        fun onHidden()

        fun onShowed()

        fun onFullscreenOpened()
    }

    companion object {

        const val STATE_HIDDEN = 1
        const val STATE_SHOWED = 2
        const val STATE_FULLSCREEN = 3

        private const val INIT_ANIMATION_DURATION = 200L
        private const val HIDE_ANIMATION_DURATION = 200L
        private const val MOVING_ANIMATION_DURATION = 150L

        private const val KEYBOARD_SWITCHING_DELAY = 100L
    }
}

