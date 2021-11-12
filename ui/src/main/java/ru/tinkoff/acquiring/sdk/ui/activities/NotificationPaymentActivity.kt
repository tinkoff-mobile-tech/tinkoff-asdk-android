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
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import ru.tinkoff.acquiring.sdk.AcquiringSdk
import ru.tinkoff.acquiring.sdk.TinkoffAcquiring
import ru.tinkoff.acquiring.sdk.exceptions.AcquiringApiException
import ru.tinkoff.acquiring.sdk.exceptions.AcquiringSdkException
import ru.tinkoff.acquiring.sdk.localization.AsdkLocalization
import ru.tinkoff.acquiring.sdk.models.AsdkState
import ru.tinkoff.acquiring.sdk.models.GooglePayParams
import ru.tinkoff.acquiring.sdk.models.LoadState
import ru.tinkoff.acquiring.sdk.models.LoadedState
import ru.tinkoff.acquiring.sdk.models.LoadingState
import ru.tinkoff.acquiring.sdk.models.SingleEvent
import ru.tinkoff.acquiring.sdk.models.options.screen.PaymentOptions
import ru.tinkoff.acquiring.sdk.models.result.PaymentResult
import ru.tinkoff.acquiring.sdk.network.AcquiringApi
import ru.tinkoff.acquiring.sdk.ui.customview.NotificationDialog
import ru.tinkoff.acquiring.sdk.ui.customview.ResultNotificationView
import ru.tinkoff.acquiring.sdk.utils.GooglePayHelper
import ru.tinkoff.acquiring.sdk.viewmodel.NotificationPaymentViewModel
import ru.tinkoff.acquiring.sdk.viewmodel.ViewModelProviderFactory

/**
 * @author Mariya Chernyadieva
 */
internal class NotificationPaymentActivity : AppCompatActivity() {

    private lateinit var paymentOptions: PaymentOptions
    private lateinit var viewModel: NotificationPaymentViewModel

    private var notificationDialog: NotificationDialog? = null
    private var progressDialog: NotificationDialog? = null
    private var resultIntent: PendingIntent? = null

    private var isDialogShowing = false
    private var dialogType: DialogType? = null
    private var dialogMessage = ""

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

        private const val STATE_DIALOG_SHOWING = "state_dialog_showing"
        private const val STATE_DIALOG_TYPE = "state_dialog_type"
        private const val STATE_DIALOG_MESSAGE = "state_dialog_message"

        @Throws(AcquiringSdkException::class)
        fun createPendingIntent(context: Context,
                                options: PaymentOptions,
                                requestCode: Int?,
                                paymentMethod: PaymentMethod,
                                notificationId: Int? = null,
                                googlePayParams: GooglePayParams? = null): PendingIntent {
            options.validateRequiredFields()

            val intent = Intent(context, NotificationPaymentActivity::class.java).apply {
                putExtra(EXTRA_GOOGLE_PARAMS, googlePayParams)
                putExtra(EXTRA_PAYMENT_OPTIONS, options)
                putExtra(EXTRA_NOTIFICATION_ID, notificationId)
                putExtra(EXTRA_PAYMENT_SYSTEM, paymentMethod)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            if (requestCode != null && context is Activity) {
                val flags = when {
                    Build.VERSION.SDK_INT < Build.VERSION_CODES.S -> PendingIntent.FLAG_UPDATE_CURRENT
                    else -> PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
                }
                val resultPendingIntent = context.createPendingResult(requestCode,
                        intent,
                        flags)
                intent.putExtra(EXTRA_PENDING_INTENT, resultPendingIntent)
            }

            val preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            var pendingIntentCode = preferences.getInt(PREF_INTENT_COUNTER_KEY, START_COUNTER_VALUE)

            val flags = when {
                Build.VERSION.SDK_INT < Build.VERSION_CODES.S -> PendingIntent.FLAG_UPDATE_CURRENT
                else -> PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            }
            val pendingIntent = PendingIntent.getActivity(context,
                    pendingIntentCode,
                    intent,
                    flags)

            pendingIntentCode = if (pendingIntentCode == Int.MAX_VALUE) START_COUNTER_VALUE else pendingIntentCode + 1
            preferences.edit().putInt(PREF_INTENT_COUNTER_KEY, pendingIntentCode).apply()

            return pendingIntent
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        savedInstanceState?.let {
            isDialogShowing = it.getBoolean(STATE_DIALOG_SHOWING)
            dialogType = it.getSerializable(STATE_DIALOG_TYPE) as DialogType?
            dialogMessage = it.getString(STATE_DIALOG_MESSAGE, "")
        }

        paymentOptions = intent.getParcelableExtra<PaymentOptions>(EXTRA_PAYMENT_OPTIONS) as PaymentOptions
        resultIntent = intent.getParcelableExtra(EXTRA_PENDING_INTENT) as PendingIntent?

        AsdkLocalization.init(this, paymentOptions.features.localizationSource)

        val sdk = AcquiringSdk(paymentOptions.terminalKey, paymentOptions.publicKey)
        viewModel = ViewModelProvider(this,
                ViewModelProviderFactory(paymentOptions.features.handleErrorsInSdk,
                        sdk))[NotificationPaymentViewModel::class.java]

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            sendBroadcast(Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS))
        }

