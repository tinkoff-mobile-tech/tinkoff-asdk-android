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

import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import ru.tinkoff.acquiring.sdk.R
import ru.tinkoff.acquiring.sdk.ui.customview.editcard.dpToPx

internal class Shadow(
        private val context: Context,
        isDarkMode: Boolean = false,
        @ColorRes
        var backgroundColor: Int = R.color.acq_colorCardBackground
) : Drawable() {

    @ColorInt
    var shadowColor = 0
    var shadowRadius = 0f
    var backgroundRadius = 0f

    private var shadowPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var backgroundPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val shadowRect = RectF()

    init {
        val colorRes = if (isDarkMode) android.R.color.transparent else R.color.acq_colorShadow

        shadowColor = ContextCompat.getColor(context, colorRes)
        shadowRadius = if (isDarkMode) 0f else context.resources.getDimension(R.dimen.acq_shadow_radius)
        backgroundRadius = context.resources.getDimension(R.dimen.acq_card_radius)

        shadowPaint.color = Color.WHITE
        backgroundPaint.color = ContextCompat.getColor(context, backgroundColor)
        shadowPaint.setShadowLayer(shadowRadius, 0f, 0f, shadowColor)
    }

    override fun draw(canvas: Canvas) {
        val width = bounds.width()
        val height = bounds.height()
        val left = bounds.left.toFloat()
        val top = bounds.top.toFloat()

        if (shadowRadius != 0f) {
            val shadowBitmap = createShadowBitmap(width, height)
            canvas.drawBitmap(
                    shadowBitmap,
                    left + 0 - 2 * shadowRadius,
                    top + 0 - 2 * shadowRadius,
                    null
            )
        }
        val backgroundOffset = 12.dpToPx(context)
        val backgroundRect = RectF(
                left + backgroundOffset,
                top + backgroundOffset,
                left + width.toFloat() - backgroundOffset,
                top + height.toFloat() - backgroundOffset
        )
        canvas.drawRoundRect(
                backgroundRect,
                backgroundRadius,
                backgroundRadius,
                backgroundPaint
        )
    }

    private fun createShadowBitmap(width: Int, height: Int): Bitmap {
        val bitmapWidth = 4 * shadowRadius + width
        val bitmapHeight = 4 * shadowRadius + height
        val shadowOffset = 2.dpToPx(context)
        shadowPaint.setShadowLayer(
                shadowRadius,
                width.toFloat() + 2 * shadowRadius,
                height.toFloat() + 2 * shadowRadius + shadowOffset,
                shadowColor
        )
        val bitmap = Bitmap.createBitmap(
                bitmapWidth.toInt(),
                bitmapHeight.toInt(),
                Bitmap.Config.ARGB_8888
        )
        val shadowBlur = 10.dpToPx(context)
        shadowRect.set(
                -width.toFloat() + shadowBlur,
                -height.toFloat() + shadowBlur,
                -shadowBlur,
                -shadowBlur
        )
        Canvas(bitmap).drawRoundRect(
                shadowRect,
                shadowRadius,
                shadowRadius,
                shadowPaint
        )
        return bitmap
    }

    override fun setAlpha(alpha: Int) {
        shadowPaint.alpha = alpha
        backgroundPaint.alpha = alpha
        invalidateSelf()
    }

    override fun getOpacity(): Int {
        return PixelFormat.TRANSLUCENT
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        shadowPaint.colorFilter = colorFilter
        backgroundPaint.colorFilter = colorFilter
        invalidateSelf()
    }
}