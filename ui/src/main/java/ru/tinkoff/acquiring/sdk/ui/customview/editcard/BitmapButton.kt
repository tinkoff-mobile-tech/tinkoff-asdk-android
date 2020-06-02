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

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect

/**
 * @author Mariya Chernyadieva
 */
internal class BitmapButton(private val bitmap: Bitmap) {

    var isVisible: Boolean = false
    private val buttonRect: Rect = Rect()
    private val offsetRect: Rect = Rect()

    init {
        buttonRect.set(0, 0, bitmap.width, bitmap.height)
    }

    fun setLayoutPosition(x: Int, y: Int, offset: Int) {
        buttonRect.set(x, y, x + bitmap.width, y + bitmap.height)
        offsetRect.set(buttonRect.left - offset,
                buttonRect.top - offset,
                buttonRect.right + offset,
                buttonRect.bottom + offset
        )
    }

    fun getWidth(): Int {
        return buttonRect.width()
    }

    fun getHeight(): Int {
        return buttonRect.height()
    }

    fun isButtonClick(clickPositionX: Float, clickPositionY: Float): Boolean {
        val x = clickPositionX.toInt()
        val y = clickPositionY.toInt()
        return x > offsetRect.left && x < offsetRect.right && y > offsetRect.top && y < offsetRect.bottom
    }

    fun drawButton(canvas: Canvas, paint: Paint) {
        canvas.drawBitmap(bitmap, buttonRect.left.toFloat(), buttonRect.top.toFloat(), paint)
    }
}