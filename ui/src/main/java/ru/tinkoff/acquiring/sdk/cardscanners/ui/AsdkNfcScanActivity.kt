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
import android.app.AlertDialog
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
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import ru.tinkoff.acquiring.sdk.R
import ru.tinkoff.acquiring.sdk.cardscanners.models.AsdkScannedCardData
import ru.tinkoff.acquiring.sdk.localization.AsdkLocalization
import ru.tinkoff.core.components.nfc.NfcHelper
import ru.tinkoff.core.components.nfc.NfcUtils

/**
 * @author Mariya Chernyadieva
 */
internal class AsdkNfcScanActivity : AppCompatActivity() {

    private lateinit var nfcHelper: NfcHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.acq_activity_nfc)

        nfcHelper = NfcHelper.createAndRegisterObserver(this, object : NfcHelper.Callback {
            override fun onResult(bundle: Bundle) =
                onResult(bundle.getString(NfcHelper.CARD_NUMBER)!!, bundle.getString(NfcHelper.EXPIRY_DATE)!!)

            override fun onException(p0: java.lang.Exception?) = onException()

            override fun onNfcNotSupported() = onException()

            override fun onNfcDisabled() = showDialog()
        })

        setupTranslucentStatusBar()

        val nfsDescription = findViewById<TextView>(R.id.acq_nfc_tv_description)
        nfsDescription.setText(R.string.acq_scan_by_nfc_description)

        val closeBtn = findViewById<Button>(R.id.acq_nfc_btn_close)
        closeBtn.setText(R.string.acq_scan_by_nfc_close)
        closeBtn.setOnClickListener { finish() }

        applyBackgroundColor()
    }

    private fun onResult(cardNumber: String, expireDate: String) {
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

    private fun showDialog() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.acq_nfc_is_disable))
            .setMessage(getString(R.string.acq_nfc_is_disable))
            .setPositiveButton(android.R.string.ok) { _, _ ->
                NfcUtils.openNfcSettingsForResult(this, REQUEST_CODE_SETTINGS)
            }.setNegativeButton(android.R.string.cancel) { _, _ -> finish() }
            .show()
    }

    companion object {

        const val EXTRA_CARD = "card_extra"
        const val RESULT_ERROR = 256

        const val REQUEST_CODE_SETTINGS = 0

        private const val ALPHA_MASK = -0x33000001
    }
}
