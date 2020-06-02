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

package ru.tinkoff.acquiring.sdk.cardscanners.ui

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import ru.tinkoff.acquiring.sdk.R
import ru.tinkoff.acquiring.sdk.cardscanners.models.AsdkScannedCardData
import ru.tinkoff.acquiring.sdk.localization.AsdkLocalization
import ru.tinkoff.core.nfc.BaseNfcActivity
import ru.tinkoff.core.nfc.ImperfectAlgorithmException
import ru.tinkoff.core.nfc.MalformedDataException

/**
 * @author Mariya Chernyadieva
 */
internal class AsdkNfcScanActivity : BaseNfcActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.acq_activity_nfc)

        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        setupTranslucentStatusBar()

        val nfsDescription = findViewById<TextView>(R.id.acq_nfc_tv_description)
        nfsDescription.text = AsdkLocalization.resources.nfcDescription

        val closeBtn = findViewById<Button>(R.id.acq_nfc_btn_close)
        closeBtn.text = AsdkLocalization.resources.nfcCloseButton
        closeBtn.setOnClickListener { finish() }

        applyBackgroundColor()
    }

    override fun onResult(cardNumber: String, expireDate: String) {
        val card = AsdkScannedCardData(cardNumber, expireDate, "")
        val intent = Intent()
        intent.putExtra(EXTRA_CARD, card)
        setResult(Activity.RESULT_OK, intent)
        finish()
    }

    override fun onBackPressed() {
        setResult(Activity.RESULT_CANCELED)
        super.onBackPressed()
    }

    override fun onException(exception: Exception) {
        onException()
    }

    override fun onClarifiedException(ex: MalformedDataException) {
        onException()
    }

    override fun onClarifiedException(ex: ImperfectAlgorithmException) {
        onException()
    }

    override fun getNfcDisabledDialogMessage(): String {
        return AsdkLocalization.resources.nfcDialogDisableTitle ?: getString(R.string.acq_nfc_need_enable)
    }

    override fun getNfcDisabledDialogTitle(): String {
        return AsdkLocalization.resources.nfcDialogDisableMessage ?: getString(R.string.acq_nfc_is_disable)
    }

    private fun setupTranslucentStatusBar() {
        if (Build.VERSION.SDK_INT in 19..20) {
            window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        }
        if (Build.VERSION.SDK_INT >= 21) {
            window.statusBarColor = Color.TRANSPARENT
        }
    }

    private fun applyBackgroundColor() {
        val rootView = findViewById<LinearLayout>(R.id.acq_nfc_ll_root)
        val currentBackground = rootView.background as ColorDrawable
        val currentColor = currentBackground.color

        if (currentColor == ContextCompat.getColor(this, R.color.acq_colorNfcBackground)) {
            val newColor = ALPHA_MASK and currentColor
            rootView.setBackgroundColor(newColor)
        }
    }

    private fun onException() {
        setResult(RESULT_ERROR)
        finish()
    }

    companion object {

        const val EXTRA_CARD = "card_extra"
        const val RESULT_ERROR = 256

        private const val ALPHA_MASK = -0x33000001
    }
}
