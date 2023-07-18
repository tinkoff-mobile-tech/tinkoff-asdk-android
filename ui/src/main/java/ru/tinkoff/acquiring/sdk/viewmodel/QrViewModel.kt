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

package ru.tinkoff.acquiring.sdk.viewmodel

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import ru.tinkoff.acquiring.sdk.AcquiringSdk
import ru.tinkoff.acquiring.sdk.models.DefaultScreenState
import ru.tinkoff.acquiring.sdk.models.LoadedState
import ru.tinkoff.acquiring.sdk.models.LoadingState
import ru.tinkoff.acquiring.sdk.models.SingleEvent
import ru.tinkoff.acquiring.sdk.models.enums.DataTypeQr
import ru.tinkoff.acquiring.sdk.models.enums.ResponseStatus
import ru.tinkoff.acquiring.sdk.models.options.screen.PaymentOptions
import ru.tinkoff.acquiring.sdk.payment.methods.InitConfigurator.configure

internal class QrViewModel(
    application: Application,
    handleErrorsInSdk: Boolean,
    sdk: AcquiringSdk
) : BaseAcquiringViewModel(application, handleErrorsInSdk, sdk) {

    private val qrLinkResult: MutableLiveData<SingleEvent<String?>> = MutableLiveData()
    private val qrImageResult: MutableLiveData<String> = MutableLiveData()
    private val paymentResult: MutableLiveData<Long> = MutableLiveData()

    val qrLinkResultLiveData: LiveData<SingleEvent<String?>> = qrLinkResult
    val qrImageResultLiveData: LiveData<String> = qrImageResult
    val paymentResultLiveData: LiveData<Long> = paymentResult

    fun getDynamicQr(paymentOptions: PaymentOptions) {
        changeScreenState(DefaultScreenState)
        changeScreenState(LoadingState)

        coroutine.call(sdk.init { configure(paymentOptions) },
                onSuccess = {
                    getQr(it.paymentId!!, DataTypeQr.IMAGE)
                })
    }

    fun getStaticQr() {
        changeScreenState(DefaultScreenState)
        changeScreenState(LoadingState)

        val request = sdk.getStaticQr {
            data = DataTypeQr.IMAGE
        }

        coroutine.call(request,
                onSuccess = { response ->
                    qrImageResult.value = response.data!!
                    changeScreenState(LoadedState)
                })
    }

    fun getStaticQrLink() {
        val request = sdk.getStaticQr {
            data = DataTypeQr.PAYLOAD
        }

        coroutine.call(request,
                onSuccess = { response ->
                    qrLinkResult.value = SingleEvent(response.data)
                })
    }

    fun getDynamicQrLink(paymentOptions: PaymentOptions) {
        coroutine.call(sdk.init { configure(paymentOptions) },
                onSuccess = {
                    getQr(it.paymentId!!, DataTypeQr.PAYLOAD)
                })
    }

    private fun getQr(paymentId: Long, type: DataTypeQr) {
        val request = sdk.getQr {
            this.paymentId = paymentId
            dataType = type
        }

        coroutine.call(request,
                onSuccess = {
                    when (type) {
                        DataTypeQr.IMAGE -> {
                            qrImageResult.value = it.data!!
                            coroutine.runWithDelay(15000) {
                                getState(paymentId)
                            }
                            changeScreenState(LoadedState)
                        }
                        DataTypeQr.PAYLOAD -> qrLinkResult.value = SingleEvent(it.data)
                    }
                })
    }

    private fun getState(paymentId: Long) {
        val request = sdk.getState {
            this.paymentId = paymentId
        }

        coroutine.call(request,
                onSuccess = { response ->
                    if (response.status == ResponseStatus.CONFIRMED || response.status == ResponseStatus.AUTHORIZED) {
                        paymentResult.value = response.paymentId!!
                    } else {
                        coroutine.runWithDelay(5000) {
                            getState(paymentId)
                        }
                    }
                })
    }
}
