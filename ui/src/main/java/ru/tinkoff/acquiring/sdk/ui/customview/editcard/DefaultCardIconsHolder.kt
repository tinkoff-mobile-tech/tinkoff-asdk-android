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
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import ru.tinkoff.acquiring.sdk.R
import ru.tinkoff.acquiring.sdk.ui.customview.editcard.CardPaymentSystem.MAESTRO
import ru.tinkoff.acquiring.sdk.ui.customview.editcard.CardPaymentSystem.MASTER_CARD
import ru.tinkoff.acquiring.sdk.ui.customview.editcard.CardPaymentSystem.MIR
import ru.tinkoff.acquiring.sdk.ui.customview.editcard.CardPaymentSystem.UNION_PAY
import ru.tinkoff.acquiring.sdk.ui.customview.editcard.CardPaymentSystem.VISA

/**
 * @author Mariya Chernyadieva
 */
internal class DefaultCardIconsHolder(private val context: Context) : EditCardSystemIconsHolder {

    override fun getCardSystemLogo(cardNumber: String): Bitmap? {
        val logoRes = when (CardPaymentSystem.resolve(cardNumber)) {
            MASTER_CARD -> R.drawable.acq_ic_master
            VISA -> R.drawable.acq_ic_visa_blue
            MIR -> R.drawable.acq_ic_mir
            MAESTRO -> R.drawable.acq_ic_maestro
            UNION_PAY -> R.drawable.acq_ic_union_pay
            else -> return null
        }

        return getBitmapFromVectorDrawable(logoRes)
    }

    private fun getBitmapFromVectorDrawable(drawableId: Int): Bitmap? {
        var drawable = ContextCompat.getDrawable(context, drawableId) ?: return null

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            drawable = DrawableCompat.wrap(drawable).mutate()
        }

        val bitmap = Bitmap.createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)

        return bitmap
    }
}