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
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import ru.tinkoff.acquiring.sdk.exceptions.AcquiringSdkException
import ru.tinkoff.acquiring.sdk.models.*
import ru.tinkoff.acquiring.sdk.models.options.screen.PaymentOptions
import ru.tinkoff.acquiring.sdk.models.result.AsdkResult
import ru.tinkoff.acquiring.sdk.redesign.dialog.*
import ru.tinkoff.acquiring.sdk.threeds.ThreeDsHelper
import ru.tinkoff.acquiring.sdk.ui.fragments.YandexPaymentStubFragment
import ru.tinkoff.acquiring.sdk.viewmodel.PaymentViewModel

/**
 * @author Ivan Golovachev
 */
internal class YandexPaymentActivity : TransparentActivity() {

    private lateinit var paymentViewModel: PaymentViewModel
    private lateinit var paymentOptions: PaymentOptions
    private var asdkState: AsdkState = DefaultState
    private var paymentLCEDialogFragment: PaymentLCEDialogFragment = PaymentLCEDialogFragment()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        paymentOptions = options as PaymentOptions
        asdkState = paymentOptions.asdkState

        initViews()
        bottomContainer.isVisible = false

        paymentViewModel = provideViewModel(PaymentViewModel::class.java) as PaymentViewModel
        observeLiveData()

        showFragment(YandexPaymentStubFragment())

        (asdkState as? YandexPayState)?.let {
            paymentViewModel.startYandexPayPayment(paymentOptions, it.yandexToken, it.needTokenSign)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        dismissDialog()
    }

    override fun handleLoadState(loadState: LoadState) {
        super.handleLoadState(loadState)
        when (loadState) {
            is LoadingState -> {
                showDialog(paymentLCEDialogFragment)
            }
        }
    }

    private fun observeLiveData() {
        with(paymentViewModel) {
            loadStateLiveData.observe(this@YandexPaymentActivity, Observer { handleLoadState(it) })
            screenStateLiveData.observe(this@YandexPaymentActivity, Observer { handleScreenState(it) })
            screenChangeEventLiveData.observe(this@YandexPaymentActivity, Observer { handleScreenChangeEvent(it) })
            paymentResultLiveData.observe(this@YandexPaymentActivity, Observer { paymentLCEDialogFragment.success { finishWithSuccess(it) } })
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
                        paymentLCEDialogFragment.failure { finishWithError(e) }
                    }
                }
                else -> Unit
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == THREE_DS_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK && data != null) {
                paymentLCEDialogFragment.success {
                    onF()
                   // finishWithSuccess(data.getSerializableExtra(ThreeDsHelper.Launch.RESULT_DATA) as AsdkResult)
                }
            } else if (resultCode == ThreeDsHelper.Launch.RESULT_ERROR) {
                paymentLCEDialogFragment.failure {
                    onSucccess()
                 //   finishWithError(data?.getSerializableExtra(ThreeDsHelper.Launch.ERROR_DATA) as Throwable)
                }
            } else {
                setResult(Activity.RESULT_CANCELED)
                finish()
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun handleScreenState(screenState: ScreenState) {
        when (screenState) {
            is FinishWithErrorScreenState -> paymentLCEDialogFragment.failure {
                finishWithError(screenState.error)
            }
            is ErrorScreenState -> paymentLCEDialogFragment.failure {
                finishWithError(IllegalStateException(screenState.message))
            }
            else -> Unit
        }
    }

    private fun onSucccess() {
        paymentLCEDialogFragment.failure {
            onF()
        }
    }

    private fun onF() {
        paymentLCEDialogFragment.success {
            onSucccess()
        }
    }
}