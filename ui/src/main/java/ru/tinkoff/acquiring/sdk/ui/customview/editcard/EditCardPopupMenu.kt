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

package ru.tinkoff.acquiring.sdk.ui.customview.editcard

import android.content.Context
import android.graphics.Color
import android.graphics.Point
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import ru.tinkoff.acquiring.sdk.R

/**
 * @author Mariya Chernyadieva
 */
internal class EditCardPopupMenu(private val context: Context) : PopupWindow() {

    private var positionX: Int = 0
    private var positionY: Int = 0
    private var screenWidth: Int = 0
    private var onItemClickListener: OnPopupMenuItemClickListener? = null

    init {
        width = ViewGroup.LayoutParams.WRAP_CONTENT
        height = ViewGroup.LayoutParams.WRAP_CONTENT

        val display = (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay
        val point = Point()
        display.getSize(point)
        screenWidth = point.x

        setBackgroundDrawable(ContextCompat.getDrawable(context, android.R.drawable.editbox_background))
        contentView = createView()
    }

    fun setPosition(x: Int, y: Int) {
        this.positionX = x
        this.positionY = y
    }

    fun addItem(@StringRes name: Int, id: Int, visible: Boolean = true) {
        val container = contentView as LinearLayout
        var itemView = container.findViewById<TextView>(id)

        if (itemView != null) {
            itemView.apply {
                text = context.resources.getString(name)
                visibility = if (visible) View.VISIBLE else View.GONE
            }
            return
        }

        val attrs = intArrayOf(R.attr.selectableItemBackground)
        val typedArray = context.obtainStyledAttributes(attrs)
        val rippleAnimation = typedArray.getResourceId(0, 0)
        typedArray.recycle()

        itemView = TextView(context).apply {
            this.id = id
            text = context.resources.getString(name)
            layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            )
            visibility = if (visible) View.VISIBLE else View.GONE

            val paddingPx = ITEM_PADDING_DP.dpToPx(context).toInt()
            setPadding(paddingPx, paddingPx, paddingPx, paddingPx)
            setTextColor(Color.BLACK)
            setTypeface(this.typeface, Typeface.BOLD)
            setBackgroundResource(rippleAnimation)
            setOnClickListener {
                onItemClickListener?.onItemClick(it)
            }
        }

        container.addView(itemView)
    }

    fun setOnPopupMenuItemClickListener(listener: OnPopupMenuItemClickListener) {
        this.onItemClickListener = listener
    }

    fun show(parentView: View) {
        contentView.measure(width, height)

        if (positionX + contentView.measuredWidth > screenWidth) {
            positionX = parentView.measuredWidth - contentView.measuredWidth
        } else {
            positionX -= contentView.measuredWidth / 2
        }

        positionY = positionY - contentView.measuredHeight - MENU_VERTICAL_OFFSET_DP.dpToPx(context).toInt()

        showAtLocation(parentView, Gravity.NO_GRAVITY, positionX, positionY)
    }

    private fun createView(): View {
        val linearLayout = LinearLayout(context)
        linearLayout.layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        )
        linearLayout.orientation = LinearLayout.HORIZONTAL

        return linearLayout
    }

    companion object {

        private const val ITEM_PADDING_DP = 10
        private const val MENU_VERTICAL_OFFSET_DP = 12
    }

    interface OnPopupMenuItemClickListener {

        fun onItemClick(view: View)
    }
}