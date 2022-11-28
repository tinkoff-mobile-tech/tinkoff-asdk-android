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
import ru.tinkoff.acquiring.sdk.R
import ru.tinkoff.acquiring.sdk.exceptions.AcquiringApiException
import ru.tinkoff.acquiring.sdk.exceptions.AcquiringSdkException
import ru.tinkoff.acquiring.sdk.models.DefaultScreenState
import ru.tinkoff.acquiring.sdk.models.ErrorScreenState
import ru.tinkoff.acquiring.sdk.models.LoadedState
import ru.tinkoff.acquiring.sdk.models.LoadingState
import ru.tinkoff.acquiring.sdk.models.LoopConfirmationScreenState
import ru.tinkoff.acquiring.sdk.models.ThreeDsScreenState
import ru.tinkoff.acquiring.sdk.models.enums.ResponseStatus
import ru.tinkoff.acquiring.sdk.models.paysources.CardData
import ru.tinkoff.acquiring.sdk.models.result.CardResult
import ru.tinkoff.acquiring.sdk.network.AcquiringApi
import ru.tinkoff.acquiring.sdk.utils.ErrorResolver

/**
 * @author Mariya Chernyadieva
 */
internal class AttachCardViewModel(
    application: Application,
    handleErrorsInSdk: Boolean,
    sdk: AcquiringSdk
) : BaseAcquiringViewModel(application, handleErrorsInSdk, sdk) {

    private lateinit var cardData: CardData
    private val attachCardResult: MutableLiveData<CardResult> = MutableLiveData()
    private val needHandleErrorsInSdk = handleErrorsInSdk
    val attachCardResultLiveData: LiveData<CardResult> = attachCardResult

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
                    attachCardResult.value = CardResult(it.cardId)
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
                        ResponseStatus.THREE_DS_CHECKING -> changeScreenState(ThreeDsScreenState(it.getThreeDsData(), null))
                        ResponseStatus.LOOP_CHECKING -> changeScreenState(LoopConfirmationScreenState(it.requestKey!!))
                        null -> attachCardResult.value = CardResult(it.cardId)
                        else -> {
                            val throwable = AcquiringSdkException(IllegalStateException("ResponseStatus = ${it.status}"))
                            handleException(throwable)
                        }
                    }
                    changeScreenState(LoadedState)
                },
                onFailure = {
                    if (needHandleErrorsInSdk && it is AcquiringApiException) {
                        if (it.response != null && AcquiringApi.errorCodesAttachedCard.contains(it.response!!.errorCode)) {
                            changeScreenState(LoadedState)
                            changeScreenState(ErrorScreenState(ErrorResolver.resolve(it,
                                context.getString(R.string.acq_attach_card_error))))
                        } else handleException(it)
                    } else handleException(it)
                }
        )
    }
}