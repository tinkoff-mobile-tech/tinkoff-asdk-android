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

import android.app.Activity
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.view.WindowManager
import android.widget.PopupWindow

/**
 * @author Mariya Chernyadieva
 */
internal class SystemKeyboardManager(private val activity: Activity) : PopupWindow(activity), OnGlobalLayoutListener {

    var heightListener: KeyboardHeightListener? = null

    private val rootView: View = View(activity)
    private var heightMax = 0

    init {
        contentView = rootView

        rootView.viewTreeObserver.addOnGlobalLayoutListener(this)
        setBackgroundDrawable(ColorDrawable(0))

        width = 0
        height = ViewGroup.LayoutParams.MATCH_PARENT

        softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        inputMethodMode = INPUT_METHOD_NEEDED
    }

    fun init(): SystemKeyboardManager {
        if (!isShowing) {
            val view: View = activity.window.decorView
            showAtLocation(view, Gravity.NO_GRAVITY, 0, 0)
        }
        return this
    }

    fun detach() {
        dismiss()
    }

    override fun onGlobalLayout() {
        val rect = Rect()
        rootView.getWindowVisibleDisplayFrame(rect)
        if (rect.bottom > heightMax) {
            heightMax = rect.bottom
        }

        val keyboardHeight: Int = heightMax - rect.bottom
        if (heightListener != null) {
            heightListener!!.onHeightChanged(keyboardHeight)
        }
    }

    interface KeyboardHeightListener {
        fun onHeightChanged(height: Int)
    }
}