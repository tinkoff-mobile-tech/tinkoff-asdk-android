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
import android.os.Bundle
import android.view.View
import androidx.lifecycle.Observer
import ru.tinkoff.acquiring.sdk.R
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
import ru.tinkoff.acquiring.sdk.models.OpenTinkoffPayBankScreenState
import ru.tinkoff.acquiring.sdk.models.OpenTinkoffPayBankState
import ru.tinkoff.acquiring.sdk.models.PaymentScreenState
import ru.tinkoff.acquiring.sdk.models.RejectedCardScreenState
import ru.tinkoff.acquiring.sdk.models.RejectedState
import ru.tinkoff.acquiring.sdk.models.Screen
import ru.tinkoff.acquiring.sdk.models.ScreenState
import ru.tinkoff.acquiring.sdk.models.SingleEvent
import ru.tinkoff.acquiring.sdk.models.ThreeDsDataCollectScreenState
import ru.tinkoff.acquiring.sdk.models.ThreeDsScreenState
import ru.tinkoff.acquiring.sdk.models.options.screen.PaymentOptions
import ru.tinkoff.acquiring.sdk.threeds.ThreeDsHelper
import ru.tinkoff.acquiring.sdk.ui.customview.NotificationDialog
import ru.tinkoff.acquiring.sdk.ui.fragments.PaymentFragment
import ru.tinkoff.acquiring.sdk.viewmodel.PaymentViewModel
import java.lang.IllegalStateException

/**
 * @author Mariya Chernyadieva
 */
internal class PaymentActivity : TransparentActivity() {

    private lateinit var paymentViewModel: PaymentViewModel
    private lateinit var paymentOptions: PaymentOptions
    private var asdkState: AsdkState = DefaultState

    private var shouldRequestTinkoffPayState = true

    private val progressDialog: NotificationDialog by lazy {
        NotificationDialog(this).apply { showProgress() }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        paymentOptions = options as PaymentOptions
        asdkState = paymentOptions.asdkState

        initViews()
        if (asdkState is BrowseFpsBankState || asdkState is FpsState || asdkState is OpenTinkoffPayBankState) {
            bottomContainer.visibility = View.GONE
        }

        paymentViewModel = provideViewModel(PaymentViewModel::class.java) as PaymentViewModel
        observeLiveData()

        if (savedInstanceState == null) {
            if (asdkState is OpenTinkoffPayBankState) {
                // if the activity was launched to open Tinkoff Pay deeplink it will be opened
                // before onResume, hence we shouldn't request Tinkoff Pay payment state in
                // subsequent onResume
                shouldRequestTinkoffPayState = false
            }
            paymentViewModel.checkoutAsdkState(asdkState)
        }
    }

    override fun onResume() {
        super.onResume()
        handleFpsResume()
        handleTinkoffPayResume()
    }

    private fun handleFpsResume() {
        val screenState = paymentViewModel.screenStateLiveData.value
        if (screenState is FpsBankFormShowedScreenState) {
            paymentViewModel.requestPaymentState(screenState.paymentId)
        }
    }

    private fun handleTinkoffPayResume() {
        if (!shouldRequestTinkoffPayState) {
            shouldRequestTinkoffPayState = true
            return
        }

        val screenState = paymentViewModel.screenChangeEventLiveData.value?.value
        if (screenState is OpenTinkoffPayBankScreenState) {
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
                if (data == null && (asdkState is FpsState || asdkState is BrowseFpsBankState)) {
                    finishWithCancel()
                } else {
                    data?.getStringExtra(EXTRA_SBP_BANK_PACKAGE_NAME)?.let { packageName ->
                        openSbpDeepLinkInBank(packageName)
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
                is ThreeDsScreenState -> paymentViewModel.launchOnMain {
                    try {
                        ThreeDsHelper.Launch(this@PaymentActivity,
                            THREE_DS_REQUEST_CODE, options, screen.data, screen.transaction)
                    } catch (e: Throwable) {
                        finishWithError(e)
                    }
                }
                is ThreeDsDataCollectScreenState -> {
                    paymentViewModel.collectedDeviceData = ThreeDsHelper.CollectData(this, screen.response)
                }
                is BrowseFpsBankScreenState -> openBankChooser(screen.deepLink, screen.banks)
                is FpsScreenState -> paymentViewModel.startFpsPayment(paymentOptions)
                is OpenTinkoffPayBankScreenState -> openTinkoffPayDeepLinkInBank(screen.deepLink)
                else -> Unit
            }
        }
    }

    private fun handleScreenState(screenState: ScreenState) {
        when (screenState) {
            is FinishWithErrorScreenState -> finishWithError(screenState.error)
            is ErrorScreenState -> {
                if (asdkState is FpsState || asdkState is BrowseFpsBankState || asdkState is OpenTinkoffPayBankState) {
                    finishWithError(AcquiringSdkException(NetworkException(screenState.message)))
                } else {
                    showError(screenState.message)
                }
            }
            is FpsBankFormShowedScreenState -> {
                if (asdkState is FpsState || asdkState is BrowseFpsBankState) {
                    finishWithCancel()
                }
            }
            else -> Unit
        }
    }

    @SuppressLint("QueryPermissionsNeeded")
    private fun openBankChooser(deepLink: String, banks: Set<Any?>?) {
        if (!banks.isNullOrEmpty()) {
            val supportedBanks = getBankApps(deepLink, banks)
            val intent = BankChooseActivity.createIntent(this, options, supportedBanks, deepLink)
            startActivityForResult(intent, SBP_BANK_CHOOSE_REQUEST_CODE)
        } else {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse(deepLink)
            val chooserIntent = Intent.createChooser(intent, getString(R.string.acq_fps_chooser_title))
            startActivityForResult(chooserIntent, SBP_BANK_REQUEST_CODE)
        }
    }

    @SuppressLint("QueryPermissionsNeeded")
    private fun getBankApps(link: String, banks: Set<Any?>): List<String> {
        // get sbp packages
        val sbpIntent = Intent(Intent.ACTION_VIEW)
        sbpIntent.setDataAndNormalize(Uri.parse(link))
        val sbpPackages = packageManager.queryIntentActivities(sbpIntent, 0)
                .map { it.activityInfo.packageName }

        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("http://"))
        val browserPackages = packageManager.queryIntentActivities(browserIntent, 0)
                .map { it.activityInfo.packageName }
        // filter out browsers
        val nonBrowserSbpPackages = sbpPackages.filter { it !in browserPackages }

        // get bank packages
        val bankPackages = packageManager.getInstalledApplications(0)
                .map { it.packageName }.filter { it in banks }

        // merge two lists
        return mutableListOf<String>().apply {
            addAll(nonBrowserSbpPackages)
            addAll(bankPackages)
        }.distinct()
    }

    private fun openSbpDeepLinkInBank(packageName: String) {
        val payload = (paymentViewModel.screenChangeEventLiveData.value?.value as BrowseFpsBankScreenState).deepLink
        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = Uri.parse(payload)
        intent.setPackage(packageName)
        startActivityForResult(intent, SBP_BANK_REQUEST_CODE)
    }

    private fun openTinkoffPayDeepLinkInBank(deepLink: String) {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = Uri.parse(deepLink)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
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