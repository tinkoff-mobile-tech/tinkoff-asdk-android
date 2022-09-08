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
import com.emvco3ds.sdk.spec.CompletionEvent
import com.emvco3ds.sdk.spec.ProtocolErrorEvent
import com.emvco3ds.sdk.spec.RuntimeErrorEvent
import com.emvco3ds.sdk.spec.Transaction
import ru.tinkoff.acquiring.sdk.AcquiringSdk
import ru.tinkoff.acquiring.sdk.exceptions.AcquiringSdkException
import ru.tinkoff.acquiring.sdk.models.LoadedState
import ru.tinkoff.acquiring.sdk.models.LoadingState
import ru.tinkoff.acquiring.sdk.models.ThreeDsData
import ru.tinkoff.acquiring.sdk.models.enums.ResponseStatus
import ru.tinkoff.acquiring.sdk.models.result.AsdkResult
import ru.tinkoff.acquiring.sdk.models.result.CardResult
import ru.tinkoff.acquiring.sdk.models.result.PaymentResult
import ru.tinkoff.acquiring.sdk.threeds.ThreeDsHelper
import ru.tinkoff.acquiring.sdk.threeds.ThreeDsHelper.cleanupSafe
import ru.tinkoff.acquiring.sdk.threeds.ThreeDsStatusCanceled
import ru.tinkoff.acquiring.sdk.threeds.ThreeDsStatusError
import ru.tinkoff.acquiring.sdk.threeds.ThreeDsStatusSuccess
import ru.tinkoff.acquiring.sdk.ui.activities.BaseAcquiringActivity
import ru.tinkoff.core.components.threedswrapper.ChallengeStatusReceiverAdapter
import ru.tinkoff.core.components.threedswrapper.ThreeDSWrapper
import ru.tinkoff.core.components.threedswrapper.ThreeDSWrapper.Companion.closeSafe

internal class ThreeDsViewModel(
    application: Application,
    handleErrorsInSdk: Boolean,
    sdk: AcquiringSdk
) : BaseAcquiringViewModel(application, handleErrorsInSdk, sdk) {

    private val asdkResult: MutableLiveData<AsdkResult> = MutableLiveData()
    val resultLiveData: LiveData<AsdkResult> = asdkResult

    fun submitAuthorization(threeDsData: ThreeDsData, transStatus: String) {
        changeScreenState(LoadingState)

        val request = sdk.submit3DSAuthorization(threeDsData.tdsServerTransId!!, transStatus)

        coroutine.call(request,
            onSuccess = {
                requestState(threeDsData)
            }, onFailure = {
                requestState(threeDsData)
            })
    }

    fun requestState(threeDsData: ThreeDsData) {
        if (threeDsData.isPayment) {
            requestPaymentState(threeDsData.paymentId)
        } else if (threeDsData.isAttaching) {
            requestAddCardState(threeDsData.requestKey)
        }
    }

    fun requestPaymentState(paymentId: Long?) {
        changeScreenState(LoadingState)

        val request = sdk.getState {
            this.paymentId = paymentId
        }

        coroutine.call(request,
            onSuccess = { response ->
                if (response.status == ResponseStatus.CONFIRMED || response.status == ResponseStatus.AUTHORIZED) {
                    asdkResult.value = PaymentResult(response.paymentId)
                } else {
                    val throwable = AcquiringSdkException(IllegalStateException("PaymentState = ${response.status}"))
                    handleException(throwable)
                }
                changeScreenState(LoadedState)
            })
    }

    fun requestAddCardState(requestKey: String?) {
        changeScreenState(LoadingState)

        val request = sdk.getAddCardState {
            this.requestKey = requestKey
        }

        coroutine.call(request,
            onSuccess = { response ->
                if (response.status == ResponseStatus.COMPLETED) {
                    asdkResult.value = CardResult(response.cardId)
                } else {
                    val throwable = AcquiringSdkException(IllegalStateException("AsdkState = ${response.status}"))
                    handleException(throwable)
                }
                changeScreenState(LoadedState)
            })
    }
}