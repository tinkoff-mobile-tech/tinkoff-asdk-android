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

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.lifecycle.Observer
import ru.tinkoff.acquiring.sdk.exceptions.AcquiringSdkException
import ru.tinkoff.acquiring.sdk.exceptions.NetworkException
import ru.tinkoff.acquiring.sdk.models.AsdkState
import ru.tinkoff.acquiring.sdk.models.BrowseFpsBankScreenState
import ru.tinkoff.acquiring.sdk.models.BrowseFpsBankState
import ru.tinkoff.acquiring.sdk.models.DefaultState
import ru.tinkoff.acquiring.sdk.models.ErrorButtonClickedEvent
import ru.tinkoff.acquiring.sdk.models.ErrorScreenState
import ru.tinkoff.acquiring.sdk.models.FinishWithErrorScreenState
import ru.tinkoff.acquiring.sdk.models.FpsBankFormShowedScreenState
import ru.tinkoff.acquiring.sdk.models.FpsScreenState
import ru.tinkoff.acquiring.sdk.models.FpsState
import ru.tinkoff.acquiring.sdk.models.LoadState
import ru.tinkoff.acquiring.sdk.models.LoadedState
import ru.tinkoff.acquiring.sdk.models.LoadingState
import ru.tinkoff.acquiring.sdk.models.PaymentScreenState
import ru.tinkoff.acquiring.sdk.models.RejectedCardScreenState
import ru.tinkoff.acquiring.sdk.models.RejectedState
import ru.tinkoff.acquiring.sdk.models.Screen
import ru.tinkoff.acquiring.sdk.models.ScreenState
import ru.tinkoff.acquiring.sdk.models.SingleEvent
import ru.tinkoff.acquiring.sdk.models.ThreeDsDataCollectScreenState
import ru.tinkoff.acquiring.sdk.models.ThreeDsScreenState
import ru.tinkoff.acquiring.sdk.models.options.screen.PaymentOptions
import ru.tinkoff.acquiring.sdk.ui.customview.NotificationDialog
import ru.tinkoff.acquiring.sdk.ui.fragments.PaymentFragment
import ru.tinkoff.acquiring.sdk.viewmodel.PaymentViewModel

/**
 * @author Mariya Chernyadieva
 */
internal class PaymentActivity : TransparentActivity() {

    private lateinit var paymentViewModel: PaymentViewModel
    private lateinit var paymentOptions: PaymentOptions
    private var asdkState: AsdkState = DefaultState

    private val progressDialog: NotificationDialog by lazy {
        NotificationDialog(this).apply { showProgress() }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        paymentOptions = options as PaymentOptions
        asdkState = paymentOptions.asdkState

        initViews()
        if (asdkState is BrowseFpsBankState || asdkState is FpsState) {
            bottomContainer.visibility = View.GONE
        }

        paymentViewModel = provideViewModel(PaymentViewModel::class.java) as PaymentViewModel
        observeLiveData()

        if (savedInstanceState == null) {
            paymentViewModel.checkoutAsdkState(asdkState)
        }
    }

