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
import ru.tinkoff.acquiring.sdk.exceptions.AcquiringApiException
import ru.tinkoff.acquiring.sdk.exceptions.AcquiringSdkException
import ru.tinkoff.acquiring.sdk.localization.AsdkLocalization
import ru.tinkoff.acquiring.sdk.models.DefaultScreenState
import ru.tinkoff.acquiring.sdk.models.ErrorScreenState
import ru.tinkoff.acquiring.sdk.models.LoadedState
import ru.tinkoff.acquiring.sdk.models.LoadingState
import ru.tinkoff.acquiring.sdk.models.LoopConfirmationScreenState
import ru.tinkoff.acquiring.sdk.models.ThreeDsScreenState
import ru.tinkoff.acquiring.sdk.models.enums.ResponseStatus
import ru.tinkoff.acquiring.sdk.models.paysources.CardData
import ru.tinkoff.acquiring.sdk.models.result.AttachCardResult
import ru.tinkoff.acquiring.sdk.network.AcquiringApi

/**
 * @author Mariya Chernyadieva
 */
internal class AttachCardViewModel(sdk: AcquiringSdk) : BaseAcquiringViewModel(sdk) {

    private lateinit var cardData: CardData
    private val attachCardResult: MutableLiveData<AttachCardResult> = MutableLiveData()
    val attachCardResultLiveData: LiveData<AttachCardResult> = attachCardResult

    fun showCardInput() {
        changeScreenState(DefaultScreenState)
    }

    fun startAttachCard(cardData: CardData, customerKey: String, checkType: String, data: Map<String, String>?) {
        this.cardData = cardData

        changeScreenState(LoadingState)

        val addCardRequest = sdk.addCard {
            this.customerKey = customerKey
            this.checkType = checkType
        }

        coroutine.call(addCardRequest,
                onSuccess = {
                    attachCard(it.requestKey!!, data)
                })
    }

    fun submitRandomAmount(requestKey: String, amount: Long) {
        val request = sdk.submitRandomAmount {
            this.requestKey = requestKey
            this.amount = amount
        }

        coroutine.call(request,
                onSuccess = {
                    attachCardResult.value = AttachCardResult(it.cardId)
                    changeScreenState(LoadedState)
                })
    }

    private fun attachCard(requestKey: String, data: Map<String, String>?) {
        val attachCardRequest = sdk.attachCard {
            this.requestKey = requestKey
            this.data = data
            this.cardData = this@AttachCardViewModel.cardData
        }

        coroutine.call(attachCardRequest,
                onSuccess = {
                    when (it.status) {
                        ResponseStatus.THREE_DS_CHECKING -> changeScreenState(ThreeDsScreenState(it.getThreeDsData()))
                        ResponseStatus.LOOP_CHECKING -> changeScreenState(LoopConfirmationScreenState(it.requestKey!!))
                        null -> attachCardResult.value = AttachCardResult(it.cardId)
                        else -> {
                            val throwable = AcquiringSdkException(IllegalStateException("ResponseStatus = ${it.status}"))
                            handleException(throwable)
                        }
                    }
                    changeScreenState(LoadedState)
                },
                onFailure = {
                    if (it is AcquiringApiException) {
                        if (it.response != null && AcquiringApi.errorCodesAttachedCard.contains(it.response!!.errorCode)) {
                            changeScreenState(LoadedState)
                            changeScreenState(ErrorScreenState(AsdkLocalization.resources.addCardErrorErrorAttached
                                    ?: AsdkLocalization.resources.payDialogErrorFallbackMessage!!))
                        } else handleException(it)
                    } else handleException(it)
                }
        )
    }
}