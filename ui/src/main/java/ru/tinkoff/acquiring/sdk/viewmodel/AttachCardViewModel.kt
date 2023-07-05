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
import ru.tinkoff.acquiring.sdk.models.result.CardResult
import ru.tinkoff.acquiring.sdk.network.AcquiringApi
import ru.tinkoff.acquiring.sdk.responses.AttachCardResponse
import ru.tinkoff.acquiring.sdk.responses.Check3dsVersionResponse
import ru.tinkoff.acquiring.sdk.threeds.ThreeDsHelper
import ru.tinkoff.acquiring.sdk.utils.panSuffix
import kotlin.properties.ReadWriteProperty

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
                    check3dsVersionIfNeed(cardData, it.paymentId, it.requestKey!!,data)
                })
    }

    fun submitRandomAmount(requestKey: String, amount: Long) {
        val request = sdk.submitRandomAmount {
            this.requestKey = requestKey
            this.amount = amount
        }

        coroutine.call(request,
                onSuccess = {
                    attachCardResult.value = CardResult(it.cardId, cardData.pan.panSuffix())
                    changeScreenState(LoadedState)
                })
    }

    fun check3dsVersionIfNeed(cardData: CardData,
                              paymentId: Long?,
                              requestKey: String,
                              data: Map<String, String>?
    ) {
        if (paymentId == null) {
            attachCard(requestKey, data)
        } else {
            val check3DsRequest = sdk.check3DsVersion {
                this.paymentId = paymentId
                this.paymentSource = cardData
            }
            coroutine.call(
                check3DsRequest,
                onSuccess = {
                    if (it.is3DsVersionV2()) {
                        val check3dsMap = ThreeDsHelper.CollectData.invoke(context, it)
                        ThreeDsHelper.CollectData.addExtraData(check3dsMap, it)
                        attachCard(requestKey, check3dsMap + (data ?: mapOf()) , it)
                    } else {
                        attachCard(requestKey, data, it)
                    }
                }
            )
        }
    }

    private fun attachCard(requestKey: String,
                           data: Map<String, String>?,
                           check3dsVersionResponse: Check3dsVersionResponse? = null) {
        val attachCardRequest = sdk.attachCard {
            this.requestKey = requestKey
            this.data = data
            this.cardData = this@AttachCardViewModel.cardData

            if (check3dsVersionResponse?.is3DsVersionV2() == true) {
                this.addContentHeader()
                this.addUserAgentHeader()
            }
        }
        coroutine.call(attachCardRequest,
                onSuccess = { handleAttachSuccess(it, check3dsVersionResponse) },
                onFailure = ::handleAttachError
        )
    }

    private fun handleAttachSuccess(it: AttachCardResponse,
                                    check3dsVersionResponse: Check3dsVersionResponse?){
            when (it.status) {
                ResponseStatus.THREE_DS_CHECKING -> {
                    val _3dsData = it.getThreeDsData()
                    if (check3dsVersionResponse?.is3DsVersionV2() == true) {
                        ThreeDsHelper.CollectData.addExtraThreeDsData(
                            data = _3dsData,
                            acsTransId = checkNotNull(it.acsTransId),
                            serverTransId = checkNotNull(check3dsVersionResponse.serverTransId),
                            version = checkNotNull(check3dsVersionResponse.version)
                        )
                    }
                    changeScreenState(
                        ThreeDsScreenState(
                            _3dsData,
                            null,
                            cardData.pan.panSuffix()
                        )
                    )
                }
                ResponseStatus.LOOP_CHECKING -> changeScreenState(LoopConfirmationScreenState(it.requestKey!!))
                null -> attachCardResult.value = CardResult(it.cardId, cardData.pan.panSuffix())
                else -> {
                    val throwable =
                        AcquiringSdkException(IllegalStateException("ResponseStatus = ${it.status}"))
                    handleException(throwable)
                }
            }
            changeScreenState(LoadedState)
        }

    private fun handleAttachError(it: Exception) {
        if (needHandleErrorsInSdk && it is AcquiringApiException) {
            if (it.response != null && AcquiringApi.errorCodesAttachedCard.contains(it.response!!.errorCode)) {
                changeScreenState(LoadedState)
                changeScreenState(ErrorScreenState(AsdkLocalization.resources.addCardErrorErrorAttached
                    ?: AsdkLocalization.resources.payDialogErrorFallbackMessage!!))
            } else handleException(it)
        } else handleException(it)
    }
}
