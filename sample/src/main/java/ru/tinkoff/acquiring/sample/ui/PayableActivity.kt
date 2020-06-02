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

package ru.tinkoff.acquiring.sample.ui

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import ru.tinkoff.acquiring.sample.R
import ru.tinkoff.acquiring.sample.SampleApplication
import ru.tinkoff.acquiring.sample.utils.SessionParams
import ru.tinkoff.acquiring.sample.utils.SettingsSdkManager
import ru.tinkoff.acquiring.sdk.TinkoffAcquiring.Companion.RESULT_ERROR
import ru.tinkoff.acquiring.sdk.localization.AsdkSource
import ru.tinkoff.acquiring.sdk.localization.Language
import ru.tinkoff.acquiring.sdk.models.AsdkState
import ru.tinkoff.acquiring.sdk.models.GooglePayParams
import ru.tinkoff.acquiring.sdk.models.options.screen.PaymentOptions
import ru.tinkoff.acquiring.sdk.payment.PaymentListener
import ru.tinkoff.acquiring.sdk.payment.PaymentListenerAdapter
import ru.tinkoff.acquiring.sdk.utils.GooglePayHelper
import ru.tinkoff.acquiring.sdk.utils.Money
import java.util.*
import kotlin.math.abs

/**
 * @author Mariya Chernyadieva
 */
@SuppressLint("Registered")
open class PayableActivity : AppCompatActivity() {

    protected var totalPrice: Money = Money()
    protected var title: String = ""
    protected var description: String = ""
    protected lateinit var settings: SettingsSdkManager

    private lateinit var progressDialog: AlertDialog
    private var errorDialog: AlertDialog? = null
    private val paymentListener = createPaymentListener()
    private var isProgressShowing = false
    private var isErrorShowing = false
    private var tinkoffAcquiring = SampleApplication.tinkoffAcquiring
    private val orderId: String
        get() = abs(Random().nextInt()).toString()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        savedInstanceState?.let {
            totalPrice = it.getSerializable(STATE_PAYMENT_AMOUNT) as Money
            isProgressShowing = it.getBoolean(STATE_LOADING_SHOW)
            isErrorShowing = it.getBoolean(STATE_ERROR_SHOW)
        }

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        settings = SettingsSdkManager(this)

        initDialogs()

