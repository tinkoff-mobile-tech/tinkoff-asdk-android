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

import android.os.Bundle
import androidx.lifecycle.Observer
import ru.tinkoff.acquiring.sdk.models.*
import ru.tinkoff.acquiring.sdk.models.ErrorButtonClickedEvent
import ru.tinkoff.acquiring.sdk.models.ErrorScreenState
import ru.tinkoff.acquiring.sdk.models.FinishWithErrorScreenState
import ru.tinkoff.acquiring.sdk.models.LoadState
import ru.tinkoff.acquiring.sdk.models.LoadedState
import ru.tinkoff.acquiring.sdk.models.LoadingState
import ru.tinkoff.acquiring.sdk.models.Screen
import ru.tinkoff.acquiring.sdk.models.ScreenState
import ru.tinkoff.acquiring.sdk.models.SingleEvent
import ru.tinkoff.acquiring.sdk.models.ThreeDsScreenState
import ru.tinkoff.acquiring.sdk.models.options.screen.PaymentOptions
import ru.tinkoff.acquiring.sdk.threeds.ThreeDsHelper
import ru.tinkoff.acquiring.sdk.ui.customview.NotificationDialog
import ru.tinkoff.acquiring.sdk.ui.fragments.YandexPaymentStubFragment
import ru.tinkoff.acquiring.sdk.viewmodel.PaymentViewModel

/**
 * @author Mariya Chernyadieva
 */
internal class YandexPaymentActivity : TransparentActivity() {

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

        paymentViewModel = provideViewModel(PaymentViewModel::class.java) as PaymentViewModel
        observeLiveData()

        showFragment(YandexPaymentStubFragment())
        (asdkState as? YandexPayState)?.let {
            paymentViewModel.startYandexPayPayment(paymentOptions, it.yandexToken, it.needTokenSign)
        }
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
            loadStateLiveData.observe(this@YandexPaymentActivity, Observer { handleLoadState(it) })
            screenStateLiveData.observe(this@YandexPaymentActivity, Observer { handleScreenState(it) })
            screenChangeEventLiveData.observe(this@YandexPaymentActivity, Observer { handleScreenChangeEvent(it) })
            paymentResultLiveData.observe(this@YandexPaymentActivity, Observer { finishWithSuccess(it) })
        }
    }

    private fun handleScreenChangeEvent(screenChangeEvent: SingleEvent<Screen>) {
        screenChangeEvent.getValueIfNotHandled()?.let { screen ->
            when (screen) {
                is ThreeDsScreenState -> paymentViewModel.coroutine.launchOnMain {
                    try {
                        ThreeDsHelper.Launch(this@YandexPaymentActivity,
                            THREE_DS_REQUEST_CODE, options, screen.data, screen.transaction)
                    } catch (e: Throwable) {
                        finishWithError(e)
                    }
                }
                else -> Unit
            }
        }
    }

    private fun handleScreenState(screenState: ScreenState) {
        when (screenState) {
            is FinishWithErrorScreenState -> finishWithError(screenState.error)
            is ErrorScreenState -> {
                showError(screenState.message)
            }
            else -> Unit
        }
    }

    private fun showError(message: String) {
        showErrorScreen(message) {
            hideErrorScreen()
            paymentViewModel.createEvent(ErrorButtonClickedEvent)
        }
    }
}