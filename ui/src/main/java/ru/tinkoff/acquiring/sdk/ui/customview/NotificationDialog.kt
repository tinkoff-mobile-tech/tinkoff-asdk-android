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

import android.app.Dialog
import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Build
import android.view.MotionEvent
import android.view.Window
import android.view.WindowManager
import androidx.core.content.ContextCompat
import ru.tinkoff.acquiring.sdk.R
import ru.tinkoff.acquiring.sdk.ui.customview.ResultNotificationView.Companion.INVISIBLE
import ru.tinkoff.acquiring.sdk.ui.customview.ResultNotificationView.Companion.LOADING
import ru.tinkoff.acquiring.sdk.ui.customview.ResultNotificationView.Companion.SUCCESS

/**
 * @author Mariya Chernyadieva
 */
internal class NotificationDialog(
        context: Context
) : Dialog(context, R.style.AcquiringResultNotificationDialog),
        ResultNotificationView.ResultNotificationViewListener {

    private val resultNotificationView = ResultNotificationView(
            context
    ).apply {
        addListener(this@NotificationDialog)
    }

    init {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window?.apply {
            setBackgroundDrawableResource(android.R.color.transparent)
            clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
                statusBarColor = ContextCompat.getColor(context, android.R.color.transparent)
                navigationBarColor = ContextCompat.getColor(context, android.R.color.transparent)
            }
        }
        setCancelable(false)
        setContentView(resultNotificationView)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        return if (resultNotificationView.status != SUCCESS) {
            false
        } else {
            super.onTouchEvent(event)
        }
    }

    override fun cancel() {
        resultNotificationView.hide()
    }

    override fun onClick(status: Int, event: MotionEvent) {
        if (status == SUCCESS) cancel()
    }

    override fun onAction() {
        setCancelable(true)
    }

    override fun onHide() {
        super.cancel()
    }

    override fun onStop() {
        super.onStop()
        resultNotificationView.stopAllAnimation()
    }

    fun showProgress() {
        if (resultNotificationView.status == INVISIBLE) {
            resultNotificationView.showProgress()
        }
    }

    fun showAction(
            text: String? = null,
            icon: Drawable? = ContextCompat.getDrawable(context, R.drawable.acq_icon_done)
    ) {
        show { resultNotificationView.showAction(text, icon) }
    }

    fun showSuccess(text: String? = null) {
        show { resultNotificationView.showSuccess(text) }
    }

    fun showError(text: String? = null) {
        show { resultNotificationView.showError(text) }
    }

    fun showWarning(text: String? = null) {
        show { resultNotificationView.showWarning(text) }
    }

    private inline fun show(action: () -> Unit) {
        if (this.resultNotificationView.status == INVISIBLE || resultNotificationView.status == LOADING) action()
    }

    fun addListener(listener: ResultNotificationView.ResultNotificationViewListener) {
        resultNotificationView.addListener(listener)
    }

    fun removeListener(listener: ResultNotificationView.ResultNotificationViewListener) {
        resultNotificationView.removeListener(listener)
    }
}