        SampleApplication.paymentProcess?.subscribe(paymentListener)
    }

    override fun onDestroy() {
        super.onDestroy()
        SampleApplication.paymentProcess?.unsubscribe()
        if (progressDialog.isShowing) {
            progressDialog.dismiss()
        }
        if (errorDialog != null && errorDialog!!.isShowing) {
            errorDialog!!.dismiss()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            PAYMENT_REQUEST_CODE -> handlePaymentResult(resultCode)
            GOOGLE_PAY_REQUEST_CODE -> handleGooglePayResult(resultCode, data)
            else -> super.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.run {
            putSerializable(STATE_PAYMENT_AMOUNT, totalPrice)
            putBoolean(STATE_LOADING_SHOW, isProgressShowing)
            putBoolean(STATE_ERROR_SHOW, isErrorShowing)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    protected open fun onSuccessPayment() {
        PaymentResultActivity.start(this, totalPrice)
        SampleApplication.paymentProcess = null
    }

    protected fun initPayment() {
        tinkoffAcquiring.openPaymentScreen(this, createPaymentOptions(), PAYMENT_REQUEST_CODE)
    }

    protected fun setupGooglePay() {
        val googlePayButton = findViewById<View>(R.id.btn_google_pay)

        val googleParams = GooglePayParams(settings.terminalKey,
                environment = SessionParams.GPAY_TEST_ENVIRONMENT
        )

        val googlePayHelper = GooglePayHelper(googleParams)

        googlePayHelper.initGooglePay(this) { ready ->
            if (ready) {
                googlePayButton.visibility = View.VISIBLE
                googlePayButton.setOnClickListener {
                    googlePayHelper.openGooglePay(this@PayableActivity, totalPrice, GOOGLE_PAY_REQUEST_CODE)
                }
            } else {
                googlePayButton.visibility = View.GONE
            }
        }
    }

    private fun createPaymentOptions(): PaymentOptions {
        val terminalKey = settings.terminalKey
        val sessionParams = SessionParams[terminalKey]

        return PaymentOptions()
                .setOptions {
                    orderOptions {
                        orderId = this@PayableActivity.orderId
                        amount = totalPrice
                        title = this@PayableActivity.title
                        description = this@PayableActivity.description
                        recurrentPayment = settings.isRecurrentPayment
                    }
                    customerOptions {
                        customerKey = sessionParams.customerKey
                        checkType = settings.checkType
                        email = sessionParams.customerEmail
                    }
                    featuresOptions {
                        localizationSource = AsdkSource(Language.RU)
                        handleCardListErrorInSdk = settings.handleCardListErrorInSdk
                        useSecureKeyboard = settings.isCustomKeyboardEnabled
                        cameraCardScanner = settings.cameraScanner
                        fpsEnabled = settings.isFpsEnabled
                        darkThemeMode = settings.resolveDarkThemeMode()
                        theme = settings.resolvePaymentStyle()
                    }
                }
    }

    private fun createPaymentListener(): PaymentListener {
        return object : PaymentListenerAdapter() {

            override fun onSuccess(paymentId: Long, cardId: String?) {
                hideProgressDialog()
                onSuccessPayment()
            }

            override fun onUiNeeded(state: AsdkState) {
                hideProgressDialog()
                tinkoffAcquiring.openPaymentScreen(
                        this@PayableActivity,
                        createPaymentOptions(),
                        PAYMENT_REQUEST_CODE,
                        state)
            }

            override fun onError(throwable: Throwable) {
                hideProgressDialog()
                showErrorDialog()
                SampleApplication.paymentProcess = null
            }
        }
    }

    private fun handlePaymentResult(resultCode: Int) {
        when (resultCode) {
            RESULT_OK -> onSuccessPayment()
            RESULT_CANCELED -> Toast.makeText(this, R.string.payment_cancelled, Toast.LENGTH_SHORT).show()
            RESULT_ERROR -> Toast.makeText(this, R.string.payment_failed, Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleGooglePayResult(resultCode: Int, data: Intent?) {
        if (data != null && resultCode == Activity.RESULT_OK) {
            val token = GooglePayHelper.getGooglePayToken(data)
            if (token == null) {
                showErrorDialog()
            } else {
                showProgressDialog()
                SampleApplication.paymentProcess = tinkoffAcquiring
                        .initPayment(token, createPaymentOptions())
                        .subscribe(paymentListener)
                        .start()
            }
        } else if (resultCode != Activity.RESULT_CANCELED) {
            showErrorDialog()
        }
    }

    private fun showErrorDialog() {
        errorDialog = AlertDialog.Builder(this).apply {
            setTitle(R.string.error_title)
            setMessage(getString(R.string.error_message))
            setNeutralButton("OK") { dialog, _ ->
                dialog.dismiss()
                isErrorShowing = false
            }
        }.show()
        isErrorShowing = true
    }

    private fun initDialogs() {
        progressDialog = AlertDialog.Builder(this).apply {
            setCancelable(false)
            setView(layoutInflater.inflate(R.layout.loading_view, null))
        }.create()

        if (isProgressShowing) {
            showProgressDialog()
        }
        if (isErrorShowing) {
            showErrorDialog()
        }
    }

    private fun showProgressDialog() {
        progressDialog.show()
        isProgressShowing = true
    }

    private fun hideProgressDialog() {
        progressDialog.dismiss()
        isProgressShowing = false
    }

    companion object {

        const val PAYMENT_REQUEST_CODE = 1
        const val GOOGLE_PAY_REQUEST_CODE = 5

        private const val STATE_PAYMENT_AMOUNT = "payment_amount"
        private const val STATE_LOADING_SHOW = "loading_show"
        private const val STATE_ERROR_SHOW = "error_show"
    }
}
