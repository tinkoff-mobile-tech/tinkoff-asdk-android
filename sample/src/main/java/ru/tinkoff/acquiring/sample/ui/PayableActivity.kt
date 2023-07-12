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
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.fragment.app.commit
import kotlinx.coroutines.Dispatchers
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import ru.tinkoff.acquiring.sample.R
import ru.tinkoff.acquiring.sample.SampleApplication
import ru.tinkoff.acquiring.sample.ui.MainActivity.Companion.toast
import ru.tinkoff.acquiring.sample.utils.CombInitDelegate
import ru.tinkoff.acquiring.sample.utils.SettingsSdkManager
import ru.tinkoff.acquiring.sample.utils.TerminalsManager
import ru.tinkoff.acquiring.sdk.AcquiringSdk.Companion.log
import ru.tinkoff.acquiring.sdk.exceptions.AcquiringSdkTimeoutException
import ru.tinkoff.acquiring.sdk.localization.AsdkSource
import ru.tinkoff.acquiring.sdk.localization.Language
import ru.tinkoff.acquiring.sdk.models.Card
import ru.tinkoff.acquiring.sdk.models.options.screen.PaymentOptions
import ru.tinkoff.acquiring.sdk.models.options.screen.SavedCardsOptions
import ru.tinkoff.acquiring.sdk.payment.*
import ru.tinkoff.acquiring.sdk.redesign.cards.list.ChooseCardLauncher
import ru.tinkoff.acquiring.sdk.redesign.common.LauncherConstants.EXTRA_ERROR
import ru.tinkoff.acquiring.sdk.redesign.common.LauncherConstants.EXTRA_PAYMENT_ID
import ru.tinkoff.acquiring.sdk.redesign.common.LauncherConstants.RESULT_ERROR
import ru.tinkoff.acquiring.sdk.threeds.ThreeDsHelper
import ru.tinkoff.acquiring.sdk.redesign.mainform.MainFormLauncher
import ru.tinkoff.acquiring.sdk.redesign.mirpay.MirPayLauncher
import ru.tinkoff.acquiring.sdk.redesign.payment.PaymentByCardLauncher
import ru.tinkoff.acquiring.sdk.redesign.recurrent.RecurrentPayLauncher
import ru.tinkoff.acquiring.sdk.redesign.sbp.SbpPayLauncher
import ru.tinkoff.acquiring.sdk.redesign.tpay.TpayLauncher
import ru.tinkoff.acquiring.sdk.redesign.tpay.models.enableMirPay
import ru.tinkoff.acquiring.sdk.redesign.tpay.models.enableTinkoffPay
import ru.tinkoff.acquiring.sdk.redesign.tpay.models.getTinkoffPayVersion
import ru.tinkoff.acquiring.sdk.utils.Money
import ru.tinkoff.acquiring.sdk.utils.getLongOrNull
import ru.tinkoff.acquiring.yandexpay.YandexButtonFragment
import ru.tinkoff.acquiring.yandexpay.addYandexResultListener
import ru.tinkoff.acquiring.yandexpay.createYandexPayButtonFragment
import ru.tinkoff.acquiring.yandexpay.models.YandexPayData
import ru.tinkoff.acquiring.yandexpay.models.enableYandexPay
import ru.tinkoff.acquiring.yandexpay.models.mapYandexPayData
import java.util.*
import kotlin.collections.ArrayList
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
    private var isProgressShowing = false
    private var isErrorShowing = false
    protected var tinkoffAcquiring = SampleApplication.tinkoffAcquiring
    private val orderId: String
        get() = abs(Random().nextInt()).toString()
    private var acqFragment: YandexButtonFragment? = null
    private val combInitDelegate: CombInitDelegate = CombInitDelegate(tinkoffAcquiring.sdk, Dispatchers.IO)
    private val byCardPayment = registerForActivityResult(PaymentByCardLauncher.Contract) { result ->
        when (result) {
            is PaymentByCardLauncher.Success -> {
                toast("byCardPayment Success : ${result.paymentId}")
            }
            is PaymentByCardLauncher.Error -> toast(result.error.message ?: getString(R.string.error_title))
            is PaymentByCardLauncher.Canceled -> toast("byCardPayment canceled")
        }
    }
    private val spbPayment = registerForActivityResult(SbpPayLauncher.Contract) { result ->
        when (result) {
            is SbpPayLauncher.Success -> {
                toast("SBP Success")
            }
            is SbpPayLauncher.Error -> toast(result.error.message ?: getString(R.string.error_title))
            is SbpPayLauncher.Canceled -> toast("SBP canceled")
            is SbpPayLauncher.NoBanks -> Unit
        }
    }
    private val byMainFormPayment = registerForActivityResult(MainFormLauncher.Contract) { result ->
        when (result) {
            is MainFormLauncher.Canceled -> toast("payment canceled")
            is MainFormLauncher.Error ->  toast(result.error.message ?: getString(R.string.error_title))
            is MainFormLauncher.Success ->  toast("payment Success-  paymentId:${result.paymentId}")
        }
    }
    private val recurrentPayment = registerForActivityResult(RecurrentPayLauncher.Contract) { result ->
        when (result) {
            is RecurrentPayLauncher.Canceled -> toast("payment canceled")
            is RecurrentPayLauncher.Error -> toast(result.error.message ?: getString(R.string.error_title))
            is RecurrentPayLauncher.Success -> toast("payment Success-  paymentId:${result.paymentId}")
        }
    }
    private val cardsForRecurrent =
        registerForActivityResult(ChooseCardLauncher.Contract) { result ->
            when (result) {
                is ChooseCardLauncher.Canceled -> Unit
                is ChooseCardLauncher.Error -> Unit
                is ChooseCardLauncher.Success -> launchRecurrent(result.card)
                is ChooseCardLauncher.NeedInputNewCard -> Unit
            }
        }

    private val tpayPayment = registerForActivityResult(TpayLauncher.Contract) { result ->
        when (result) {
            is TpayLauncher.Canceled -> toast("tpay canceled")
            is TpayLauncher.Error -> toast(result.error.message ?: getString(R.string.error_title))
            is TpayLauncher.Success -> toast("payment Success-  paymentId:${result.paymentId}")
        }
    }
    private val mirPayment = registerForActivityResult(MirPayLauncher.Contract) { result ->
        when (result) {
            is MirPayLauncher.Canceled -> toast("MirPay canceled")
            is MirPayLauncher.Error -> toast(result.error.message ?: getString(R.string.error_title))
            is MirPayLauncher.Success -> toast("payment Success-  paymentId:${result.paymentId}")
        }
    }

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
    }

    override fun onDestroy() {
        super.onDestroy()
        if (progressDialog.isShowing) {
            progressDialog.dismiss()
        }
        if (errorDialog != null && errorDialog!!.isShowing) {
            errorDialog!!.dismiss()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            PAYMENT_REQUEST_CODE, DYNAMIC_QR_PAYMENT_REQUEST_CODE -> handlePaymentResult(resultCode, data)
            YANDEX_PAY_REQUEST_CODE -> handleYandexPayResult(resultCode, data)
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
        acqFragment?.let {
            supportFragmentManager.putFragment(outState, YANDEX_PAY_FRAGMENT_KEY, it)
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
    }

    protected fun initPayment() {
        if (settings.isRecurrentPayment) {
            RecurrentPaymentProcess.init(SampleApplication.tinkoffAcquiring.sdk, application, ThreeDsHelper.CollectData)
            cardsForRecurrent.launch(createSavedCardOptions())
        } else {
            val options = createPaymentOptions().apply {
                this.setTerminalParams(
                    terminalKey = TerminalsManager.selectedTerminal.terminalKey,
                    publicKey = TerminalsManager.selectedTerminal.publicKey
                )
            }
            PaymentByCardProcess.init(SampleApplication.tinkoffAcquiring.sdk, application, ThreeDsHelper.CollectData)
            byMainFormPayment.launch(MainFormLauncher.StartData(options))
        }
    }

    protected fun openDynamicQrScreen() {
        tinkoffAcquiring.openDynamicQrScreen(this, createPaymentOptions(), DYNAMIC_QR_PAYMENT_REQUEST_CODE)
    }

    protected fun startSbpPayment() {
        lifecycleScope.launch {
            val opt = createPaymentOptions()
            opt.setTerminalParams(
                TerminalsManager.selectedTerminal.terminalKey,
                TerminalsManager.selectedTerminal.publicKey
            )
            if (settings.isEnableCombiInit) {
                showProgressDialog()
                runCatching { combInitDelegate.sendInit(opt).paymentId!! }
                    .onFailure {
                        hideProgressDialog()
                        showErrorDialog()
                    }
                    .onSuccess {
                        hideProgressDialog()
                        tinkoffAcquiring.initSbpPaymentSession()
                        spbPayment.launch(SbpPayLauncher.StartData(it, opt))
                    }
            } else {
                tinkoffAcquiring.initSbpPaymentSession()
                spbPayment.launch(SbpPayLauncher.StartData(opt))
            }
        }
    }

    protected fun setupTinkoffPay() {
        if (!settings.isTinkoffPayEnabled) return

        val tinkoffPayButton = findViewById<View>(R.id.tinkoff_pay_button)

        tinkoffAcquiring.checkTerminalInfo({ status ->
            if (status.enableTinkoffPay().not()) return@checkTerminalInfo

            tinkoffPayButton.visibility = View.VISIBLE

            val opt = createPaymentOptions()
            opt.setTerminalParams(
                TerminalsManager.selectedTerminal.terminalKey,
                TerminalsManager.selectedTerminal.publicKey
            )
            val version = checkNotNull(status?.getTinkoffPayVersion())
            tinkoffAcquiring.initTinkoffPayPaymentSession()
            tinkoffPayButton.setOnClickListener {
                tpayPayment.launch(TpayLauncher.StartData(opt, version))
            }
        })
    }

    protected fun setupMirPay() {
        val mirPayButton = findViewById<View>(R.id.mir_pay_button)

        tinkoffAcquiring.checkTerminalInfo({ status ->
            if (status.enableMirPay().not()) return@checkTerminalInfo

            mirPayButton.visibility = View.VISIBLE

            val opt = createPaymentOptions()
            opt.setTerminalParams(
                TerminalsManager.selectedTerminal.terminalKey,
                TerminalsManager.selectedTerminal.publicKey
            )
            tinkoffAcquiring.initMirPayPaymentSession()
            mirPayButton.setOnClickListener {
                mirPayment.launch(MirPayLauncher.StartData(opt))
            }
        })
    }

    protected fun setupYandexPay(theme: Int? = null, savedInstanceState: Bundle?) {
        if (!settings.yandexPayEnabled) return

        val yandexPayButtonContainer = findViewById<View>(R.id.btn_yandex_container)

        tinkoffAcquiring.checkTerminalInfo({ terminalInfo ->
            val yandexPayData = terminalInfo?.mapYandexPayData() ?: return@checkTerminalInfo

            yandexPayButtonContainer.isVisible = terminalInfo.enableYandexPay()
            val paymentOptions = createPaymentOptions().apply {
                val session = TerminalsManager.init(this@PayableActivity).selectedTerminal
                this.setTerminalParams(
                    terminalKey = session.terminalKey, publicKey = session.publicKey
                )
            }

            val yaFragment = createYandexButtonFragment(savedInstanceState, paymentOptions, yandexPayData, theme)

            if (supportFragmentManager.isDestroyed.not()) {
                supportFragmentManager.commit { replace(yandexPayButtonContainer.id, yaFragment) }
            }

            acqFragment = yaFragment

        }, {
            yandexPayButtonContainer.visibility = View.GONE
            showErrorDialog()
        })
    }

    protected fun setupRecurrentParentPayment() {
        val recurrentButton : TextView? = findViewById(R.id.recurrent_pay)
        recurrentButton?.isVisible = settings.isRecurrentPayment
        recurrentButton?.setOnClickListener {
            val paymentOptions = createPaymentOptions().apply {
                val session = TerminalsManager.selectedTerminal
                this.setTerminalParams(
                    terminalKey = session.terminalKey, publicKey = session.publicKey
                )
            }
            PaymentByCardProcess.init(SampleApplication.tinkoffAcquiring.sdk, application, ThreeDsHelper.CollectData)
            byCardPayment.launch(PaymentByCardLauncher.StartData(paymentOptions, ArrayList()))
        }
    }

    private fun createPaymentOptions(): PaymentOptions {
        val sessionParams = TerminalsManager.selectedTerminal

        return PaymentOptions()
            .setOptions {
                orderOptions {
                    orderId = this@PayableActivity.orderId
                    amount = totalPrice
                    title = this@PayableActivity.title
                    description = this@PayableActivity.description
                    recurrentPayment = settings.isRecurrentPayment
                    successURL = "https://www.google.com/search?q=success"
                    failURL = "https://www.google.com/search?q=fail"
                    additionalData = mutableMapOf(
                        "test_additional_data_key_1" to "test_additional_data_value_2",
                        "test_additional_data_key_2" to "test_additional_data_value_2"
                    )
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
                    validateExpiryDate = settings.validateExpiryDate
                    cameraCardScanner = settings.cameraScanner
                    cameraCardScannerContract = settings.cameraScannerContract
                    fpsEnabled = settings.isFpsEnabled
                    tinkoffPayEnabled = settings.isTinkoffPayEnabled
                    darkThemeMode = settings.resolveDarkThemeMode()
                    theme = settings.resolvePaymentStyle()
                    userCanSelectCard = true
                    duplicateEmailToReceipt = true
                }
            }
    }

    private fun createSavedCardOptions(): SavedCardsOptions {
        val settings = SettingsSdkManager(this)
        val params = TerminalsManager.selectedTerminal

        return SampleApplication.tinkoffAcquiring.savedCardsOptions {
            withArrowBack = true
            customerOptions {
                customerKey = params.customerKey
                checkType = settings.checkType
                email = params.customerEmail
            }
            featuresOptions {
                useSecureKeyboard = settings.isCustomKeyboardEnabled
                validateExpiryDate = settings.validateExpiryDate
                cameraCardScanner = settings.cameraScanner
                cameraCardScannerContract = settings.cameraScannerContract
                darkThemeMode = settings.resolveDarkThemeMode()
                theme = settings.resolveAttachCardStyle()
                userCanSelectCard = true
                selectedCardId = ""
                showOnlyRecurrentCards = true
            }
        }
    }

    protected fun handlePaymentResult(resultCode: Int, data: Intent?) {
        when (resultCode) {
            RESULT_OK -> onSuccessPayment()
            RESULT_CANCELED -> Toast.makeText(this, R.string.payment_cancelled, Toast.LENGTH_SHORT).show()
            RESULT_ERROR -> {
                commonErrorHandler(data)
            }
        }
    }

    protected fun handleYandexPayResult(resultCode: Int, data: Intent?) {
        when (resultCode) {
            RESULT_OK -> {
                acqFragment?.options = createPaymentOptions()
            }
            RESULT_CANCELED -> Toast.makeText(this, R.string.payment_cancelled, Toast.LENGTH_SHORT).show()
            RESULT_ERROR -> {
                commonErrorHandler(data)
            }
        }
    }

    protected fun showErrorDialog() {
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

    open fun createYandexButtonFragment(savedInstanceState: Bundle?,
                                           paymentOptions: PaymentOptions,
                                           yandexPayData: YandexPayData,
                                           theme: Int?) : YandexButtonFragment {
        return savedInstanceState?.let {
            try {
                (supportFragmentManager.getFragment(savedInstanceState, YANDEX_PAY_FRAGMENT_KEY) as? YandexButtonFragment)?.also {
                    tinkoffAcquiring.addYandexResultListener(
                        fragment = it,
                        activity = this,
                        yandexPayRequestCode = YANDEX_PAY_REQUEST_CODE,
                        onYandexErrorCallback = { showErrorDialog() },
                        onYandexCancelCallback = {
                            Toast.makeText(this, R.string.payment_cancelled, Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            } catch (i: IllegalStateException) {
                null
            }
        } ?: tinkoffAcquiring.createYandexPayButtonFragment(
            activity = this,
            yandexPayData = yandexPayData,
            options = paymentOptions,
            yandexPayRequestCode = YANDEX_PAY_REQUEST_CODE,
            themeId = theme,
            onYandexErrorCallback = { showErrorDialog() },
            onYandexCancelCallback = {
                Toast.makeText(this, R.string.payment_cancelled, Toast.LENGTH_SHORT).show()
            }
        )
    }

    protected fun showProgressDialog() {
        progressDialog.show()
        isProgressShowing = true
    }

    protected fun hideProgressDialog() {
        progressDialog.dismiss()
        isProgressShowing = false
    }

    private fun getErrorFromIntent(data: Intent?): Throwable? {
        return (data?.getSerializableExtra(EXTRA_ERROR) as? Throwable)
    }

    private fun Throwable.logIfTimeout() {
        (this as? AcquiringSdkTimeoutException)?.run {
            if (paymentId != null) {
                log("paymentId : $paymentId")
            }
            if (status != null) {
                log("status : $status")
            }
        }
    }

    private fun launchRecurrent(card: Card) {
        val options = createPaymentOptions().apply {
            this.setTerminalParams(
                terminalKey = TerminalsManager.selectedTerminal.terminalKey,
                publicKey = TerminalsManager.selectedTerminal.publicKey
            )
        }
        recurrentPayment.launch(
            RecurrentPayLauncher.StartData(card, options)
        )
    }

    private fun commonErrorHandler(data: Intent?) {
        val error = getErrorFromIntent(data)
        val paymentIdFromIntent = data?.getLongOrNull(EXTRA_PAYMENT_ID)
        val message = configureToastMessage(error, paymentIdFromIntent)
        log("toast message: $message")
        error?.printStackTrace()
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }


    private fun configureToastMessage(error: Throwable?, paymentId: Long?): String {
        val acqSdkTimeout  = error as? AcquiringSdkTimeoutException
        val payment = paymentId ?: acqSdkTimeout?.paymentId
        val status = acqSdkTimeout?.status
        return buildString {
            append(getString(R.string.payment_failed))
            append(" ")
            payment?.let { append("paymentId: $it") }
            append(" ")
            status?.let { append("status: $it") }
        }
    }


    companion object {

        const val PAYMENT_REQUEST_CODE = 1
        const val DYNAMIC_QR_PAYMENT_REQUEST_CODE = 2
        const val YANDEX_PAY_REQUEST_CODE = 6

        private const val STATE_PAYMENT_AMOUNT = "payment_amount"
        private const val STATE_LOADING_SHOW = "loading_show"
        private const val STATE_ERROR_SHOW = "error_show"

        const val YANDEX_PAY_FRAGMENT_KEY = "yandex_fragment_key"
    }
}
