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

package ru.tinkoff.acquiring.sdk.ui.activities

import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationManagerCompat
import ru.tinkoff.acquiring.sdk.AcquiringSdk
import ru.tinkoff.acquiring.sdk.TinkoffAcquiring
import ru.tinkoff.acquiring.sdk.exceptions.AcquiringApiException
import ru.tinkoff.acquiring.sdk.exceptions.AcquiringSdkException
import ru.tinkoff.acquiring.sdk.localization.AsdkLocalization
import ru.tinkoff.acquiring.sdk.models.AsdkState
import ru.tinkoff.acquiring.sdk.models.GooglePayParams
import ru.tinkoff.acquiring.sdk.models.options.screen.PaymentOptions
import ru.tinkoff.acquiring.sdk.models.paysources.GooglePay
import ru.tinkoff.acquiring.sdk.network.AcquiringApi
import ru.tinkoff.acquiring.sdk.payment.PaymentListenerAdapter
import ru.tinkoff.acquiring.sdk.payment.PaymentProcess
import ru.tinkoff.acquiring.sdk.ui.customview.NotificationDialog
import ru.tinkoff.acquiring.sdk.ui.customview.ResultNotificationView
import ru.tinkoff.acquiring.sdk.utils.GooglePayHelper

/**
 * @author Mariya Chernyadieva
 */
internal class NotificationPaymentActivity : AppCompatActivity() {

    private var paymentProcess: PaymentProcess? = null
    private var progressDialog: NotificationDialog? = null
    private var resultIntent: PendingIntent? = null
    private lateinit var paymentOptions: PaymentOptions

