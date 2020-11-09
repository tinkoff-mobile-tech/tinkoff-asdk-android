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

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import ru.tinkoff.acquiring.sdk.AcquiringSdk
import ru.tinkoff.acquiring.sdk.exceptions.AcquiringSdkException
import ru.tinkoff.acquiring.sdk.models.AsdkState
import ru.tinkoff.acquiring.sdk.models.BrowseSbpBankScreenState
import ru.tinkoff.acquiring.sdk.models.BrowseSbpBankState
import ru.tinkoff.acquiring.sdk.models.Card
import ru.tinkoff.acquiring.sdk.models.CollectDataState
import ru.tinkoff.acquiring.sdk.models.DefaultScreenState
import ru.tinkoff.acquiring.sdk.models.FinishWithErrorScreenState
import ru.tinkoff.acquiring.sdk.models.LoadedState
import ru.tinkoff.acquiring.sdk.models.LoadingState
import ru.tinkoff.acquiring.sdk.models.PaymentScreenState
import ru.tinkoff.acquiring.sdk.models.PaymentSource
import ru.tinkoff.acquiring.sdk.models.RejectedCardScreenState
import ru.tinkoff.acquiring.sdk.models.RejectedState
import ru.tinkoff.acquiring.sdk.models.ThreeDsDataCollectScreenState
import ru.tinkoff.acquiring.sdk.models.ThreeDsScreenState
import ru.tinkoff.acquiring.sdk.models.ThreeDsState
import ru.tinkoff.acquiring.sdk.models.enums.CardStatus
import ru.tinkoff.acquiring.sdk.models.enums.ResponseStatus
import ru.tinkoff.acquiring.sdk.models.options.screen.PaymentOptions
import ru.tinkoff.acquiring.sdk.models.result.PaymentResult
import ru.tinkoff.acquiring.sdk.payment.PaymentListener
import ru.tinkoff.acquiring.sdk.payment.PaymentListenerAdapter
import ru.tinkoff.acquiring.sdk.payment.PaymentProcess

/**
 * @author Mariya Chernyadieva
 */
internal class PaymentViewModel(sdk: AcquiringSdk) : BaseAcquiringViewModel(sdk) {

    private val paymentResult: MutableLiveData<PaymentResult> = MutableLiveData()
    private var cardsResult: MutableLiveData<List<Card>> = MutableLiveData()

    private val paymentListener: PaymentListener = createPaymentListener()
    private val paymentProcess: PaymentProcess = PaymentProcess(sdk)

    private var requestPaymentStateCount = 0

    val paymentResultLiveData: LiveData<PaymentResult> = paymentResult
    val cardsResultLiveData: LiveData<List<Card>> = cardsResult

    var collectedDeviceData: MutableMap<String, String> = mutableMapOf()

    override fun onCleared() {
        super.onCleared()
        paymentProcess.stop()
    }

    fun checkoutAsdkState(state: AsdkState) {
        when (state) {
            is ThreeDsState -> changeScreenState(ThreeDsScreenState(state.data))
            is RejectedState -> changeScreenState(RejectedCardScreenState(state.cardId, state.rejectedPaymentId))
            else -> changeScreenState(PaymentScreenState)
        }
    }

    fun getCardList(handleErrorInSdk: Boolean, customerKey: String?, recurrentPayment: Boolean) {
        changeScreenState(DefaultScreenState)

        if (customerKey == null) {
            cardsResult.value = listOf()
        } else {
            changeScreenState(LoadingState)

            val request = sdk.getCardList {
                this.customerKey = customerKey
            }

            coroutine.call(request,
                    onSuccess = {
                        val activeCards = it.cards.filter { card ->
                            if (recurrentPayment) {
                                card.status == CardStatus.ACTIVE && !card.rebillId.isNullOrEmpty()
                            } else {
                                card.status == CardStatus.ACTIVE
                            }
                        }
                        cardsResult.value = activeCards
                        changeScreenState(LoadedState)
                    },
                    onFailure = {
                        if (handleErrorInSdk) {
                            changeScreenState(LoadedState)
                            cardsResult.value = mutableListOf()
                        } else {
                            coroutine.runWithDelay(800) {
                                changeScreenState(FinishWithErrorScreenState(it))
                            }
                        }
                    })
        }
    }

    fun startPayment(paymentOptions: PaymentOptions, paymentSource: PaymentSource, email: String? = null) {
        changeScreenState(LoadingState)
        paymentProcess.createPaymentProcess(paymentSource, paymentOptions, email).subscribe(paymentListener).start()
    }

    fun startFpsPayment(paymentOptions: PaymentOptions) {
        changeScreenState(LoadingState)
        paymentProcess.createSbpPaymentProcess(paymentOptions).subscribe(createPaymentListener()).start()
    }

    fun finishPayment(paymentId: Long, paymentSource: PaymentSource, email: String? = null) {
        changeScreenState(LoadingState)
        paymentProcess.createFinishProcess(paymentId, paymentSource, email).subscribe(paymentListener).start()
    }

    fun requestPaymentState(paymentId: Long?) {
        val request = sdk.getState {
            this.paymentId = paymentId
        }

        coroutine.call(request,
                onSuccess = { response ->
                    requestPaymentStateCount++
                    when (response.status) {
                        ResponseStatus.CONFIRMED -> {
                            paymentResult.value = PaymentResult(response.paymentId)
                            requestPaymentStateCount = 0
                            changeScreenState(LoadedState)
                        }
                        ResponseStatus.FORM_SHOWED -> {
                            requestPaymentStateCount = 0
                            changeScreenState(LoadedState)
                        }
                        else -> {
                            if (requestPaymentStateCount == 1) {
                                changeScreenState(LoadingState)
                                coroutine.runWithDelay(1000) {
                                    requestPaymentState(paymentId)
                                }
                            } else {
                                changeScreenState(LoadedState)
                                val throwable = AcquiringSdkException(IllegalStateException("PaymentState = ${response.status}"))
                                handleException(throwable)
                            }
                        }
                    }
                },
                onFailure = {
                    requestPaymentStateCount = 0
                    handleException(it)
                })
    }

    private fun createPaymentListener(): PaymentListener {
        return object : PaymentListenerAdapter() {

            override fun onSuccess(paymentId: Long, cardId: String?) {
                changeScreenState(LoadedState)
                paymentResult.value = PaymentResult(paymentId, cardId)
            }

            override fun onUiNeeded(state: AsdkState) {
                when (state) {
                    is ThreeDsState -> {
                        changeScreenState(ThreeDsScreenState(state.data))
                        coroutine.runWithDelay(500) {
                            changeScreenState(LoadedState)
                        }
                    }
                    is RejectedState -> {
                        changeScreenState(LoadedState)
                        changeScreenState(RejectedCardScreenState(state.cardId, state.rejectedPaymentId))
                    }
                    is CollectDataState -> {
                        changeScreenState(ThreeDsDataCollectScreenState(state.response))
                        state.data.putAll(collectedDeviceData)
                    }
                    is BrowseSbpBankState -> {
                        changeScreenState(LoadedState)
                        changeScreenState(BrowseSbpBankScreenState(state.paymentId, state.deepLink))
                    }
                }
            }

            override fun onError(throwable: Throwable) {
                changeScreenState(LoadedState)
                handleException(throwable)
            }
        }
    }
}