    override fun onResume() {
        super.onResume()
        val screenState = paymentViewModel.screenStateLiveData.value
        if (screenState is FpsBankFormShowedScreenState) {
            paymentViewModel.requestPaymentState(screenState.paymentId)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            SBP_BANK_REQUEST_CODE -> {
                val screenState = paymentViewModel.screenChangeEventLiveData.value?.value
                if (screenState is BrowseFpsBankScreenState) {
                    paymentViewModel.requestPaymentState(screenState.paymentId)
                }
            }
            SBP_BANK_CHOOSE_REQUEST_CODE -> {
                if (data == null && asdkState is FpsState) {
                    finishWithCancel()
                } else {
                    data?.getStringExtra(EXTRA_SBP_BANK_PACKAGE_NAME)?.let { packageName ->
                        openDeepLinkInBank(packageName)
                    }
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (progressDialog.isShowing) {
            progressDialog.dismiss()
        }
    }

    override fun handleLoadState(loadState: LoadState) {
        super.handleLoadState(loadState)
        if (asdkState is FpsState) {
            when (loadState) {
                is LoadingState -> progressDialog.show()
                is LoadedState -> progressDialog.dismiss()
            }
        }
    }

    private fun observeLiveData() {
        with(paymentViewModel) {
            loadStateLiveData.observe(this@PaymentActivity, Observer { handleLoadState(it) })
            screenStateLiveData.observe(this@PaymentActivity, Observer { handleScreenState(it) })
            screenChangeEventLiveData.observe(this@PaymentActivity, Observer { handleScreenChangeEvent(it) })
            paymentResultLiveData.observe(this@PaymentActivity, Observer { finishWithSuccess(it) })
        }
    }

    private fun handleScreenChangeEvent(screenChangeEvent: SingleEvent<Screen>) {
        screenChangeEvent.getValueIfNotHandled()?.let { screen ->
            when (screen) {
                is PaymentScreenState -> showFragment(PaymentFragment.newInstance(paymentOptions.customer.customerKey))
                is RejectedCardScreenState -> {
                    val state = RejectedState(screen.cardId, screen.rejectedPaymentId)
                    showFragment(PaymentFragment.newInstance(paymentOptions.customer.customerKey, state))
                }
                is ThreeDsScreenState -> openThreeDs(screen.data)
                is ThreeDsDataCollectScreenState -> {
                    paymentViewModel.collectedDeviceData = ThreeDsActivity.collectData(this, screen.response)
                }
                is BrowseFpsBankScreenState -> openBankChooser(screen.deepLink, screen.banks)
                is FpsScreenState -> paymentViewModel.startFpsPayment(paymentOptions)
                else -> Unit
            }
        }
    }

    private fun handleScreenState(screenState: ScreenState) {
        when (screenState) {
            is FinishWithErrorScreenState -> finishWithError(screenState.error)
            is ErrorScreenState -> {
                if (asdkState is FpsState) {
                    finishWithError(AcquiringSdkException(NetworkException(screenState.message)))
                } else {
                    showError(screenState.message)
                }
            }
            is FpsBankFormShowedScreenState -> if (asdkState is FpsState) finishWithCancel()
            else -> Unit
        }
    }

    @SuppressLint("QueryPermissionsNeeded")
    private fun openBankChooser(deepLink: String, banks: Set<Any?>?) {
        var intent = Intent(Intent.ACTION_VIEW)
        intent.data = Uri.parse(deepLink)

        if (!banks.isNullOrEmpty() && Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            val activities = packageManager.queryIntentActivities(intent, 0)
            val supportedBanks = activities.filter { banks.contains(it.activityInfo.packageName) }
                    .map { it.activityInfo.packageName }
            intent = BankChooseActivity.createIntent(this, options, supportedBanks, deepLink)
            startActivityForResult(intent, SBP_BANK_CHOOSE_REQUEST_CODE)
        } else {
            startActivityForResult(intent, SBP_BANK_REQUEST_CODE)
        }
    }

    private fun openDeepLinkInBank(packageName: String) {
        val payload = (paymentViewModel.screenChangeEventLiveData.value?.value as BrowseFpsBankScreenState).deepLink
        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = Uri.parse(payload)
        intent.setPackage(packageName)
        startActivityForResult(intent, SBP_BANK_REQUEST_CODE)
    }

    private fun showError(message: String) {
        showErrorScreen(message) {
            hideErrorScreen()
            paymentViewModel.createEvent(ErrorButtonClickedEvent)
        }
    }

    companion object {
        private const val SBP_BANK_REQUEST_CODE = 112
        private const val SBP_BANK_CHOOSE_REQUEST_CODE = 113

        internal const val EXTRA_SBP_BANK_PACKAGE_NAME = "sbp_bank_package_name"
    }
}