    companion object {
        private const val GOOGLE_PAY_REQUEST_CODE = 789
        private const val TINKOFF_PAY_REQUEST_CODE = 987

        private const val DEFAULT_NOTIFICATION_ID = Int.MIN_VALUE

        private const val EXTRA_GOOGLE_PARAMS = "google_params"
        private const val EXTRA_PAYMENT_OPTIONS = "payment_options"
        private const val EXTRA_PENDING_INTENT = "extra_pending_intent"
        private const val EXTRA_NOTIFICATION_ID = "notification_id"
        private const val EXTRA_PAYMENT_SYSTEM = "payment_system"

        private const val PREF_NAME = "asdk_preferences"
        private const val PREF_INTENT_COUNTER_KEY = "intent_counter_key"
        private const val START_COUNTER_VALUE = 0

        fun createPendingIntent(context: Context,
                                options: PaymentOptions,
                                requestCode: Int?,
                                paymentMethod: PaymentMethod,
                                notificationId: Int? = null,
                                googlePayParams: GooglePayParams? = null): PendingIntent {
            val intent = Intent(context, NotificationPaymentActivity::class.java).apply {
                putExtra(EXTRA_GOOGLE_PARAMS, googlePayParams)
                putExtra(EXTRA_PAYMENT_OPTIONS, options)
                putExtra(EXTRA_NOTIFICATION_ID, notificationId)
                putExtra(EXTRA_PAYMENT_SYSTEM, paymentMethod)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            if (requestCode != null && context is Activity) {
                val resultPendingIntent = context.createPendingResult(requestCode,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT)
                intent.putExtra(EXTRA_PENDING_INTENT, resultPendingIntent)
            }

            val preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            var pendingIntentCode = preferences.getInt(PREF_INTENT_COUNTER_KEY, START_COUNTER_VALUE)

            val pendingIntent = PendingIntent.getActivity(context,
                    pendingIntentCode,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT)

            pendingIntentCode = if (pendingIntentCode == Int.MAX_VALUE) START_COUNTER_VALUE else pendingIntentCode + 1
            preferences.edit().putInt(PREF_INTENT_COUNTER_KEY, pendingIntentCode).apply()

            return pendingIntent
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        paymentOptions = intent.getParcelableExtra(EXTRA_PAYMENT_OPTIONS) as PaymentOptions
        resultIntent = intent.getParcelableExtra(EXTRA_PENDING_INTENT) as PendingIntent?

        AsdkLocalization.init(this, paymentOptions.features.localizationSource)

        sendBroadcast(Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS))
        initPaymentScreen()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (resultCode) {
            Activity.RESULT_OK -> when (requestCode) {
                GOOGLE_PAY_REQUEST_CODE -> handleGooglePayResult(data)
                TINKOFF_PAY_REQUEST_CODE -> handleTinkoffPayResult(data)
                else -> super.onActivityResult(requestCode, resultCode, data)
            }
            Activity.RESULT_CANCELED -> sendCanceledResult()
            TinkoffAcquiring.RESULT_ERROR -> {
                val error = data?.getSerializableExtra(TinkoffAcquiring.EXTRA_ERROR) as Throwable?
                handleErrorResult(error)
            }
            else -> super.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun onBackPressed() {
        //disable
    }

    override fun onDestroy() {
        super.onDestroy()
        paymentProcess?.unsubscribe()
        paymentProcess?.stop()
        progressDialog?.dismiss()
    }

    private fun initPaymentScreen() {
        val googlePayParams = intent.getSerializableExtra(EXTRA_GOOGLE_PARAMS) as GooglePayParams?

        when (intent.getSerializableExtra(EXTRA_PAYMENT_SYSTEM) as PaymentMethod) {
            PaymentMethod.GPAY -> {
                if (googlePayParams == null) {
                    handleErrorResult(AcquiringSdkException(IllegalStateException("Google Pay is not available. Params are null")))
                } else {
                    showGooglePay(googlePayParams)
                }
            }
            PaymentMethod.TINKOFF -> showTinkoffPay()
        }
    }

    private fun showGooglePay(googleParams: GooglePayParams) {
        val googlePayHelper = GooglePayHelper(googleParams)
        val price = paymentOptions.order.amount

        googlePayHelper.initGooglePay(this) { ready ->
            if (ready) {
                googlePayHelper.openGooglePay(this, price, GOOGLE_PAY_REQUEST_CODE)
            } else {
                handleErrorResult(AcquiringSdkException(IllegalStateException("Google Pay is not available")))
            }
        }
    }

    private fun showTinkoffPay() {
        val intent = BaseAcquiringActivity.createIntent(this, paymentOptions, PaymentActivity::class.java)
        startActivityForResult(intent, TINKOFF_PAY_REQUEST_CODE)
    }

    private fun handleGooglePayResult(data: Intent?) {
        if (data != null) {
            val token = GooglePayHelper.getGooglePayToken(data)
            if (token == null) {
                handleErrorResult(AcquiringSdkException(IllegalStateException("Invalid Google Pay result. Token is null")))
            } else {
                processPayment(token)
            }
        } else {
            handleErrorResult(AcquiringSdkException(IllegalStateException("Invalid Google Pay result")))
        }
    }

    private fun handleTinkoffPayResult(data: Intent?) {
        val paymentId = data?.getLongExtra(TinkoffAcquiring.EXTRA_PAYMENT_ID, 0)
        val cardId = data?.getStringExtra(TinkoffAcquiring.EXTRA_CARD_ID)
        handleSuccessResult(paymentId, cardId)
    }

    private fun processPayment(token: String) {
        progressDialog = NotificationDialog(this).apply {
            showProgress()
            setCancelable(false)
            show()
        }

        val sdk = AcquiringSdk(paymentOptions.terminalKey,
                paymentOptions.password,
                paymentOptions.publicKey)

        paymentProcess = PaymentProcess(sdk).createPaymentProcess(GooglePay(token), paymentOptions)
                .subscribe(object : PaymentListenerAdapter() {
                    override fun onSuccess(paymentId: Long, cardId: String?) {
                        progressDialog?.dismiss()
                        handleSuccessResult(paymentId, cardId)
                    }

                    override fun onUiNeeded(state: AsdkState) {
                        progressDialog?.dismiss()
                        paymentOptions.asdkState = state
                        showTinkoffPay()
                    }

                    override fun onError(throwable: Throwable) {
                        progressDialog?.dismiss()
                        handleErrorResult(throwable)
                    }
                }).start()
    }

    private fun tryToRemoveNotification() {
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, DEFAULT_NOTIFICATION_ID)
        if (notificationId != DEFAULT_NOTIFICATION_ID) {
            NotificationManagerCompat.from(this).cancel(notificationId)
        }
    }

    private fun onDialogHide(hide : () -> Unit): ResultNotificationView.ResultNotificationViewListener {
        return object : ResultNotificationView.ResultNotificationViewListener {
            override fun onHide() {
                hide()
            }
        }
    }

    private fun getErrorMessage(error: Throwable?): String {
        val fallbackMessage = AsdkLocalization.resources.notificationMessageError ?: ""
        return when (error) {
            is AcquiringApiException -> {
                if (error.response != null) {
                    val errorCode = error.response?.errorCode
                    if (errorCode != null && AcquiringApi.errorCodesForUserShowing.contains(errorCode)) {
                        error.response!!.message ?: fallbackMessage
                    } else fallbackMessage
                } else fallbackMessage
            }
            else -> fallbackMessage
        }
    }

    private fun showErrorDialog(error: Throwable?) {
        NotificationDialog(this).apply {
            showError(getErrorMessage(error))
            addListener(onDialogHide { finish() })
        }.show()
    }

    private fun showSuccessDialog() {
        NotificationDialog(this).apply {
            showSuccess(AsdkLocalization.resources.notificationMessageSuccess ?: "")
            addListener(onDialogHide { finish() })
        }.show()
    }

    private fun handleErrorResult(error: Throwable?) {
        if (resultIntent == null) {
            showErrorDialog(error)
        } else {
            val intent = Intent()
            intent.putExtra(TinkoffAcquiring.EXTRA_ERROR, error)
            try {
                resultIntent!!.send(this, TinkoffAcquiring.RESULT_ERROR, intent)
            } catch (e: PendingIntent.CanceledException) {
                showErrorDialog(error)
            } finally {
                finish()
            }
        }
    }

    private fun handleSuccessResult(paymentId: Long?, cardId: String?) {
        tryToRemoveNotification()

        if (resultIntent == null) {
            showSuccessDialog()
        } else {
            val intent = Intent()
            intent.putExtra(TinkoffAcquiring.EXTRA_PAYMENT_ID, paymentId)
            intent.putExtra(TinkoffAcquiring.EXTRA_CARD_ID, cardId)
            try {
                resultIntent!!.send(this, Activity.RESULT_OK, intent)
            } catch (e: PendingIntent.CanceledException) {
                showSuccessDialog()
            } finally {
                finish()
            }
        }
    }

    private fun sendCanceledResult() {
        try {
            resultIntent?.send(Activity.RESULT_CANCELED)
        } catch (e: PendingIntent.CanceledException) {
            //ignore
        } finally {
            finish()
        }
    }

    enum class PaymentMethod {
        TINKOFF, GPAY
    }
}