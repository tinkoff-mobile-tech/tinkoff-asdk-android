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
import android.animation.ObjectAnimator
import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.graphics.BitmapFactory
import android.graphics.Color
import android.util.AttributeSet
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.FrameLayout
import android.widget.GridLayout
import androidx.core.content.ContextCompat
import ru.tinkoff.acquiring.sdk.R
import ru.tinkoff.acquiring.sdk.ui.customview.editcard.dpToPx
import kotlin.math.min

/**
 * @author Mariya Chernyadieva
 */
internal class SecureKeyboard @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr), OnClickListener {

    var keyClickListener: OnKeyClickListener? = null

    private val keyboardHeight: Int
    private var keyboardBackgroundColor: Int = 0
    private var keyboardKeyTextColor: Int = 0

    private var isOpen = false
    private var needOpen = false
    private var openRunning = false
    private var hideRunning = false

    init {
        val attrsArray = context.obtainStyledAttributes(attrs, R.styleable.SecureKeyboard)
        try {
            attrsArray.apply {
                val defaultColor = ContextCompat.getColor(context, R.color.acq_colorKeyboardBackground)
                keyboardBackgroundColor = getColor(R.styleable.SecureKeyboard_acqKeyboardBackgroundColor, defaultColor)
                keyboardKeyTextColor = getColor(R.styleable.SecureKeyboard_acqKeyboardKeyTextColor, Color.WHITE)
            }
        } finally {
            attrsArray.recycle()
        }

        this.visibility = View.GONE

        keyboardHeight = if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
            DEFAULT_KEYBOARD_HEIGHT_DP
        } else {
            LANDSCAPE_KEYBOARD_HEIGHT_DP
        }
        createKeyboard()
    }

    internal fun isShowing(): Boolean {
        return openRunning || isOpen
    }

    private fun createKeyboard() {
        val gridLayout = GridLayout(context).apply {
            layoutParams = LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, keyboardHeight.dpToPx(context).toInt())
            columnCount = 3
            rowCount = 4
            orientation = GridLayout.HORIZONTAL
            setBackgroundColor(keyboardBackgroundColor)
            setPadding(40.dpToPx(context).toInt(), 0, 40.dpToPx(context).toInt(), 0)
        }

        val keyLayoutParams = LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        for (i in 1..9) {
            val key = KeyView(context).apply {
                layoutParams = keyLayoutParams
                keyCode = i
                textColor = keyboardKeyTextColor
                keyColor = keyboardBackgroundColor
                setOnClickListener(this@SecureKeyboard)
            }
            gridLayout.addView(key)
        }

        val keyZero = KeyView(context).apply {
            layoutParams = GridLayout.LayoutParams(
                    GridLayout.spec(3, GridLayout.CENTER),
                    GridLayout.spec(1, GridLayout.CENTER)
            )
            layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT
            layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
            keyCode = 0
            textColor = keyboardKeyTextColor
            keyColor = keyboardBackgroundColor
            setOnClickListener(this@SecureKeyboard)
        }
        gridLayout.addView(keyZero)

        val keyDel = KeyView(context).apply {
            layoutParams = GridLayout.LayoutParams(
                    GridLayout.spec(3, GridLayout.CENTER),
                    GridLayout.spec(2, GridLayout.CENTER)
            )
            layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT
            layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
            keyCode = 10
            textColor = keyboardKeyTextColor
            keyColor = keyboardBackgroundColor
            contentImage = BitmapFactory.decodeResource(resources, R.drawable.acq_back_arrow)
            setOnClickListener(this@SecureKeyboard)
        }
        gridLayout.addView(keyDel)

        this.addView(gridLayout)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        var heightSize = MeasureSpec.getSize(heightMeasureSpec)

        val keyboardHeightInPx = keyboardHeight.dpToPx(context).toInt()

        heightSize = when (MeasureSpec.getMode(heightMeasureSpec)) {
            MeasureSpec.EXACTLY -> heightSize
            MeasureSpec.AT_MOST -> min(heightSize, keyboardHeightInPx)
            MeasureSpec.UNSPECIFIED -> keyboardHeightInPx
            else -> keyboardHeightInPx
        }

        setMeasuredDimension(widthSize, heightSize)
    }

    override fun onClick(view: View?) {
        val key = view as KeyView
        keyClickListener?.onKeyClick(key.keyCode)
    }

    fun show() {
        if ((!isOpen && !openRunning) || hideRunning) {
            (context.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager)
                    .hideSoftInputFromWindow(this.applicationWindowToken, InputMethodManager.HIDE_NOT_ALWAYS)
            needOpen = true
            postDelayed({
                if (needOpen && !isOpen) {
                    createVisibilityAnimator(true).start()
                }
                openRunning = false
            }, KEYBOARD_SHOW_DELAY_MILLIS.toLong())
            openRunning = true
        }
    }

    fun hide() {
        if (isOpen && !hideRunning) {
            needOpen = false
            postDelayed({
                if (!needOpen && isOpen) {
                    createVisibilityAnimator(false).start()
                }
                hideRunning = false
            }, CUSTOM_KEYBOARD_HIDE_DELAY_MILLIS.toLong())
            hideRunning = true
        }
    }

    private fun createVisibilityAnimator(show: Boolean): Animator {
        val startY: Float
        val endY: Float
        if (show) {
            startY = keyboardHeight.dpToPx(context)
            endY = 0f
        } else {
            startY = 0f
            endY = keyboardHeight.dpToPx(context)
        }

        return ObjectAnimator.ofFloat(this, View.TRANSLATION_Y, startY, endY).apply {
            duration = KEYBOARD_ANIMATION_MILLIS.toLong()
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationStart(animation: Animator) {
                    if (show) {
                        visibility = View.VISIBLE
                    }
                    isOpen = show
                }

                override fun onAnimationEnd(animation: Animator) {
                    if (!show) {
                        visibility = View.GONE
                    }
                }
            })
        }
    }


    companion object {
        private const val KEYBOARD_SHOW_DELAY_MILLIS = 200
        private const val KEYBOARD_ANIMATION_MILLIS = 200
        private const val CUSTOM_KEYBOARD_HIDE_DELAY_MILLIS = 100

        private const val DEFAULT_KEYBOARD_HEIGHT_DP = 240
        private const val LANDSCAPE_KEYBOARD_HEIGHT_DP = 170
    }

    interface OnKeyClickListener {

        fun onKeyClick(keyCode: Int)
    }
}