        if (savedInstanceState == null) {
            initPaymentScreen()
        }

        initDialogs()
        observeLiveData()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.run {
            putBoolean(STATE_DIALOG_SHOWING, isDialogShowing)
            putSerializable(STATE_DIALOG_TYPE, dialogType)
            putString(STATE_DIALOG_MESSAGE, dialogMessage)
        }
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
        progressDialog?.dismiss()
        notificationDialog?.dismiss()
    }

    private fun observeLiveData() {
        with(viewModel) {
            loadStateLiveData.observe(this@NotificationPaymentActivity, Observer { handleLoadState(it) })
            paymentResultLiveData.observe(this@NotificationPaymentActivity, Observer { handleSuccessResult(it) })
            errorLiveData.observe(this@NotificationPaymentActivity, Observer { handleErrorResult(it) })
            uiEventLiveData.observe(this@NotificationPaymentActivity, Observer { handleUiEvent(it) })
        }
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

    private fun handleUiEvent(event: SingleEvent<AsdkState>) {
        event.getValueIfNotHandled()?.let {
            paymentOptions.apply { asdkState = it }
            showTinkoffPay()
        }
    }

    private fun handleLoadState(loadState: LoadState) {
        when (loadState) {
            is LoadingState -> progressDialog?.show()
            is LoadedState -> progressDialog?.dismiss()
        }
    }

    private fun handleGooglePayResult(data: Intent?) {
        if (data != null) {
            val token = GooglePayHelper.getGooglePayToken(data)
            if (token == null) {
                handleErrorResult(AcquiringSdkException(IllegalStateException("Invalid Google Pay result. Token is null")))
            } else {
                viewModel.initPayment(token, paymentOptions)
            }
        } else {
            handleErrorResult(AcquiringSdkException(IllegalStateException("Invalid Google Pay result")))
        }
    }

    private fun handleTinkoffPayResult(data: Intent?) {
        val paymentId = data?.getLongExtra(TinkoffAcquiring.EXTRA_PAYMENT_ID, 0)
        val cardId = data?.getStringExtra(TinkoffAcquiring.EXTRA_CARD_ID)
        val rebillId = data?.getStringExtra(TinkoffAcquiring.EXTRA_REBILL_ID)
        handleSuccessResult(PaymentResult(paymentId, cardId, rebillId))
    }

    private fun tryToRemoveNotification() {
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, DEFAULT_NOTIFICATION_ID)
        if (notificationId != DEFAULT_NOTIFICATION_ID) {
            NotificationManagerCompat.from(this).cancel(notificationId)
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

    private fun initDialogs() {
        progressDialog = NotificationDialog(this).apply {
            showProgress()
            setCancelable(false)
        }

        notificationDialog = NotificationDialog(this).apply {
            addListener(onDialogHide { finish() })
        }

        if (isDialogShowing) {
            when (dialogType) {
                DialogType.ERROR -> showErrorDialog(dialogMessage)
                DialogType.SUCCESS -> showSuccessDialog()
            }
        }
    }

    private fun showErrorDialog(message: String) {
        dialogType = DialogType.ERROR
        dialogMessage = message

        notificationDialog?.apply {
            showError(message)
        }?.show()
    }

    private fun showSuccessDialog() {
        dialogType = DialogType.SUCCESS

        notificationDialog?.apply {
            showSuccess(AsdkLocalization.resources.notificationMessageSuccess ?: "")
        }?.show()
    }

    private fun onDialogHide(hide: () -> Unit): ResultNotificationView.ResultNotificationViewListener {
        return object : ResultNotificationView.ResultNotificationViewListener {
            override fun onAction() {
                isDialogShowing = true
            }

            override fun onHide() {
                isDialogShowing = false
                hide()
            }
        }
    }

    private fun handleErrorResult(error: Throwable?) {
        if (resultIntent == null) {
            showErrorDialog(getErrorMessage(error))
        } else {
            val intent = Intent()
            intent.putExtra(TinkoffAcquiring.EXTRA_ERROR, error)
            try {
                resultIntent!!.send(this, TinkoffAcquiring.RESULT_ERROR, intent)
            } catch (e: PendingIntent.CanceledException) {
                showErrorDialog(getErrorMessage(error))
            } finally {
                finish()
            }
        }
    }

    private fun handleSuccessResult(paymentResult: PaymentResult) {
        tryToRemoveNotification()

        if (resultIntent == null) {
            showSuccessDialog()
        } else {
            val intent = Intent()
            intent.putExtra(TinkoffAcquiring.EXTRA_PAYMENT_ID, paymentResult.paymentId)
            intent.putExtra(TinkoffAcquiring.EXTRA_CARD_ID, paymentResult.cardId)
            intent.putExtra(TinkoffAcquiring.EXTRA_REBILL_ID, paymentResult.rebillId)
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

    private enum class DialogType {
        ERROR, SUCCESS
    }

    enum class PaymentMethod {
        TINKOFF, GPAY
    }
}